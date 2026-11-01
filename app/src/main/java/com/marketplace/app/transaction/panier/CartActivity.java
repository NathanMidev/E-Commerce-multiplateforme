package com.marketplace.app.transaction.panier;

import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.marketplace.app.R;
import com.marketplace.app.backend.repository.CartRepository;
import com.marketplace.app.backend.repository.UserRepository;
import com.marketplace.app.base.BaseActivity;
import com.marketplace.app.base.BaseRepository;
import com.marketplace.app.feature.contact.ContactBottomSheet;
import com.marketplace.app.model.Cart;
import com.marketplace.app.model.CartItem;
import com.marketplace.app.model.User;
import com.marketplace.app.navigation.Router;
import com.marketplace.app.ui.PriceFormatter;

/**
 * Rôle : Écran Panier d'achat avec groupement par vendeur et total général.
 * Entrée : Consultation et modifications des quantités en temps réel.
 * Sortie : Validation de possibilité de checkout et ouverture de la caisse.
 */
public class CartActivity extends BaseActivity {

    private RecyclerView rvCartItems;
    private TextView tvCartEmpty;
    private TextView tvCartTotal;
    private Button btnCartCheckout;

    private CartRepository cartRepository;
    private UserRepository userRepository;
    private CartAdapter adapter;
    private Cart currentCart;

    @Override
    protected int getLayoutId() {
        return R.layout.activity_cart;
    }

    @Override
    protected void bindViews() {
        cartRepository = new CartRepository();
        userRepository = new UserRepository();

        rvCartItems = findViewById(R.id.rv_cart_items);
        tvCartEmpty = findViewById(R.id.tv_cart_empty);
        tvCartTotal = findViewById(R.id.tv_cart_total);
        btnCartCheckout = findViewById(R.id.btn_cart_checkout);

        rvCartItems.setLayoutManager(new LinearLayoutManager(this));
        adapter = new CartAdapter(new CartAdapter.OnCartItemActionListener() {
            @Override
            public void onQuantityChanged(CartItem item, int newQuantity) {
                cartRepository.setItemQuantity(item.getProductId(), newQuantity, new BaseRepository.Callback<Cart>() {
                    @Override
                    public void onSuccess(Cart result) {
                        render(result);
                    }

                    @Override
                    public void onError(String message) {
                        toast(message);
                        loadData();
                    }
                });
            }

            @Override
            public void onChatSeller(String sellerId, String sellerName) {
                userRepository.getById(sellerId, new BaseRepository.Callback<User>() {
                    @Override
                    public void onSuccess(User seller) {
                        ContactBottomSheet bottomSheet = ContactBottomSheet.newInstance(null, seller);
                        bottomSheet.show(getSupportFragmentManager(), "ContactBottomSheet");
                    }

                    @Override
                    public void onError(String message) {
                        toast("Impossible de charger les coordonnées du vendeur");
                    }
                });
            }
        });
        rvCartItems.setAdapter(adapter);

        btnCartCheckout.setOnClickListener(v -> {
            if (currentCart == null || currentCart.isEmpty()) {
                toast("Votre panier est vide");
                return;
            }
            if (!currentCart.isCheckoutPossible()) {
                toast("Certains articles sont épuisés ou dépassent le stock disponible");
                return;
            }
            Router.openCheckout(CartActivity.this);
        });
    }

    @Override
    protected void loadData() {
        if (!requireLogin(this::loadData)) return;

        cartRepository.getCart(new BaseRepository.Callback<Cart>() {
            @Override
            public void onSuccess(Cart cart) {
                currentCart = cart;
                render(cart);
            }

            @Override
            public void onError(String message) {
                showError(message, CartActivity.this::loadData);
            }
        });
    }

    @Override
    protected void render(Object data) {
        if (data instanceof Cart) {
            currentCart = (Cart) data;
            adapter.setItems(currentCart.getItems());
            tvCartTotal.setText(PriceFormatter.format(currentCart.getTotal()));

            if (currentCart.isEmpty()) {
                tvCartEmpty.setVisibility(View.VISIBLE);
                btnCartCheckout.setEnabled(false);
            } else {
                tvCartEmpty.setVisibility(View.GONE);
                btnCartCheckout.setEnabled(currentCart.isCheckoutPossible());
            }
        }
    }
}
