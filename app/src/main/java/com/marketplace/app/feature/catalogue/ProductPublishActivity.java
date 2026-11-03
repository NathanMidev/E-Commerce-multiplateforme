package com.marketplace.app.feature.catalogue;

import android.database.Cursor;
import android.net.Uri;
import android.provider.OpenableColumns;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;

import com.bumptech.glide.Glide;
import com.marketplace.app.R;
import com.marketplace.app.backend.AuthSession;
import com.marketplace.app.backend.repository.ProductRepository;
import com.marketplace.app.base.BaseActivity;
import com.marketplace.app.base.BaseRepository;
import com.marketplace.app.feature.ar.GlbModelCache;
import com.marketplace.app.feature.ar.GlbValidator;
import com.marketplace.app.feature.ar.Interactive3dModelView;
import com.marketplace.app.feature.ar.ProductMediaFirebaseService;
import com.marketplace.app.model.Category;
import com.marketplace.app.model.Product;
import com.marketplace.app.model.ProductStatus;
import com.marketplace.app.navigation.Router;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Rôle : Formulaire de publication et de modification d'un produit de la marketplace locale (Section 3).
 * Permet au vendeur de :
 * - Ajouter plusieurs images classiques (depuis le téléphone ou par URL)
 * - Sélectionner un fichier .glb depuis son téléphone (avec validation stricte d'extension, de taille et d'entête glTF)
 * - Voir le nom et la taille du fichier .glb
 * - Prévisualiser le modèle 3D avant publication (rotation & zoom)
 * - Remplacer ou supprimer le modèle .glb
 * - Publier le produit avec ou sans modèle 3D.
 */
public class ProductPublishActivity extends BaseActivity {

    private TextView tvHeaderTitle;
    private ImageView ivPreview;
    private View layoutAddPhotoPrompt;
    private TextView tvImagesCount;
    private EditText etImageUrl;

    private Button btnSelectGlb;
    private TextView tvGlbError;
    private View layoutSelectedGlbCard;
    private TextView tvGlbFileName;
    private TextView tvGlbFileSize;
    private Interactive3dModelView preview3dPublishView;
    private ProgressBar progressGlbUpload;
    private TextView tvGlbUploadProgress;

    private EditText etTitle;
    private Spinner spinnerCategory;
    private EditText etDescription;
    private EditText etPrice;
    private EditText etStock;
    private Button btnSubmit;

    private ProductRepository productRepository;
    private ProductMediaFirebaseService mediaFirebaseService;
    private final List<Category> categories = new ArrayList<>();

    // État édition / médias sélectionnés
    private String editingProductId;
    private Product existingProduct;
    private final List<String> selectedImageUrls = new ArrayList<>();
    private final List<Uri> selectedLocalImageUris = new ArrayList<>();

    private Uri selectedGlbUri = null;
    private String selectedGlbFileName = null;
    private long selectedGlbFileSize = 0L;
    private byte[] selectedGlbBytes = null;
    private boolean removeExistingGlbRequested = false;

    private ActivityResultLauncher<String> pickMultipleImagesLauncher;
    private ActivityResultLauncher<String[]> pickGlbDocumentLauncher;

    @Override
    protected int getLayoutId() {
        return R.layout.activity_product_publish;
    }

    @Override
    protected void bindViews() {
        productRepository = new ProductRepository();
        mediaFirebaseService = new ProductMediaFirebaseService();
        editingProductId = getIntent().getStringExtra(Router.EXTRA_PRODUCT_ID);

        tvHeaderTitle = findViewById(R.id.tv_publish_header_title);
        ivPreview = findViewById(R.id.iv_publish_preview);
        layoutAddPhotoPrompt = findViewById(R.id.layout_add_photo_prompt);
        tvImagesCount = findViewById(R.id.tv_images_count);
        etImageUrl = findViewById(R.id.et_publish_image_url);

        btnSelectGlb = findViewById(R.id.btn_select_glb);
        tvGlbError = findViewById(R.id.tv_glb_error);
        layoutSelectedGlbCard = findViewById(R.id.layout_selected_glb_card);
        tvGlbFileName = findViewById(R.id.tv_glb_file_name);
        tvGlbFileSize = findViewById(R.id.tv_glb_file_size);
        preview3dPublishView = findViewById(R.id.preview_3d_publish_view);
        progressGlbUpload = findViewById(R.id.progress_glb_upload);
        tvGlbUploadProgress = findViewById(R.id.tv_glb_upload_progress);

        etTitle = findViewById(R.id.et_publish_title);
        spinnerCategory = findViewById(R.id.spinner_publish_category);
        etDescription = findViewById(R.id.et_publish_description);
        etPrice = findViewById(R.id.et_publish_price);
        etStock = findViewById(R.id.et_publish_stock);
        btnSubmit = findViewById(R.id.btn_submit_publish);

        registerFilePickers();

        findViewById(R.id.frame_photo_preview).setOnClickListener(v -> pickMultipleImagesLauncher.launch("image/*"));
        findViewById(R.id.btn_pick_images).setOnClickListener(v -> pickMultipleImagesLauncher.launch("image/*"));
        findViewById(R.id.btn_clear_images).setOnClickListener(v -> {
            selectedImageUrls.clear();
            selectedLocalImageUris.clear();
            refreshImagesUi();
        });
        findViewById(R.id.btn_add_image_url).setOnClickListener(v -> addTypedImageUrl());

        btnSelectGlb.setOnClickListener(v -> launchGlbPicker());
        findViewById(R.id.btn_replace_glb).setOnClickListener(v -> launchGlbPicker());
        findViewById(R.id.btn_remove_glb).setOnClickListener(v -> removeSelectedOrExistingGlb());

        btnSubmit.setOnClickListener(v -> submitProduct());
    }

