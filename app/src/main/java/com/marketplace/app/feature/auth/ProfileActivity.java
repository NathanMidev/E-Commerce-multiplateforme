package com.marketplace.app.feature.auth;

import android.net.Uri;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.AdapterView;
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
import com.marketplace.app.backend.repository.UserRepository;
import com.marketplace.app.base.BaseActivity;
import com.marketplace.app.base.BaseRepository;
import com.marketplace.app.model.User;
import com.marketplace.app.navigation.Router;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Rôle : Écran de complétion et de gestion du profil utilisateur :
 * - Photo de profil (sélection galerie, aperçu avant validation, remplacement, suppression, upload Firebase Storage)
 * - Nom complet (displayName) et Nom d'utilisateur unique (username) distincts
 * - Unique numéro WhatsApp avec sélecteur de pays, indicatif international automatique, formatage national et normalisation E.164
 * - Lien du profil Facebook facultatif (ajout, validation, modification, suppression).
 */
public class ProfileActivity extends BaseActivity {

    private ImageView ivProfileAvatar;
    private TextView tvProfileName;
    private TextView tvProfileUsername;
    private TextView tvProfileEmail;
    private TextView tvProfileRoleBadge;
    private TextView tvAvatarPreviewBadge;
    private ProgressBar progressAvatarUpload;
    private Button btnSelectAvatar;
    private Button btnRemoveAvatar;

    private View cardPublish;
    private View cardOrders;
    private Button btnLogout;

    private EditText etDisplayName;
    private EditText etUsername;
    private TextView tvUsernameStatus;

    private Spinner spinnerWhatsappCountry;
    private TextView tvWhatsappDialCode;
    private EditText etWhatsapp;
    private TextView tvWhatsappE164Preview;

    private EditText etFacebookUrl;
    private TextView btnClearFacebookUrl;
    private EditText etAddress;
    private Button btnSave;

    private UserRepository userRepository;
    private ProfileStorageService profileStorageService;

    private final List<PhoneNormalizer.CountryDialOption> countryOptions =
            PhoneNormalizer.getSupportedCountries();
    private PhoneNormalizer.CountryDialOption selectedCountry = countryOptions.get(0);
    private boolean formattingWhatsapp = false;

    private Uri pendingAvatarUri = null;
    private boolean avatarDeletionRequested = false;
    private String currentSavedAvatarUrl = null;

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

        ivProfileAvatar = findViewById(R.id.iv_profile_avatar);
        tvProfileName = findViewById(R.id.tv_profile_name);
        tvProfileUsername = findViewById(R.id.tv_profile_username);
        tvProfileEmail = findViewById(R.id.tv_profile_email);
        tvProfileRoleBadge = findViewById(R.id.tv_profile_role_badge);
        tvAvatarPreviewBadge = findViewById(R.id.tv_avatar_preview_badge);
        progressAvatarUpload = findViewById(R.id.progress_avatar_upload);
        btnSelectAvatar = findViewById(R.id.btn_select_avatar);
        btnRemoveAvatar = findViewById(R.id.btn_remove_avatar);

        cardPublish = findViewById(R.id.card_profile_publish);
        cardOrders = findViewById(R.id.card_profile_orders);
        btnLogout = findViewById(R.id.btn_logout);

        etDisplayName = findViewById(R.id.et_display_name);
        etUsername = findViewById(R.id.et_username);
        tvUsernameStatus = findViewById(R.id.tv_username_status);

        spinnerWhatsappCountry = findViewById(R.id.spinner_whatsapp_country);
        tvWhatsappDialCode = findViewById(R.id.tv_whatsapp_dial_code);
        etWhatsapp = findViewById(R.id.et_whatsapp);
        tvWhatsappE164Preview = findViewById(R.id.tv_whatsapp_e164_preview);

        etFacebookUrl = findViewById(R.id.et_facebook_url);
        btnClearFacebookUrl = findViewById(R.id.btn_clear_facebook_url);
        etAddress = findViewById(R.id.et_address);
        btnSave = findViewById(R.id.btn_save_profile);

        setupCountrySpinner();
        setupInputWatchers();

        if (btnSelectAvatar != null) {
            btnSelectAvatar.setOnClickListener(v -> pickAvatarLauncher.launch("image/*"));
        }
        if (ivProfileAvatar != null) {
            ivProfileAvatar.setOnClickListener(v -> pickAvatarLauncher.launch("image/*"));
        }
        if (btnRemoveAvatar != null) {
            btnRemoveAvatar.setOnClickListener(v -> onRemoveAvatarClicked());
        }
        if (btnClearFacebookUrl != null) {
            btnClearFacebookUrl.setOnClickListener(v -> {
                if (etFacebookUrl != null) etFacebookUrl.setText("");
                btnClearFacebookUrl.setVisibility(View.GONE);
            });
        }

        btnSave.setOnClickListener(v -> performSave());

        if (cardPublish != null) {
            cardPublish.setOnClickListener(v -> Router.openProductPublish(this));
        }

        if (cardOrders != null) {
            cardOrders.setOnClickListener(v -> Router.openOrders(this));
        }

