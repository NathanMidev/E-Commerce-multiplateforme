package com.marketplace.core.model;

import java.io.Serializable;

public class ChatMessage implements Serializable {
    private String id;
    private String conversationId;
    private String senderId;
    private String receiverId;
    private String content;
    private String productId;
    private long timestamp;
    private boolean isRead;

    public ChatMessage() {}

    public ChatMessage(String id, String conversationId, String senderId, String receiverId,
                       String content, String productId, long timestamp, boolean isRead) {
        this.id = id;
        this.conversationId = conversationId;
        this.senderId = senderId;
        this.receiverId = receiverId;
        this.content = content;
        this.productId = productId;
        this.timestamp = timestamp;
        this.isRead = isRead;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getConversationId() { return conversationId; }
    public void setConversationId(String conversationId) { this.conversationId = conversationId; }

    public String getSenderId() { return senderId; }
    public void setSenderId(String senderId) { this.senderId = senderId; }

    public String getReceiverId() { return receiverId; }
    public void setReceiverId(String receiverId) { this.receiverId = receiverId; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public String getProductId() { return productId; }
    public void setProductId(String productId) { this.productId = productId; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    public boolean isRead() { return isRead; }
    public void setRead(boolean read) { isRead = read; }
}
