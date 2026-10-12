package com.marketplace.feature.chat;

import com.marketplace.core.firebase.FirebaseChatService;
import com.marketplace.core.model.ChatMessage;
import com.marketplace.feature.chat.router.ChatModuleRouter;
import org.junit.Test;

import java.util.List;
import static org.junit.Assert.*;

/**
 * Test unitaire isolé du module Chat (:feature:chat).
 * Conforme à la règle d'architecture : aucun autre module fonctionnel (produits, ar, etc.) n'est exécuté.
 */
public class ChatUnitTest {

    @Test
    public void testChatModuleRouterIsolated() {
        ChatModuleRouter router = new ChatModuleRouter();
        assertNotNull("Le routeur chat doit pouvoir s'instancier isolément", router);
        router.openChatWithSeller(null, "s1", "Vendeur", "p1", "Produit");
        router.openConversationsList(null);
    }

    @Test
    public void testFirebaseChatMessaging() {
        FirebaseChatService chatService = new FirebaseChatService();
        String convId = "conv_test_123";

        chatService.sendMessage(convId, "user_buyer", "user_seller", "Bonjour, le produit est-il disponible ?", "p1");
        List<ChatMessage> messages = chatService.getMessages(convId);

        assertEquals(1, messages.size());
        assertEquals("Bonjour, le produit est-il disponible ?", messages.get(0).getContent());
        assertEquals("user_buyer", messages.get(0).getSenderId());
    }
}
