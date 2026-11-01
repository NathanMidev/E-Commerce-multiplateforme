package com.marketplace.app.feature.catalogue;

import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;

import com.bumptech.glide.Glide;
import com.marketplace.app.R;
import com.marketplace.app.backend.AuthSession;
import com.marketplace.app.backend.repository.ProductRepository;
import com.marketplace.app.base.BaseActivity;
import com.marketplace.app.base.BaseRepository;
import com.marketplace.app.model.Category;
import com.marketplace.app.model.Product;
import com.marketplace.app.model.ProductStatus;
import com.marketplace.app.navigation.Router;

import java.util.ArrayList;
import java.util.List;

/**
 * Rôle : Formulaire de dépôt d'annonce / publication de produit inspiré de Deal.mg.
 * Entrée : Photos, titre, catégorie, prix en Ariary, stock, description et option 3D SAM.
 * Sortie : Enregistrement de l'article sur l'API ou le mock backend et mise en ligne immédiate.
 */
public class ProductPublishActivity extends BaseActivity {

    private ImageView ivPreview;
    private View layoutAddPhotoPrompt;
    private EditText etImageUrl;
    private EditText etTitle;
    private Spinner spinnerCategory;
    private EditText etDescription;
    private EditText etPrice;
    private EditText etStock;
    private CheckBox cbEnableAr;
    private Button btnScan3d;
    private Button btnSubmit;

    private ProductRepository productRepository;
    private final List<Category> categories = new ArrayList<>();

    @Override
    protected int getLayoutId() {
        return R.layout.activity_product_publish;
    }

    @Override
    protected void bindViews() {
        productRepository = new ProductRepository();

        ivPreview = findViewById(R.id.iv_publish_preview);
        layoutAddPhotoPrompt = findViewById(R.id.layout_add_photo_prompt);
        etImageUrl = findViewById(R.id.et_publish_image_url);
        etTitle = findViewById(R.id.et_publish_title);
        spinnerCategory = findViewById(R.id.spinner_publish_category);
        etDescription = findViewById(R.id.et_publish_description);
        etPrice = findViewById(R.id.et_publish_price);
        etStock = findViewById(R.id.et_publish_stock);
        cbEnableAr = findViewById(R.id.cb_publish_enable_ar);
        btnScan3d = findViewById(R.id.btn_publish_scan_3d);
        btnSubmit = findViewById(R.id.btn_submit_publish);

        btnScan3d.setOnClickListener(v -> {
            String catId = getSelectedCategoryId();
            Router.openScanner(this, "prod_custom_" + System.currentTimeMillis(), catId);
        });

        btnSubmit.setOnClickListener(v -> submitProduct());

        etImageUrl.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) {
                String url = etImageUrl.getText().toString().trim();
                if (!url.isEmpty()) {
                    Glide.with(this).load(url).into(ivPreview);
                    layoutAddPhotoPrompt.setVisibility(View.GONE);
                }
            }
        });
    }

    @Override
    protected void loadData() {
        if (!requireLogin(null)) return;

        productRepository.listCategories(new BaseRepository.Callback<List<Category>>() {
            @Override
            public void onSuccess(List<Category> result) {
                categories.clear();
                if (result != null) categories.addAll(result);

                List<String> names = new ArrayList<>();
                for (Category c : categories) names.add(c.getName());

                ArrayAdapter<String> adapter = new ArrayAdapter<>(
                        ProductPublishActivity.this,
                        android.R.layout.simple_spinner_dropdown_item,
                        names
                );
                spinnerCategory.setAdapter(adapter);
            }

            @Override
            public void onError(String message) {}
        });
    }

    @Override
    protected void render(Object data) {}

    private String getSelectedCategoryId() {
        int pos = spinnerCategory.getSelectedItemPosition();
        if (pos >= 0 && pos < categories.size()) {
            return categories.get(pos).getId();
        }
        return "cat_furniture";
    }

    private void submitProduct() {
        String title = etTitle.getText().toString().trim();
        String desc = etDescription.getText().toString().trim();
        String priceStr = etPrice.getText().toString().trim();
        String stockStr = etStock.getText().toString().trim();
        String imgUrl = etImageUrl.getText().toString().trim();

        if (title.isEmpty()) {
            toast("Veuillez renseigner un titre pour votre annonce");
            return;
        }

        double price = 0;
        try {
            price = Double.parseDouble(priceStr);
        } catch (NumberFormatException e) {
            toast("Veuillez renseigner un prix valide en Ariary");
            return;
        }

        int stock = 1;
        try {
            if (!stockStr.isEmpty()) stock = Integer.parseInt(stockStr);
        } catch (NumberFormatException ignored) {}

        btnSubmit.setEnabled(false);

        Product p = new Product();
        p.setTitle(title);
        p.setDescription(desc);
        p.setPrice(price);
        p.setStock(stock);
        p.setCategoryId(getSelectedCategoryId());
        p.setSellerId(AuthSession.getInstance().getUserId());
        p.setStatus(ProductStatus.DISPONIBLE);
        p.setArSupported(cbEnableAr.isChecked());
        p.setThumbnailUrl(imgUrl.isEmpty() ? "https://images.unsplash.com/photo-1555041469-a586c61ea9bc" : imgUrl);
        if (cbEnableAr.isChecked()) {
            p.setModel3dUrl("https://modelviewer.dev/shared-assets/models/Astronaut.glb");
        }

        productRepository.create(p, new BaseRepository.Callback<Product>() {
            @Override
            public void onSuccess(Product created) {
                btnSubmit.setEnabled(true);
                toast("Votre annonce a été publiée avec succès !");
                Router.openProductDetail(ProductPublishActivity.this, created.getId());
                finish();
            }

            @Override
            public void onError(String message) {
                btnSubmit.setEnabled(true);
                showError("Erreur lors de la publication : " + message, null);
            }
        });
    }
}
