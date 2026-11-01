package com.marketplace.app.feature.boutique;

import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.marketplace.app.R;
import com.marketplace.app.backend.repository.ProductRepository;
import com.marketplace.app.backend.repository.UserRepository;
import com.marketplace.app.base.BaseActivity;
import com.marketplace.app.base.BaseRepository;
import com.marketplace.app.feature.catalogue.ProductCardAdapter;
import com.marketplace.app.feature.contact.ContactBottomSheet;
import com.marketplace.app.model.FilterCriteria;
import com.marketplace.app.model.Product;
import com.marketplace.app.model.User;
import com.marketplace.app.navigation.Router;
import com.marketplace.app.ui.SellerBadge;

import java.util.List;

/**
 * Rôle : Écran Boutique Vendeur dédié (coordonnées, SellerBadge, réputation et articles du vendeur).
 * Entrée : EXTRA_SELLER_ID.
 * Sortie : Affichage des produits filtrés par sellerId et bouton de contact.
 */
public class BoutiqueActivity extends BaseActivity {

    private TextView tvBoutiqueName;
    private TextView tvBoutiqueSales;
    private TextView tvBoutiqueCity;
    private SellerBadge badgeSeller;
    private Button btnContact;
    private RecyclerView rvProducts;

    private UserRepository userRepository;
    private ProductRepository productRepository;
    private ProductCardAdapter adapter;

    private String sellerId;
    private User currentSeller;

    @Override
    protected int getLayoutId() {
        return R.layout.activity_boutique;
    }

    @Override
    protected void bindViews() {
        userRepository = new UserRepository();
        productRepository = new ProductRepository();

        sellerId = getIntent().getStringExtra(Router.EXTRA_SELLER_ID);

        tvBoutiqueName = findViewById(R.id.tv_boutique_name);
        tvBoutiqueSales = findViewById(R.id.tv_boutique_sales);
        tvBoutiqueCity = findViewById(R.id.tv_boutique_city);
        badgeSeller = findViewById(R.id.badge_boutique_seller);
        btnContact = findViewById(R.id.btn_boutique_contact);
        rvProducts = findViewById(R.id.rv_boutique_products);

        rvProducts.setLayoutManager(new GridLayoutManager(this, 2));
        adapter = new ProductCardAdapter(new ProductCardAdapter.OnProductActionClickListener() {
            @Override
            public void onProductClick(Product product) {
                Router.openProductDetail(BoutiqueActivity.this, product.getId());
            }

            @Override
            public void onArClick(Product product) {
                Router.openArViewer(BoutiqueActivity.this, product.getId(), product.getTitle(), product.getModel3dUrl());
            }
        });
        rvProducts.setAdapter(adapter);

        btnContact.setOnClickListener(v -> {
            if (currentSeller != null) {
                ContactBottomSheet bottomSheet = ContactBottomSheet.newInstance(null, currentSeller);
                bottomSheet.show(getSupportFragmentManager(), "ContactBottomSheet");
            }
        });
    }

    @Override
    protected void loadData() {
        if (sellerId == null || sellerId.trim().isEmpty()) {
            showError("Vendeur non spécifié", this::finish);
            return;
        }

        userRepository.getById(sellerId, new BaseRepository.Callback<User>() {
            @Override
            public void onSuccess(User user) {
                currentSeller = user;
                render(user);
                loadSellerProducts();
            }

            @Override
            public void onError(String message) {
                showError(message, BoutiqueActivity.this::loadData);
            }
        });
    }

    private void loadSellerProducts() {
        FilterCriteria filter = new FilterCriteria();
        filter.setSellerId(sellerId);
        productRepository.list(filter, new BaseRepository.Callback<List<Product>>() {
            @Override
            public void onSuccess(List<Product> result) {
                if (result != null) adapter.setItems(result);
            }

            @Override
            public void onError(String message) {}
        });
    }

    @Override
    protected void render(Object data) {
        if (data instanceof User) {
            User u = (User) data;
            tvBoutiqueName.setText(u.getName() != null ? u.getName() : "Boutique Vendeur");
            tvBoutiqueSales.setText(u.getTotalSales() + " vente(s) confirmée(s)");
            if (u.getCity() != null) tvBoutiqueCity.setText(u.getCity());
            badgeSeller.setRole(u.resolveRole());
        }
    }
}
