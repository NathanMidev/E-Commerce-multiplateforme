package com.marketplace.app.feature.contact;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.marketplace.app.R;
import com.marketplace.app.backend.AuthSession;
import com.marketplace.app.backend.repository.ContactRepository;
import com.marketplace.app.base.BaseRepository;
import com.marketplace.app.model.ContactChannel;
import com.marketplace.app.model.ContactRequest;
import com.marketplace.app.model.Product;
import com.marketplace.app.model.User;
import com.marketplace.app.navigation.Router;

/**
 * Rôle : BottomSheet de discussion directe avec le vendeur (WhatsApp, Messenger, Appel).
 * Entrée : Produit et profil du vendeur.
 * Sortie : Lancement de l'Intent correspondant et enregistrement de l'intention ContactRequest.
 */
public class ContactBottomSheet extends BottomSheetDialogFragment {

    private Product product;
    private User seller;
    private ContactRepository contactRepository;

    public static ContactBottomSheet newInstance(Product product, User seller) {
        ContactBottomSheet fragment = new ContactBottomSheet();
        fragment.product = product;
        fragment.seller = seller;
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        contactRepository = new ContactRepository();
        Context context = requireContext();

        android.widget.LinearLayout root = new android.widget.LinearLayout(context);
        root.setOrientation(android.widget.LinearLayout.VERTICAL);
        root.setPadding(48, 36, 48, 48);

        TextView tvTitle = new TextView(context);
        tvTitle.setText("Discuter avec le vendeur par :");
        tvTitle.setTextSize(16f);
        tvTitle.setTypeface(tvTitle.getTypeface(), android.graphics.Typeface.BOLD);
        tvTitle.setPadding(0, 0, 0, 32);
        root.addView(tvTitle);

        String singleContactNumber = (seller != null) ? seller.getWhatsapp() : null;
        if (singleContactNumber != null && !singleContactNumber.trim().isEmpty()) {
            Button btnWa = new Button(context);
            btnWa.setText("💬 Discussion WhatsApp (" + singleContactNumber + ")");
            btnWa.setOnClickListener(v -> openWhatsApp());
            root.addView(btnWa);

            Button btnCall = new Button(context);
            btnCall.setText("📞 Appel téléphonique (" + singleContactNumber + ")");
            btnCall.setOnClickListener(v -> openPhoneCall());
            root.addView(btnCall);
        }

        if (seller != null && seller.hasFacebookProfile()) {
            Button btnFbProfile = new Button(context);
            btnFbProfile.setText("🌐 Voir le profil Facebook");
            btnFbProfile.setOnClickListener(v -> openFacebookProfile());
            root.addView(btnFbProfile);
        }

        return root;
    }

    private boolean checkLoginAndRecord(ContactChannel channel) {
        if (!AuthSession.getInstance().isLoggedIn()) {
            dismiss();
            Router.openLogin(requireContext());
            return false;
        }

        // Enregistre l'intention de contact dans l'API REST
        ContactRequest req = new ContactRequest();
        req.setProductId(product != null ? product.getId() : null);
        req.setProductTitle(product != null ? product.getTitle() : null);
        req.setSellerId(seller != null ? seller.getId() : null);
        req.setBuyerId(AuthSession.getInstance().getUserId());
        req.setChannel(channel);
        req.setInitialMessage("Intention de contact via " + channel);

        contactRepository.create(req, new BaseRepository.Callback<ContactRequest>() {
            @Override public void onSuccess(ContactRequest result) {}
            @Override public void onError(String message) {}
        });
        return true;
    }

    private void openWhatsApp() {
        if (!checkLoginAndRecord(ContactChannel.WHATSAPP)) return;
        String url = ContactLinks.buildWhatsAppUrl(seller.getWhatsapp(),
                product != null ? product.getTitle() : "",
                product != null ? product.getPrice() : 0,
                getString(R.string.app_name));
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
            dismiss();
        } catch (ActivityNotFoundException e) {
            Toast.makeText(requireContext(), "Application WhatsApp introuvable", Toast.LENGTH_SHORT).show();
        }
    }

    private void openFacebookProfile() {
        if (!checkLoginAndRecord(ContactChannel.MESSENGER)) return;
        String url = ContactLinks.buildFacebookProfileUrl(seller.getFacebookLink());
        if (url == null) {
            url = ContactLinks.buildMessengerUrl(seller.getFacebookLink());
        }
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
            dismiss();
        } catch (ActivityNotFoundException e) {
            Toast.makeText(requireContext(), "Impossible d'ouvrir le profil Facebook", Toast.LENGTH_SHORT).show();
        }
    }

    private void openPhoneCall() {
        if (!checkLoginAndRecord(ContactChannel.APPEL)) return;
        String telUri = ContactLinks.buildPhoneCallUri(seller.getWhatsapp());
        if (telUri == null) return;
        try {
            Intent callIntent = new Intent(Intent.ACTION_DIAL, Uri.parse(telUri));
            startActivity(callIntent);
            dismiss();
        } catch (ActivityNotFoundException e) {
            Toast.makeText(requireContext(), "Application téléphone introuvable", Toast.LENGTH_SHORT).show();
        }
    }
}
