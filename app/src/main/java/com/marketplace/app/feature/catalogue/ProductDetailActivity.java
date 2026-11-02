package com.marketplace.app.feature.catalogue;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.marketplace.app.R;
import com.marketplace.app.backend.AuthSession;
import com.marketplace.app.backend.repository.CartRepository;
import com.marketplace.app.backend.repository.ProductRepository;
import com.marketplace.app.backend.repository.UserRepository;
import com.marketplace.app.base.BaseActivity;
import com.marketplace.app.base.BaseRepository;
import com.marketplace.app.feature.ar.ArLauncher;
import com.marketplace.app.feature.ar.GlbModelCache;
import com.marketplace.app.feature.ar.GlbValidator;
import com.marketplace.app.feature.ar.Interactive3dModelView;
import com.marketplace.app.feature.contact.ContactBottomSheet;
import com.marketplace.app.feature.contact.ContactLinks;
import com.marketplace.app.feature.import1688.AdminAccessControl;
import com.marketplace.app.model.Cart;
import com.marketplace.app.model.Product;
import com.marketplace.app.model.User;
import com.marketplace.app.navigation.Router;
import com.marketplace.app.ui.PriceFormatter;

import java.util.List;

/**
 * Rôle : Fiche détaillée d'un produit de la marketplace locale (Sections 5 & 6).
 * Fonctionnalités :
 * - Galerie d'images classiques (miniatures cliquables)
 * - Si le produit possède un modèle .glb valide :
 *   - Petit indicateur « 3D »
 *   - Visualiseur 3D interactif (rotation 360° et zoom)
 *   - Petite icône AR en haut à droite du visualiseur 3D ouvrant l'expérience ARCore
 * - Si le produit n'a pas de modèle .glb :
 *   - Aucun visualiseur 3D vide n'est affiché.
 */
public class ProductDetailActivity extends BaseActivity {

    private ImageView ivDetailImage;
    private View scrollImageThumbnails;
    private LinearLayout layoutImageThumbnails;
    private TextView tv3dBadge;

    private View card3dViewerContainer;
    private Interactive3dModelView interactive3dView;
    private ProgressBar progress3dLoading;
    private TextView tv3dViewerInfo;
    private ImageButton btnArTopRightIcon;

    private ImageView ivSellerAvatar;
    private TextView tvTitle;
    private TextView tvPrice;
    private TextView tvStock;
    private TextView tvStatus;
    private TextView tvSeller;
    private TextView tvSellerCity;
    private TextView tvDescription;
    private Button btnEditProductMedia;
    private Button btnVisitBoutique;
    private Button btnSellerFacebook;

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
        scrollImageThumbnails = findViewById(R.id.scroll_image_thumbnails);
        layoutImageThumbnails = findViewById(R.id.layout_image_thumbnails);
        tv3dBadge = findViewById(R.id.tv_detail_3d_badge);

        card3dViewerContainer = findViewById(R.id.card_3d_viewer_container);
        interactive3dView = findViewById(R.id.view_interactive_3d);
        progress3dLoading = findViewById(R.id.progress_3d_loading);
        tv3dViewerInfo = findViewById(R.id.tv_3d_viewer_info);
        btnArTopRightIcon = findViewById(R.id.btn_ar_top_right_icon);

        ivSellerAvatar = findViewById(R.id.iv_seller_avatar);
        tvTitle = findViewById(R.id.tv_detail_title);
        tvPrice = findViewById(R.id.tv_detail_price);
        tvStock = findViewById(R.id.tv_detail_stock);
        tvStatus = findViewById(R.id.tv_detail_status);
        tvSeller = findViewById(R.id.tv_detail_seller);
        tvSellerCity = findViewById(R.id.tv_seller_city);
        tvDescription = findViewById(R.id.tv_detail_description);
        btnEditProductMedia = findViewById(R.id.btn_edit_product_media);
        btnVisitBoutique = findViewById(R.id.btn_visit_boutique);
        btnSellerFacebook = findViewById(R.id.btn_seller_facebook);

        btnActionChat = findViewById(R.id.btn_action_chat);
        btnActionAddCart = findViewById(R.id.btn_action_add_cart);
        btnActionCheckout = findViewById(R.id.btn_action_checkout);

        // Contrôles du visualiseur 3D
        findViewById(R.id.btn_3d_zoom_in).setOnClickListener(v -> {
            if (interactive3dView != null) {
                interactive3dView.setZoomFactor(interactive3dView.getZoomFactor() + 0.25f);
            }
        });
        findViewById(R.id.btn_3d_zoom_out).setOnClickListener(v -> {
            if (interactive3dView != null) {
                interactive3dView.setZoomFactor(interactive3dView.getZoomFactor() - 0.25f);
            }
        });
        findViewById(R.id.btn_3d_reset_view).setOnClickListener(v -> {
            if (interactive3dView != null) {
                interactive3dView.resetCameraView();
            }
        });

        // Bouton icône AR en haut à droite du visualiseur 3D (Section 6)
        btnArTopRightIcon.setOnClickListener(v -> {
            if (currentProduct != null && currentProduct.hasValidGlbModel()) {
                ArLauncher.launchArExperienceIfCompatible(
                        this,
                        currentProduct.getId(),
                        currentProduct.getTitle(),
                        currentProduct.getModel3dUrl()
                );
            }
        });

        btnEditProductMedia.setOnClickListener(v -> {
            if (currentProduct != null) {
                Router.openProductEdit(this, currentProduct.getId());
            }
        });

        btnVisitBoutique.setOnClickListener(v -> {
            if (currentProduct != null && currentProduct.getSellerId() != null) {
                Router.openBoutique(this, currentProduct.getSellerId());
            }
        });

