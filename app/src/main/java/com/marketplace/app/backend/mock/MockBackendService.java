package com.marketplace.app.backend.mock;

import com.marketplace.app.backend.AuthSession;
import com.marketplace.app.model.Cart;
import com.marketplace.app.model.CartItem;
import com.marketplace.app.model.Category;
import com.marketplace.app.model.ContactChannel;
import com.marketplace.app.model.ContactRequest;
import com.marketplace.app.model.FilterCriteria;
import com.marketplace.app.model.ImportAgencyConfig;
import com.marketplace.app.model.ImportOrder;
import com.marketplace.app.model.ImportStatusHistoryEntry;
import com.marketplace.app.model.ImportTrackingStatus;
import com.marketplace.app.model.Order;
import com.marketplace.app.model.OrderItem;
import com.marketplace.app.model.OrderStatus;
import com.marketplace.app.model.Payment;
import com.marketplace.app.model.PaymentMethod;
import com.marketplace.app.model.PaymentStatus;
import com.marketplace.app.model.Product;
import com.marketplace.app.model.ProductStatus;
import com.marketplace.app.model.User;
import com.marketplace.app.model.UserRole;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Rôle : Simulation complète et fidèle en mémoire du backend PostgreSQL pour mode démo / tests JVM.
 * Entrée : Appels synchrones/asynchrones sans dépendance réseau.
 * Sortie : Validation des règles de gestion (stock atomique, attribution des ventes, statuts).
 */
public class MockBackendService {

    private static MockBackendService instance;

    private final List<Product> products = new ArrayList<>();
    private final List<Category> categories = new ArrayList<>();
    private final Map<String, User> users = new LinkedHashMap<>();
    private final Map<String, Map<String, Integer>> carts = new HashMap<>();
    private final List<Order> orders = new ArrayList<>();
    private final List<ContactRequest> contactRequests = new ArrayList<>();
    private final Map<String, Payment> payments = new HashMap<>();
    private final List<ImportOrder> importOrders = new ArrayList<>();
    private ImportAgencyConfig importAgencyConfig = ImportAgencyConfig.createDefaultConfig();
    private long orderSequence = 0;
    private long contactSequence = 0;
    private long importOrderSequence = 0;

    public MockBackendService() {
        initInitialData();
        seedUsers();
    }

    public static synchronized MockBackendService getInstance() {
        if (instance == null) {
            instance = new MockBackendService();
        }
        return instance;
    }

    public synchronized void resetData() {
        products.clear();
        categories.clear();
        users.clear();
        carts.clear();
        orders.clear();
        contactRequests.clear();
        payments.clear();
        importOrders.clear();
        importAgencyConfig = ImportAgencyConfig.createDefaultConfig();
        orderSequence = 0;
        contactSequence = 0;
        importOrderSequence = 0;
        initInitialData();
        seedUsers();
    }