    private void registerFilePickers() {
        pickMultipleImagesLauncher = registerForActivityResult(
                new ActivityResultContracts.GetMultipleContents(),
                uris -> {
                    if (uris != null && !uris.isEmpty()) {
                        selectedLocalImageUris.addAll(uris);
                        for (Uri u : uris) {
                            String s = u.toString();
                            if (!selectedImageUrls.contains(s)) {
                                selectedImageUrls.add(s);
                            }
                        }
                        refreshImagesUi();
                    }
                }
        );

        pickGlbDocumentLauncher = registerForActivityResult(
                new ActivityResultContracts.OpenDocument(),
                uri -> {
                    if (uri != null) {
                        handlePickedGlbUri(uri);
                    }
                }
        );
    }

    private void launchGlbPicker() {
        tvGlbError.setVisibility(View.GONE);
        pickGlbDocumentLauncher.launch(new String[]{"*/*"});
    }

    private void addTypedImageUrl() {
        String url = etImageUrl.getText().toString().trim();
        if (url.isEmpty()) {
            toast("Veuillez saisir une URL d'image");
            return;
        }
        if (!selectedImageUrls.contains(url)) {
            selectedImageUrls.add(url);
        }
        etImageUrl.setText("");
        refreshImagesUi();
    }

    private void refreshImagesUi() {
        int total = selectedImageUrls.size();
        if (total == 0) {
            tvImagesCount.setText("Aucune photo sélectionnée");
            ivPreview.setImageDrawable(null);
            layoutAddPhotoPrompt.setVisibility(View.VISIBLE);
        } else {
            tvImagesCount.setText(total + " photo(s) prête(s) pour la galerie du produit");
            layoutAddPhotoPrompt.setVisibility(View.GONE);
            Glide.with(this)
                    .load(selectedImageUrls.get(0))
                    .placeholder(android.R.drawable.ic_menu_gallery)
                    .into(ivPreview);
        }
    }

    private void handlePickedGlbUri(Uri uri) {
        String displayName = queryFileName(uri);
        long size = queryFileSize(uri);

        try (InputStream is = getContentResolver().openInputStream(uri)) {
            if (is == null) {
                showGlbValidationError("Impossible de lire le fichier sélectionné");
                return;
            }
            byte[] fileBytes = readUpToMaxBytes(is, GlbValidator.getMaxGlbSizeBytes() + 1024);
            long actualSize = size > 0 ? size : fileBytes.length;

            String validationError = GlbValidator.validateGlbHeader(displayName, actualSize, fileBytes);
            if (validationError != null) {
                showGlbValidationError(validationError);
                return;
            }

            this.selectedGlbUri = uri;
            this.selectedGlbFileName = displayName;
            this.selectedGlbFileSize = actualSize;
            this.selectedGlbBytes = fileBytes;
            this.removeExistingGlbRequested = false;

            tvGlbError.setVisibility(View.GONE);
            layoutSelectedGlbCard.setVisibility(View.VISIBLE);
            tvGlbFileName.setText("Fichier : " + displayName);
            tvGlbFileSize.setText("Taille : " + GlbValidator.formatFileSize(actualSize) + " • Format .glb vérifié");
            btnSelectGlb.setText("📦 Remplacer le fichier .glb sélectionné");

            GlbModelCache.SharedGlbResource previewRes = GlbModelCache.getInstance()
                    .loadFromBytes("preview://" + displayName, fileBytes);
            preview3dPublishView.setModelResource(previewRes);
            toast("Modèle 3D .glb validé et prêt pour la prévisualisation");
        } catch (Exception e) {
            showGlbValidationError("Erreur lecture du fichier .glb : " + e.getMessage());
        }
    }

    private void showGlbValidationError(String errorMsg) {
        tvGlbError.setText(errorMsg);
        tvGlbError.setVisibility(View.VISIBLE);
        toast(errorMsg);
    }

