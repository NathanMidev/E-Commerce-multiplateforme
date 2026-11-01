package com.marketplace.core.network;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.marketplace.core.model.Cart;
import com.marketplace.core.model.CheckoutRequest;
import com.marketplace.core.model.Order;
import com.marketplace.core.model.OrderStatus;
import com.marketplace.core.model.PaymentMethod;
import com.marketplace.core.model.Product;
import com.marketplace.core.model.ProductStatus;
import com.marketplace.core.model.User;
import com.marketplace.core.model.UserRole;

import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Règles du panier et des commandes (mode démo hors ligne ; le serveur applique les mêmes). */
public class CommerceMockTest {

    private static final String ALICE = "u_alice";
    private static final String BOB = "u_bob";

    /** Session de test : on change d'utilisateur en modifiant `current`. */
    private static class TestSession implements AuthSession {
        String current;
        @Override public String getUserId() { return current; }
        @Override public String getIdToken() { return current == null ? null : "token-" + current; }
    }

    /** Le mock répond de façon synchrone : on récupère directement le résultat. */
    private static class Result<T> implements PostgresBackendApi.ApiCallback<T> {
        T value;
        String error;
        @Override public void onSuccess(T result) { value = result; }
        @Override public void onError(String errorMessage) { error = errorMessage; }
        T ok() {
            assertNull("Erreur inattendue : " + error, error);
            assertNotNull(value);
            return value;
        }
    }

    private TestSession session;
    private MockPostgresBackendService api;

    @Before
    public void setUp() {
        session = new TestSession();
        api = new MockPostgresBackendService(session);
    }

    // ------------------------------------------------------------------ helpers

    private int stockOf(String productId) {
        Result<Product> r = new Result<>();
        api.getProductById(productId, r);
        return r.ok().getStock();
    }

    private Product product(String productId) {
        Result<Product> r = new Result<>();
        api.getProductById(productId, r);
        return r.ok();
    }

    private Result<Cart> addToCart(String productId, int quantity) {
        Result<Cart> r = new Result<>();
        api.addToCart(productId, quantity, r);
        return r;
    }

    private void completeProfile(String uid) {
        session.current = uid;
        Map<String, String> fields = new HashMap<>();
        fields.put("name", "Nom " + uid);
        fields.put("address", "Lot 12 Ankorondrano");
        fields.put("whatsapp", "+261341234567");
        Result<User> r = new Result<>();
        api.updateMyProfile(fields, r);
        r.ok();
    }

    private List<Order> checkout(String uid) {
        session.current = uid;
        Result<List<Order>> r = new Result<>();
        api.checkout(new CheckoutRequest(null, null, PaymentMethod.MVOLA, "Appeler avant"), r);
        return r.ok();
    }

    private Result<Order> changeStatus(String uid, Order order, OrderStatus status) {
        session.current = uid;
        Result<Order> r = new Result<>();
        api.updateOrderStatus(order.getId(), status, r);
        return r;
    }

    private Order orderOfSeller(List<Order> orders, String sellerId) {
        for (Order o : orders) {
            if (sellerId.equals(o.getSellerId())) return o;
        }
        throw new AssertionError("Aucune commande pour le vendeur " + sellerId);
    }

    // -------------------------------------------------------------------- tests

    @Test
    public void everythingRequiresLogin() {
        session.current = null;
        assertEquals("Connexion requise", addToCart("prod_1", 1).error);
        Result<Cart> cart = new Result<>();
        api.getCart(cart);
        assertEquals("Connexion requise", cart.error);
        Result<List<Order>> orders = new Result<>();
        api.getOrders(false, orders);
        assertEquals("Connexion requise", orders.error);
    }

    @Test
    public void cartAccumulatesAndRespectsStock() {
        session.current = ALICE;
        int stock = stockOf("prod_1");
        assertTrue("le test suppose au moins 3 unités en stock", stock >= 3);

        assertEquals(2, addToCart("prod_1", 2).ok().getItemCount());
        Cart cart = addToCart("prod_1", 1).ok();
        assertEquals("les ajouts se cumulent", 3, cart.getItems().get(0).getQuantity());
        assertEquals(1, cart.getItems().size());
        assertEquals(Math.round(189.99 * 3 * 100) / 100.0, cart.getTotal(), 0.001);

        Result<Cart> tooMany = addToCart("prod_1", stock);
        assertTrue(tooMany.error.startsWith("Stock insuffisant"));

        Result<Cart> set = new Result<>();
        api.setCartItemQuantity("prod_1", 1, set);
        assertEquals(1, set.ok().getItemCount());
        Result<Cart> removed = new Result<>();
        api.setCartItemQuantity("prod_1", 0, removed);
        assertTrue("quantité 0 = retrait", removed.ok().isEmpty());
    }

    @Test
    public void invalidCartOperationsAreRefused() {
        session.current = ALICE;
        assertEquals("Produit introuvable", addToCart("nope", 1).error);
        assertEquals("Quantité invalide", addToCart("prod_1", 0).error);
    }

