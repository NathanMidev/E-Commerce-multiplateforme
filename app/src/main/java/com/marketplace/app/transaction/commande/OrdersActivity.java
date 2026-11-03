package com.marketplace.app.transaction.commande;

import android.view.View;
import android.widget.Button;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.marketplace.app.R;
import com.marketplace.app.backend.repository.OrderRepository;
import com.marketplace.app.base.BaseActivity;
import com.marketplace.app.base.BaseRepository;
import com.marketplace.app.model.Order;
import com.marketplace.app.model.OrderStatus;

import java.util.List;

/**
 * Rôle : Écran d'historique et suivi des commandes (Achats et Ventes).
 * Entrée : Filtrage par rôle (acheteur ou vendeur) et mise à jour des statuts.
 * Sortie : Progression du cycle de vente et incrémentation automatique du score vendeur.
 */
public class OrdersActivity extends BaseActivity {

    private Button btnTabBuyer;
    private Button btnTabSeller;
    private RecyclerView rvOrders;

    private OrderRepository orderRepository;
    private OrdersAdapter adapter;
    private boolean isSellerMode = false;

    @Override
    protected int getLayoutId() {
        return R.layout.activity_orders;
    }

    @Override
    protected void bindViews() {
        orderRepository = new OrderRepository();

        btnTabBuyer = findViewById(R.id.btn_tab_buyer);
        btnTabSeller = findViewById(R.id.btn_tab_seller);
        rvOrders = findViewById(R.id.rv_orders);

        View btnFaq = findViewById(R.id.btn_orders_faq);
        if (btnFaq != null) {
            btnFaq.setOnClickListener(v -> com.marketplace.app.navigation.Router.openFaq(this));
        }

        rvOrders.setLayoutManager(new LinearLayoutManager(this));
        setupAdapter();

        btnTabBuyer.setOnClickListener(v -> {
            isSellerMode = false;
            btnTabBuyer.setBackgroundColor(getResources().getColor(R.color.primary));
            btnTabSeller.setBackgroundColor(android.graphics.Color.TRANSPARENT);
            setupAdapter();
            loadData();
        });

        btnTabSeller.setOnClickListener(v -> {
            isSellerMode = true;
            btnTabSeller.setBackgroundColor(getResources().getColor(R.color.primary));
            btnTabBuyer.setBackgroundColor(android.graphics.Color.TRANSPARENT);
            setupAdapter();
            loadData();
        });
    }

    private void setupAdapter() {
        adapter = new OrdersAdapter(isSellerMode, (order, newStatus) -> {
            orderRepository.changeStatus(order.getId(), newStatus, new BaseRepository.Callback<Order>() {
                @Override
                public void onSuccess(Order result) {
                    toast("Statut mis à jour : " + newStatus.name());
                    loadData();
                }

                @Override
                public void onError(String message) {
                    showError(message, null);
                }
            });
        });
        rvOrders.setAdapter(adapter);
    }

    @Override
    protected void loadData() {
        if (!requireLogin(this::loadData)) return;

        orderRepository.list(isSellerMode, new BaseRepository.Callback<List<Order>>() {
            @Override
            public void onSuccess(List<Order> result) {
                render(result);
            }

            @Override
            public void onError(String message) {
                showError(message, OrdersActivity.this::loadData);
            }
        });
    }

    @Override
    protected void render(Object data) {
        if (data instanceof List) {
            adapter.setItems((List<Order>) data);
        }
    }
}
