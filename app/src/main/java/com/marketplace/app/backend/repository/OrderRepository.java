package com.marketplace.app.backend.repository;

import com.marketplace.app.backend.ApiClient;
import com.marketplace.app.backend.AuthSession;
import com.marketplace.app.backend.mock.MockBackendService;
import com.marketplace.app.base.BaseRepository;
import com.marketplace.app.model.Order;
import com.marketplace.app.model.OrderStatus;
import com.marketplace.app.model.PaymentMethod;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Response;

/**
 * Rôle : Repository des commandes (passage en caisse et cycle de vie des statuts).
 * Entrée : checkout, list, changeStatus.
 * Sortie : Liste de commandes créées ou commande mise à jour.
 */
public class OrderRepository extends BaseRepository<Order> {

    private final boolean useMock;

    public OrderRepository() {
        this(ApiClient.isUseMock());
    }

    public OrderRepository(boolean useMock) {
        this.useMock = useMock;
    }

    @Override
    public void list(Object filter, Callback<List<Order>> callback) {
        boolean asSeller = (filter instanceof Boolean) && (Boolean) filter;
        String status = (filter instanceof String) ? (String) filter : null;
        if (useMock) {
            String uid = AuthSession.getInstance().getUserId();
            postSuccess(callback, MockBackendService.getInstance().getOrders(uid, asSeller));
            return;
        }

        ApiClient.getApi().getOrders(asSeller ? "seller" : "buyer", status).enqueue(new retrofit2.Callback<List<Order>>() {
            @Override
            public void onResponse(Call<List<Order>> call, Response<List<Order>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    postSuccess(callback, response.body());
                } else {
                    postError(callback, parseHttpError(response));
                }
            }

            @Override
            public void onFailure(Call<List<Order>> call, Throwable t) {
                postError(callback, parseNetworkError(t));
            }
        });
    }

    @Override
    public void getById(String id, Callback<Order> callback) {
        if (useMock) {
            Order o = MockBackendService.getInstance().getOrderById(id);
            if (o != null) postSuccess(callback, o);
            else postError(callback, "Commande introuvable");
            return;
        }

        ApiClient.getApi().getOrderById(id).enqueue(new retrofit2.Callback<Order>() {
            @Override
            public void onResponse(Call<Order> call, Response<Order> response) {
                if (response.isSuccessful() && response.body() != null) {
                    postSuccess(callback, response.body());
                } else {
                    postError(callback, parseHttpError(response));
                }
            }

            @Override
            public void onFailure(Call<Order> call, Throwable t) {
                postError(callback, parseNetworkError(t));
            }
        });
    }

    public void checkout(String address, String phone, PaymentMethod method, String note, Callback<List<Order>> callback) {
        if (useMock) {
            String uid = AuthSession.getInstance().getUserId();
            try {
                List<Order> orders = MockBackendService.getInstance().checkout(uid, address, phone, method, note);
                postSuccess(callback, orders);
            } catch (Exception e) {
                postError(callback, e.getMessage());
            }
            return;
        }

        Map<String, Object> body = new HashMap<>();
        if (address != null) body.put("deliveryAddress", address);
        if (phone != null) body.put("deliveryPhone", phone);
        if (method != null) body.put("paymentMethod", method.name());
        if (note != null) body.put("note", note);

        ApiClient.getApi().checkout(body).enqueue(new retrofit2.Callback<Map<String, List<Order>>>() {
            @Override
            public void onResponse(Call<Map<String, List<Order>>> call, Response<Map<String, List<Order>>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    postSuccess(callback, response.body().get("orders"));
                } else {
                    postError(callback, parseHttpError(response));
                }
            }

            @Override
            public void onFailure(Call<Map<String, List<Order>>> call, Throwable t) {
                postError(callback, parseNetworkError(t));
            }
        });
    }

    public void changeStatus(String orderId, OrderStatus status, Callback<Order> callback) {
        if (useMock) {
            String uid = AuthSession.getInstance().getUserId();
            try {
                Order updated = MockBackendService.getInstance().updateOrderStatus(uid, orderId, status);
                postSuccess(callback, updated);
            } catch (Exception e) {
                postError(callback, e.getMessage());
            }
            return;
        }

        Map<String, String> body = new HashMap<>();
        body.put("status", status.name());
        ApiClient.getApi().updateOrderStatus(orderId, body).enqueue(new retrofit2.Callback<Order>() {
            @Override
            public void onResponse(Call<Order> call, Response<Order> response) {
                if (response.isSuccessful() && response.body() != null) {
                    postSuccess(callback, response.body());
                } else {
                    postError(callback, parseHttpError(response));
                }
            }

            @Override
            public void onFailure(Call<Order> call, Throwable t) {
                postError(callback, parseNetworkError(t));
            }
        });
    }

    @Override
    public void create(Order item, Callback<Order> callback) {
        postSuccess(callback, item);
    }

    @Override
    public void update(Order item, Callback<Order> callback) {
        postSuccess(callback, item);
    }

    @Override
    public void delete(String id, Callback<Boolean> callback) {
        changeStatus(id, OrderStatus.ANNULEE, new Callback<Order>() {
            @Override
            public void onSuccess(Order result) {
                postSuccess(callback, true);
            }

            @Override
            public void onError(String message) {
                postError(callback, message);
            }
        });
    }
}
