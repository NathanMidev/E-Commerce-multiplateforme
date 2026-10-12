package com.marketplace.feature.chat.router;

import android.content.Context;
import android.content.Intent;
import com.marketplace.core.navigation.ChatRouter;
import com.marketplace.feature.chat.ui.ChatConversationActivity;
import com.marketplace.feature.chat.ui.ChatListActivity;

/**
 * Routeur interne pour le module Chat.
 * Implémente le contrat ChatRouter défini dans :core:navigation.
 */
public class ChatModuleRouter implements ChatRouter {

    @Override
    public void openChatWithSeller(Context context, String sellerId, String sellerName, String productId, String productTitle) {
        if (context == null) return;
        Intent intent = new Intent(context, ChatConversationActivity.class);
        intent.putExtra(ChatConversationActivity.EXTRA_SELLER_ID, sellerId);
        intent.putExtra(ChatConversationActivity.EXTRA_SELLER_NAME, sellerName);
        intent.putExtra(ChatConversationActivity.EXTRA_PRODUCT_ID, productId);
        intent.putExtra(ChatConversationActivity.EXTRA_PRODUCT_TITLE, productTitle);
        context.startActivity(intent);
    }

    @Override
    public void openConversationsList(Context context) {
        if (context == null) return;
        Intent intent = new Intent(context, ChatListActivity.class);
        context.startActivity(intent);
    }
}
