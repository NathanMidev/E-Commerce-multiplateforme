package com.marketplace.app.feature.auth;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.bumptech.glide.Glide;
import com.marketplace.app.BuildConfig;
import com.marketplace.app.R;
import com.marketplace.app.backend.ApiClient;
import com.marketplace.app.backend.AuthSession;
import com.marketplace.app.backend.repository.UserRepository;
import com.marketplace.app.base.BaseActivity;
import com.marketplace.app.base.BaseRepository;
import com.marketplace.app.feature.import1688.AdminAccessControl;
import com.marketplace.app.model.User;
import com.marketplace.app.model.UserRole;
import com.marketplace.app.navigation.Router;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Rôle : Écran Profil BIZINEVO — Espace personnel minimaliste et structuré.
 * Deux sections principales :
 * 1. SECTION A — MON PROFIL :
 *    - En-tête : photo circulaire, nom complet, @username, badge état complétion ("Profil complété" ou "Profil à compléter"), bouton ✏️ Modifier.
 *    - Mode consultation normale : cartes d'informations personnelles (nom, username, email, WhatsApp avec actions directes, Facebook, adresse).
 *    - Mode édition / complétion : formulaire prérempli masqué par défaut, affiché uniquement sur action volontaire ou premier profil incomplet.
 * 2. SECTION B — MON COMPTE :
 *    - Lignes d'accès directes : Mes commandes, Mes demandes 1688, Mes favoris, Aide et FAQ, Paramètres, Déconnexion.
 */
public class ProfileActivity extends BaseActivity {

    // Vues d'état & chargement
    private SwipeRefreshLayout swipeRefresh;
    private View layoutLoading;
    private View layoutError;
    private TextView tvErrorMessage;
    private View btnRetry;
    private View layoutMainContent;

    // En-tête profil
    private ImageButton btnBack;
    private ImageView ivAvatar;
    private TextView tvName;
    private TextView tvUsername;
    private TextView tvRoleBadge;
    private TextView tvStatusBadge;
    private View btnEditProfile;

    // SECTION A : Mode consultation (Profil complété)
    private View layoutDisplayInfo;
    private TextView txtDisplayFullname;
    private TextView txtDisplayUsername;
    private TextView txtDisplayEmail;
    private TextView txtDisplayWhatsapp;
    private View btnActionWhatsapp;
    private View btnActionCall;
    private View layoutDisplayFacebook;
    private TextView txtDisplayFacebook;
    private TextView txtDisplayAddress;

    // SECTION A : Mode édition / complétion
    private View layoutEditForm;
    private TextView tvFormTitle;
    private TextView tvFormSubtitle;
    private View btnSelectAvatar;
    private View btnRemoveAvatar;
    private TextView tvAvatarPreviewBadge;
    private ProgressBar progressAvatarUpload;
    private EditText etDisplayName;
    private EditText etUsername;
    private TextView tvUsernameStatus;
    private Spinner spinnerCountry;
    private TextView tvDialCode;
    private EditText etWhatsapp;
    private TextView tvWhatsappPreview;
    private EditText etFacebook;
    private View btnClearFacebook;
    private EditText etAddress;
    private View btnCancelEdit;
    private View btnSaveProfile;
    private ProgressBar progressSave;

    // SECTION B : Lignes d'accès Mon compte
    private View rowOrders;
    private View rowImport1688;
    private View rowFavorites;
    private View rowFaq;
    private View rowSettings;
    private View rowLogout;
    private View btnPublishShortcut;

    // Services & données
    private UserRepository userRepository;
    private ProfileStorageService profileStorageService;
    private User currentUser = null;

    private final List<PhoneNormalizer.CountryDialOption> countryOptions =
            PhoneNormalizer.getSupportedCountries();
    private PhoneNormalizer.CountryDialOption selectedCountry = countryOptions.get(0);
    private boolean formattingWhatsapp = false;

    private Uri pendingAvatarUri = null;
    private boolean avatarDeletionRequested = false;
    private String currentSavedAvatarUrl = null;
    private boolean isUserInitiatedEditMode = false;

