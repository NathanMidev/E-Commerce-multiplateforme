package com.marketplace.app.feature.catalogue;

import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.marketplace.app.R;
import com.marketplace.app.backend.repository.CartRepository;
import com.marketplace.app.backend.repository.ProductRepository;
import com.marketplace.app.backend.repository.UserRepository;
import com.marketplace.app.base.BaseActivity;
import com.marketplace.app.base.BaseRepository;
import com.marketplace.app.feature.contact.ContactBottomSheet;
import com.marketplace.app.model.Cart;
import com.marketplace.app.model.Product;
import com.marketplace.app.model.User;
import com.marketplace.app.navigation.Router;
import com.marketplace.app.ui.PriceFormatter;

/**
 * Rôle : Fiche détaillée du produit avec barre d'action fixe [Discuter] [Ajouter au panier] [Commander].
 * Entrée : EXTRA_PRODUCT_ID.
 * Sortie : Consultation, visualisation 3D AR, contact vendeur et achat direct.
 */
public class ProductDetailActivity extends BaseActivity {

    private ImageView ivDetailImage;
    private TextView tvTitle;
    private TextView tvPrice;
    private TextView tvStock;
    private TextView tvStatus;
    private TextView tvSeller;
    private TextView tvSellerCity;
    private TextView tvDescription;
    private TextView tv3dBadge;
    private Button btnViewAr;
    private Button btnScan3d;
    private Button btnVisitBoutique;

    private Button btnActionChat;
    private Button btnActionAddCart;
    private Button btnActionCheckout;

    private ProductRepository productRepository;
    private UserRepository userRepository;
    private CartRepository cartRepository;

    private Product currentProduct;
    private User currentSeller;
    private String productId;

    @Override
    protected int getLayoutId() {
        return R.layout.activity_product_detail;
    }

    @Override
    protected void bindViews() {
        productRepository = new ProductRepository();
        userRepository = new UserRepository();
        cartRepository = new CartRepository();

        productId = getIntent().getStringExtra(Router.EXTRA_PRODUCT_ID);
        if (productId == null) productId = "prod_1";

        ivDetailImage = findViewById(R.id.iv_detail_image);
        tvTitle = findViewById(R.id.tv_detail_title);
        tvPrice = findViewById(R.id.tv_detail_price);
        tvStock = findViewById(R.id.tv_detail_stock);
        tvStatus = findViewById(R.id.tv_detail_status);
        tvSeller = findViewById(R.id.tv_detail_seller);
        tvSellerCity = findViewById(R.id.tv_seller_city);
        tvDescription = findViewById(R.id.tv_detail_description);
        tv3dBadge = findViewById(R.id.tv_detail_3d_badge);
        btnViewAr = findViewById(R.id.btn_view_ar);
        btnScan3d = findViewById(R.id.btn_scan_3d);
        btnVisitBoutique = findViewById(R.id.btn_visit_boutique);

        btnActionChat = findViewById(R.id.btn_action_chat);
        btnActionAddCart = findViewById(R.id.btn_action_add_cart);
        btnActionCheckout = findViewById(R.id.btn_action_checkout);

        btnViewAr.setOnClickListener(v -> {
            if (currentProduct != null) {
                Router.openArViewer(this, currentProduct.getId(), currentProduct.getTitle(), currentProduct.getModel3dUrl());
            }
        });

        btnScan3d.setOnClickListener(v -> {
            if (currentProduct != null) {
                Router.openScanner(this, currentProduct.getId(), currentProduct.getCategoryId());
            }
        });

        btnVisitBoutique.setOnClickListener(v -> {
            if (currentProduct != null && currentProduct.getSellerId() != null) {
                Router.openBoutique(this, currentProduct.getSellerId());
            }
        });

        btnActionChat.setOnClickListener(v -> {
            if (currentProduct != null) {
                ContactBottomSheet bottomSheet = ContactBottomSheet.newInstance(currentProduct, currentSeller);
                bottomSheet.show(getSupportFragmentManager(), "ContactBottomSheet");
            }
        });

        btnActionAddCart.setOnClickListener(v -> addToCart(false));
        btnActionCheckout.setOnClickListener(v -> addToCart(true));
    }

    @Override
    protected void loadData() {
        productRepository.getById(productId, new BaseRepository.Callback<Product>() {
            @Override
            public void onSuccess(Product product) {
                currentProduct = product;
                render(product);
                loadSeller(product.getSellerId());
            }

            @Override
            public void onError(String message) {
                showError("Erreur : " + message, ProductDetailActivity.this::loadData);
            }
        });
    }

    private void loadSeller(String sellerId) {
        if (sellerId == null) return;
        userRepository.getById(sellerId, new BaseRepository.Callback<User>() {
            @Override
            public void onSuccess(User user) {
                currentSeller = user;
                if (tvSellerCity != null && user.getCity() != null) {
                    tvSellerCity.setText(user.getCity());
                }
            }

            @Override
            public void onError(String message) {}
        });
    }

    @Override
    protected void render(Object data) {
        if (data instanceof Product) {
            Product p = (Product) data;
            tvTitle.setText(p.getTitle());
            tvPrice.setText(PriceFormatter.format(p.getPrice()));
            tvDescription.setText(p.getDescription());
            tvStock.setText("Stock : " + p.getStock() + " unités");
            tvSeller.setText("Boutique : " + (p.getSellerName() != null ? p.getSellerName() : "Vendeur"));

            if (p.getStatus() != null) {
                tvStatus.setText(p.getStatus().name());
            }

            if (p.isArSupported() && p.getModel3dUrl() != null && !p.getModel3dUrl().isEmpty()) {
                tv3dBadge.setVisibility(View.VISIBLE);
                btnViewAr.setVisibility(View.VISIBLE);
            } else {
                tv3dBadge.setVisibility(View.GONE);
                btnViewAr.setVisibility(View.GONE);
            }

            Glide.with(this)
                    .load(p.getThumbnailUrl())
                    .placeholder(android.R.drawable.ic_menu_gallery)
                    .into(ivDetailImage);
        }
    }

    private void addToCart(boolean openCartImmediately) {
        if (!requireLogin(null)) return;
        if (currentProduct == null) return;

        cartRepository.addItem(currentProduct.getId(), 1, new BaseRepository.Callback<Cart>() {
            @Override
            public void onSuccess(Cart result) {
                toast("Produit ajouté au panier !");
                if (openCartImmediately) {
                    Router.openCart(ProductDetailActivity.this);
                }
            }

            @Override
            public void onError(String message) {
                showError(message, null);
            }
        });
    }
}
