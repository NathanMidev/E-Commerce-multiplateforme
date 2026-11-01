package com.marketplace.core.network;

import com.marketplace.core.model.Order;

import java.util.ArrayList;
import java.util.List;

/** Réponse de POST /orders/checkout : { "orders": [ ... ] }. */
public class CheckoutResponse {
    private List<Order> orders = new ArrayList<>();

    public List<Order> getOrders() {
        return orders != null ? orders : new ArrayList<>();
    }
}
