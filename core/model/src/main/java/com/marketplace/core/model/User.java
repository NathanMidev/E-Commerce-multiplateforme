package com.marketplace.core.model;

import java.io.Serializable;

public class User implements Serializable {
    private String id;
    private String name;
    private String email;
    private boolean isSeller;
    private String avatarUrl;
    private String shopName;

    public User() {}

    public User(String id, String name, String email, boolean isSeller, String avatarUrl, String shopName) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.isSeller = isSeller;
        this.avatarUrl = avatarUrl;
        this.shopName = shopName;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public boolean isSeller() { return isSeller; }
    public void setSeller(boolean seller) { isSeller = seller; }

    public String getAvatarUrl() { return avatarUrl; }
    public void setAvatarUrl(String avatarUrl) { this.avatarUrl = avatarUrl; }

    public String getShopName() { return shopName; }
    public void setShopName(String shopName) { this.shopName = shopName; }
}