    private void removeSelectedOrExistingGlb() {
        selectedGlbUri = null;
        selectedGlbFileName = null;
        selectedGlbFileSize = 0L;
        selectedGlbBytes = null;
        removeExistingGlbRequested = true;
        preview3dPublishView.releaseResources();
        layoutSelectedGlbCard.setVisibility(View.GONE);
        tvGlbError.setVisibility(View.GONE);
        btnSelectGlb.setText("📦 Sélectionner un fichier .glb depuis le téléphone");
        toast("Modèle 3D retiré : le produit sera enregistré uniquement avec ses photos classiques");
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

                if (editingProductId != null && !editingProductId.isEmpty()) {
                    loadExistingProductForEdit(editingProductId);
                }
            }

            @Override
            public void onError(String message) {}
        });
    }

    private void loadExistingProductForEdit(String productId) {
        productRepository.getById(productId, new BaseRepository.Callback<Product>() {
            @Override
            public void onSuccess(Product product) {
                existingProduct = product;
                tvHeaderTitle.setText("Modifier le produit local");
                btnSubmit.setText("Enregistrer les modifications");

                etTitle.setText(product.getTitle());
                etDescription.setText(product.getDescription());
                etPrice.setText(String.valueOf(Math.round(product.getPrice())));
                etStock.setText(String.valueOf(product.getStock()));

                selectedImageUrls.clear();
                if (product.getImageUrls() != null) {
                    selectedImageUrls.addAll(product.getImageUrls());
                }
                refreshImagesUi();

                if (product.hasValidGlbModel()) {
                    layoutSelectedGlbCard.setVisibility(View.VISIBLE);
                    tvGlbFileName.setText("Modèle actuel : model.glb");
                    String sz = product.getModel3dSize() > 0
                            ? GlbValidator.formatFileSize(product.getModel3dSize())
                            : "Hébergé sur Firebase Storage";
                    tvGlbFileSize.setText("Taille : " + sz + " • Actif");
                    GlbModelCache.getInstance().loadModelAsync(
                            ProductPublishActivity.this,
                            product.getModel3dUrl(),
                            new GlbModelCache.LoadCallback() {
                                @Override public void onProgress(int percent) {}
                                @Override
                                public void onLoaded(GlbModelCache.SharedGlbResource resource) {
                                    preview3dPublishView.setModelResource(resource);
                                }
                                @Override public void onError(String errorMessage) {}
                            }
                    );
                }
            }

            @Override
            public void onError(String message) {
                toast("Impossible de charger le produit : " + message);
            }
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
        String pendingTypedUrl = etImageUrl.getText().toString().trim();
        if (!pendingTypedUrl.isEmpty() && !selectedImageUrls.contains(pendingTypedUrl)) {
            selectedImageUrls.add(pendingTypedUrl);
        }

        if (title.isEmpty()) {
            toast("Veuillez renseigner un titre pour votre annonce");
            return;
        }

        double price;
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

        Product p = existingProduct != null ? existingProduct : new Product();
        p.setTitle(title);
        p.setDescription(desc);
        p.setPrice(price);
        p.setStock(stock);
        p.setCategoryId(getSelectedCategoryId());
        if (p.getSellerId() == null || p.getSellerId().isEmpty()) {
            p.setSellerId(AuthSession.getInstance().getUserId());
        }
        p.setStatus(ProductStatus.DISPONIBLE);
        p.setImageUrls(new ArrayList<>(selectedImageUrls));
        if (!selectedImageUrls.isEmpty()) {
            p.setThumbnailUrl(selectedImageUrls.get(0));
        }

        BaseRepository.Callback<Product> afterProductSaved = new BaseRepository.Callback<Product>() {
            @Override
            public void onSuccess(Product savedProduct) {
                handleMediaUploadsAfterSave(savedProduct);
            }

            @Override
            public void onError(String message) {
                btnSubmit.setEnabled(true);
                showError("Erreur lors de l'enregistrement du produit : " + message, null);
            }
        };

        if (existingProduct != null && existingProduct.getId() != null) {
            productRepository.update(p, afterProductSaved);
        } else {
            productRepository.create(p, afterProductSaved);
        }
    }

    private void handleMediaUploadsAfterSave(Product savedProduct) {
        if (savedProduct == null) {
            btnSubmit.setEnabled(true);
            return;
        }

        if (!selectedLocalImageUris.isEmpty()) {
            progressGlbUpload.setVisibility(View.VISIBLE);
            tvGlbUploadProgress.setVisibility(View.VISIBLE);
            tvGlbUploadProgress.setText("Upload des photos vers Firebase Storage...");
            mediaFirebaseService.uploadProductImages(
                    savedProduct,
                    new ArrayList<>(selectedLocalImageUris),
                    new ProductMediaFirebaseService.ImageUploadCallback() {
                        @Override
                        public void onProgress(int percent) {
                            progressGlbUpload.setProgress(percent);
                            tvGlbUploadProgress.setText("Upload photos Firebase Storage : " + percent + "%");
                        }

                        @Override
                        public void onSuccess(List<String> uploadedImageUrls) {
                            selectedLocalImageUris.clear();
                            progressGlbUpload.setVisibility(View.GONE);
                            tvGlbUploadProgress.setVisibility(View.GONE);
                            handleGlbUploadStep(savedProduct);
                        }

                        @Override
                        public void onError(String message) {
                            selectedLocalImageUris.clear();
                            progressGlbUpload.setVisibility(View.GONE);
                            tvGlbUploadProgress.setVisibility(View.GONE);
                            handleGlbUploadStep(savedProduct);
                        }
                    }
            );
            return;
        }

        handleGlbUploadStep(savedProduct);
    }

    private void handleGlbUploadStep(Product savedProduct) {
        // Cas 1 : Suppression demandée d'un modèle GLB existant
        if (removeExistingGlbRequested && selectedGlbBytes == null) {
            mediaFirebaseService.deleteGlbModel(savedProduct, new ProductMediaFirebaseService.MediaDeleteCallback() {
                @Override
                public void onSuccess() {
                    finalizePublication(savedProduct);
                }

                @Override
                public void onError(String message) {
                    btnSubmit.setEnabled(true);
                    showError(message, null);
                }
            });
            return;
        }

        // Cas 2 : Upload ou remplacement d'un fichier .glb sélectionné
        if (selectedGlbBytes != null && selectedGlbFileName != null) {
            progressGlbUpload.setVisibility(View.VISIBLE);
            tvGlbUploadProgress.setVisibility(View.VISIBLE);
            mediaFirebaseService.uploadOrReplaceGlbModel(
                    savedProduct,
                    selectedGlbFileName,
                    selectedGlbFileSize,
                    selectedGlbBytes,
                    selectedGlbUri,
                    false,
                    new ProductMediaFirebaseService.GlbUploadCallback() {
                        @Override
                        public void onProgress(int percent) {
                            progressGlbUpload.setProgress(percent);
                            tvGlbUploadProgress.setText("Upload Firebase Storage (.glb) : " + percent + "%");
                        }

                        @Override
                        public void onSuccess(String model3dUrl, long model3dSize, Map<String, Object> firestoreMetadata) {
                            progressGlbUpload.setVisibility(View.GONE);
                            tvGlbUploadProgress.setVisibility(View.GONE);
                            finalizePublication(savedProduct);
                        }

                        @Override
                        public void onError(String message) {
                            progressGlbUpload.setVisibility(View.GONE);
                            tvGlbUploadProgress.setVisibility(View.GONE);
                            btnSubmit.setEnabled(true);
                            showGlbValidationError("Échec upload .glb : " + message);
                        }
                    }
            );
            return;
        }

        // Cas 3 : Publication avec images uniquement (sans modèle 3D)
        finalizePublication(savedProduct);
    }

    private void finalizePublication(Product savedProduct) {
        btnSubmit.setEnabled(true);
        toast("Produit enregistré avec succès !");
        Router.openProductDetail(ProductPublishActivity.this, savedProduct.getId());
        finish();
    }

    private String queryFileName(Uri uri) {
        String result = null;
        if ("content".equalsIgnoreCase(uri.getScheme())) {
            try (Cursor cursor = getContentResolver().query(uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    int idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                    if (idx >= 0) result = cursor.getString(idx);
                }
            } catch (Exception ignored) {}
        }
        if (result == null) {
            result = uri.getLastPathSegment();
        }
        return result != null ? result : "model.glb";
    }

    private long queryFileSize(Uri uri) {
        if ("content".equalsIgnoreCase(uri.getScheme())) {
            try (Cursor cursor = getContentResolver().query(uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    int idx = cursor.getColumnIndex(OpenableColumns.SIZE);
                    if (idx >= 0 && !cursor.isNull(idx)) {
                        return cursor.getLong(idx);
                    }
                }
            } catch (Exception ignored) {}
        }
        return -1L;
    }

    private static byte[] readUpToMaxBytes(InputStream is, long maxBytes) throws Exception {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        byte[] buf = new byte[8192];
        long total = 0;
        int n;
        while ((n = is.read(buf)) != -1) {
            total += n;
            bos.write(buf, 0, n);
            if (total > maxBytes) break;
        }
        return bos.toByteArray();
    }

    @Override
    protected void onDestroy() {
        if (preview3dPublishView != null) {
            preview3dPublishView.releaseResources();
        }
        super.onDestroy();
    }
}
