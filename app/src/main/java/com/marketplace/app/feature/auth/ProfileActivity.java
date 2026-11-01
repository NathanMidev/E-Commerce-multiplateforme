package com.marketplace.app.feature.auth;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;

import com.marketplace.app.R;
import com.marketplace.app.backend.AuthSession;
import com.marketplace.app.backend.repository.UserRepository;
import com.marketplace.app.base.BaseActivity;
import com.marketplace.app.base.BaseRepository;
import com.marketplace.app.model.User;
import com.marketplace.app.navigation.Router;

import java.util.HashMap;
import java.util.Map;

/**
 * Rôle : Écran d'édition et finalisation du profil utilisateur (WhatsApp, Adresse, Téléphone).
 * Entrée : Informations saisies ou existantes du profil.
 * Sortie : Validation des contacts obligatoires pour pouvoir vendre ou commander.
 */
public class ProfileActivity extends BaseActivity {

    private EditText etWhatsapp;
    private EditText etAddress;
    private EditText etPhone;
    private Button btnSave;

    private UserRepository userRepository;

    @Override
    protected int getLayoutId() {
        return R.layout.activity_user_profile;
    }

    @Override
    protected void bindViews() {
        userRepository = new UserRepository();
        etWhatsapp = findViewById(R.id.et_whatsapp);
        etAddress = findViewById(R.id.et_address);
        etPhone = findViewById(R.id.et_phone);
        btnSave = findViewById(R.id.btn_save_profile);

        btnSave.setOnClickListener(v -> performSave());
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
            public void onError(String message) {
                // Première connexion : formulaire vierge
            }
        });
    }

    @Override
    protected void render(Object data) {
        if (data instanceof User) {
            User user = (User) data;
            if (user.getWhatsapp() != null) etWhatsapp.setText(user.getWhatsapp());
            if (user.getAddress() != null) etAddress.setText(user.getAddress());
            if (user.getPhoneNumber() != null) etPhone.setText(user.getPhoneNumber());
        }
    }

    private void performSave() {
        String whatsapp = etWhatsapp.getText().toString().trim();
        String address = etAddress.getText().toString().trim();
        String phone = etPhone.getText().toString().trim();

        if (whatsapp.isEmpty() || address.isEmpty()) {
            toast("Le WhatsApp et l'adresse sont obligatoires pour valider vos coordonnées");
            return;
        }

        btnSave.setEnabled(false);
        Map<String, String> fields = new HashMap<>();
        fields.put("whatsapp", whatsapp);
        fields.put("address", address);
        fields.put("phoneNumber", phone.isEmpty() ? whatsapp : phone);

        userRepository.updateMyProfile(fields, new BaseRepository.Callback<User>() {
            @Override
            public void onSuccess(User user) {
                btnSave.setEnabled(true);
                toast("Profil mis à jour !");
                Router.openMain(ProfileActivity.this);
                finish();
            }

            @Override
            public void onError(String message) {
                btnSave.setEnabled(true);
                showError("Erreur : " + message, null);
            }
        });
    }
}
