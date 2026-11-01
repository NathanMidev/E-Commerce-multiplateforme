package com.marketplace.app.backend.repository;

import com.marketplace.app.backend.ApiClient;
import com.marketplace.app.backend.mock.MockBackendService;
import com.marketplace.app.base.BaseRepository;
import com.marketplace.app.model.Payment;
import com.marketplace.app.model.PaymentStatus;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Response;

/**
 * Rôle : Repository de gestion du paiement déclaratif.
 * Entrée : Consultation et mise à jour du statut PAYE / ECHOUE.
 * Sortie : Modèle Payment.
 */
public class PaymentRepository extends BaseRepository<Payment> {

    private final boolean useMock;

    public PaymentRepository() {
        this(ApiClient.isUseMock());
    }

    public PaymentRepository(boolean useMock) {
        this.useMock = useMock;
    }

    @Override
    public void list(Object filter, Callback<List<Payment>> callback) {
        postSuccess(callback, Collections.emptyList());
    }

    @Override
    public void getById(String id, Callback<Payment> callback) {
        getByOrderId(id, callback);
    }

    public void getByOrderId(String orderId, Callback<Payment> callback) {
        if (useMock) {
            Payment p = MockBackendService.getInstance().getPayment(orderId);
            postSuccess(callback, p);
            return;
        }

        ApiClient.getApi().getPayment(orderId).enqueue(new retrofit2.Callback<Payment>() {
            @Override
            public void onResponse(Call<Payment> call, Response<Payment> response) {
                if (response.isSuccessful() && response.body() != null) {
                    postSuccess(callback, response.body());
                } else {
                    postError(callback, parseHttpError(response));
                }
            }

            @Override
            public void onFailure(Call<Payment> call, Throwable t) {
                postError(callback, parseNetworkError(t));
            }
        });
    }

    public void updateStatus(String orderId, PaymentStatus status, Callback<Payment> callback) {
        if (useMock) {
            Payment p = MockBackendService.getInstance().updatePaymentStatus(orderId, status);
            postSuccess(callback, p);
            return;
        }

        Map<String, String> body = new HashMap<>();
        body.put("status", status.name());
        ApiClient.getApi().updatePaymentStatus(orderId, body).enqueue(new retrofit2.Callback<Payment>() {
            @Override
            public void onResponse(Call<Payment> call, Response<Payment> response) {
                if (response.isSuccessful() && response.body() != null) {
                    postSuccess(callback, response.body());
                } else {
                    postError(callback, parseHttpError(response));
                }
            }

            @Override
            public void onFailure(Call<Payment> call, Throwable t) {
                postError(callback, parseNetworkError(t));
            }
        });
    }

    @Override
    public void create(Payment item, Callback<Payment> callback) {
        postSuccess(callback, item);
    }

    @Override
    public void update(Payment item, Callback<Payment> callback) {
        postSuccess(callback, item);
    }

    @Override
    public void delete(String id, Callback<Boolean> callback) {
        postSuccess(callback, true);
    }
}
