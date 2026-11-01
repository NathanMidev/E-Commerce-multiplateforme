package com.marketplace.app.model;

import com.marketplace.app.base.BaseModel;

import java.util.ArrayList;
import java.util.List;

/**
 * Rôle : Demande ou intention de contact initiée avec un vendeur.
 * Entrée : ID produit, acheteur, vendeur, canal utilisé (WhatsApp, Messenger, Appel).
 * Sortie : Modèle sérialisable pour le logging et l'historique de contact.
 */
public class ContactRequest extends BaseModel {
    private String productId;
    private String productTitle;
    private String sellerId;
    private String buyerId;
    private ContactChannel channel;
    private String initialMessage;
    private boolean answered;
    private long createdAt;

    public ContactRequest() {
        super();
        this.answered = false;
        this.createdAt = System.currentTimeMillis();
    }

    public ContactRequest(String id, String productId, String productTitle, String sellerId,
                          String buyerId, ContactChannel channel, String initialMessage) {
        super(id);
        this.productId = productId;
        this.productTitle = productTitle;
        this.sellerId = sellerId;
        this.buyerId = buyerId;
        this.channel = channel;
        this.initialMessage = initialMessage;
        this.answered = false;
        this.createdAt = System.currentTimeMillis();
    }

    @Override
    public List<String> validate() {
        List<String> errors = new ArrayList<>();
        if (sellerId == null || sellerId.trim().isEmpty()) {
            errors.add("L'identifiant du vendeur est obligatoire");
        }
        if (channel == null) {
            errors.add("Le canal de contact est obligatoire");
        }
        return errors;
    }

    public String getProductId() { return productId; }
    public void setProductId(String productId) { this.productId = productId; }

    public String getProductTitle() { return productTitle; }
    public void setProductTitle(String productTitle) { this.productTitle = productTitle; }

    public String getSellerId() { return sellerId; }
    public void setSellerId(String sellerId) { this.sellerId = sellerId; }

    public String getBuyerId() { return buyerId; }
    public void setBuyerId(String buyerId) { this.buyerId = buyerId; }

    public ContactChannel getChannel() { return channel; }
    public void setChannel(ContactChannel channel) { this.channel = channel; }

    public String getInitialMessage() { return initialMessage; }
    public void setInitialMessage(String initialMessage) { this.initialMessage = initialMessage; }

    public boolean isAnswered() { return answered; }
    public void setAnswered(boolean answered) { this.answered = answered; }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }
}
