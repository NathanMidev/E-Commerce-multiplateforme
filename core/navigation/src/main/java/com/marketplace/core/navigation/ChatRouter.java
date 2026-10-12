package com.marketplace.core.navigation;

import android.content.Context;

/**
 * Contrat de routage pour le module Chat (feature:chat).
 * Permet d'ouvrir une discussion avec le vendeur d'un produit.
 */
public interface ChatRouter {
    void openChatWithSeller(Context context, String sellerId, String sellerName, String productId, String productTitle);
    void openConversationsList(Context context);
}