    @Test
    public void cannotBuyOwnProduct() {
        Product mine = new Product("p_alice", "Ma table", "desc", 50.0, ProductStatus.DISPONIBLE,
                "cat_furniture", ALICE, "Alice", "", false, null, null, 3, System.currentTimeMillis());
        Result<Product> created = new Result<>();
        api.createProduct(mine, created);
        created.ok();

        session.current = ALICE;
        assertEquals("Vous ne pouvez pas acheter votre propre produit", addToCart("p_alice", 1).error);
        session.current = BOB;
        assertEquals(1, addToCart("p_alice", 1).ok().getItemCount());
    }

    @Test
    public void everyUserHasHisOwnCart() {
        session.current = ALICE;
        addToCart("prod_2", 1).ok();
        session.current = BOB;
        Result<Cart> bobCart = new Result<>();
        api.getCart(bobCart);
        assertTrue(bobCart.ok().isEmpty());
    }

    @Test
    public void checkoutSplitsOrdersBySellerAndReservesStock() {
        completeProfile(ALICE);
        int stock1 = stockOf("prod_1");
        int stock3 = stockOf("prod_3");
        addToCart("prod_1", 2).ok();
        addToCart("prod_3", 1).ok();

        List<Order> orders = checkout(ALICE);
        assertEquals("une commande par vendeur", 2, orders.size());

        Order o1 = orderOfSeller(orders, "seller_101");
        Order o3 = orderOfSeller(orders, "seller_103");
        assertEquals(Math.round(189.99 * 2 * 100) / 100.0, o1.getTotal(), 0.001);
        assertEquals(220.0, o3.getTotal(), 0.001);
        for (Order o : orders) {
            assertEquals(OrderStatus.EN_ATTENTE, o.getStatus());
            assertEquals(PaymentMethod.MVOLA, o.getPaymentMethod());
            assertEquals("Lot 12 Ankorondrano", o.getDeliveryAddress());
            assertEquals("+261341234567", o.getDeliveryPhone());
            assertEquals(ALICE, o.getBuyerId());
            assertEquals(1, o.getItems().size());
            assertTrue(o.getReference().startsWith("CMD-"));
        }
        assertEquals(stock1 - 2, stockOf("prod_1"));
        assertEquals(stock3 - 1, stockOf("prod_3"));

        Result<Cart> cart = new Result<>();
        api.getCart(cart);
        assertTrue("le panier est vidé", cart.ok().isEmpty());
    }

    @Test
    public void checkoutIsRefusedWhenCartEmptyOrNoDeliveryInfo() {
        session.current = ALICE;
        Result<List<Order>> empty = new Result<>();
        api.checkout(new CheckoutRequest(), empty);
        assertTrue(empty.error.contains("Adresse de livraison")); // l'adresse est vérifiée en premier

        completeProfile(ALICE);
        Result<List<Order>> stillEmpty = new Result<>();
        api.checkout(new CheckoutRequest(), stillEmpty);
        assertEquals("Votre panier est vide", stillEmpty.error);
    }

    @Test
    public void checkoutIsAtomicWhenOneItemBecomesUnavailable() {
        completeProfile(ALICE);
        completeProfile(BOB);
        int stock3 = stockOf("prod_3");
        int stock1 = stockOf("prod_1");

        session.current = ALICE;
        addToCart("prod_1", 1).ok();
        addToCart("prod_3", 1).ok();

        session.current = BOB;                    // Bob rafle tout le stock de prod_3
        addToCart("prod_3", stock3).ok();
        checkout(BOB);
        assertEquals(0, stockOf("prod_3"));
        assertEquals(ProductStatus.INDISPONIBLE, product("prod_3").getStatus());

        session.current = ALICE;
        Result<List<Order>> refused = new Result<>();
        api.checkout(new CheckoutRequest(), refused);
        assertEquals("Certains articles ne sont plus disponibles dans la quantité demandée", refused.error);
        assertEquals("prod_1 n'est pas touché", stock1, stockOf("prod_1"));
        Result<Cart> cart = new Result<>();
        api.getCart(cart);
        assertEquals("le panier est conservé", 2, cart.ok().getItems().size());
        assertFalse("l'article épuisé est signalé", cart.ok().isCheckoutPossible());
        Result<List<Order>> alicesOrders = new Result<>();
        api.getOrders(false, alicesOrders);
        assertTrue(alicesOrders.ok().isEmpty());
    }

