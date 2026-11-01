package com.marketplace.app.backend.repository;

import com.marketplace.app.backend.ApiClient;
import com.marketplace.app.backend.AuthSession;
import com.marketplace.app.backend.mock.MockBackendService;
import com.marketplace.app.base.BaseRepository;
import com.marketplace.app.feature.import1688.AdminAccessControl;
import com.marketplace.app.feature.import1688.CurrencyConversionService;
import com.marketplace.app.model.ImportAgencyConfig;
import com.marketplace.app.model.ImportOrder;
import com.marketplace.app.model.ImportTrackingStatus;
import com.marketplace.app.model.PaymentStatus;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Response;

/**
 * Rôle : Repository centralisant les demandes d'achat Import 1688, la validation MVola et le suivi logistique.
 * Entrée : Appels API REST /api/import1688/* ou simulation en mémoire explicitement marquée [SIMULÉ].
 * Sortie : Objets ImportOrder, ImportAgencyConfig et vérification stricte des droits administrateur.
 */
public class ImportOrderRepository extends BaseRepository<ImportOrder> {

    private final boolean useMock;

    public ImportOrderRepository() {
        this(ApiClient.isUseMock());
    }

    public ImportOrderRepository(boolean useMock) {
        this.useMock = useMock;
    }

    public void getAgencyConfig(Callback<ImportAgencyConfig> callback) {
        if (useMock) {
            ImportAgencyConfig cfg = MockBackendService.getInstance().getImportAgencyConfig();
            CurrencyConversionService.getInstance().setConfig(cfg);
            postSuccess(callback, cfg);
            return;
        }

        ApiClient.getApi().getImportAgencyConfig().enqueue(new retrofit2.Callback<ImportAgencyConfig>() {
            @Override
            public void onResponse(Call<ImportAgencyConfig> call, Response<ImportAgencyConfig> response) {
                if (response.isSuccessful() && response.body() != null) {
                    CurrencyConversionService.getInstance().setConfig(response.body());
                    postSuccess(callback, response.body());
                } else if (response.code() == 404) {
                    postError(callback, "Route /api/import1688/config non implémentée sur le backend (404)");
                } else {
                    postError(callback, parseHttpError(response));
                }
            }

            @Override
            public void onFailure(Call<ImportAgencyConfig> call, Throwable t) {
                postError(callback, parseNetworkError(t));
            }
        });
    }

    public void updateAgencyConfig(ImportAgencyConfig config, Callback<ImportAgencyConfig> callback) {
        final String adminUid;
        try {
            adminUid = AdminAccessControl.requireAdminUserId();
        } catch (SecurityException se) {
            postError(callback, se.getMessage());
            return;
        }

        List<String> errors = config != null ? config.validate() : null;
        if (config == null || (errors != null && !errors.isEmpty())) {
            postError(callback, errors != null && !errors.isEmpty() ? errors.get(0) : "Configuration invalide");
            return;
        }

        if (useMock) {
            try {
                ImportAgencyConfig updated = MockBackendService.getInstance().updateImportAgencyConfig(config, adminUid);
                CurrencyConversionService.getInstance().setConfig(updated);
                postSuccess(callback, updated);
            } catch (Exception e) {
                postError(callback, e.getMessage());
            }
            return;
        }

        ApiClient.getApi().updateImportAgencyConfig(config).enqueue(new retrofit2.Callback<ImportAgencyConfig>() {
            @Override
            public void onResponse(Call<ImportAgencyConfig> call, Response<ImportAgencyConfig> response) {
                if (response.isSuccessful() && response.body() != null) {
                    CurrencyConversionService.getInstance().setConfig(response.body());
                    postSuccess(callback, response.body());
                } else {
                    postError(callback, parseHttpError(response));
                }
            }

            @Override
            public void onFailure(Call<ImportAgencyConfig> call, Throwable t) {
                postError(callback, parseNetworkError(t));
            }
        });
    }

    @Override
    public void list(Object filter, Callback<List<ImportOrder>> callback) {
        listOrders(false, filter instanceof String ? (String) filter : null, callback);
    }

