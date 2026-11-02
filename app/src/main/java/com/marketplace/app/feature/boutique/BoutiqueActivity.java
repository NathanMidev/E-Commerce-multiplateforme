package com.marketplace.app.feature.boutique;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.marketplace.app.R;
import com.marketplace.app.backend.repository.ProductRepository;
import com.marketplace.app.backend.repository.UserRepository;
import com.marketplace.app.base.BaseActivity;
import com.marketplace.app.base.BaseRepository;
import com.marketplace.app.feature.ar.ArLauncher;
import com.marketplace.app.feature.catalogue.ProductCardAdapter;
import com.marketplace.app.feature.contact.ContactBottomSheet;
import com.marketplace.app.feature.contact.ContactLinks;
import com.marketplace.app.model.FilterCriteria;
import com.marketplace.app.model.Product;
import com.marketplace.app.model.User;
import com.marketplace.app.navigation.Router;
import com.marketplace.app.ui.SellerBadge;

import java.util.List;

/**
 * Rôle : Écran Boutique Vendeur dédié (photo de profil, nom complet + @username,
 * accès au profil Facebook si renseigné, SellerBadge, réputation et articles du vendeur).
 */
public class BoutiqueActivity extends BaseActivity {

    private ImageView ivBoutiqueAvatar;
    private TextView tvBoutiqueName;
    private TextView tvBoutiqueUsername;
    private TextView tvBoutiqueSales;
    private TextView tvBoutiqueCity;
    private SellerBadge badgeSeller;
    private Button btnContact;
    private Button btnFacebookProfile;
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

        ivBoutiqueAvatar = findViewById(R.id.iv_boutique_avatar);
        tvBoutiqueName = findViewById(R.id.tv_boutique_name);
        tvBoutiqueUsername = findViewById(R.id.tv_boutique_username);
        tvBoutiqueSales = findViewById(R.id.tv_boutique_sales);
        tvBoutiqueCity = findViewById(R.id.tv_boutique_city);
        badgeSeller = findViewById(R.id.badge_boutique_seller);
        btnContact = findViewById(R.id.btn_boutique_contact);
        btnFacebookProfile = findViewById(R.id.btn_boutique_facebook);
        rvProducts = findViewById(R.id.rv_boutique_products);

        rvProducts.setLayoutManager(new GridLayoutManager(this, 2));
        adapter = new ProductCardAdapter(new ProductCardAdapter.OnProductActionClickListener() {
            @Override
            public void onProductClick(Product product) {
                Router.openProductDetail(BoutiqueActivity.this, product.getId());
            }

            @Override
            public void onArClick(Product product) {
                ArLauncher.launchArExperienceIfCompatible(
                        BoutiqueActivity.this,
                        product.getId(),
                        product.getTitle(),
                        product.getModel3dUrl()
                );
            }
        });
        rvProducts.setAdapter(adapter);

        btnContact.setOnClickListener(v -> {
            if (currentSeller != null) {
                ContactBottomSheet bottomSheet = ContactBottomSheet.newInstance(null, currentSeller);
                bottomSheet.show(getSupportFragmentManager(), "ContactBottomSheet");
            }
        });

        if (btnFacebookProfile != null) {
            btnFacebookProfile.setOnClickListener(v -> {
                if (currentSeller != null && currentSeller.hasFacebookProfile()) {
                    String fbUrl = ContactLinks.buildFacebookProfileUrl(currentSeller.getFacebookLink());
                    if (fbUrl != null) {
                        try {
                            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(fbUrl)));
                        } catch (ActivityNotFoundException e) {
                            toast("Impossible d'ouvrir le lien Facebook");
                        }
                    }
                }
            });
        }
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

            if (tvBoutiqueUsername != null) {
                if (u.getUsername() != null && !u.getUsername().trim().isEmpty()) {
                    tvBoutiqueUsername.setText("@" + u.getUsername().trim());
                    tvBoutiqueUsername.setVisibility(View.VISIBLE);
                } else {
                    tvBoutiqueUsername.setVisibility(View.GONE);
                }
            }

            if (ivBoutiqueAvatar != null) {
                if (u.hasCustomAvatar()) {
                    Glide.with(this)
                            .load(u.getAvatarUrl())
                            .placeholder(R.drawable.ic_default_avatar)
                            .error(R.drawable.ic_default_avatar)
                            .circleCrop()
                            .into(ivBoutiqueAvatar);
                } else {
                    ivBoutiqueAvatar.setImageResource(R.drawable.ic_default_avatar);
                }
            }

            tvBoutiqueSales.setText(u.getTotalSales() + " vente(s) confirmée(s)");
            if (u.getCity() != null) tvBoutiqueCity.setText("📍 " + u.getCity());
            badgeSeller.setRole(u.resolveRole());

            if (btnFacebookProfile != null) {
                btnFacebookProfile.setVisibility(u.hasFacebookProfile() ? View.VISIBLE : View.GONE);
            }
        }
    }
}
