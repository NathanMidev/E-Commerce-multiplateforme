package com.marketplace.app.feature.faq;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.marketplace.app.R;

import java.util.ArrayList;
import java.util.List;

/**
 * Écran FAQ pour les Commandes et les Demandes d'importation 1688 (Section 3).
 * - Sections dépliables (accordéons) avec interface fluide et intuitive.
 * - Répond à toutes les questions logistiques, de paiement, de délais et de transport.
 * - Aucune mention de majoration interne ou de formule confidentielle.
 * - Bouton direct d'appel au support client : 038 11 928 23.
 */
public class FaqActivity extends AppCompatActivity {

    public static final String SUPPORT_PHONE = "0381192823";
    public static final String SUPPORT_PHONE_DISPLAY = "038 11 928 23";

    public static class FaqItem {
        public final String question;
        public final String answer;

        public FaqItem(String question, String answer) {
            this.question = question;
            this.answer = answer;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_faq);

        findViewById(R.id.btn_faq_back).setOnClickListener(v -> finish());

        LinearLayout container = findViewById(R.id.layout_faq_accordion_container);
        List<FaqItem> items = getFaqItems();

        LayoutInflater inflater = LayoutInflater.from(this);
        for (FaqItem item : items) {
            View itemView = inflater.inflate(R.layout.item_faq_accordion, container, false);
            TextView txtQuestion = itemView.findViewById(R.id.txt_faq_question);
            TextView txtAnswer = itemView.findViewById(R.id.txt_faq_answer);
            TextView txtArrow = itemView.findViewById(R.id.txt_faq_arrow);
            View layoutHeader = itemView.findViewById(R.id.layout_faq_header);
            View layoutBody = itemView.findViewById(R.id.layout_faq_body);

            txtQuestion.setText(item.question);
            txtAnswer.setText(item.answer);

            layoutHeader.setOnClickListener(v -> {
                boolean isExpanded = layoutBody.getVisibility() == View.VISIBLE;
                if (isExpanded) {
                    layoutBody.setVisibility(View.GONE);
                    txtArrow.setText("▼");
                } else {
                    layoutBody.setVisibility(View.VISIBLE);
                    txtArrow.setText("▲");
                }
            });

            container.addView(itemView);
        }

        // Support Téléphonique
        findViewById(R.id.btn_call_support).setOnClickListener(v -> {
            try {
                Intent callIntent = new Intent(Intent.ACTION_DIAL);
                callIntent.setData(Uri.parse("tel:" + SUPPORT_PHONE));
                startActivity(callIntent);
            } catch (Exception e) {
                Toast.makeText(this, "Numéro du support : " + SUPPORT_PHONE_DISPLAY, Toast.LENGTH_LONG).show();
            }
        });

        // Email Support
        View btnEmail = findViewById(R.id.btn_email_support);
        if (btnEmail != null) {
            btnEmail.setOnClickListener(v -> {
                try {
                    Intent emailIntent = new Intent(Intent.ACTION_SENDTO);
                    emailIntent.setData(Uri.parse("mailto:"));
                    emailIntent.putExtra(Intent.EXTRA_SUBJECT, "Demande d'information — Importation 1688 / Commandes");
                    startActivity(Intent.createChooser(emailIntent, "Envoyer un email au support"));
                } catch (Exception e) {
                    Toast.makeText(this, "Aucune application de messagerie disponible", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    public static List<FaqItem> getFaqItems() {
        List<FaqItem> list = new ArrayList<>();

        list.add(new FaqItem(
                "Comment fonctionne un achat sur 1688 ?",
                "Vous pouvez parcourir les produits disponibles sur 1688 directement depuis notre application. Lorsque vous trouvez un produit qui vous intéresse, vous pouvez envoyer une demande d'importation.\n\n"
                        + "Notre équipe prend en charge l'achat auprès du fournisseur sur 1688 après confirmation du paiement du produit.\n\n"
                        + "Vous serez ensuite informé de l'évolution de votre commande jusqu'à son arrivée chez le transitaire à destination."
        ));

        list.add(new FaqItem(
                "Comment payer ma commande ?",
                "Le paiement du prix d'achat du produit est effectué auprès de notre service via MVola, selon les instructions communiquées lors de la validation de votre demande.\n\n"
                        + "Après confirmation du paiement, notre équipe procède à l'achat du produit sur 1688."
        ));

        list.add(new FaqItem(
                "Combien de temps faut-il pour recevoir mon produit ?",
                "Le délai dépend notamment du type de produit, de son poids, de son volume et du mode de transport choisi.\n\n"
                        + "• Voie maritime : généralement adaptée aux produits lourds, volumineux ou aux commandes importantes.\n"
                        + "• Voie aérienne : généralement adaptée aux produits légers et aux envois nécessitant un acheminement plus rapide.\n\n"
                        + "Les délais peuvent varier selon le fournisseur, la préparation de la commande, le transport et les opérations logistiques."
        ));

        list.add(new FaqItem(
                "Puis-je choisir mon propre mode de transport ?",
                "Oui. Si vous souhaitez imposer un délai particulier ou choisir votre propre mode de transport, vous pouvez nous contacter avant la validation de votre demande.\n\n"
                        + "Vous pouvez nous contacter directement au :\n"
                        + SUPPORT_PHONE_DISPLAY + "\n\n"
                        + "Notre équipe vous orientera selon vos besoins et les possibilités logistiques."
        ));

        list.add(new FaqItem(
                "Le prix affiché sur 1688 comprend-il tous les frais ?",
                "Non. Le prix affiché correspond uniquement au prix du produit en Ariary.\n\n"
                        + "Les frais de transport international ne sont pas inclus.\n\n"
                        + "Lorsque votre produit arrive chez le transitaire, celui-ci vous contacte directement grâce aux coordonnées renseignées afin de vous communiquer le montant des frais de transport à régler."
        ));

        list.add(new FaqItem(
                "Quand dois-je payer les frais de transport ?",
                "Les frais de transport sont communiqués séparément par le transitaire lorsque celui-ci réceptionne votre produit.\n\n"
                        + "Vous serez contacté directement par le transitaire pour connaître le montant et les modalités de paiement de ces frais."
        ));

        list.add(new FaqItem(
                "Comment suivre ma commande ?",
                "Vous pouvez consulter l'évolution de votre demande depuis la rubrique « Mes commandes » ou « Demandes 1688 ».\n\n"
                        + "Les étapes peuvent notamment comprendre :\n"
                        + "1. Paiement en attente.\n"
                        + "2. Paiement confirmé.\n"
                        + "3. Achat effectué auprès du fournisseur.\n"
                        + "4. Préparation par le fournisseur.\n"
                        + "5. Expédition vers le transitaire.\n"
                        + "6. Réception chez le transitaire."
        ));

        list.add(new FaqItem(
                "Qui me contacte lorsque mon produit arrive chez le transitaire ?",
                "Le transitaire vous contacte directement à partir des coordonnées renseignées lors de votre commande pour vous informer de la réception du produit et vous communiquer les frais de transport."
        ));

        list.add(new FaqItem(
                "Puis-je annuler une demande d'importation ?",
                "Pour toute demande d'annulation, contactez notre service client le plus rapidement possible.\n\n"
                        + "Une demande déjà achetée auprès du fournisseur ou expédiée peut être soumise à des conditions particulières. L'annulation ne doit donc jamais être présentée comme automatiquement garantie."
        ));

        return list;
    }
}