    @Test
    public void orderLifecycleCountsASaleOnlyWhenBuyerConfirmsDelivery() {
        completeProfile(ALICE);
        addToCart("prod_1", 1).ok();
        Order order = checkout(ALICE).get(0);
        assertEquals("seller_101", order.getSellerId());

        // l'acheteur ne peut pas confirmer / expédier
        assertEquals("Action non autorisée pour votre rôle", changeStatus(ALICE, order, OrderStatus.CONFIRMEE).error);
        // on ne saute pas d'étape
        assertEquals("Transition impossible : EN_ATTENTE -> EXPEDIEE", changeStatus("seller_101", order, OrderStatus.EXPEDIEE).error);

        assertEquals(OrderStatus.CONFIRMEE, changeStatus("seller_101", order, OrderStatus.CONFIRMEE).ok().getStatus());
        assertEquals(OrderStatus.EXPEDIEE, changeStatus("seller_101", order, OrderStatus.EXPEDIEE).ok().getStatus());
        assertNotNull(order.getShippedAt());

        // le vendeur ne peut pas se déclarer « livré »
        assertEquals("Action non autorisée pour votre rôle", changeStatus("seller_101", order, OrderStatus.LIVREE).error);
        session.current = "seller_101";
        Result<User> before = new Result<>();
        api.getMyProfile(before);
        assertEquals("pas de vente avant la confirmation de l'acheteur", 0, before.ok().getTotalSales());

        assertEquals(OrderStatus.LIVREE, changeStatus(ALICE, order, OrderStatus.LIVREE).ok().getStatus());
        assertNotNull(order.getDeliveredAt());

        session.current = "seller_101";
        Result<User> after = new Result<>();
        api.getMyProfile(after);
        assertEquals(1, after.ok().getTotalSales());
        assertEquals("le niveau est calculé à partir des ventes", UserRole.SELLER, after.ok().resolveRole());

        assertTrue("plus annulable une fois livrée",
                changeStatus(ALICE, order, OrderStatus.ANNULEE).error.startsWith("Transition impossible"));
    }

    @Test
    public void cancellationRestocksTheProduct() {
        completeProfile(ALICE);
        int stock = stockOf("prod_3");
        addToCart("prod_3", stock).ok();
        Order order = checkout(ALICE).get(0);
        assertEquals(0, stockOf("prod_3"));
        assertEquals(ProductStatus.INDISPONIBLE, product("prod_3").getStatus());

        assertEquals(OrderStatus.ANNULEE, changeStatus(ALICE, order, OrderStatus.ANNULEE).ok().getStatus());
        assertEquals(stock, stockOf("prod_3"));
        assertEquals(ProductStatus.DISPONIBLE, product("prod_3").getStatus());
        assertNotNull(order.getCancelledAt());

        session.current = "seller_103";
        Result<User> seller = new Result<>();
        api.getMyProfile(seller);
        assertEquals("une annulation n'est pas une vente", 0, seller.ok().getTotalSales());
    }

    @Test
    public void buyerMayCancelOnlyWhilePending() {
        completeProfile(ALICE);
        addToCart("prod_2", 1).ok();
        Order order = checkout(ALICE).get(0);
        assertTrue(order.canBuyerCancel());
        assertTrue(order.canSellerConfirm());

        changeStatus("seller_102", order, OrderStatus.CONFIRMEE).ok();
        assertFalse(order.canBuyerCancel());
        assertTrue(order.canSellerShip());
        assertEquals("Action non autorisée pour votre rôle", changeStatus(ALICE, order, OrderStatus.ANNULEE).error);
        assertEquals(OrderStatus.ANNULEE, changeStatus("seller_102", order, OrderStatus.ANNULEE).ok().getStatus());
    }

    @Test
    public void ordersAreVisibleOnlyToTheirBuyerAndSeller() {
        completeProfile(ALICE);
        addToCart("prod_2", 1).ok();
        Order order = checkout(ALICE).get(0);

        session.current = "u_stranger";
        Result<Order> stranger = new Result<>();
        api.getOrderById(order.getId(), stranger);
        assertEquals("Commande introuvable", stranger.error);

        session.current = ALICE;
        Result<List<Order>> purchases = new Result<>();
        api.getOrders(false, purchases);
        assertEquals(1, purchases.ok().size());

        session.current = "seller_102";
        Result<List<Order>> sales = new Result<>();
        api.getOrders(true, sales);
        assertEquals(1, sales.ok().size());
        assertEquals("le vendeur voit où livrer", "Lot 12 Ankorondrano", sales.ok().get(0).getDeliveryAddress());
        Result<List<Order>> asBuyer = new Result<>();
        api.getOrders(false, asBuyer);
        assertTrue(asBuyer.ok().isEmpty());
    }

    @Test
    public void profileUpdateAcceptsOnlyKnownFields() {
        session.current = ALICE;
        Map<String, String> bad = new HashMap<>();
        bad.put("totalSales", "999");
        Result<User> refused = new Result<>();
        api.updateMyProfile(bad, refused);
        assertEquals("Champ non modifiable : totalSales", refused.error);

        Map<String, String> good = new HashMap<>();
        good.put("city", "Antananarivo");
        good.put("facebookLink", "https://facebook.com/alice");
        Result<User> ok = new Result<>();
        api.updateMyProfile(good, ok);
        assertEquals("Antananarivo", ok.ok().getCity());
        assertEquals(0, ok.ok().getTotalSales());
    }
}