        if (btnSellerFacebook != null) {
            btnSellerFacebook.setOnClickListener(v -> {
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
    protected void onResume() {
        super.onResume();
        loadData();
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
                if (tvSeller != null && user != null) {
                    tvSeller.setText("Boutique : " + user.getFormattedSellerIdentity());
                }
                if (tvSellerCity != null && user != null && user.getCity() != null) {
                    tvSellerCity.setText("📍 " + user.getCity());
                }
                if (ivSellerAvatar != null && user != null) {
                    if (user.hasCustomAvatar()) {
                        Glide.with(ProductDetailActivity.this)
                                .load(user.getAvatarUrl())
                                .placeholder(R.drawable.ic_default_avatar)
                                .error(R.drawable.ic_default_avatar)
                                .circleCrop()
                                .into(ivSellerAvatar);
                    } else {
                        ivSellerAvatar.setImageResource(R.drawable.ic_default_avatar);
                    }
                }
                if (btnSellerFacebook != null && user != null) {
                    btnSellerFacebook.setVisibility(user.hasFacebookProfile() ? View.VISIBLE : View.GONE);
                }
            }

            @Override
            public void onError(String message) {}
        });
    }

    @Override
    protected void render(Object data) {
        if (!(data instanceof Product)) return;
        Product p = (Product) data;

        tvTitle.setText(p.getTitle());
        tvPrice.setText(PriceFormatter.format(p.getPrice()));
        tvDescription.setText(p.getDescription());
        tvStock.setText("Stock : " + p.getStock() + " unités");
        tvSeller.setText("Boutique : " + (p.getSellerName() != null ? p.getSellerName() : "Vendeur"));

        if (p.getStatus() != null) {
            tvStatus.setText(p.getStatus().name());
        }

        // Bouton d'édition visible pour le vendeur propriétaire ou un admin
        String currentUid = AuthSession.getInstance().getUserId();
        boolean isOwner = currentUid != null && currentUid.equals(p.getSellerId());
        btnEditProductMedia.setVisibility((isOwner || AdminAccessControl.isCurrentUserAdmin()) ? View.VISIBLE : View.GONE);

        // Galerie d'images classiques
        renderImageGallery(p);

        // Visualiseur 3D interactif et bouton AR en haut à droite (Sections 5 & 6)
        if (p.hasValidGlbModel()) {
            tv3dBadge.setText("3D");
            tv3dBadge.setVisibility(View.VISIBLE);
            card3dViewerContainer.setVisibility(View.VISIBLE);
            btnArTopRightIcon.setVisibility(View.VISIBLE);

            String sizeInfo = p.getModel3dSize() > 0
                    ? " (" + GlbValidator.formatFileSize(p.getModel3dSize()) + ")"
                    : "";
            tv3dViewerInfo.setText("Modèle 3D (.glb)" + sizeInfo + " • Rotation & Zoom");

            progress3dLoading.setVisibility(View.VISIBLE);
            GlbModelCache.getInstance().loadModelAsync(this, p.getModel3dUrl(), new GlbModelCache.LoadCallback() {
                @Override
                public void onProgress(int percent) {
                    progress3dLoading.setVisibility(percent < 100 ? View.VISIBLE : View.GONE);
                }

                @Override
                public void onLoaded(GlbModelCache.SharedGlbResource resource) {
                    progress3dLoading.setVisibility(View.GONE);
                    interactive3dView.setModelResource(resource);
                }

                @Override
                public void onError(String errorMessage) {
                    progress3dLoading.setVisibility(View.GONE);
                    tv3dViewerInfo.setText("Erreur chargement .glb : " + errorMessage);
                    btnArTopRightIcon.setVisibility(View.GONE);
                }
            });
        } else {
            // Produit sans modèle GLB : ne jamais afficher de visualiseur 3D vide
            tv3dBadge.setVisibility(View.GONE);
            card3dViewerContainer.setVisibility(View.GONE);
            btnArTopRightIcon.setVisibility(View.GONE);
            if (interactive3dView != null) {
                interactive3dView.releaseResources();
            }
        }
    }

    private void renderImageGallery(Product p) {
        List<String> images = p.getImageUrls();
        String mainUrl = p.getThumbnailUrl();
        Glide.with(this)
                .load(mainUrl)
                .placeholder(android.R.drawable.ic_menu_gallery)
                .into(ivDetailImage);

        if (layoutImageThumbnails == null || scrollImageThumbnails == null) return;
        layoutImageThumbnails.removeAllViews();

        if (images != null && images.size() > 1) {
            scrollImageThumbnails.setVisibility(View.VISIBLE);
            int sizePx = (int) (getResources().getDisplayMetrics().density * 62);
            int marginPx = (int) (getResources().getDisplayMetrics().density * 8);

            for (String imgUrl : images) {
                ImageView thumb = new ImageView(this);
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(sizePx, sizePx);
                lp.setMarginEnd(marginPx);
                thumb.setLayoutParams(lp);
                thumb.setScaleType(ImageView.ScaleType.CENTER_CROP);
                thumb.setBackgroundResource(R.drawable.bg_search_pill);
                Glide.with(this)
                        .load(imgUrl)
                        .placeholder(android.R.drawable.ic_menu_gallery)
                        .into(thumb);
                thumb.setOnClickListener(v -> Glide.with(this)
                        .load(imgUrl)
                        .placeholder(android.R.drawable.ic_menu_gallery)
                        .into(ivDetailImage));
                layoutImageThumbnails.addView(thumb);
            }
        } else {
            scrollImageThumbnails.setVisibility(View.GONE);
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

    @Override
    protected void onDestroy() {
        if (interactive3dView != null) {
            interactive3dView.releaseResources();
        }
        super.onDestroy();
    }
}
