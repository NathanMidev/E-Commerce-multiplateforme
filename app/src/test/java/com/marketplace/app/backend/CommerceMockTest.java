package com.marketplace.app.backend;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.marketplace.app.backend.mock.MockBackendService;
import com.marketplace.app.backend.repository.CartRepository;
import com.marketplace.app.backend.repository.OrderRepository;
import com.marketplace.app.backend.repository.ProductRepository;
import com.marketplace.app.base.BaseRepository;
import com.marketplace.app.model.Cart;
import com.marketplace.app.model.FilterCriteria;
import com.marketplace.app.model.Order;
import com.marketplace.app.model.OrderStatus;
import com.marketplace.app.model.PaymentMethod;
import com.marketplace.app.model.Product;
import com.marketplace.app.model.ProductStatus;

import org.junit.Before;
import org.junit.Test;

import java.util.List;
import java.util.Map;

/**
 * Rôle : Test unitaire JVM complet reproduisant et validant toutes les règles métier du panier et des commandes.
 * Entrée : Repositories en mode mock (CartRepository, OrderRepository, ProductRepository).
 * Sortie : Vérification de stock atomique, cumul panier, isolation utilisateurs, droits vendeur/acheteur.
 */
public class CommerceMockTest {

    private static final String ALICE = "u_alice";
    private static final String BOB = "u_bob";

    private CartRepository cartRepo;
    private OrderRepository orderRepo;
    private ProductRepository productRepo;
    private MockBackendService mockService;

    private static class SyncResult<T> implements BaseRepository.Callback<T> {
        T data;
        String error;

        @Override
        public void onSuccess(T result) {
            this.data = result;
        }

        @Override
        public void onError(String errorMessage) {
            this.error = errorMessage;
        }

        T expectSuccess() {
            assertNull("Erreur inattendue : " + error, error);
            assertNotNull(data);
            return data;
        }
    }

    @Before
    public void setUp() {
        mockService = MockBackendService.getInstance();
        mockService.resetData();
        cartRepo = new CartRepository(true);
        orderRepo = new OrderRepository(true);
        productRepo = new ProductRepository(true);
        AuthSession.getInstance().clear();
    }

    @Test
    public void testEverythingRequiresLogin() {
        AuthSession.getInstance().clear();
        SyncResult<Cart> cartRes = new SyncResult<>();
        cartRepo.addItem("prod_1", 1, cartRes);
        assertEquals("Connexion requise", cartRes.error);

        SyncResult<List<Order>> orderRes = new SyncResult<>();
        orderRepo.checkout("Lot 1", "03400", PaymentMethod.A_LA_LIVRAISON, "", orderRes);
        assertEquals("Connexion requise", orderRes.error);
    }

    @Test
    public void testCartAccumulationAndStockCheck() {
        AuthSession.getInstance().updateSession(ALICE, "token");
        SyncResult<Cart> res = new SyncResult<>();
        cartRepo.addItem("prod_1", 2, res);
        Cart cart = res.expectSuccess();
        assertEquals(2, cart.getItemCount());

        res = new SyncResult<>();
        cartRepo.addItem("prod_1", 1, res);
        cart = res.expectSuccess();
        assertEquals(3, cart.getItemCount());
        assertEquals(1, cart.getItems().size());

        // Stock initial de prod_1 = 5. Ajout de 3 de plus dépasse 5
        res = new SyncResult<>();
        cartRepo.addItem("prod_1", 3, res);
        assertTrue(res.error.startsWith("Stock insuffisant"));

        // Retrait en fixant à 0
        res = new SyncResult<>();
        cartRepo.setItemQuantity("prod_1", 0, res);
        assertTrue(res.expectSuccess().isEmpty());
    }

