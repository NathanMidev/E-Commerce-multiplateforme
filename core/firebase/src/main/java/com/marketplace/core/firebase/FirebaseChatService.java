package com.marketplace.core.firebase;

import com.marketplace.core.model.ChatMessage;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Service Firebase pour le chat temps réel entre acheteurs et vendeurs.
 */
public class FirebaseChatService {

    public interface MessageListener {
        void onNewMessage(ChatMessage message);
    }

    private final Map<String, List<ChatMessage>> conversations = new HashMap<>();

    public void sendMessage(String conversationId, String senderId, String receiverId, String content, String productId) {
        ChatMessage message = new ChatMessage(
                UUID.randomUUID().toString(),
                conversationId,
                senderId,
                receiverId,
                content,
                productId,
                System.currentTimeMillis(),
                false
        );

        List<ChatMessage> list = conversations.get(conversationId);
        if (list == null) {
            list = new ArrayList<>();
            conversations.put(conversationId, list);
        }
        list.add(message);
    }

    public List<ChatMessage> getMessages(String conversationId) {
        List<ChatMessage> list = conversations.get(conversationId);
        return list != null ? new ArrayList<>(list) : new ArrayList<ChatMessage>();
    }
}