    private void initInitialData() {
        categories.add(new Category("cat_furniture", "Mobilier & Déco", "Meubles et objets scannables en 3D", "ic_furniture"));
        categories.add(new Category("cat_electronics", "Électronique", "Appareils, gadgets et accessoires", "ic_electronics"));
        categories.add(new Category("cat_fashion", "Mode & Chaussures", "Vêtements, sacs et sneakers avec essayage AR", "ic_fashion"));
        categories.add(new Category("cat_art", "Art & Sculptures", "Objets uniques avec modèles 3D .glb", "ic_art"));

        Product p1 = new Product(
                "prod_1",
                "Fauteuil Scandinave Velours",
                "Fauteuil confortable avec structure bois massif. Modèle 3D .glb haute fidélité compatible ARCore.",
                189.99,
                ProductStatus.DISPONIBLE,
                "cat_furniture",
                "seller_101",
                "Atelier Nordique",
                "https://via.placeholder.com/400x300.png?text=Fauteuil+Nordique",
                true,
                "https://modelviewer.dev/shared-assets/models/Astronaut.glb",
                Arrays.asList(
                        "https://via.placeholder.com/400x300.png?text=Fauteuil+Nordique+Vue+1",
                        "https://via.placeholder.com/400x300.png?text=Fauteuil+Nordique+Vue+2"
                ),
                5,
                System.currentTimeMillis() - 86400000
        );
        p1.setModel3dFormat("glb");
        p1.setModel3dSize(2457600L);

        Product p2 = new Product(
                "prod_2",
                "Lampe Design Industrielle",
                "Lampe à poser en acier brossé. Placez et dupliquez la lampe directement sur votre table en AR.",
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
        p2.setModel3dFormat("glb");
        p2.setModel3dSize(1843200L);

        Product p3 = new Product(
                "prod_3",
                "Sneakers Édition Limitée",
                "Paire de sneakers exclusives. Modèle 3D .glb interactif à 360°.",
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
        p3.setModel3dFormat("glb");
        p3.setModel3dSize(1126400L);

        Product p4 = new Product(
                "prod_4",
                "Table Basse Bois de Palissandre",
                "Table basse artisanale malgache en palissandre massif (produit local sans modèle 3D, galerie photos uniquement).",
                145.00,
                ProductStatus.DISPONIBLE,
                "cat_furniture",
                "seller_101",
                "Atelier Nordique",
                "https://via.placeholder.com/400x300.png?text=Table+Palissandre",
                false,
                "",
                Arrays.asList(
                        "https://via.placeholder.com/400x300.png?text=Table+Palissandre+1",
                        "https://via.placeholder.com/400x300.png?text=Table+Palissandre+2"
                ),
                4,
                System.currentTimeMillis() - 10000000
        );

        products.add(p1);
        products.add(p2);
        products.add(p3);
        products.add(p4);
    }

    private void seedUsers() {
        seedUser("seller_101", "Atelier Nordique", "+261341234567", "https://facebook.com/atelier.nordique");
        seedUser("seller_102", "Luminaires & Co", "+261331122334", "https://m.me/luminaires.co");
        seedUser("seller_103", "SneakersHub", "+261320011223", "https://facebook.com/sneakershub");
    }

    private void seedUser(String id, String name, String whatsapp, String fb) {
        User user = new User(id, name, id + "@example.mg", "https://via.placeholder.com/150");
        user.setCity("Antananarivo");
        user.setAddress("Lot IV Antananarivo");
        user.setWhatsapp(whatsapp);
        user.setPhoneNumber(whatsapp);
        user.setFacebookLink(fb);
        users.put(id, user);
    }

    public synchronized User getOrCreateProfile(String uid) {
        User user = users.get(uid);
        if (user == null) {
            user = new User(uid, "Utilisateur", uid + "@marketplace.mg", "");
            user.setCity("Antananarivo");
            users.put(uid, user);
        }
        return user;
    }

    // --- Catalogue
    public synchronized List<Category> getCategories() {
        return new ArrayList<>(categories);
    }

    public synchronized List<Product> getProducts(FilterCriteria filter) {
        if (filter == null) return new ArrayList<>(products);
        List<Product> result = new ArrayList<>();
        for (Product p : products) {
            if (filter.matches(p)) {
                result.add(p);
            }
        }
        return result;
    }

    public synchronized Product getProductById(String productId) {
        for (Product p : products) {
            if (p.getId().equals(productId)) return p;
        }
        return null;
    }

    public synchronized Product createProduct(Product product) {
        if (product.getId() == null || product.getId().trim().isEmpty()) {
            product.setId("prod_" + (products.size() + 1));
        }
        product.setUpdatedAt(System.currentTimeMillis());
        products.add(product);
        return product;
    }

    public synchronized Product updateProduct(Product updated) {
        if (updated == null || updated.getId() == null) return updated;
        for (int i = 0; i < products.size(); i++) {
            if (updated.getId().equals(products.get(i).getId())) {
                updated.setUpdatedAt(System.currentTimeMillis());
                products.set(i, updated);
                return updated;
            }
        }
        products.add(updated);
        return updated;
    }

    public synchronized boolean updateProductModel3d(String productId, String model3dUrl) {
        Product p = getProductById(productId);
        if (p != null) {
            if (model3dUrl == null || model3dUrl.trim().isEmpty()) {
                p.clear3DModel();
            } else {
                p.setModel3dUrl(model3dUrl);
                p.setModel3dFormat("glb");
                p.setUpdatedAt(System.currentTimeMillis());
            }
            return true;
        }
        return false;
    }

    // --- Panier
    private Map<String, Integer> cartLines(String uid) {
        Map<String, Integer> lines = carts.get(uid);
        if (lines == null) {
            lines = new LinkedHashMap<>();
            carts.put(uid, lines);
        }
        return lines;
    }

    public synchronized Cart getCart(String uid) {
        if (uid == null) return new Cart();
        Map<String, Integer> lines = cartLines(uid);
        List<CartItem> items = new ArrayList<>();
        for (Map.Entry<String, Integer> line : lines.entrySet()) {
            Product p = getProductById(line.getKey());
            if (p != null) {
                items.add(new CartItem(p.getId(), p.getTitle(), p.getThumbnailUrl(), p.getPrice(),
                        line.getValue(), p.getStock(), p.getSellerId(), p.getSellerName()));
            }
        }
        return new Cart(items);
    }

    public synchronized Cart addToCart(String uid, String productId, int quantity) throws Exception {
        if (uid == null) throw new IllegalStateException("Connexion requise");
        if (quantity < 1) throw new IllegalArgumentException("Quantité invalide");
        Product p = getProductById(productId);
        if (p == null) throw new IllegalArgumentException("Produit introuvable");
        if (uid.equals(p.getSellerId())) throw new IllegalStateException("Vous ne pouvez pas acheter votre propre produit");
        if (p.getStatus() == ProductStatus.INDISPONIBLE || p.getStock() <= 0) throw new IllegalStateException("Produit indisponible");

        Map<String, Integer> lines = cartLines(uid);
        int current = lines.getOrDefault(productId, 0);
        int target = current + quantity;
        if (target > p.getStock()) {
            throw new IllegalStateException("Stock insuffisant : " + p.getStock() + " disponible(s)");
        }
        lines.put(productId, target);
        return getCart(uid);
    }

    public synchronized Cart setCartItemQuantity(String uid, String productId, int quantity) throws Exception {
        if (uid == null) throw new IllegalStateException("Connexion requise");
        if (quantity < 0) throw new IllegalArgumentException("Quantité invalide");
        Map<String, Integer> lines = cartLines(uid);
        if (quantity == 0) {
            lines.remove(productId);
        } else {
            Product p = getProductById(productId);
            if (p == null) throw new IllegalArgumentException("Produit introuvable");
            if (quantity > p.getStock()) {
                throw new IllegalStateException("Stock insuffisant : " + p.getStock() + " disponible(s)");
            }
            lines.put(productId, quantity);
        }
        return getCart(uid);
    }

    public synchronized Cart removeFromCart(String uid, String productId) {
        if (uid != null) {
            cartLines(uid).remove(productId);
        }
        return getCart(uid);
    }

    public synchronized Cart clearCart(String uid) {
        if (uid != null) {
            cartLines(uid).clear();
        }
        return getCart(uid);
    }

    // --- Commandes
    public synchronized List<Order> checkout(String uid, String deliveryAddress, String deliveryPhone,
                                             PaymentMethod paymentMethod, String note) throws Exception {
        if (uid == null) throw new IllegalStateException("Connexion requise");
        User buyer = getOrCreateProfile(uid);

        String addr = (deliveryAddress != null && !deliveryAddress.trim().isEmpty()) ? deliveryAddress : buyer.getAddress();
        String phone = (deliveryPhone != null && !deliveryPhone.trim().isEmpty()) ? deliveryPhone :
                (buyer.getPhoneNumber() != null ? buyer.getPhoneNumber() : buyer.getWhatsapp());

        if (addr == null || addr.trim().isEmpty()) {
            throw new IllegalArgumentException("Adresse de livraison requise (ou renseignez l'adresse de votre profil)");
        }
        if (phone == null || phone.trim().isEmpty()) {
            throw new IllegalArgumentException("Téléphone de livraison requis (ou renseignez votre profil)");
        }

        Map<String, Integer> lines = cartLines(uid);
        if (lines.isEmpty()) {
            throw new IllegalStateException("Votre panier est vide");
        }

        // Vérification atomique de stock
        for (Map.Entry<String, Integer> entry : lines.entrySet()) {
            Product p = getProductById(entry.getKey());
            if (p == null || p.getStatus() == ProductStatus.INDISPONIBLE || p.getStock() < entry.getValue()) {
                throw new IllegalStateException("Certains articles ne sont plus disponibles dans la quantité demandée");
            }
        }

        // Groupement par vendeur
        Map<String, List<OrderItem>> bySeller = new LinkedHashMap<>();
        for (Map.Entry<String, Integer> entry : lines.entrySet()) {
            Product p = getProductById(entry.getKey());
            if (!bySeller.containsKey(p.getSellerId())) {
                bySeller.put(p.getSellerId(), new ArrayList<>());
            }
            bySeller.get(p.getSellerId()).add(new OrderItem(p.getId(), p.getTitle(), p.getThumbnailUrl(), p.getPrice(), entry.getValue()));
            p.setStock(p.getStock() - entry.getValue());
        }

        List<Order> created = new ArrayList<>();
        for (Map.Entry<String, List<OrderItem>> entry : bySeller.entrySet()) {
            orderSequence++;
            String sellerId = entry.getKey();
            User seller = getOrCreateProfile(sellerId);
            Order o = new Order("ord_" + orderSequence, orderSequence, uid, buyer.getName(),
                    sellerId, seller.getName(), paymentMethod, addr, phone, note, entry.getValue());
            orders.add(o);
            created.add(o);

            // Initialisation paiement déclaratif
            Payment payment = new Payment("pay_" + o.getId(), o.getId(), o.getPaymentMethod(),
                    PaymentStatus.EN_ATTENTE, o.getTotal(), "TX-" + o.getReference());
            payments.put(o.getId(), payment);
        }
        lines.clear();
        return created;
    }

    public synchronized List<Order> getOrders(String uid, boolean asSeller) {
        if (uid == null) return new ArrayList<>();
        List<Order> result = new ArrayList<>();
        for (int i = orders.size() - 1; i >= 0; i--) {
            Order o = orders.get(i);
            if (asSeller ? uid.equals(o.getSellerId()) : uid.equals(o.getBuyerId())) {
                result.add(o);
            }
        }
        return result;
    }

    public synchronized Order getOrderById(String orderId) {
        for (Order o : orders) {
            if (o.getId().equals(orderId)) return o;
        }
        return null;
    }

    public synchronized Order updateOrderStatus(String uid, String orderId, OrderStatus newStatus) throws Exception {
        if (uid == null) throw new IllegalStateException("Connexion requise");
        Order order = getOrderById(orderId);
        if (order == null) throw new IllegalArgumentException("Commande introuvable");

        boolean isSeller = uid.equals(order.getSellerId());
        boolean isBuyer = uid.equals(order.getBuyerId());
        if (!isSeller && !isBuyer) throw new IllegalStateException("Accès interdit à cette commande");

        OrderStatus current = order.getStatus();
        boolean allowed = false;
        if (isSeller) {
            if (current == OrderStatus.EN_ATTENTE && (newStatus == OrderStatus.CONFIRMEE || newStatus == OrderStatus.ANNULEE)) allowed = true;
            if (current == OrderStatus.CONFIRMEE && (newStatus == OrderStatus.EXPEDIEE || newStatus == OrderStatus.ANNULEE)) allowed = true;
        } else {
            if (current == OrderStatus.EN_ATTENTE && newStatus == OrderStatus.ANNULEE) allowed = true;
            if (current == OrderStatus.EXPEDIEE && newStatus == OrderStatus.LIVREE) allowed = true;
        }

        if (!allowed) {
            boolean otherPartyAllowed = isSeller ?
                    (current == OrderStatus.EXPEDIEE && newStatus == OrderStatus.LIVREE) :
                    (current == OrderStatus.EN_ATTENTE && newStatus == OrderStatus.CONFIRMEE || current == OrderStatus.CONFIRMEE && newStatus == OrderStatus.EXPEDIEE);
            if (otherPartyAllowed) {
                throw new IllegalStateException("Action non autorisée pour votre rôle");
            }
            throw new IllegalStateException("Transition impossible : " + current + " -> " + newStatus);
        }

        order.applyStatus(newStatus);

        if (newStatus == OrderStatus.LIVREE) {
            User seller = getOrCreateProfile(order.getSellerId());
            seller.setTotalSales(seller.getTotalSales() + 1);
        } else if (newStatus == OrderStatus.ANNULEE) {
            // Restitution de stock
            for (OrderItem item : order.getItems()) {
                Product p = getProductById(item.getProductId());
                if (p != null) {
                    boolean wasEmpty = p.getStock() <= 0;
                    p.setStock(p.getStock() + item.getQuantity());
                    if (wasEmpty && p.getStatus() == ProductStatus.INDISPONIBLE) {
                        p.setStatus(ProductStatus.DISPONIBLE);
                    }
                }
            }
        }
        return order;
    }

    // --- Contacts & Paiements
    public synchronized ContactRequest addContactRequest(ContactRequest req) {
        contactSequence++;
        if (req.getId() == null || req.getId().isEmpty()) {
            req.setId("cnt_" + contactSequence);
        }
        contactRequests.add(req);
        return req;
    }

    public synchronized List<ContactRequest> getContactRequests(String uid, boolean asSeller) {
        List<ContactRequest> result = new ArrayList<>();
        for (ContactRequest c : contactRequests) {
            if (asSeller ? uid.equals(c.getSellerId()) : uid.equals(c.getBuyerId())) {
                result.add(c);
            }
        }
        return result;
    }

    public synchronized Payment getPayment(String orderId) {
        return payments.get(orderId);
    }

    public synchronized Payment updatePaymentStatus(String orderId, PaymentStatus status) {
        Payment p = payments.get(orderId);
        if (p != null) {
            p.setStatus(status);
            p.setUpdatedAt(System.currentTimeMillis());
        }
        return p;
    }

    // --- Service d'Importation 1688 (Simulation locale explicitement identifiée)
    public synchronized ImportAgencyConfig getImportAgencyConfig() {
        return importAgencyConfig;
    }

    public synchronized ImportAgencyConfig updateImportAgencyConfig(ImportAgencyConfig updated, String adminUid) {
        List<String> errors = updated.validate();
        if (!errors.isEmpty()) {
            throw new IllegalArgumentException(errors.get(0));
        }
        updated.setExchangeRateUpdatedAt(System.currentTimeMillis());
        updated.setExchangeRateUpdatedBy(adminUid);
        this.importAgencyConfig = updated;
        return this.importAgencyConfig;
    }

    public synchronized ImportOrder createImportOrder(ImportOrder draft) {
        List<String> errors = draft.validate();
        if (!errors.isEmpty()) {
            throw new IllegalArgumentException(errors.get(0));
        }
        importOrderSequence++;
        long now = System.currentTimeMillis();
        String id = String.format(java.util.Locale.US, "IMP1688-%05d", importOrderSequence);

        draft.setImportOrderId(id);
        draft.setPaymentStatus(PaymentStatus.EN_ATTENTE);
        draft.setTrackingStatus(ImportTrackingStatus.PAIEMENT_EN_ATTENTE);
        draft.setAmountConfirmedByAdmin(false);
        draft.setCreatedAt(now);
        draft.setUpdatedAt(now);
        draft.setSimulated(true);

        // Coordonnées de l'importateur désigné par l'agence
        draft.setImporterName(importAgencyConfig.getImporterName());
        draft.setImporterCompany(importAgencyConfig.getImporterCompany());
        draft.setImporterPhone(importAgencyConfig.getImporterPhone());
        draft.setImporterWhatsapp(importAgencyConfig.getImporterWhatsapp());
        draft.setImporterAddress(importAgencyConfig.getImporterAddress());
        draft.setImporterInstructions(importAgencyConfig.getImporterInstructions());

        List<ImportStatusHistoryEntry> history = new ArrayList<>();
        history.add(new ImportStatusHistoryEntry(
                ImportTrackingStatus.PAIEMENT_EN_ATTENTE,
                now,
                "Demande d'achat soumise avec estimation",
                draft.getUserId() != null ? draft.getUserId() : "client"
        ));
        draft.setStatusHistory(history);

        importOrders.add(draft);
        return draft;
    }

    public synchronized List<ImportOrder> getImportOrders(String uid, boolean asAdmin, String searchId) {
        List<ImportOrder> result = new ArrayList<>();
        String q = searchId != null ? searchId.trim().toLowerCase() : "";
        for (int i = importOrders.size() - 1; i >= 0; i--) {
            ImportOrder o = importOrders.get(i);
            if (!asAdmin && (uid == null || !uid.equals(o.getUserId()))) {
                continue;
            }
            if (!q.isEmpty()) {
                boolean matchId = o.getImportOrderId() != null && o.getImportOrderId().toLowerCase().contains(q);
                boolean matchName = o.getProductName() != null && o.getProductName().toLowerCase().contains(q);
                boolean matchCustomer = o.getCustomerName() != null && o.getCustomerName().toLowerCase().contains(q);
                if (!matchId && !matchName && !matchCustomer) {
                    continue;
                }
            }
            result.add(o);
        }
        return result;
    }

    public synchronized ImportOrder getImportOrderById(String importOrderId) {
        if (importOrderId == null) return null;
        for (ImportOrder o : importOrders) {
            if (importOrderId.equalsIgnoreCase(o.getImportOrderId())) {
                return o;
            }
        }
        return null;
    }

    public synchronized ImportOrder confirmImportOrderAmount(
            String importOrderId,
            double verifiedPriceCny,
            double exchangeRate,
            double serviceFeeMga,
            String adminUid
    ) {
        ImportOrder order = getImportOrderById(importOrderId);
        if (order == null) throw new IllegalArgumentException("Commande Import 1688 introuvable");
        if (verifiedPriceCny <= 0 || exchangeRate <= 0 || serviceFeeMga < 0) {
            throw new IllegalArgumentException("Montants ou taux invalides");
        }
        double estimatedMga = Math.round(verifiedPriceCny * order.getQuantity() * exchangeRate);
        double totalMga = Math.round(estimatedMga + serviceFeeMga);

        order.setPriceCNY(verifiedPriceCny);
        order.setExchangeRate(exchangeRate);
        order.setEstimatedPriceMGA(estimatedMga);
        order.setServiceFeeMGA(serviceFeeMga);
        order.setTotalAmountMGA(totalMga);
        order.setAmountConfirmedByAdmin(true);
        order.setUpdatedAt(System.currentTimeMillis());
        return order;
    }

    public synchronized ImportOrder submitImportPaymentReference(String importOrderId, String paymentRef) {
        ImportOrder order = getImportOrderById(importOrderId);
        if (order == null) throw new IllegalArgumentException("Commande Import 1688 introuvable");
        if (!order.isAmountConfirmedByAdmin()) {
            throw new IllegalStateException("Le montant définitif doit d'abord être confirmé par l'agence avant le règlement MVola");
        }
        if (paymentRef == null || paymentRef.trim().isEmpty()) {
            throw new IllegalArgumentException("La référence de transaction MVola est requise");
        }
        order.setPaymentReference(paymentRef.trim());
        // Règle stricte : la saisie client maintient le statut à EN_ATTENTE jusqu'à vérification admin
        order.setPaymentStatus(PaymentStatus.EN_ATTENTE);
        order.setTrackingStatus(ImportTrackingStatus.PAIEMENT_EN_ATTENTE);
        order.setUpdatedAt(System.currentTimeMillis());
        return order;
    }

    public synchronized ImportOrder verifyImportPayment(
            String importOrderId,
            PaymentStatus newPaymentStatus,
            String note,
            String adminUid
    ) {
        ImportOrder order = getImportOrderById(importOrderId);
        if (order == null) throw new IllegalArgumentException("Commande Import 1688 introuvable");
        if (newPaymentStatus != PaymentStatus.PAYE && newPaymentStatus != PaymentStatus.ECHOUE) {
            throw new IllegalArgumentException("Statut de vérification invalide : PAYE ou ECHOUE attendu");
        }
        long now = System.currentTimeMillis();
        order.setPaymentStatus(newPaymentStatus);
        order.setUpdatedAt(now);

        if (newPaymentStatus == PaymentStatus.PAYE) {
            order.setTrackingStatus(ImportTrackingStatus.PAIEMENT_CONFIRME);
            order.getStatusHistory().add(new ImportStatusHistoryEntry(
                    ImportTrackingStatus.PAIEMENT_CONFIRME,
                    now,
                    note != null && !note.trim().isEmpty() ? note.trim() : "Paiement MVola vérifié (" + order.getPaymentReference() + ")",
                    adminUid
            ));
        } else {
            order.setTrackingStatus(ImportTrackingStatus.PAIEMENT_EN_ATTENTE);
            order.getStatusHistory().add(new ImportStatusHistoryEntry(
                    ImportTrackingStatus.PAIEMENT_EN_ATTENTE,
                    now,
                    note != null && !note.trim().isEmpty() ? "Paiement refusé : " + note.trim() : "Paiement MVola refusé après vérification",
                    adminUid
            ));
        }
        return order;
    }

    public synchronized ImportOrder recordReal1688Purchase(
            String importOrderId,
            String supplierOrderRef,
            double actualPaidCny,
            String purchaseDate,
            String note,
            String adminUid
    ) {
        ImportOrder order = getImportOrderById(importOrderId);
        if (order == null) throw new IllegalArgumentException("Commande Import 1688 introuvable");
        if (order.getPaymentStatus() != PaymentStatus.PAYE) {
            throw new IllegalStateException("Le paiement MVola doit être vérifié (PAYE) avant d'enregistrer l'achat réel sur 1688");
        }
        if (supplierOrderRef == null || supplierOrderRef.trim().isEmpty()) {
            throw new IllegalArgumentException("La référence de commande fournisseur 1688 est obligatoire");
        }
        if (actualPaidCny <= 0) {
            throw new IllegalArgumentException("Le montant réel payé en CNY doit être supérieur à 0");
        }
        long now = System.currentTimeMillis();
        order.setSupplierOrderReference(supplierOrderRef.trim());
        order.setActualPurchasePriceCNY(actualPaidCny);
        order.setPurchaseDate(purchaseDate != null && !purchaseDate.trim().isEmpty()
                ? purchaseDate.trim()
                : new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.FRANCE).format(new java.util.Date(now)));
        order.setAdminPurchaseNote(note);
        order.setTrackingStatus(ImportTrackingStatus.ACHAT_EFFECTUE);
        order.setUpdatedAt(now);

        order.getStatusHistory().add(new ImportStatusHistoryEntry(
                ImportTrackingStatus.ACHAT_EFFECTUE,
                now,
                "Achat 1688 réf. " + supplierOrderRef.trim() + " (" + actualPaidCny + " ¥)"
                        + (note != null && !note.trim().isEmpty() ? " - " + note.trim() : ""),
                adminUid
        ));
        return order;
    }

    public synchronized ImportOrder updateImportTrackingStatus(
            String importOrderId,
            ImportTrackingStatus nextStatus,
            String note,
            String adminUid
    ) {
        ImportOrder order = getImportOrderById(importOrderId);
        if (order == null) throw new IllegalArgumentException("Commande Import 1688 introuvable");
        if (nextStatus == null) throw new IllegalArgumentException("Nouveau statut requis");
        if (order.getPaymentStatus() != PaymentStatus.PAYE && nextStatus.getStepOrder() >= ImportTrackingStatus.PAIEMENT_CONFIRME.getStepOrder()) {
            throw new IllegalStateException("Impossible d'avancer le suivi : le paiement n'est pas confirmé");
        }
        if (nextStatus.getStepOrder() >= ImportTrackingStatus.PREPARATION_FOURNISSEUR.getStepOrder()
                && (order.getSupplierOrderReference() == null || order.getSupplierOrderReference().isEmpty())) {
            throw new IllegalStateException("L'achat réel sur 1688 doit être enregistré avant l'étape " + nextStatus.name());
        }

        long now = System.currentTimeMillis();
        order.setTrackingStatus(nextStatus);
        // Synchroniser les coordonnées de l'importateur courant
        order.setImporterName(importAgencyConfig.getImporterName());
        order.setImporterCompany(importAgencyConfig.getImporterCompany());
        order.setImporterPhone(importAgencyConfig.getImporterPhone());
        order.setImporterWhatsapp(importAgencyConfig.getImporterWhatsapp());
        order.setImporterAddress(importAgencyConfig.getImporterAddress());
        order.setImporterInstructions(importAgencyConfig.getImporterInstructions());
        order.setUpdatedAt(now);

        order.getStatusHistory().add(new ImportStatusHistoryEntry(
                nextStatus,
                now,
                note != null && !note.trim().isEmpty() ? note.trim() : nextStatus.getDescription(),
                adminUid
        ));
        return order;
    }
}
