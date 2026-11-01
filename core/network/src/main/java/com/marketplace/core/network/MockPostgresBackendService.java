package com.marketplace.core.network;

import com.marketplace.core.model.Cart;
import com.marketplace.core.model.CartItem;
import com.marketplace.core.model.Category;
import com.marketplace.core.model.CheckoutRequest;
import com.marketplace.core.model.FilterCriteria;
import com.marketplace.core.model.Order;
import com.marketplace.core.model.OrderItem;
import com.marketplace.core.model.OrderStatus;
import com.marketplace.core.model.PaymentMethod;
import com.marketplace.core.model.Product;
import com.marketplace.core.model.ProductStatus;
import com.marketplace.core.model.User;
import com.marketplace.core.model.UserRole;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class MockPostgresBackendService implements PostgresBackendApi {

    private final List<Product> products = new ArrayList<>();
    private final List<Category> categories = new ArrayList<>();

    // Mode démo hors ligne : profils, paniers et commandes en mémoire (perdus à la fermeture de l'app)
    private final AuthSession session;
    private final Map<String, User> users = new LinkedHashMap<>();
    private final Map<String, Map<String, Integer>> carts = new HashMap<>();
    private final List<Order> orders = new ArrayList<>();
    private long orderSequence = 0;

    /** Sans session : catalogue seulement (les opérations de panier/commande répondent « Connexion requise »). */
    public MockPostgresBackendService() {
        this(null);
    }

    public MockPostgresBackendService(AuthSession session) {
        this.session = session;
        initInitialData();
        seedUsers();
    }

    private void initInitialData() {
        categories.add(new Category("cat_furniture", "Mobilier & Déco", "Meubles et objets de décoration scannables en 3D", "ic_furniture"));
        categories.add(new Category("cat_electronics", "Électronique", "Appareils, gadgets et accessoires", "ic_electronics"));
        categories.add(new Category("cat_fashion", "Mode & Chaussures", "Vêtements, sacs et sneakers avec essayage AR", "ic_fashion"));
        categories.add(new Category("cat_art", "Art & Sculptures", "Objets uniques numérisés par SAM", "ic_art"));

        Product p1 = new Product(
                "prod_1",
                "Fauteuil Scandinave Velours",
                "Fauteuil confortable avec structure bois massif. Modèle 3D reconstruit par IA Meta SAM.",
                189.99,
                ProductStatus.DISPONIBLE,
                "cat_furniture",
                "seller_101",
                "Atelier Nordique",
                "https://via.placeholder.com/400x300.png?text=Fauteuil+Nordique",
                true,
                "https://modelviewer.dev/shared-assets/models/Astronaut.glb", // Modèle de test AR GLB
                Arrays.asList("https://via.placeholder.com/400x300.png?text=Fauteuil+Nordique"),
                5,
                System.currentTimeMillis() - 86400000
        );

        Product p2 = new Product(
                "prod_2",
                "Lampe Design Industrielle",
                "Lampe à poser en acier brossé. Scannez et placez la lampe directement sur votre table en AR.",
                59.00,
                ProductStatus.NOUVEAU,
                "cat_furniture",
                "seller_102",
                "Luminaires & Co",
                "https://via.placeholder.com/400x300.png?text=Lampe+Design",
                true,
                "https://raw.githubusercontent.com/KhronosGroup/glTF-Sample-Models/master/2.0/BoomBox/glTF-Binary/BoomBox.glb",
                Arrays.asList("https://via.placeholder.com/400x300.png?text=Lampe+Design"),
                12,
                System.currentTimeMillis() - 43200000
        );

        Product p3 = new Product(
                "prod_3",
                "Sneakers Édition Limitée",
                "Paire de sneakers exclusives. Reconstitution 3D photoréaliste via Meta SAM.",
                220.00,
                ProductStatus.A_VENDRE,
                "cat_fashion",
                "seller_103",
                "SneakersHub",
                "https://via.placeholder.com/400x300.png?text=Sneakers+3D",
                true,
                "https://raw.githubusercontent.com/KhronosGroup/glTF-Sample-Models/master/2.0/Duck/glTF-Binary/Duck.glb",
                Arrays.asList("https://via.placeholder.com/400x300.png?text=Sneakers+3D"),
                2,
                System.currentTimeMillis() - 20000000
        );

        products.add(p1);
        products.add(p2);
        products.add(p3);
    }

    @Override
    public void getProducts(FilterCriteria filter, ApiCallback<List<Product>> callback) {
        if (filter == null) {
            callback.onSuccess(new ArrayList<>(products));
            return;
        }
        List<Product> filtered = new ArrayList<>();
        for (Product p : products) {
            if (filter.matches(p)) {
                filtered.add(p);
            }
        }
        callback.onSuccess(filtered);
    }

    @Override
    public void getProductById(String productId, ApiCallback<Product> callback) {
        for (Product p : products) {
            if (p.getId().equals(productId)) {
                callback.onSuccess(p);
                return;
            }
        }
        callback.onError("Produit introuvable: " + productId);
    }

    @Override
    public void getCategories(ApiCallback<List<Category>> callback) {
        callback.onSuccess(new ArrayList<>(categories));
    }

    @Override
    public void createProduct(Product product, ApiCallback<Product> callback) {
        if (product.getId() == null || product.getId().isEmpty()) {
            product.setId("prod_" + (products.size() + 1));
        }
        products.add(product);
        callback.onSuccess(product);
    }

    @Override
    public void updateProductModel3d(String productId, String model3dUrl, ApiCallback<Boolean> callback) {
        for (Product p : products) {
            if (p.getId().equals(productId)) {
                p.setModel3dUrl(model3dUrl);
                callback.onSuccess(true);
                return;
            }
        }
        callback.onError("Produit non trouvé pour la mise à jour 3D");
    }

    @Override
    public void getUserProfile(String userId, ApiCallback<User> callback) {
        // 1. Utilisation du nouveau constructeur (id, name, email, avatarUrl)
        User user = new User(
                userId,
                "Utilisateur Marketplace",
                "contact@marketplace.com",
                "https://via.placeholder.com/150"
        );

        // 2. Initialisation des nouveaux champs "deal.mg" via les setters
        user.setAddress("Antananarivo, Madagascar");
        user.setWhatsapp("+261340000000");
        user.setFacebookLink("https://facebook.com/page.vendeur");
        user.setPhoneNumber("0340000000");

        // 3. Simulation de statistiques pour le rôle dynamique
        // Avec 10 ventes, resolveRole() renverra automatiquement UserRole.SELLER
        user.setTotalSales(10);
        user.setResponseScore(4.5);

        callback.onSuccess(user);
    }

    // =====================================================================================
    //  Profil, panier et commandes (mêmes règles que le serveur backend/api)
    // =====================================================================================

    private void seedUsers() {
        seedUser("seller_101", "Atelier Nordique");
        seedUser("seller_102", "Luminaires & Co");
        seedUser("seller_103", "SneakersHub");
    }

    private void seedUser(String id, String name) {
        User user = new User(id, name, null, "");
        user.setCity("Antananarivo");
        users.put(id, user);
    }

    private User profileOf(String uid) {
        User user = users.get(uid);
        if (user == null) {
            user = new User(uid, "Utilisateur", null, "");
            users.put(uid, user);
        }
        return user;
    }

    /** Identifiant de l'utilisateur connecté ; sinon signale l'erreur au callback et renvoie null. */
    private String requireUser(ApiCallback<?> callback) {
        String uid = session != null ? session.getUserId() : null;
        if (uid == null) {
            callback.onError("Connexion requise");
        }
        return uid;
    }

    private Product lookupProduct(String productId) {
        for (Product p : products) {
            if (p.getId().equals(productId)) return p;
        }
        return null;
    }

    private static String firstNonBlank(String... values) {
        for (String v : values) {
            if (v != null && !v.trim().isEmpty()) return v.trim();
        }
        return null;
    }

    @Override
    public void getMyProfile(ApiCallback<User> callback) {
        String uid = requireUser(callback);
        if (uid == null) return;
        callback.onSuccess(profileOf(uid));
    }

    @Override
    public void updateMyProfile(Map<String, String> fields, ApiCallback<User> callback) {
        String uid = requireUser(callback);
        if (uid == null) return;
        User user = profileOf(uid);
        for (Map.Entry<String, String> entry : fields.entrySet()) {
            String value = firstNonBlank(entry.getValue());
            switch (entry.getKey()) {
                case "name": if (value != null) user.setName(value); break;
                case "avatarUrl": user.setAvatarUrl(value); break;
                case "city": user.setCity(value); break;
                case "address": user.setAddress(value); break;
                case "phoneNumber": user.setPhoneNumber(value); break;
                case "whatsapp": user.setWhatsapp(value); break;
                case "facebookLink": user.setFacebookLink(value); break;
                default:
                    callback.onError("Champ non modifiable : " + entry.getKey());
                    return;
            }
        }
        callback.onSuccess(user);
    }

    // ------------------------------------------------------------------------- panier

    private Cart buildCart(String uid) {
        List<CartItem> items = new ArrayList<>();
        Map<String, Integer> lines = carts.get(uid);
        if (lines != null) {
            for (Map.Entry<String, Integer> line : lines.entrySet()) {
                Product p = lookupProduct(line.getKey());
                if (p == null) continue;
                items.add(new CartItem(p.getId(), p.getTitle(), p.getThumbnailUrl(), p.getPrice(),
                        line.getValue(), p.getStock(), p.getSellerId(), p.getSellerName()));
            }
        }
        return new Cart(items);
    }

    private Map<String, Integer> cartLines(String uid) {
        Map<String, Integer> lines = carts.get(uid);
        if (lines == null) {
            lines = new LinkedHashMap<>();
            carts.put(uid, lines);
        }
        return lines;
    }

    /** Message d'erreur si le produit ne peut pas être présent en cette quantité dans le panier, sinon null. */
    private String cartProblem(Product p, String uid, int quantity) {
        if (p == null) return "Produit introuvable";
        if (uid.equals(p.getSellerId())) return "Vous ne pouvez pas acheter votre propre produit";
        if (p.getStatus() == ProductStatus.INDISPONIBLE || p.getStock() <= 0) return "Produit indisponible";
        if (quantity > p.getStock()) return "Stock insuffisant : " + p.getStock() + " disponible(s)";
        return null;
    }

    @Override
    public void getCart(ApiCallback<Cart> callback) {
        String uid = requireUser(callback);
        if (uid == null) return;
        callback.onSuccess(buildCart(uid));
    }

    @Override
    public void addToCart(String productId, int quantity, ApiCallback<Cart> callback) {
        String uid = requireUser(callback);
        if (uid == null) return;
        if (quantity < 1) {
            callback.onError("Quantité invalide");
            return;
        }
        Map<String, Integer> lines = cartLines(uid);
        Integer existing = lines.get(productId);
        int total = (existing != null ? existing : 0) + quantity;
        String problem = cartProblem(lookupProduct(productId), uid, total);
        if (problem != null) {
            callback.onError(problem);
            return;
        }
        lines.put(productId, total);
        callback.onSuccess(buildCart(uid));
    }

    @Override
    public void setCartItemQuantity(String productId, int quantity, ApiCallback<Cart> callback) {
        String uid = requireUser(callback);
        if (uid == null) return;
        if (quantity < 0) {
            callback.onError("Quantité invalide");
            return;
        }
        Map<String, Integer> lines = cartLines(uid);
        if (quantity == 0) {
            lines.remove(productId);
        } else {
            String problem = cartProblem(lookupProduct(productId), uid, quantity);
            if (problem != null) {
                callback.onError(problem);
                return;
            }
            lines.put(productId, quantity);
        }
        callback.onSuccess(buildCart(uid));
    }

    @Override
    public void removeFromCart(String productId, ApiCallback<Cart> callback) {
        String uid = requireUser(callback);
        if (uid == null) return;
        cartLines(uid).remove(productId);
        callback.onSuccess(buildCart(uid));
    }

    @Override
    public void clearCart(ApiCallback<Cart> callback) {
        String uid = requireUser(callback);
        if (uid == null) return;
        cartLines(uid).clear();
        callback.onSuccess(buildCart(uid));
    }

    // --------------------------------------------------------------------- commandes

    @Override
    public void checkout(CheckoutRequest request, ApiCallback<List<Order>> callback) {
        String uid = requireUser(callback);
        if (uid == null) return;
        User me = profileOf(uid);

        String address = firstNonBlank(request != null ? request.getDeliveryAddress() : null, me.getAddress());
        String phone = firstNonBlank(request != null ? request.getDeliveryPhone() : null,
                me.getPhoneNumber(), me.getWhatsapp());
        if (address == null) {
            callback.onError("Adresse de livraison requise (ou renseignez l'adresse de votre profil)");
            return;
        }
        if (phone == null) {
            callback.onError("Téléphone de livraison requis (ou renseignez votre profil)");
            return;
        }

        Map<String, Integer> lines = cartLines(uid);
        if (lines.isEmpty()) {
            callback.onError("Votre panier est vide");
            return;
        }

        // Tout est vérifié AVANT de modifier quoi que ce soit : une commande est complète ou n'existe pas.
        for (Map.Entry<String, Integer> line : lines.entrySet()) {
            if (cartProblem(lookupProduct(line.getKey()), uid, line.getValue()) != null) {
                callback.onError("Certains articles ne sont plus disponibles dans la quantité demandée");
                return;
            }
        }

        // Une commande par vendeur ; le stock est réservé immédiatement.
        Map<String, List<OrderItem>> bySeller = new LinkedHashMap<>();
        for (Map.Entry<String, Integer> line : lines.entrySet()) {
            Product p = lookupProduct(line.getKey());
            List<OrderItem> items = bySeller.get(p.getSellerId());
            if (items == null) {
                items = new ArrayList<>();
                bySeller.put(p.getSellerId(), items);
            }
            items.add(new OrderItem(p.getId(), p.getTitle(), p.getThumbnailUrl(), p.getPrice(), line.getValue()));
            p.setStock(p.getStock() - line.getValue());
        }

        PaymentMethod method = request != null ? request.getPaymentMethod() : null;
        String note = request != null ? request.getNote() : null;
        List<Order> created = new ArrayList<>();
        for (Map.Entry<String, List<OrderItem>> entry : bySeller.entrySet()) {
            orderSequence++;
            Order order = new Order("ord_" + orderSequence, orderSequence, uid, me.getName(),
                    entry.getKey(), profileOf(entry.getKey()).getName(),
                    method, address, phone, note, entry.getValue());
            orders.add(order);
            created.add(order);
        }
        lines.clear();
        callback.onSuccess(created);
    }

    @Override
    public void getOrders(boolean asSeller, ApiCallback<List<Order>> callback) {
        String uid = requireUser(callback);
        if (uid == null) return;
        List<Order> result = new ArrayList<>();
        for (int i = orders.size() - 1; i >= 0; i--) { // les plus récentes d'abord
            Order order = orders.get(i);
            if (asSeller ? uid.equals(order.getSellerId()) : uid.equals(order.getBuyerId())) {
                result.add(order);
            }
        }
        callback.onSuccess(result);
    }

    private Order findOrderFor(String orderId, String uid) {
        for (Order order : orders) {
            if (order.getId().equals(orderId)
                    && (uid.equals(order.getBuyerId()) || uid.equals(order.getSellerId()))) {
                return order;
            }
        }
        return null;
    }

    @Override
    public void getOrderById(String orderId, ApiCallback<Order> callback) {
        String uid = requireUser(callback);
        if (uid == null) return;
        Order order = findOrderFor(orderId, uid);
        if (order == null) {
            callback.onError("Commande introuvable");
            return;
        }
        callback.onSuccess(order);
    }

    // Qui peut faire quoi : vendeur = confirmer / expédier / annuler (avant expédition),
    // acheteur = annuler tant qu'en attente, et confirmer la RÉCEPTION (ce qui compte comme une vente).
    private static boolean sellerMay(OrderStatus from, OrderStatus to) {
        switch (from) {
            case EN_ATTENTE: return to == OrderStatus.CONFIRMEE || to == OrderStatus.ANNULEE;
            case CONFIRMEE: return to == OrderStatus.EXPEDIEE || to == OrderStatus.ANNULEE;
            default: return false;
        }
    }

    private static boolean buyerMay(OrderStatus from, OrderStatus to) {
        switch (from) {
            case EN_ATTENTE: return to == OrderStatus.ANNULEE;
            case EXPEDIEE: return to == OrderStatus.LIVREE;
            default: return false;
        }
    }

    @Override
    public void updateOrderStatus(String orderId, OrderStatus newStatus, ApiCallback<Order> callback) {
        String uid = requireUser(callback);
        if (uid == null) return;
        Order order = findOrderFor(orderId, uid);
        if (order == null) {
            callback.onError("Commande introuvable");
            return;
        }

        boolean isSeller = uid.equals(order.getSellerId());
        OrderStatus from = order.getStatus();
        boolean allowed = isSeller ? sellerMay(from, newStatus) : buyerMay(from, newStatus);
        if (!allowed) {
            boolean otherPartyMay = isSeller ? buyerMay(from, newStatus) : sellerMay(from, newStatus);
            callback.onError(otherPartyMay
                    ? "Action non autorisée pour votre rôle"
                    : "Transition impossible : " + from + " -> " + newStatus);
            return;
        }

        order.applyStatus(newStatus);

        if (newStatus == OrderStatus.LIVREE) {
            // Réception confirmée par l'acheteur : +1 vente pour le vendeur (sert au calcul du niveau)
            User seller = profileOf(order.getSellerId());
            seller.setTotalSales(seller.getTotalSales() + 1);
        } else if (newStatus == OrderStatus.ANNULEE) {
            // Le stock réservé au passage en caisse est restitué
            for (OrderItem item : order.getItems()) {
                Product p = item.getProductId() != null ? lookupProduct(item.getProductId()) : null;
                if (p == null) continue;
                boolean wasSoldOut = p.getStock() <= 0;
                p.setStock(p.getStock() + item.getQuantity());
                if (wasSoldOut && p.getStatus() == ProductStatus.INDISPONIBLE) {
                    p.setStatus(ProductStatus.DISPONIBLE);
                }
            }
        }
        callback.onSuccess(order);
    }
}
