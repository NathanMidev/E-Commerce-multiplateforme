package com.marketplace.app.feature.import1688;

import com.google.gson.Gson;
import com.marketplace.app.backend.AuthSession;
import com.marketplace.app.backend.mock.MockBackendService;
import com.marketplace.app.backend.repository.ImportOrderRepository;
import com.marketplace.app.base.BaseRepository;
import com.marketplace.app.model.ImportAgencyConfig;
import com.marketplace.app.model.ImportOrder;
import com.marketplace.app.model.ImportTrackingStatus;
import com.marketplace.app.model.PaymentStatus;

import org.junit.Before;
import org.junit.Test;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import okhttp3.MediaType;
import okhttp3.ResponseBody;
import retrofit2.Response;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Tests unitaires complets pour le module « Service d'Achat Agence Import 1688 » (Section 25).
 * Couvre :
 * - Conversion monétaire CNY -> MGA
 * - Calcul des quantités et des frais de service d'achat de l'agence
 * - Validation et sérialisation JSON (Gson) de ImportOrder
 * - Gestion des statuts de paiement MVola (interdiction d'auto-confirmation par le client)
 * - Enregistrement de l'achat réel sur 1688 et progression des 6 statuts de suivi jusqu'à RECU_PAR_IMPORTATEUR
 * - Contrôle d'accès administrateur (AdminAccessControl)
 * - Gestion des erreurs HTTP API
 */
public class Import1688UnitTest {

    private CurrencyConversionService conversionService;
    private MockBackendService mockBackend;

    @Before
    public void setUp() {
        MockBackendService.getInstance().resetData();
        mockBackend = MockBackendService.getInstance();
        conversionService = CurrencyConversionService.getInstance();
        conversionService.setConfig(ImportAgencyConfig.createDefaultConfig());
        AuthSession.getInstance().clear();
        AdminAccessControl.grantAdminForUid(null, false);
    }

    @Test
    public void testCnyToMgaConversionAndServiceFees() {
        // Configuration d'un taux 1 CNY = 650 Ar et commission 10%
        conversionService.updateExchangeRate(650.0, "admin-test");
        assertEquals(650.0, conversionService.getExchangeRate(), 0.001);

        // Application de la formule : Prix en MGA = Prix CNY × Taux CNY/MGA × 1,15
        // Exemple illustratif : 100 CNY × 650 × 1.15 = 74 750 Ar
        double finalMga = conversionService.convertCnyToMgaFinal(100.0);
        assertEquals(74750.0, finalMga, 0.01);

        double finalMgaWithRate = CurrencyConversionService.convertCnyToMgaFinalWithRate(100.0, 650.0);
        assertEquals(74750.0, finalMgaWithRate, 0.01);

        // Produit à 25 CNY x 4 unités = 100 CNY -> 74 750 Ar
        double productMga = conversionService.convertProductTotalToMga(25.0, 4);
        assertEquals(74750.0, productMga, 0.01);

        // Frais de prestation d'achat de l'agence (10%) = 7 475 Ar
        double feeMga = conversionService.calculateServiceFeeMga(productMga);
        assertEquals(7475.0, feeMga, 0.01);

        // Montant total à régler à l'agence par MVola (hors transport international) = 82 225 Ar
        double totalMga = CurrencyConversionService.calculateAgencyTotalMga(productMga, feeMga);
        assertEquals(82225.0, totalMga, 0.01);

        // Règle commerciale stricte : Résumé sans divulgation de la marge de 15%
        String summary = conversionService.formatEstimateSummary(25.0, 4);
        assertFalse(summary.contains("15"));
        assertFalse(summary.contains("majoration"));
        assertFalse(summary.contains("marge"));
        assertTrue(summary.contains("Hors frais de transport international"));

        // Vérification de la mention explicite d'exclusion des frais de transport international
        assertTrue(CurrencyConversionService.TRANSPORT_DISCLAIMER.contains("transport international ne sont pas inclus"));
    }

    @Test
    public void testImportOrderValidationAndJsonSerialization() {
        ImportOrder invalidOrder = new ImportOrder();
        List<String> errors = invalidOrder.validate();
        assertFalse(errors.isEmpty());

        ImportOrder validOrder = new ImportOrder();
        validOrder.setUserId("buyer-123");
        validOrder.setCustomerName("Rabe Jean");
        validOrder.setCustomerPhone("+261340011122");
        validOrder.setProductUrl("https://detail.1688.com/offer/99887766.html");
        validOrder.setProductName("Montre connectée Bluetooth");
        validOrder.setProductVariant("Noir • Bracelet silicone");
        validOrder.setQuantity(5);
        validOrder.setPriceCNY(40.0);
        validOrder.setExchangeRate(650.0);
        validOrder.setEstimatedPriceMGA(130000.0);
        validOrder.setServiceFeeMGA(13000.0);
        validOrder.setTotalAmountMGA(143000.0);

        assertTrue(validOrder.validate().isEmpty());

        // Sérialisation / Désérialisation Gson
        Gson gson = new Gson();
        String json = gson.toJson(validOrder);
        ImportOrder restored = gson.fromJson(json, ImportOrder.class);
        assertEquals("Rabe Jean", restored.getCustomerName());
        assertEquals("https://detail.1688.com/offer/99887766.html", restored.getProductUrl());
        assertEquals(5, restored.getQuantity());
        assertEquals(40.0, restored.getPriceCNY(), 0.01);
        assertEquals(PaymentStatus.EN_ATTENTE, restored.getPaymentStatus());
        assertEquals(ImportTrackingStatus.PAIEMENT_EN_ATTENTE, restored.getTrackingStatus());
    }

    @Test
    public void testMvolaPaymentNeverAutoConfirmedAndFullTrackingLifecycle() {
        // 1. Client soumet une demande d'achat 1688
        AuthSession.getInstance().updateSession("client-01", "token-client");
        ImportOrder req = new ImportOrder();
        req.setUserId("client-01");
        req.setCustomerName("Soa Nirina");
        req.setCustomerPhone("0341234567");
        req.setProductUrl("https://m.1688.com/offer/11223344.html");
        req.setProductName("Lot écouteurs TWS");
        req.setProductVariant("Blanc");
        req.setQuantity(10);
        req.setPriceCNY(18.0);
        req.setExchangeRate(650.0);

        ImportOrder created = mockBackend.createImportOrder(req);
        assertNotNull(created.getImportOrderId());
        assertEquals(PaymentStatus.EN_ATTENTE, created.getPaymentStatus());
        assertEquals(ImportTrackingStatus.PAIEMENT_EN_ATTENTE, created.getTrackingStatus());
        assertFalse(created.isAmountConfirmedByAdmin());

        String orderId = created.getImportOrderId();

        // 2. L'administrateur vérifie le produit sur 1688 et confirme le montant définitif
        ImportOrder amountConfirmed = mockBackend.confirmImportOrderAmount(orderId, 18.0, 650.0, 12000.0, "admin-uid");
        assertTrue(amountConfirmed.isAmountConfirmedByAdmin());
        // 18 * 10 * 650 = 117 000 Ar + 12 000 Ar frais = 129 000 Ar
        assertEquals(129000.0, amountConfirmed.getTotalAmountMGA(), 0.01);

        // 3. Le client soumet sa référence de paiement MVola ("J'ai effectué le paiement")
        // RÈGLE STRICTE : le paiement NE DOIT JAMAIS passer automatiquement à PAYE
        ImportOrder afterClientMvola = mockBackend.submitImportPaymentReference(orderId, "MVOLA-REF-778899");
        assertEquals("MVOLA-REF-778899", afterClientMvola.getPaymentReference());
        assertEquals(PaymentStatus.EN_ATTENTE, afterClientMvola.getPaymentStatus());
        assertEquals(ImportTrackingStatus.PAIEMENT_EN_ATTENTE, afterClientMvola.getTrackingStatus());

        // 4. L'administrateur vérifie et confirme la réception du paiement MVola
        ImportOrder afterAdminVerify = mockBackend.verifyImportPayment(orderId, PaymentStatus.PAYE, "Reçu sur compte MVola Agence", "admin-uid");
        assertEquals(PaymentStatus.PAYE, afterAdminVerify.getPaymentStatus());
        assertEquals(ImportTrackingStatus.PAIEMENT_CONFIRME, afterAdminVerify.getTrackingStatus());

        // 5. L'administrateur effectue l'achat réel sur 1688 avec la carte bancaire de l'agence
        ImportOrder afterPurchase = mockBackend.recordReal1688Purchase(
                orderId, "1688-ORDER-20269988", 180.0, "2026-11-01", "Acheté par carte agence", "admin-uid");
        assertEquals(ImportTrackingStatus.ACHAT_EFFECTUE, afterPurchase.getTrackingStatus());
        assertEquals("1688-ORDER-20269988", afterPurchase.getSupplierOrderReference());
        assertEquals(180.0, afterPurchase.getActualPurchasePriceCNY(), 0.01);

        // 6. Progression logistique jusqu'à l'importateur
        ImportOrder step4 = mockBackend.updateImportTrackingStatus(
                orderId, ImportTrackingStatus.PREPARATION_FOURNISSEUR, "En préparation à Shenzhen", "admin-uid");
        assertEquals(ImportTrackingStatus.PREPARATION_FOURNISSEUR, step4.getTrackingStatus());

        ImportOrder step5 = mockBackend.updateImportTrackingStatus(
                orderId, ImportTrackingStatus.EXPEDIE_VERS_IMPORTATEUR, "Expédié vers entrepôt Guangzhou", "admin-uid");
        assertEquals(ImportTrackingStatus.EXPEDIE_VERS_IMPORTATEUR, step5.getTrackingStatus());
        assertFalse(step5.isReceivedByImporter());

        ImportOrder step6 = mockBackend.updateImportTrackingStatus(
                orderId, ImportTrackingStatus.RECU_PAR_IMPORTATEUR, "Colis réceptionné par l'importateur", "admin-uid");
        assertEquals(ImportTrackingStatus.RECU_PAR_IMPORTATEUR, step6.getTrackingStatus());
        assertTrue(step6.isReceivedByImporter());
        assertTrue(ImportOrder.FINAL_IMPORTER_MESSAGE.contains("Votre produit a été réceptionné par notre prestataire logistique"));
        assertTrue(step6.getStatusHistory().size() >= 6);
    }

    @Test
    public void testAdminAccessControlBlocksStandardCustomer() {
        // Connexion d'un client standard (non-admin)
        AuthSession.getInstance().updateSession("standard-buyer", "token-buyer");
        AdminAccessControl.grantAdminForUid("standard-buyer", false);
        assertFalse(AdminAccessControl.isCurrentUserAdmin());

        ImportOrderRepository repo = new ImportOrderRepository(true);

        // Tentative de modification du taux de change par un client standard -> bloquée
        AtomicReference<String> errorRef = new AtomicReference<>();
        ImportAgencyConfig cfg = ImportAgencyConfig.createDefaultConfig();
        cfg.setExchangeRateCnyToMga(500.0);
        repo.updateAgencyConfig(cfg, new BaseRepository.Callback<ImportAgencyConfig>() {
            @Override
            public void onSuccess(ImportAgencyConfig data) {}

            @Override
            public void onError(String message) {
                errorRef.set(message);
            }
        });
        assertNotNull(errorRef.get());
        assertTrue(errorRef.get().contains("403"));

        // Attribution explicite du droit admin -> autorisée
        AdminAccessControl.grantAdminForUid("standard-buyer", true);
        assertTrue(AdminAccessControl.isCurrentUserAdmin());

        AtomicReference<ImportAgencyConfig> successRef = new AtomicReference<>();
        repo.updateAgencyConfig(cfg, new BaseRepository.Callback<ImportAgencyConfig>() {
            @Override
            public void onSuccess(ImportAgencyConfig data) {
                successRef.set(data);
            }

            @Override
            public void onError(String message) {}
        });
        assertNotNull(successRef.get());
        assertEquals(500.0, successRef.get().getExchangeRateCnyToMga(), 0.01);
    }

    @Test
    public void testApiHttpErrorHandling() {
        ImportOrderRepository repo = new ImportOrderRepository(true);
        Response<Object> err401 = Response.error(401,
                ResponseBody.create("{\"error\":\"Unauthorized\"}", MediaType.parse("application/json")));
        Response<Object> err403 = Response.error(403,
                ResponseBody.create("{\"error\":\"Forbidden\"}", MediaType.parse("application/json")));
        Response<Object> err500 = Response.error(500,
                ResponseBody.create("{\"error\":\"Server crash\"}", MediaType.parse("application/json")));

        String msg401 = repo.parseHttpError(err401);
        String msg403 = repo.parseHttpError(err403);
        String msg500 = repo.parseHttpError(err500);

        assertTrue(msg401.contains("401") || msg401.contains("Unauthorized"));
        assertTrue(msg403.contains("403") || msg403.contains("Forbidden"));
        assertTrue(msg500.contains("500") || msg500.contains("Server crash"));
    }
}