    @Test
    public void testCheckoutSplitsOrdersBySellerAndReservesStock() {
        AuthSession.getInstance().updateSession(ALICE, "token");
        cartRepo.addItem("prod_1", 2, new SyncResult<>()); // seller_101, stock 5 -> 3
        cartRepo.addItem("prod_3", 1, new SyncResult<>()); // seller_103, stock 2 -> 1

        SyncResult<List<Order>> orderRes = new SyncResult<>();
        orderRepo.checkout("Lot 12 Ankorondrano", "+261341234567", PaymentMethod.MVOLA, "Urgent", orderRes);
        List<Order> orders = orderRes.expectSuccess();
        assertEquals(2, orders.size());

        Product p1 = mockService.getProductById("prod_1");
        Product p3 = mockService.getProductById("prod_3");
        assertEquals(3, p1.getStock());
        assertEquals(1, p3.getStock());

        // Panier vidé
        SyncResult<Cart> cartRes = new SyncResult<>();
        cartRepo.getCart(cartRes);
        assertTrue(cartRes.expectSuccess().isEmpty());
    }

    @Test
    public void testAtomicCheckoutRefusalWhenStockDepleted() {
        AuthSession.getInstance().updateSession(ALICE, "token");
        cartRepo.addItem("prod_3", 2, new SyncResult<>()); // stock initial = 2

        // Bob rafle le stock
        AuthSession.getInstance().updateSession(BOB, "token");
        cartRepo.addItem("prod_3", 2, new SyncResult<>());
        orderRepo.checkout("Lot Bob", "+2613300", PaymentMethod.A_LA_LIVRAISON, "", new SyncResult<>());

        assertEquals(0, mockService.getProductById("prod_3").getStock());

        // Alice tente de commander
        AuthSession.getInstance().updateSession(ALICE, "token");
        SyncResult<List<Order>> orderRes = new SyncResult<>();
        orderRepo.checkout("Lot Alice", "+2613400", PaymentMethod.A_LA_LIVRAISON, "", orderRes);
        assertEquals("Certains articles ne sont plus disponibles dans la quantité demandée", orderRes.error);
    }

    @Test
    public void testOrderLifecycleAndCancellationRestock() {
        AuthSession.getInstance().updateSession(ALICE, "token");
        cartRepo.addItem("prod_3", 2, new SyncResult<>());
        SyncResult<List<Order>> orderRes = new SyncResult<>();
        orderRepo.checkout("Lot Alice", "+2613400", PaymentMethod.A_LA_LIVRAISON, "", orderRes);
        Order order = orderRes.expectSuccess().get(0);
        assertEquals(0, mockService.getProductById("prod_3").getStock());

        // Vendeur confirme
        AuthSession.getInstance().updateSession("seller_103", "token");
        SyncResult<Order> changeRes = new SyncResult<>();
        orderRepo.changeStatus(order.getId(), OrderStatus.CONFIRMEE, changeRes);
        assertEquals(OrderStatus.CONFIRMEE, changeRes.expectSuccess().getStatus());

        // Vendeur annule -> restitution du stock
        changeRes = new SyncResult<>();
        orderRepo.changeStatus(order.getId(), OrderStatus.ANNULEE, changeRes);
        assertEquals(OrderStatus.ANNULEE, changeRes.expectSuccess().getStatus());
        assertEquals(2, mockService.getProductById("prod_3").getStock());
        assertEquals(ProductStatus.DISPONIBLE, mockService.getProductById("prod_3").getStatus());
    }

    @Test
    public void testQueryMapConversion() {
        FilterCriteria f = new FilterCriteria();
        f.setCategoryId("cat_furniture");
        f.setMinPrice(50.0);
        f.setMaxPrice(200.0);
        f.setArOnly(true);
        f.setSearchQuery("table");
        f.setSortBy("price_asc");

        Map<String, String> map = ProductRepository.toQuery(f);
        assertEquals("cat_furniture", map.get("categoryId"));
        assertEquals("50.0", map.get("minPrice"));
        assertEquals("200.0", map.get("maxPrice"));
        assertEquals("true", map.get("arOnly"));
        assertEquals("table", map.get("searchQuery"));
        assertEquals("price_asc", map.get("sortBy"));
    }
}
