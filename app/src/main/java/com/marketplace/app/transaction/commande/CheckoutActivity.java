package com.marketplace.app.transaction.commande;

import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioGroup;

import com.marketplace.app.R;
import com.marketplace.app.backend.repository.OrderRepository;
import com.marketplace.app.backend.repository.UserRepository;
import com.marketplace.app.base.BaseActivity;
import com.marketplace.app.base.BaseRepository;
import com.marketplace.app.model.Order;
import com.marketplace.app.model.PaymentMethod;
import com.marketplace.app.model.User;
import com.marketplace.app.navigation.Router;

import java.util.List;

/**
 * Rôle : Écran de passage en caisse (pré-remplissage profil, adresse, téléphone, sélection paiement).
 * Entrée : Informations de livraison et paiement choisi.
 * Sortie : Création transactionnelle d'une commande par vendeur et redirection vers les commandes.
 */
public class CheckoutActivity extends BaseActivity {

    private EditText etAddress;
    private EditText etPhone;
    private EditText etNote;
    private RadioGroup rgPayment;
    private Button btnConfirm;

    private UserRepository userRepository;
    private OrderRepository orderRepository;

    @Override
    protected int getLayoutId() {
        return R.layout.activity_checkout;
    }

    @Override
    protected void bindViews() {
        userRepository = new UserRepository();
        orderRepository = new OrderRepository();

        etAddress = findViewById(R.id.et_checkout_address);
        etPhone = findViewById(R.id.et_checkout_phone);
        etNote = findViewById(R.id.et_checkout_note);
        rgPayment = findViewById(R.id.rg_payment_method);
        btnConfirm = findViewById(R.id.btn_confirm_checkout);

        btnConfirm.setOnClickListener(v -> performCheckout());
    }

    @Override
    protected void loadData() {
        if (!requireLogin(this::loadData)) return;

        // Pré-remplissage depuis le profil
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
            if (user.getAddress() != null && etAddress.getText().toString().isEmpty()) {
                etAddress.setText(user.getAddress());
            }
            if (user.getPhoneNumber() != null && etPhone.getText().toString().isEmpty()) {
                etPhone.setText(user.getPhoneNumber());
            } else if (user.getWhatsapp() != null && etPhone.getText().toString().isEmpty()) {
                etPhone.setText(user.getWhatsapp());
            }
        }
    }

    private void performCheckout() {
        String address = etAddress.getText().toString().trim();
        String phone = etPhone.getText().toString().trim();
        String note = etNote.getText().toString().trim();

        if (address.isEmpty() || phone.isEmpty()) {
            toast("L'adresse et le téléphone de livraison sont obligatoires");
            return;
        }

        PaymentMethod method = PaymentMethod.A_LA_LIVRAISON;
        int checkedId = rgPayment.getCheckedRadioButtonId();
        if (checkedId == R.id.rb_mvola) {
            method = PaymentMethod.MVOLA;
        } else if (checkedId == R.id.rb_orange_money) {
            method = PaymentMethod.ORANGE_MONEY;
        } else if (checkedId == R.id.rb_airtel_money) {
            method = PaymentMethod.AIRTEL_MONEY;
        }

        btnConfirm.setEnabled(false);
        orderRepository.checkout(address, phone, method, note.isEmpty() ? null : note, new BaseRepository.Callback<List<Order>>() {
            @Override
            public void onSuccess(List<Order> orders) {
                btnConfirm.setEnabled(true);
                toast(orders.size() + " commande(s) validée(s) avec succès !");
                Router.openOrders(CheckoutActivity.this);
                finish();
            }

            @Override
            public void onError(String message) {
                btnConfirm.setEnabled(true);
                showError("Impossible de finaliser la commande : " + message, null);
            }
        });
    }
}
