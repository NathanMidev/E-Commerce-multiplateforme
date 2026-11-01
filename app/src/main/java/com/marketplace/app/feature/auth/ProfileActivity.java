package com.marketplace.app.feature.auth;

import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

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
 * Rôle : Écran de profil utilisateur inspiré de Deal.mg (coordonnées, raccourcis, déconnexion).
 * Entrée : Informations du compte connecté (WhatsApp, adresse, rôle).
 * Sortie : Mise à jour du profil, publication d'annonce et gestion des commandes.
 */
public class ProfileActivity extends BaseActivity {

    private TextView tvProfileName;
    private TextView tvProfileEmail;
    private TextView tvProfileRoleBadge;
    private View cardPublish;
    private View cardOrders;
    private Button btnLogout;

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

        tvProfileName = findViewById(R.id.tv_profile_name);
        tvProfileEmail = findViewById(R.id.tv_profile_email);
        tvProfileRoleBadge = findViewById(R.id.tv_profile_role_badge);
        cardPublish = findViewById(R.id.card_profile_publish);
        cardOrders = findViewById(R.id.card_profile_orders);
        btnLogout = findViewById(R.id.btn_logout);

        etWhatsapp = findViewById(R.id.et_whatsapp);
        etAddress = findViewById(R.id.et_address);
        etPhone = findViewById(R.id.et_phone);
        btnSave = findViewById(R.id.btn_save_profile);

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
        if (data instanceof User) {
            User user = (User) data;
            if (tvProfileName != null && user.getName() != null) {
                tvProfileName.setText(user.getName());
            }
            if (tvProfileEmail != null && user.getEmail() != null) {
                tvProfileEmail.setText(user.getEmail());
            }
            if (tvProfileRoleBadge != null && user.resolveRole() != null) {
                tvProfileRoleBadge.setText("COMPTE " + user.resolveRole().name());
            }
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
                toast("Vos coordonnées ont été enregistrées avec succès !");
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