        if (btnLogout != null) {
            btnLogout.setOnClickListener(v -> {
                FirebaseAuthSession.signOut();
                toast("Déconnexion réussie");
                Router.openLogin(this);
                finish();
            });
        }
    }

    private void setupCountrySpinner() {
        if (spinnerWhatsappCountry == null) return;
        ArrayAdapter<PhoneNormalizer.CountryDialOption> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                countryOptions
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerWhatsappCountry.setAdapter(adapter);
        spinnerWhatsappCountry.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedCountry = countryOptions.get(position);
                if (tvWhatsappDialCode != null) {
                    tvWhatsappDialCode.setText(selectedCountry.getDialCode());
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

        if (etFacebookUrl != null) {
            etFacebookUrl.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    if (btnClearFacebookUrl != null) {
                        btnClearFacebookUrl.setVisibility(s.length() > 0 ? View.VISIBLE : View.GONE);
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
            tvUsernameStatus.setText("❌ @" + norm + " est déjà utilisé par un autre utilisateur");
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
            if (tvWhatsappE164Preview != null) {
                tvWhatsappE164Preview.setText("Utilisé automatiquement pour WhatsApp (wa.me) et les appels (tel:).");
                tvWhatsappE164Preview.setTextColor(getResources().getColor(R.color.text_secondary, getTheme()));
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

        if (tvWhatsappE164Preview != null) {
            String e164 = PhoneNormalizer.normalizeToE164(selectedCountry.getDialCode(), formatted);
            if (e164 != null) {
                tvWhatsappE164Preview.setText("✅ Format international E.164 : " + e164
                        + " (Appel tel:" + e164 + " & wa.me/" + e164.substring(1) + ")");
                tvWhatsappE164Preview.setTextColor(0xFF059669);
            } else {
                tvWhatsappE164Preview.setText("⚠️ Format attendu pour " + selectedCountry.getCountryName()
                        + " : " + selectedCountry.getDialCode() + " " + selectedCountry.getExampleHint());
                tvWhatsappE164Preview.setTextColor(getResources().getColor(R.color.status_red, getTheme()));
            }
        }
    }

    private void onNewAvatarSelected(Uri uri) {
        pendingAvatarUri = uri;
        avatarDeletionRequested = false;
        if (ivProfileAvatar != null) {
            Glide.with(this)
                    .load(uri)
                    .placeholder(R.drawable.ic_default_avatar)
                    .error(R.drawable.ic_default_avatar)
                    .circleCrop()
                    .into(ivProfileAvatar);
        }
        if (tvAvatarPreviewBadge != null) {
            tvAvatarPreviewBadge.setVisibility(View.VISIBLE);
        }
        if (btnRemoveAvatar != null) {
            btnRemoveAvatar.setVisibility(View.VISIBLE);
        }
    }

    private void onRemoveAvatarClicked() {
        pendingAvatarUri = null;
        avatarDeletionRequested = true;
        if (ivProfileAvatar != null) {
            ivProfileAvatar.setImageResource(R.drawable.ic_default_avatar);
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
        if (!requireLogin(null)) return;

        userRepository.getMyProfile(new BaseRepository.Callback<User>() {
            @Override
            public void onSuccess(User user) {
                render(user);
            }

            @Override
            public void onError(String message) {}
        });
    }

    @Override
    protected void render(Object data) {
        if (!(data instanceof User)) return;
        User user = (User) data;

        String displayName = user.getName() != null ? user.getName() : "";
        if (tvProfileName != null) {
            tvProfileName.setText(!displayName.isEmpty() ? displayName : "Mon Compte");
        }
        if (etDisplayName != null && !displayName.isEmpty() && !"Utilisateur".equalsIgnoreCase(displayName)) {
            etDisplayName.setText(displayName);
        }

        String username = user.getUsername();
        if (tvProfileUsername != null) {
            if (username != null && !username.trim().isEmpty()) {
                tvProfileUsername.setText("@" + username.trim());
                tvProfileUsername.setVisibility(View.VISIBLE);
            } else {
                tvProfileUsername.setText("@nom_utilisateur_a_definir");
                tvProfileUsername.setVisibility(View.VISIBLE);
            }
        }
        if (etUsername != null && username != null && !username.trim().isEmpty()) {
            etUsername.setText(username.trim());
        }

        if (tvProfileEmail != null && user.getEmail() != null) {
            tvProfileEmail.setText(user.getEmail());
        }
        if (tvProfileRoleBadge != null && user.resolveRole() != null) {
            tvProfileRoleBadge.setText("COMPTE " + user.resolveRole().name());
        }

        // Photo de profil (avec image par défaut ic_default_avatar si aucune photo n'est définie)
        currentSavedAvatarUrl = user.hasCustomAvatar() ? user.getAvatarUrl() : null;
        pendingAvatarUri = null;
        avatarDeletionRequested = false;
        if (tvAvatarPreviewBadge != null) {
            tvAvatarPreviewBadge.setVisibility(View.GONE);
        }
        if (ivProfileAvatar != null) {
            if (currentSavedAvatarUrl != null) {
                Glide.with(this)
                        .load(currentSavedAvatarUrl)
                        .placeholder(R.drawable.ic_default_avatar)
                        .error(R.drawable.ic_default_avatar)
                        .circleCrop()
                        .into(ivProfileAvatar);
                if (btnRemoveAvatar != null) btnRemoveAvatar.setVisibility(View.VISIBLE);
            } else {
                ivProfileAvatar.setImageResource(R.drawable.ic_default_avatar);
                if (btnRemoveAvatar != null) btnRemoveAvatar.setVisibility(View.GONE);
            }
        }

        // Numéro unique WhatsApp : pré-sélection du pays et formatage national
        String storedWhatsapp = user.getWhatsapp();
        if (storedWhatsapp != null && !storedWhatsapp.trim().isEmpty()) {
            PhoneNormalizer.ParsedPhone parsed = PhoneNormalizer.parseStoredPhone(storedWhatsapp);
            selectedCountry = parsed.getCountry();
            int idx = countryOptions.indexOf(selectedCountry);
            if (idx >= 0 && spinnerWhatsappCountry != null) {
                spinnerWhatsappCountry.setSelection(idx);
            }
            if (tvWhatsappDialCode != null) {
                tvWhatsappDialCode.setText(selectedCountry.getDialCode());
            }
            if (etWhatsapp != null) {
                formattingWhatsapp = true;
                etWhatsapp.setText(parsed.getFormattedNational());
                formattingWhatsapp = false;
                reformatCurrentWhatsappInput();
            }
        }

        // Lien Facebook (facultatif)
        if (etFacebookUrl != null) {
            String fb = user.getFacebookLink();
            etFacebookUrl.setText(fb != null ? fb : "");
            if (btnClearFacebookUrl != null) {
                btnClearFacebookUrl.setVisibility((fb != null && !fb.trim().isEmpty()) ? View.VISIBLE : View.GONE);
            }
        }

        // Adresse
        if (etAddress != null && user.getAddress() != null) {
            etAddress.setText(user.getAddress());
        }
    }

    private void performSave() {
        String displayName = etDisplayName != null ? etDisplayName.getText().toString().trim() : "";
        String rawUsername = etUsername != null ? etUsername.getText().toString().trim() : "";
        String rawNationalWhatsapp = etWhatsapp != null ? etWhatsapp.getText().toString().trim() : "";
        String rawFacebook = etFacebookUrl != null ? etFacebookUrl.getText().toString().trim() : "";
        String address = etAddress != null ? etAddress.getText().toString().trim() : "";

        // 1. Validation du nom complet
        if (displayName.length() < 2) {
            if (etDisplayName != null) etDisplayName.setError("Veuillez saisir votre nom complet ou nom de boutique (min. 2 caractères)");
            toast("Le nom complet est obligatoire");
            return;
        }

        // 2. Validation du nom d'utilisateur (username, distinct du nom complet)
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

        // 3. Validation et normalisation E.164 de l'unique numéro WhatsApp
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
            if (etFacebookUrl != null) etFacebookUrl.setError(fbError);
            toast(fbError);
            return;
        }
        String normalizedFacebook = ProfileValidator.normalizeFacebookUrl(rawFacebook);

        // 5. Validation de l'adresse
        if (address.isEmpty()) {
            if (etAddress != null) etAddress.setError("L'adresse est obligatoire");
            toast("Veuillez indiquer votre ville et adresse");
            return;
        }

        btnSave.setEnabled(false);

        // 6. Gestion de la photo de profil (Upload vers Firebase Storage ou suppression ou conservation)
        if (pendingAvatarUri != null) {
            if (progressAvatarUpload != null) {
                progressAvatarUpload.setProgress(0);
                progressAvatarUpload.setVisibility(View.VISIBLE);
            }
            profileStorageService.uploadOrReplaceProfilePhoto(uid, pendingAvatarUri, null,
                    new ProfileStorageService.PhotoUploadCallback() {
                        @Override
                        public void onProgress(int percent) {
                            if (progressAvatarUpload != null) {
                                progressAvatarUpload.setProgress(percent);
                            }
                        }

                        @Override
                        public void onSuccess(String photoUrl) {
                            if (progressAvatarUpload != null) progressAvatarUpload.setVisibility(View.GONE);
                            saveProfileFields(displayName, normalizedUsername, normalizedE164, normalizedFacebook, address, photoUrl);
                        }

                        @Override
                        public void onError(String message) {
                            if (progressAvatarUpload != null) progressAvatarUpload.setVisibility(View.GONE);
                            btnSave.setEnabled(true);
                            showError("Erreur upload photo : " + message, null);
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
                    btnSave.setEnabled(true);
                    showError("Erreur suppression photo : " + message, null);
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
                btnSave.setEnabled(true);
                toast("Profil enregistré avec succès !");
                render(user);
            }

            @Override
            public void onError(String message) {
                btnSave.setEnabled(true);
                showError("Erreur : " + message, null);
            }
        });
    }
}