    public void listOrders(boolean asAdmin, String searchQuery, Callback<List<ImportOrder>> callback) {
        if (!AuthSession.getInstance().isLoggedIn()) {
            postError(callback, "Connexion requise (401)");
            return;
        }
        if (asAdmin) {
            try {
                AdminAccessControl.requireAdminUserId();
            } catch (SecurityException se) {
                postError(callback, se.getMessage());
                return;
            }
        }

        if (useMock) {
            String uid = AuthSession.getInstance().getUserId();
            postSuccess(callback, MockBackendService.getInstance().getImportOrders(uid, asAdmin, searchQuery));
            return;
        }

        String role = asAdmin ? "admin" : "customer";
        ApiClient.getApi().getImportOrders(role, searchQuery).enqueue(new retrofit2.Callback<List<ImportOrder>>() {
            @Override
            public void onResponse(Call<List<ImportOrder>> call, Response<List<ImportOrder>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    postSuccess(callback, response.body());
                } else if (response.code() == 404) {
                    postError(callback, "Route /api/import1688/orders non implémentée sur le backend (404)");
                } else {
                    postError(callback, parseHttpError(response));
                }
            }

            @Override
            public void onFailure(Call<List<ImportOrder>> call, Throwable t) {
                postError(callback, parseNetworkError(t));
            }
        });
    }

    @Override
    public void getById(String id, Callback<ImportOrder> callback) {
        if (useMock) {
            ImportOrder order = MockBackendService.getInstance().getImportOrderById(id);
            if (order != null) {
                postSuccess(callback, order);
            } else {
                postError(callback, "Commande d'importation introuvable : " + id);
            }
            return;
        }

        ApiClient.getApi().getImportOrderById(id).enqueue(new retrofit2.Callback<ImportOrder>() {
            @Override
            public void onResponse(Call<ImportOrder> call, Response<ImportOrder> response) {
                if (response.isSuccessful() && response.body() != null) {
                    postSuccess(callback, response.body());
                } else {
                    postError(callback, parseHttpError(response));
                }
            }

            @Override
            public void onFailure(Call<ImportOrder> call, Throwable t) {
                postError(callback, parseNetworkError(t));
            }
        });
    }

    @Override
    public void create(ImportOrder item, Callback<ImportOrder> callback) {
        if (!AuthSession.getInstance().isLoggedIn()) {
            postError(callback, "Connexion requise pour soumettre une demande d'achat 1688");
            return;
        }
        if (item == null) {
            postError(callback, "Demande invalide");
            return;
        }
        item.setUserId(AuthSession.getInstance().getUserId());
        List<String> errors = item.validate();
        if (!errors.isEmpty()) {
            postError(callback, errors.get(0));
            return;
        }

        if (useMock) {
            try {
                ImportOrder created = MockBackendService.getInstance().createImportOrder(item);
                postSuccess(callback, created);
            } catch (Exception e) {
                postError(callback, e.getMessage());
            }
            return;
        }

        ApiClient.getApi().createImportOrder(item).enqueue(new retrofit2.Callback<ImportOrder>() {
            @Override
            public void onResponse(Call<ImportOrder> call, Response<ImportOrder> response) {
                if (response.isSuccessful() && response.body() != null) {
                    postSuccess(callback, response.body());
                } else if (response.code() == 404) {
                    postError(callback, "Route POST /api/import1688/orders non implémentée sur le backend (404)");
                } else {
                    postError(callback, parseHttpError(response));
                }
            }

            @Override
            public void onFailure(Call<ImportOrder> call, Throwable t) {
                postError(callback, parseNetworkError(t));
            }
        });
    }

    /**
     * Étape administrative 1 : l'administrateur vérifie le produit 1688, le prix CNY, le taux et les frais de prestation,
     * puis confirme le montant définitif à régler par MVola.
     */
    public void confirmOrderAmount(
            String importOrderId,
            double verifiedPriceCny,
            double exchangeRate,
            double serviceFeeMga,
            Callback<ImportOrder> callback
    ) {
        final String adminUid;
        try {
            adminUid = AdminAccessControl.requireAdminUserId();
        } catch (SecurityException se) {
            postError(callback, se.getMessage());
            return;
        }

        if (useMock) {
            try {
                ImportOrder updated = MockBackendService.getInstance().confirmImportOrderAmount(
                        importOrderId, verifiedPriceCny, exchangeRate, serviceFeeMga, adminUid);
                postSuccess(callback, updated);
            } catch (Exception e) {
                postError(callback, e.getMessage());
            }
            return;
        }

        Map<String, Object> body = new HashMap<>();
        body.put("priceCNY", verifiedPriceCny);
        body.put("exchangeRate", exchangeRate);
        body.put("serviceFeeMGA", serviceFeeMga);
        ApiClient.getApi().confirmImportOrderAmount(importOrderId, body).enqueue(new retrofit2.Callback<ImportOrder>() {
            @Override
            public void onResponse(Call<ImportOrder> call, Response<ImportOrder> response) {
                if (response.isSuccessful() && response.body() != null) {
                    postSuccess(callback, response.body());
                } else {
                    postError(callback, parseHttpError(response));
                }
            }

            @Override
            public void onFailure(Call<ImportOrder> call, Throwable t) {
                postError(callback, parseNetworkError(t));
            }
        });
    }

    /**
     * Étape client : soumission de la référence de transaction MVola.
     * Le statut reste strictement EN_ATTENTE jusqu'à la vérification par un administrateur.
     */
    public void submitMvolaPaymentReference(String importOrderId, String paymentReference, Callback<ImportOrder> callback) {
        if (!AuthSession.getInstance().isLoggedIn()) {
            postError(callback, "Connexion requise (401)");
            return;
        }
        if (paymentReference == null || paymentReference.trim().isEmpty()) {
            postError(callback, "Veuillez saisir la référence de transaction MVola");
            return;
        }

        if (useMock) {
            try {
                ImportOrder updated = MockBackendService.getInstance().submitImportPaymentReference(importOrderId, paymentReference);
                postSuccess(callback, updated);
            } catch (Exception e) {
                postError(callback, e.getMessage());
            }
            return;
        }

        Map<String, String> body = new HashMap<>();
        body.put("paymentReference", paymentReference.trim());
        ApiClient.getApi().submitImportPaymentReference(importOrderId, body).enqueue(new retrofit2.Callback<ImportOrder>() {
            @Override
            public void onResponse(Call<ImportOrder> call, Response<ImportOrder> response) {
                if (response.isSuccessful() && response.body() != null) {
                    postSuccess(callback, response.body());
                } else {
                    postError(callback, parseHttpError(response));
                }
            }

            @Override
            public void onFailure(Call<ImportOrder> call, Throwable t) {
                postError(callback, parseNetworkError(t));
            }
        });
    }

    /**
     * Étape administrative 2 : vérification ou refus du paiement MVola par un administrateur autorisé.
     */
    public void verifyMvolaPayment(
            String importOrderId,
            PaymentStatus decision,
            String note,
            Callback<ImportOrder> callback
    ) {
        final String adminUid;
        try {
            adminUid = AdminAccessControl.requireAdminUserId();
        } catch (SecurityException se) {
            postError(callback, se.getMessage());
            return;
        }

        if (useMock) {
            try {
                ImportOrder updated = MockBackendService.getInstance().verifyImportPayment(importOrderId, decision, note, adminUid);
                postSuccess(callback, updated);
            } catch (Exception e) {
                postError(callback, e.getMessage());
            }
            return;
        }

        Map<String, Object> body = new HashMap<>();
        body.put("paymentStatus", decision != null ? decision.name() : "");
        if (note != null) body.put("note", note);
        ApiClient.getApi().verifyImportPayment(importOrderId, body).enqueue(new retrofit2.Callback<ImportOrder>() {
            @Override
            public void onResponse(Call<ImportOrder> call, Response<ImportOrder> response) {
                if (response.isSuccessful() && response.body() != null) {
                    postSuccess(callback, response.body());
                } else {
                    postError(callback, parseHttpError(response));
                }
            }

            @Override
            public void onFailure(Call<ImportOrder> call, Throwable t) {
                postError(callback, parseNetworkError(t));
            }
        });
    }

    /**
     * Étape administrative 3 : enregistrement de l'achat réel effectué manuellement par l'agence sur 1688.
     */
    public void recordReal1688Purchase(
            String importOrderId,
            String supplierOrderReference,
            double actualPurchasePriceCny,
            String purchaseDate,
            String note,
            Callback<ImportOrder> callback
    ) {
        final String adminUid;
        try {
            adminUid = AdminAccessControl.requireAdminUserId();
        } catch (SecurityException se) {
            postError(callback, se.getMessage());
            return;
        }

        if (useMock) {
            try {
                ImportOrder updated = MockBackendService.getInstance().recordReal1688Purchase(
                        importOrderId, supplierOrderReference, actualPurchasePriceCny, purchaseDate, note, adminUid);
                postSuccess(callback, updated);
            } catch (Exception e) {
                postError(callback, e.getMessage());
            }
            return;
        }

        Map<String, Object> body = new HashMap<>();
        body.put("supplierOrderReference", supplierOrderReference);
        body.put("actualPurchasePriceCNY", actualPurchasePriceCny);
        body.put("purchaseDate", purchaseDate);
        if (note != null) body.put("note", note);
        ApiClient.getApi().recordImportRealPurchase(importOrderId, body).enqueue(new retrofit2.Callback<ImportOrder>() {
            @Override
            public void onResponse(Call<ImportOrder> call, Response<ImportOrder> response) {
                if (response.isSuccessful() && response.body() != null) {
                    postSuccess(callback, response.body());
                } else {
                    postError(callback, parseHttpError(response));
                }
            }

            @Override
            public void onFailure(Call<ImportOrder> call, Throwable t) {
                postError(callback, parseNetworkError(t));
            }
        });
    }

    /**
     * Étape administrative 4 : mise à jour chronologique du suivi logistique jusqu'à RECU_PAR_IMPORTATEUR.
     */
    public void updateTrackingStatus(
            String importOrderId,
            ImportTrackingStatus newStatus,
            String note,
            Callback<ImportOrder> callback
    ) {
        final String adminUid;
        try {
            adminUid = AdminAccessControl.requireAdminUserId();
        } catch (SecurityException se) {
            postError(callback, se.getMessage());
            return;
        }

        if (useMock) {
            try {
                ImportOrder updated = MockBackendService.getInstance().updateImportTrackingStatus(
                        importOrderId, newStatus, note, adminUid);
                postSuccess(callback, updated);
            } catch (Exception e) {
                postError(callback, e.getMessage());
            }
            return;
        }

        Map<String, String> body = new HashMap<>();
        body.put("trackingStatus", newStatus != null ? newStatus.name() : "");
        if (note != null) body.put("note", note);
        ApiClient.getApi().updateImportTrackingStatus(importOrderId, body).enqueue(new retrofit2.Callback<ImportOrder>() {
            @Override
            public void onResponse(Call<ImportOrder> call, Response<ImportOrder> response) {
                if (response.isSuccessful() && response.body() != null) {
                    postSuccess(callback, response.body());
                } else {
                    postError(callback, parseHttpError(response));
                }
            }

            @Override
            public void onFailure(Call<ImportOrder> call, Throwable t) {
                postError(callback, parseNetworkError(t));
            }
        });
    }

    @Override
    public void update(ImportOrder item, Callback<ImportOrder> callback) {
        postError(callback, "Utilisez les opérations métier explicites (confirmOrderAmount, verifyMvolaPayment, updateTrackingStatus)");
    }

    @Override
    public void delete(String id, Callback<Boolean> callback) {
        postError(callback, "La suppression d'une commande d'importation n'est pas autorisée");
    }
}
