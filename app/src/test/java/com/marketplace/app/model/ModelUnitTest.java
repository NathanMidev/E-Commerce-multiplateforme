package com.marketplace.app.model;

import org.junit.Test;

import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Rôle : Tests unitaires JVM des entités métier et validation.
 * Entrée : Modèles User, Product, Cart, Order, ContactRequest.
 * Sortie : Assertions de validation et règles de rôle.
 */
public class ModelUnitTest {

    @Test
    public void testUserValidationAndRole() {
        User user = new User();
        List<String> errors = user.validate();
        assertFalse(errors.isEmpty());
        assertEquals("Le nom est obligatoire", errors.get(0));

        user.setName("Jean");
        assertTrue(user.validate().isEmpty());

        assertEquals(UserRole.BUYER, user.resolveRole());

        user.setTotalSales(5);
        assertEquals(UserRole.SELLER, user.resolveRole());

        user.setTotalSales(50);
        user.setResponseScore(4.2);
        assertEquals(UserRole.SELLER, user.resolveRole());

        user.setResponseScore(4.8);
        assertEquals(UserRole.PRO, user.resolveRole());

        user.setLevel("PRO");
        assertEquals(UserRole.PRO, user.resolveRole());
    }

    @Test
    public void testProductValidationAndStockStatus() {
        Product p = new Product();
        List<String> errors = p.validate();
        assertFalse(errors.isEmpty());

        p.setTitle("Table en bois");
        p.setPrice(100.0);
        p.setStock(5);
        assertTrue(p.validate().isEmpty());
        assertEquals(ProductStatus.NOUVEAU, p.getStatus());

        p.setStock(0);
        assertEquals(ProductStatus.INDISPONIBLE, p.getStatus());
    }

    @Test
    public void testCartItemAndCartCalculations() {
        CartItem item1 = new CartItem("p1", "Item 1", "", 10.0, 2, 5, "s1", "Seller 1");
        assertEquals(20.0, item1.getLineTotal(), 0.001);
        assertTrue(item1.isAvailable());

        CartItem item2 = new CartItem("p2", "Item 2", "", 15.5, 1, 0, "s2", "Seller 2");
        assertFalse(item2.isAvailable());

        Cart cart = new Cart(java.util.Arrays.asList(item1, item2));
        assertEquals(3, cart.getItemCount());
        assertEquals(35.5, cart.getTotal(), 0.001);
        assertFalse("Checkout impossible avec un article indisponible", cart.isCheckoutPossible());
    }

    @Test
    public void testOrderValidationAndPermissions() {
        Order order = new Order();
        assertFalse(order.validate().isEmpty());

        order.setDeliveryAddress("Lot 123");
        order.setDeliveryPhone("+261340000000");
        assertTrue(order.validate().isEmpty());

        assertEquals(OrderStatus.EN_ATTENTE, order.getStatus());
        assertTrue(order.canSellerConfirm());
        assertTrue(order.canBuyerCancel());

        order.applyStatus(OrderStatus.CONFIRMEE);
        assertTrue(order.canSellerShip());
        assertFalse(order.canBuyerCancel());

        order.applyStatus(OrderStatus.EXPEDIEE);
        assertTrue(order.canBuyerConfirmDelivery());
    }
}
