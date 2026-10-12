package com.marketplace.core.navigation;

import android.content.Context;
import android.util.Log;

/**
 * Registre central de routage (Monolithe Modulaire).
 * 
 * Règle d'or de l'architecture :
 * - Lors des tests unitaires d'un module, les autres modules ne sont PAS appelés.
 *   Si un routeur n'est pas enregistré, une implémentation No-Op (sans crash) est utilisée.
 * - Lors de l'assemblage complet (Grand Test / Application réelle), le module ':app'
 *   enregistre les implémentations concrètes de chaque routeur.
 */
public final class RouterRegistry {

    private static final String TAG = "RouterRegistry";
    private static volatile RouterRegistry sInstance;

    private ProductRouter productRouter;
    private ArRouter arRouter;
    private ScannerRouter scannerRouter;
    private ChatRouter chatRouter;
    private AuthRouter authRouter;

    private RouterRegistry() {
        // Initialiser avec des routeurs factices (No-Op) par défaut pour éviter tout NullPointerException
        resetToNoOp();
    }

    public static RouterRegistry getInstance() {
        if (sInstance == null) {
            synchronized (RouterRegistry.class) {
                if (sInstance == null) {
                    sInstance = new RouterRegistry();
                }
            }
        }
        return sInstance;
    }

    public void registerProductRouter(ProductRouter router) {
        this.productRouter = router != null ? router : createNoOpProductRouter();
    }

    public void registerArRouter(ArRouter router) {
        this.arRouter = router != null ? router : createNoOpArRouter();
    }

    public void registerScannerRouter(ScannerRouter router) {
        this.scannerRouter = router != null ? router : createNoOpScannerRouter();
    }

    public void registerChatRouter(ChatRouter router) {
        this.chatRouter = router != null ? router : createNoOpChatRouter();
    }

    public void registerAuthRouter(AuthRouter router) {
        this.authRouter = router != null ? router : createNoOpAuthRouter();
    }

    public ProductRouter getProductRouter() { return productRouter; }
    public ArRouter getArRouter() { return arRouter; }
    public ScannerRouter getScannerRouter() { return scannerRouter; }
    public ChatRouter getChatRouter() { return chatRouter; }
    public AuthRouter getAuthRouter() { return authRouter; }

    public void resetToNoOp() {
        this.productRouter = createNoOpProductRouter();
        this.arRouter = createNoOpArRouter();
        this.scannerRouter = createNoOpScannerRouter();
        this.chatRouter = createNoOpChatRouter();
        this.authRouter = createNoOpAuthRouter();
    }

    private static void logSafe(String msg) {
        try {
            Log.d(TAG, msg);
        } catch (Throwable t) {
            System.out.println(TAG + ": " + msg);
        }
    }

    private ProductRouter createNoOpProductRouter() {
        return new ProductRouter() {
            @Override public void openCatalog(Context context) { logSafe("[NoOp] openCatalog"); }
            @Override public void openCatalogWithCategory(Context context, String categoryId, String categoryName) {
                logSafe("[NoOp] openCatalogWithCategory: " + categoryId);
            }
            @Override public void openProductDetail(Context context, String productId) {
                logSafe("[NoOp] openProductDetail: " + productId);
            }
        };
    }

    private ArRouter createNoOpArRouter() {
        return new ArRouter() {
            @Override public void openArViewer(Context context, String productId, String productName, String model3dUrl) {
                logSafe("[NoOp] openArViewer: " + productId);
            }
            @Override public void open3dPreview(Context context, String productId, String model3dUrl) {
                logSafe("[NoOp] open3dPreview: " + productId);
            }
        };
    }

    private ScannerRouter createNoOpScannerRouter() {
        return new ScannerRouter() {
            @Override public void openScanner(Context context, String productId) {
                logSafe("[NoOp] openScanner: " + productId);
            }
            @Override public void openSamJobStatus(Context context, String jobId) {
                logSafe("[NoOp] openSamJobStatus: " + jobId);
            }
        };
    }

    private ChatRouter createNoOpChatRouter() {
        return new ChatRouter() {
            @Override public void openChatWithSeller(Context context, String sellerId, String sellerName, String productId, String productTitle) {
                logSafe("[NoOp] openChatWithSeller: " + sellerId);
            }
            @Override public void openConversationsList(Context context) {
                logSafe("[NoOp] openConversationsList");
            }
        };
    }

    private AuthRouter createNoOpAuthRouter() {
        return new AuthRouter() {
            @Override public void openLogin(Context context) { logSafe("[NoOp] openLogin"); }
            @Override public void openRegister(Context context) { logSafe("[NoOp] openRegister"); }
            @Override public void openProfile(Context context, String userId) { logSafe("[NoOp] openProfile: " + userId); }
        };
    }
}