    private final ActivityResultLauncher<String> pickAvatarLauncher =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) {
                    onNewAvatarSelected(uri);
                }
            });

    @Override
    protected int getLayoutId() {
        return R.layout.activity_user_profile;
    }

    @Override
    protected void bindViews() {
        userRepository = new UserRepository();
        profileStorageService = new ProfileStorageService();

        // En-tête et états
        btnBack = findViewById(R.id.btn_profile_back);
        tvRoleBadge = findViewById(R.id.tv_profile_role_badge);
        swipeRefresh = findViewById(R.id.swipe_refresh_profile);
        layoutLoading = findViewById(R.id.layout_profile_loading);
        layoutError = findViewById(R.id.layout_profile_error);
        tvErrorMessage = findViewById(R.id.tv_profile_error_message);
        btnRetry = findViewById(R.id.btn_retry_profile);
        layoutMainContent = findViewById(R.id.layout_profile_main_content);

        // Header profil
        ivAvatar = findViewById(R.id.iv_profile_avatar);
        tvName = findViewById(R.id.tv_profile_name);
        tvUsername = findViewById(R.id.tv_profile_username);
        tvStatusBadge = findViewById(R.id.tv_profile_status_badge);
        btnEditProfile = findViewById(R.id.btn_edit_profile);

        // Consultation
        layoutDisplayInfo = findViewById(R.id.layout_profile_display_info);
        txtDisplayFullname = findViewById(R.id.txt_display_fullname);
        txtDisplayUsername = findViewById(R.id.txt_display_username);
        txtDisplayEmail = findViewById(R.id.txt_display_email);
        txtDisplayWhatsapp = findViewById(R.id.txt_display_whatsapp);
        btnActionWhatsapp = findViewById(R.id.btn_action_whatsapp);
        btnActionCall = findViewById(R.id.btn_action_call);
        layoutDisplayFacebook = findViewById(R.id.layout_display_facebook);
        txtDisplayFacebook = findViewById(R.id.txt_display_facebook);
        txtDisplayAddress = findViewById(R.id.txt_display_address);

        // Formulaire
        layoutEditForm = findViewById(R.id.layout_profile_edit_form);
        tvFormTitle = findViewById(R.id.tv_form_title);
        tvFormSubtitle = findViewById(R.id.tv_form_subtitle);
        btnSelectAvatar = findViewById(R.id.btn_select_avatar);
        btnRemoveAvatar = findViewById(R.id.btn_remove_avatar);
        tvAvatarPreviewBadge = findViewById(R.id.tv_avatar_preview_badge);
        progressAvatarUpload = findViewById(R.id.progress_avatar_upload);
        etDisplayName = findViewById(R.id.et_display_name);
        etUsername = findViewById(R.id.et_username);
        tvUsernameStatus = findViewById(R.id.tv_username_status);
        spinnerCountry = findViewById(R.id.spinner_whatsapp_country);
        tvDialCode = findViewById(R.id.tv_whatsapp_dial_code);
        etWhatsapp = findViewById(R.id.et_whatsapp);
        tvWhatsappPreview = findViewById(R.id.tv_whatsapp_e164_preview);
        etFacebook = findViewById(R.id.et_facebook_url);
        btnClearFacebook = findViewById(R.id.btn_clear_facebook_url);
        etAddress = findViewById(R.id.et_address);
        btnCancelEdit = findViewById(R.id.btn_cancel_edit);
        btnSaveProfile = findViewById(R.id.btn_save_profile);
        progressSave = findViewById(R.id.progress_save_profile);

        // Section B : Mon compte
        rowOrders = findViewById(R.id.row_account_orders);
        rowImport1688 = findViewById(R.id.row_account_import1688);
        rowFavorites = findViewById(R.id.row_account_favorites);
        rowFaq = findViewById(R.id.row_account_faq);
        rowSettings = findViewById(R.id.row_account_settings);
        rowLogout = findViewById(R.id.row_account_logout);
        btnPublishShortcut = findViewById(R.id.btn_profile_publish_shortcut);

        setupCountrySpinner();
        setupInputWatchers();
        setupClickListeners();
    }

    private void setupClickListeners() {
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        if (swipeRefresh != null) {
            swipeRefresh.setColorSchemeResources(R.color.primary);
            swipeRefresh.setOnRefreshListener(() -> loadUserProfile(true));
        }

        if (btnRetry != null) {
            btnRetry.setOnClickListener(v -> loadUserProfile(false));
            btnRetry.setOnLongClickListener(v -> {
                showServerConfigDialog();
                return true;
            });
        }

        // Action sur "Modifier mon profil"
        if (btnEditProfile != null) {
            btnEditProfile.setOnClickListener(v -> switchToEditMode(true));
        }

        // Action sur "Annuler" dans le formulaire
        if (btnCancelEdit != null) {
            btnCancelEdit.setOnClickListener(v -> cancelEditMode());
        }

        // Actions photo
        if (btnSelectAvatar != null) {
            btnSelectAvatar.setOnClickListener(v -> pickAvatarLauncher.launch("image/*"));
        }
        if (ivAvatar != null) {
            ivAvatar.setOnClickListener(v -> {
                if (layoutEditForm != null && layoutEditForm.getVisibility() == View.VISIBLE) {
                    pickAvatarLauncher.launch("image/*");
                }
            });
        }
        if (btnRemoveAvatar != null) {
            btnRemoveAvatar.setOnClickListener(v -> onRemoveAvatarClicked());
        }

        if (btnClearFacebook != null) {
            btnClearFacebook.setOnClickListener(v -> {
                if (etFacebook != null) etFacebook.setText("");
                btnClearFacebook.setVisibility(View.GONE);
            });
        }

        // Sauvegarde formulaire
        if (btnSaveProfile != null) {
            btnSaveProfile.setOnClickListener(v -> performSave());
        }

        // Actions directes de contact
        if (btnActionWhatsapp != null) {
            btnActionWhatsapp.setOnClickListener(v -> openWhatsappDirect());
        }
        if (btnActionCall != null) {
            btnActionCall.setOnClickListener(v -> openCallDirect());
        }
        if (txtDisplayFacebook != null) {
            txtDisplayFacebook.setOnClickListener(v -> openFacebookDirect());
        }

        // SECTION B : Navigation Mon Compte
        if (rowOrders != null) {
            rowOrders.setOnClickListener(v -> Router.openOrders(this));
        }
        if (rowImport1688 != null) {
            rowImport1688.setOnClickListener(v -> Router.openImport1688(this));
        }
        if (rowFavorites != null) {
            rowFavorites.setOnClickListener(v -> {
                Toast.makeText(this, "Catalogue des produits", Toast.LENGTH_SHORT).show();
                Router.openCatalogue(this);
            });
        }
        if (rowFaq != null) {
            rowFaq.setOnClickListener(v -> Router.openFaq(this));
        }
        if (rowSettings != null) {
            rowSettings.setOnClickListener(v -> showSettingsDialog());
        }
        if (rowLogout != null) {
            rowLogout.setOnClickListener(v -> performSignOut());
        }

        if (btnPublishShortcut != null) {
            btnPublishShortcut.setOnClickListener(v -> Router.openProductPublish(this));
        }
    }

    private void setupCountrySpinner() {
        if (spinnerCountry == null) return;
        ArrayAdapter<PhoneNormalizer.CountryDialOption> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                countryOptions
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCountry.setAdapter(adapter);
        spinnerCountry.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedCountry = countryOptions.get(position);
                if (tvDialCode != null) {
                    tvDialCode.setText(selectedCountry.getDialCode());
                }
                if (etWhatsapp != null) {
                    etWhatsapp.setHint(selectedCountry.getExampleHint());
                    reformatCurrentWhatsappInput();
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void setupInputWatchers() {
        if (etUsername != null) {
            etUsername.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    updateUsernameLiveStatus(s.toString());
                }
                @Override
                public void afterTextChanged(Editable s) {}
            });
        }

        if (etWhatsapp != null) {
            etWhatsapp.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {}
                @Override
                public void afterTextChanged(Editable s) {
                    if (formattingWhatsapp) return;
                    reformatCurrentWhatsappInput();
                }
            });
        }

        if (etFacebook != null) {
            etFacebook.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    if (btnClearFacebook != null) {
                        btnClearFacebook.setVisibility(s.length() > 0 ? View.VISIBLE : View.GONE);
                    }
                }
                @Override
                public void afterTextChanged(Editable s) {}
            });
        }
    }

    private void updateUsernameLiveStatus(String rawInput) {
        if (tvUsernameStatus == null) return;
        String trimmed = rawInput != null ? rawInput.trim() : "";
        if (trimmed.isEmpty()) {
            tvUsernameStatus.setText("Identifiant public distinct du nom complet (lettres, chiffres, . et _)");
            tvUsernameStatus.setTextColor(getResources().getColor(R.color.text_secondary, getTheme()));
            return;
        }
        String err = ProfileValidator.validateUsername(trimmed);
        if (err != null) {
            tvUsernameStatus.setText("⚠️ " + err);
            tvUsernameStatus.setTextColor(getResources().getColor(R.color.status_red, getTheme()));
            return;
        }
        String uid = AuthSession.getInstance().getUserId();
        String norm = ProfileValidator.normalizeUsername(trimmed);
        if (!ProfileStorageService.isUsernameAvailable(norm, uid)) {
            tvUsernameStatus.setText("❌ @" + norm + " est déjà utilisé");
            tvUsernameStatus.setTextColor(getResources().getColor(R.color.status_red, getTheme()));
        } else {
            tvUsernameStatus.setText("✅ @" + norm + " est disponible");
            tvUsernameStatus.setTextColor(0xFF059669);
        }
    }

    private void reformatCurrentWhatsappInput() {
        if (etWhatsapp == null || formattingWhatsapp) return;
        String current = etWhatsapp.getText().toString();
        if (current.trim().isEmpty()) {
            if (tvWhatsappPreview != null) {
                tvWhatsappPreview.setText("Utilisé pour WhatsApp et vos appels.");
                tvWhatsappPreview.setTextColor(getResources().getColor(R.color.text_secondary, getTheme()));
            }
            return;
        }
        formattingWhatsapp = true;
        String formatted = PhoneNormalizer.formatNationalDisplay(selectedCountry.getDialCode(), current);
        if (!formatted.equals(current)) {
            etWhatsapp.setText(formatted);
            etWhatsapp.setSelection(formatted.length());
        }
        formattingWhatsapp = false;

        if (tvWhatsappPreview != null) {
            String e164 = PhoneNormalizer.normalizeToE164(selectedCountry.getDialCode(), formatted);
            if (e164 != null) {
                tvWhatsappPreview.setText("✅ Format international : " + e164);
                tvWhatsappPreview.setTextColor(0xFF059669);
            } else {
                tvWhatsappPreview.setText("⚠️ Format attendu : " + selectedCountry.getDialCode() + " " + selectedCountry.getExampleHint());
                tvWhatsappPreview.setTextColor(getResources().getColor(R.color.status_red, getTheme()));
            }
        }
    }

    private void onNewAvatarSelected(Uri uri) {
        pendingAvatarUri = uri;
        avatarDeletionRequested = false;
        if (ivAvatar != null) {
            Glide.with(this)
                    .load(uri)
                    .placeholder(R.drawable.ic_default_avatar)
                    .error(R.drawable.ic_default_avatar)
                    .circleCrop()
                    .into(ivAvatar);
        }
        if (tvAvatarPreviewBadge != null) {
            tvAvatarPreviewBadge.setText("✨ Aperçu de la photo (sera envoyée à l'enregistrement)");
            tvAvatarPreviewBadge.setVisibility(View.VISIBLE);
        }
        if (btnRemoveAvatar != null) {
            btnRemoveAvatar.setVisibility(View.VISIBLE);
        }
    }

    private void onRemoveAvatarClicked() {
        pendingAvatarUri = null;
        avatarDeletionRequested = true;
        if (ivAvatar != null) {
            ivAvatar.setImageResource(R.drawable.ic_default_avatar);
        }
        if (tvAvatarPreviewBadge != null) {
            tvAvatarPreviewBadge.setText("🗑️ La photo sera supprimée lors de l'enregistrement");
            tvAvatarPreviewBadge.setVisibility(View.VISIBLE);
        }
        if (btnRemoveAvatar != null) {
            btnRemoveAvatar.setVisibility(View.GONE);
        }
    }

    @Override
    protected void loadData() {
        loadUserProfile(false);
    }

    /**
     * Récupère le profil réel de l'utilisateur authentifié.
     */
    private void loadUserProfile(boolean isPullToRefresh) {
        if (!requireLogin(null)) return;

        if (!isPullToRefresh && currentUser == null) {
            showLoading(true);
        }

        userRepository.getMyProfile(new BaseRepository.Callback<User>() {
            @Override
            public void onSuccess(User user) {
                if (swipeRefresh != null) swipeRefresh.setRefreshing(false);
                showLoading(false);
                hideError();
                currentUser = user;
                render(user);
            }

            @Override
            public void onError(String message) {
                if (swipeRefresh != null) swipeRefresh.setRefreshing(false);
                showLoading(false);
                if (currentUser != null) {
                    // Si un profil local existe déjà, on le conserve sans bloquer l'écran
                    toast("Connexion réseau instable (données locales conservées)");
                    render(currentUser);
                } else {
                    showErrorState("Impossible de charger votre profil (" + message + ")");
                }
            }
        });
    }

    @Override
    protected void render(Object data) {
        if (!(data instanceof User)) return;
        User user = (User) data;
        currentUser = user;

        // 1. En-tête du profil
        String displayName = user.getName() != null ? user.getName() : "";
        if (tvName != null) {
            tvName.setText(!displayName.isEmpty() ? displayName : "Mon Compte");
        }

        String username = user.getUsername();
        if (tvUsername != null) {
            if (username != null && !username.trim().isEmpty()) {
                tvUsername.setText("@" + username.trim());
            } else {
                tvUsername.setText("@nom_utilisateur_a_definir");
            }
        }

        // Rôle
        if (tvRoleBadge != null) {
            UserRole role = user.resolveRole();
            tvRoleBadge.setText(role != null ? role.name() : "MEMBRE");
        }

        // Photo de profil
        currentSavedAvatarUrl = user.hasCustomAvatar() ? user.getAvatarUrl() : null;
        pendingAvatarUri = null;
        avatarDeletionRequested = false;
        if (tvAvatarPreviewBadge != null) tvAvatarPreviewBadge.setVisibility(View.GONE);

        if (ivAvatar != null) {
            if (currentSavedAvatarUrl != null) {
                Glide.with(this)
                        .load(currentSavedAvatarUrl)
                        .placeholder(R.drawable.ic_default_avatar)
                        .error(R.drawable.ic_default_avatar)
                        .circleCrop()
                        .into(ivAvatar);
                if (btnRemoveAvatar != null) btnRemoveAvatar.setVisibility(View.VISIBLE);
            } else {
                ivAvatar.setImageResource(R.drawable.ic_default_avatar);
                if (btnRemoveAvatar != null) btnRemoveAvatar.setVisibility(View.GONE);
            }
        }

        // Détermination fiable de l'état de complétion
        boolean isComplete = user.isProfileComplete();
        updateCompletionBadge(isComplete);

        // Remplir les données affichées en mode consultation
        bindDisplayInfo(user);

        // Préremplir également les champs du formulaire d'édition
        prefillFormFields(user);

        // GESTION STRICTE DES ÉTATS (Section 5)
        if (!isComplete) {
            // État 1 : Profil non complété -> afficher le formulaire de complétion
            switchToEditMode(false);
        } else if (!isUserInitiatedEditMode) {
            // État 2 : Profil complété -> afficher uniquement la consultation, formulaire masqué
            switchToDisplayMode();
        }
    }

    private void updateCompletionBadge(boolean isComplete) {
        if (tvStatusBadge == null) return;
        if (isComplete) {
            tvStatusBadge.setText("✅ Profil complété");
            tvStatusBadge.setTextColor(0xFF065F46);
            tvStatusBadge.setBackgroundColor(0xFFECFDF5);
        } else {
            tvStatusBadge.setText("⚠️ Profil à compléter");
            tvStatusBadge.setTextColor(0xFF92400E);
            tvStatusBadge.setBackgroundColor(0xFFFEF3C7);
        }
    }

    private void bindDisplayInfo(@NonNull User user) {
        if (txtDisplayFullname != null) {
            String name = user.getName();
            txtDisplayFullname.setText(name != null && !name.trim().isEmpty() ? name : "—");
        }
        if (txtDisplayUsername != null) {
            String uname = user.getUsername();
            txtDisplayUsername.setText(uname != null && !uname.trim().isEmpty() ? "@" + uname.trim() : "Non défini");
        }
        if (txtDisplayEmail != null) {
            String email = user.getEmail();
            txtDisplayEmail.setText(email != null && !email.trim().isEmpty() ? email : "Non renseigné");
        }
        if (txtDisplayWhatsapp != null) {
            String wa = user.getWhatsapp();
            txtDisplayWhatsapp.setText(wa != null && !wa.trim().isEmpty() ? wa : "Non renseigné");
            boolean hasPhone = wa != null && !wa.trim().isEmpty();
            if (btnActionWhatsapp != null) btnActionWhatsapp.setVisibility(hasPhone ? View.VISIBLE : View.GONE);
            if (btnActionCall != null) btnActionCall.setVisibility(hasPhone ? View.VISIBLE : View.GONE);
        }

        // Profil Facebook : ne pas afficher la ligne si vide
        if (layoutDisplayFacebook != null && txtDisplayFacebook != null) {
            String fb = user.getFacebookLink();
            if (fb != null && !fb.trim().isEmpty()) {
                layoutDisplayFacebook.setVisibility(View.VISIBLE);
                txtDisplayFacebook.setText(fb.trim());
            } else {
                layoutDisplayFacebook.setVisibility(View.GONE);
            }
        }

        if (txtDisplayAddress != null) {
            String addr = user.getAddress();
            txtDisplayAddress.setText(addr != null && !addr.trim().isEmpty() ? addr : "Non renseignée");
        }
    }

    private void prefillFormFields(@NonNull User user) {
        String displayName = user.getName() != null ? user.getName() : "";
        if (etDisplayName != null && !"Utilisateur".equalsIgnoreCase(displayName)) {
            etDisplayName.setText(displayName);
        }

        String username = user.getUsername();
        if (etUsername != null && username != null && !username.trim().isEmpty()) {
            etUsername.setText(username.trim());
        }

        String storedWhatsapp = user.getWhatsapp();
        if (storedWhatsapp != null && !storedWhatsapp.trim().isEmpty()) {
            PhoneNormalizer.ParsedPhone parsed = PhoneNormalizer.parseStoredPhone(storedWhatsapp);
            selectedCountry = parsed.getCountry();
            int idx = countryOptions.indexOf(selectedCountry);
            if (idx >= 0 && spinnerCountry != null) {
                spinnerCountry.setSelection(idx);
            }
            if (tvDialCode != null) {
                tvDialCode.setText(selectedCountry.getDialCode());
            }
            if (etWhatsapp != null) {
                formattingWhatsapp = true;
                etWhatsapp.setText(parsed.getFormattedNational());
                formattingWhatsapp = false;
                reformatCurrentWhatsappInput();
            }
        }

        if (etFacebook != null) {
            String fb = user.getFacebookLink();
            etFacebook.setText(fb != null ? fb : "");
            if (btnClearFacebook != null) {
                btnClearFacebook.setVisibility((fb != null && !fb.trim().isEmpty()) ? View.VISIBLE : View.GONE);
            }
        }

        if (etAddress != null && user.getAddress() != null) {
            etAddress.setText(user.getAddress());
        }
    }

    /**
     * Bascule vers l'affichage normal (Mon profil complété) et masque définitivement le formulaire.
     */
    private void switchToDisplayMode() {
        isUserInitiatedEditMode = false;
        if (layoutEditForm != null) layoutEditForm.setVisibility(View.GONE);
        if (layoutDisplayInfo != null) layoutDisplayInfo.setVisibility(View.VISIBLE);
        if (btnEditProfile != null) btnEditProfile.setVisibility(View.VISIBLE);
    }

    /**
     * Bascule vers le formulaire de complétion / modification.
     */
    private void switchToEditMode(boolean userInitiated) {
        isUserInitiatedEditMode = userInitiated;
        if (layoutDisplayInfo != null) layoutDisplayInfo.setVisibility(View.GONE);
        if (layoutEditForm != null) layoutEditForm.setVisibility(View.VISIBLE);
        if (btnEditProfile != null) btnEditProfile.setVisibility(View.GONE);

        if (tvFormTitle != null) {
            tvFormTitle.setText(userInitiated ? "Modifier mes informations" : "Compléter mon profil");
        }
        if (tvFormSubtitle != null) {
            tvFormSubtitle.setText(userInitiated
                    ? "Modifiez vos coordonnées. Cliquez sur Enregistrer pour valider."
                    : "Ces informations sont nécessaires pour publier des annonces et passer des commandes.");
        }

        // Le bouton "Annuler" n'a de sens que si l'utilisateur a déjà un profil et a cliqué volontairement
        if (btnCancelEdit != null) {
            btnCancelEdit.setVisibility(userInitiated && currentUser != null && currentUser.isProfileComplete()
                    ? View.VISIBLE : View.GONE);
        }
    }

    private void cancelEditMode() {
        isUserInitiatedEditMode = false;
        pendingAvatarUri = null;
        avatarDeletionRequested = false;
        if (currentUser != null) {
            render(currentUser);
        } else {
            switchToDisplayMode();
        }
    }

    private void performSave() {
        String displayName = etDisplayName != null ? etDisplayName.getText().toString().trim() : "";
        String rawUsername = etUsername != null ? etUsername.getText().toString().trim() : "";
        String rawNationalWhatsapp = etWhatsapp != null ? etWhatsapp.getText().toString().trim() : "";
        String rawFacebook = etFacebook != null ? etFacebook.getText().toString().trim() : "";
        String address = etAddress != null ? etAddress.getText().toString().trim() : "";

        // 1. Validation du nom complet
        if (displayName.length() < 2) {
            if (etDisplayName != null) etDisplayName.setError("Le nom complet ou nom de boutique est obligatoire");
            toast("Veuillez saisir votre nom complet (min. 2 caractères)");
            return;
        }

        // 2. Validation du nom d'utilisateur (@username)
        String usernameError = ProfileValidator.validateUsername(rawUsername);
        if (usernameError != null) {
            if (etUsername != null) etUsername.setError(usernameError);
            toast(usernameError);
            return;
        }
        String normalizedUsername = ProfileValidator.normalizeUsername(rawUsername);
        String uid = AuthSession.getInstance().getUserId();
        if (!ProfileStorageService.isUsernameAvailable(normalizedUsername, uid)) {
            String msg = "Ce nom d'utilisateur (@" + normalizedUsername + ") est déjà pris";
            if (etUsername != null) etUsername.setError(msg);
            toast(msg);
            return;
        }

        // 3. Validation du numéro WhatsApp international
        String phoneError = PhoneNormalizer.validatePhoneWithMessage(selectedCountry.getDialCode(), rawNationalWhatsapp);
        if (phoneError != null) {
            if (etWhatsapp != null) etWhatsapp.setError(phoneError);
            toast(phoneError);
            return;
        }
        String normalizedE164 = PhoneNormalizer.normalizeToE164(selectedCountry.getDialCode(), rawNationalWhatsapp);

        // 4. Validation du lien Facebook facultatif
        String fbError = ProfileValidator.validateFacebookUrl(rawFacebook);
        if (fbError != null) {
            if (etFacebook != null) etFacebook.setError(fbError);
            toast(fbError);
            return;
        }
        String normalizedFacebook = ProfileValidator.normalizeFacebookUrl(rawFacebook);

        // 5. Validation de l'adresse
        if (address.isEmpty()) {
            if (etAddress != null) etAddress.setError("L'adresse ou ville est obligatoire");
            toast("Veuillez indiquer votre ville et adresse");
            return;
        }

        setSavingState(true);

        // 6. Téléversement éventuel de la photo ou suppression
        if (pendingAvatarUri != null) {
            if (progressAvatarUpload != null) {
                progressAvatarUpload.setProgress(0);
                progressAvatarUpload.setVisibility(View.VISIBLE);
            }
            profileStorageService.uploadOrReplaceProfilePhoto(uid, pendingAvatarUri, null,
                    new ProfileStorageService.PhotoUploadCallback() {
                        @Override
                        public void onProgress(int percent) {
                            if (progressAvatarUpload != null) progressAvatarUpload.setProgress(percent);
                        }

                        @Override
                        public void onSuccess(String photoUrl) {
                            if (progressAvatarUpload != null) progressAvatarUpload.setVisibility(View.GONE);
                            saveProfileFields(displayName, normalizedUsername, normalizedE164, normalizedFacebook, address, photoUrl);
                        }

                        @Override
                        public void onError(String message) {
                            if (progressAvatarUpload != null) progressAvatarUpload.setVisibility(View.GONE);
                            setSavingState(false);
                            toast("Erreur photo : " + message);
                        }
                    });
        } else if (avatarDeletionRequested) {
            profileStorageService.deleteProfilePhoto(uid, new ProfileStorageService.PhotoDeleteCallback() {
                @Override
                public void onSuccess() {
                    saveProfileFields(displayName, normalizedUsername, normalizedE164, normalizedFacebook, address, "");
                }

                @Override
                public void onError(String message) {
                    setSavingState(false);
                    toast("Erreur suppression photo : " + message);
                }
            });
        } else {
            saveProfileFields(displayName, normalizedUsername, normalizedE164, normalizedFacebook, address, null);
        }
    }

    private void saveProfileFields(
            String displayName,
            String username,
            String whatsappE164,
            String facebookUrl,
            String address,
            String updatedAvatarUrl
    ) {
        Map<String, String> fields = new HashMap<>();
        fields.put("name", displayName);
        fields.put("username", username);
        fields.put("dialCode", selectedCountry.getDialCode());
        fields.put("whatsapp", whatsappE164);
        fields.put("phoneNumber", whatsappE164);
        fields.put("address", address);
        fields.put("facebookLink", facebookUrl != null ? facebookUrl : "");
        if (updatedAvatarUrl != null) {
            fields.put("avatarUrl", updatedAvatarUrl);
        }

        userRepository.updateMyProfile(fields, new BaseRepository.Callback<User>() {
            @Override
            public void onSuccess(User user) {
                setSavingState(false);
                currentUser = user;
                isUserInitiatedEditMode = false;
                pendingAvatarUri = null;
                avatarDeletionRequested = false;

                // 1. Fermer le formulaire
                // 2. Recharger et actualiser l'affichage
                // 3. Revenir à la présentation normale du profil
                // 4. Confirmation discrète
                switchToDisplayMode();
                render(user);
                toast("Profil enregistré avec succès !");
            }

            @Override
            public void onError(String message) {
                setSavingState(false);
                // En cas d'échec : conserver les valeurs saisies, maintenir le formulaire ouvert
                toast("Erreur d'enregistrement : " + message);
            }
        });
    }

    private void setSavingState(boolean saving) {
        if (btnSaveProfile != null) btnSaveProfile.setEnabled(!saving);
        if (btnCancelEdit != null) btnCancelEdit.setEnabled(!saving);
        if (progressSave != null) progressSave.setVisibility(saving ? View.VISIBLE : View.GONE);
    }

    @Override
    protected void showLoading(boolean loading) {
        if (layoutLoading != null) layoutLoading.setVisibility(loading ? View.VISIBLE : View.GONE);
        if (layoutMainContent != null) layoutMainContent.setVisibility(loading ? View.GONE : View.VISIBLE);
    }

    private void showErrorState(String message) {
        if (layoutLoading != null) layoutLoading.setVisibility(View.GONE);
        if (layoutMainContent != null) layoutMainContent.setVisibility(View.GONE);
        if (layoutError != null) layoutError.setVisibility(View.VISIBLE);
        if (tvErrorMessage != null) tvErrorMessage.setText(message);
    }

    private void hideError() {
        if (layoutError != null) layoutError.setVisibility(View.GONE);
        if (layoutMainContent != null) layoutMainContent.setVisibility(View.VISIBLE);
    }

    private void openWhatsappDirect() {
        if (currentUser == null) return;
        String wa = currentUser.getWhatsapp();
        if (wa == null || wa.trim().isEmpty()) {
            toast("Numéro WhatsApp non renseigné");
            return;
        }
        String digits = wa.replaceAll("[^0-9]", "");
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/" + digits));
            startActivity(intent);
        } catch (Exception e) {
            toast("Impossible d'ouvrir WhatsApp");
        }
    }

    private void openCallDirect() {
        if (currentUser == null) return;
        String phone = currentUser.getPhoneNumber();
        if (phone == null || phone.trim().isEmpty()) {
            toast("Numéro de téléphone non renseigné");
            return;
        }
        try {
            Intent intent = new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + phone));
            startActivity(intent);
        } catch (Exception e) {
            toast("Impossible de lancer l'appel");
        }
    }

    private void openFacebookDirect() {
        if (currentUser == null) return;
        String fb = currentUser.getFacebookLink();
        if (fb == null || fb.trim().isEmpty()) return;
        String url = fb.trim();
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            url = "https://" + url;
        }
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            startActivity(intent);
        } catch (Exception e) {
            toast("Impossible d'ouvrir le lien Facebook");
        }
    }

    private void showSettingsDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Paramètres de l'application");

        boolean isAdmin = AdminAccessControl.isCurrentUserAdmin();
        String currentServer = ApiClient.getBaseUrl();
        String versionInfo = "BIZINEVO Android\nVersion " + BuildConfig.VERSION_NAME + " (Build " + BuildConfig.VERSION_CODE + ")\n\n"
                + "Compte : " + (currentUser != null && currentUser.getEmail() != null ? currentUser.getEmail() : "Connecté")
                + "\nStatut : " + (isAdmin ? "Administrateur Agence" : "Utilisateur Standard")
                + "\n\nServeur Backend actuel :\n" + currentServer;

        builder.setMessage(versionInfo);
        builder.setPositiveButton("Fermer", (dialog, which) -> dialog.dismiss());
        builder.setNeutralButton("Modifier IP Serveur", (dialog, which) -> {
            dialog.dismiss();
            showServerConfigDialog();
        });

        if (isAdmin) {
            builder.setNegativeButton("Espace Admin 1688", (dialog, which) -> {
                dialog.dismiss();
                Router.openImport1688Admin(this);
            });
        }

        builder.show();
    }

    private void showServerConfigDialog() {
        EditText input = new EditText(this);
        input.setText(ApiClient.getBaseUrl());
        input.setSingleLine(true);
        input.setPadding(40, 24, 40, 24);

        new AlertDialog.Builder(this)
                .setTitle("Configuration de l'adresse du serveur")
                .setMessage("Indiquez l'adresse IP de votre PC sur le réseau local Wi-Fi (ex: http://192.168.1.204:8080/api/) :")
                .setView(input)
                .setPositiveButton("Enregistrer", (dialog, which) -> {
                    String newUrl = input.getText().toString().trim();
                    if (!newUrl.isEmpty()) {
                        ApiClient.setBaseUrl(this, newUrl);
                        toast("Serveur mis à jour : " + ApiClient.getBaseUrl());
                        loadUserProfile(false);
                    }
                })
                .setNegativeButton("Annuler", null)
                .show();
    }

    private void performSignOut() {
        new AlertDialog.Builder(this)
                .setTitle("Déconnexion")
                .setMessage("Voulez-vous vraiment vous déconnecter de votre compte BIZINEVO ?")
                .setPositiveButton("Déconnexion", (dialog, which) -> {
                    FirebaseAuthSession.signOut();
                    toast("Déconnexion réussie");
                    Router.openLogin(this);
                    finish();
                })
                .setNegativeButton("Annuler", null)
                .show();
    }
}
