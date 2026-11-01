package com.marketplace.app.feature.import1688;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.marketplace.app.R;
import com.marketplace.app.model.ImportOrder;
import com.marketplace.app.model.ImportTrackingStatus;
import com.marketplace.app.model.PaymentStatus;
import com.marketplace.app.ui.PriceFormatter;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Adapter RecyclerView pour afficher les demandes et commandes d'achat Import 1688
 * avec double affichage monétaire (CNY ¥ / Ariary MGA) et statuts de suivi.
 */
public class ImportOrdersAdapter extends RecyclerView.Adapter<ImportOrdersAdapter.ViewHolder> {

    public interface OnOrderClickListener {
        void onOrderClick(ImportOrder order);
    }

    private final List<ImportOrder> items = new ArrayList<>();
    private final OnOrderClickListener listener;

    public ImportOrdersAdapter(OnOrderClickListener listener) {
        this.listener = listener;
    }

    public void submitList(List<ImportOrder> orders) {
        items.clear();
        if (orders != null) {
            items.addAll(orders);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_import_order, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ImportOrder order = items.get(position);
        String id = order.getImportOrderId() != null ? order.getImportOrderId() : "Nouvelle demande";
        holder.txtOrderId.setText(id);

        if (order.getCreatedAt() > 0) {
            String dateStr = new SimpleDateFormat("dd/MM/yyyy", Locale.FRANCE).format(new Date(order.getCreatedAt()));
            holder.txtOrderDate.setText(dateStr);
        } else {
            holder.txtOrderDate.setText("");
        }

        holder.txtSimulatedBadge.setVisibility(order.isSimulated() ? View.VISIBLE : View.GONE);

        holder.txtProductName.setText(order.getProductName() != null ? order.getProductName() : "Produit 1688");
        String variant = (order.getProductVariant() != null && !order.getProductVariant().isEmpty())
                ? order.getProductVariant() : "Standard";
        holder.txtVariantQty.setText(variant + " • Qté : " + order.getQuantity()
                + " • Unité : " + CurrencyConversionService.formatCny(order.getPriceCNY()));

        double totalCNY = order.getPriceCNY() * order.getQuantity();
        String confirmedTag = order.isAmountConfirmedByAdmin() ? " (Confirmé)" : " (Estimé)";
        holder.txtDualPrice.setText("Total Agence : "
                + PriceFormatter.format(order.getTotalAmountMGA())
                + " (" + CurrencyConversionService.formatCny(totalCNY) + " + frais)" + confirmedTag);

        PaymentStatus ps = order.getPaymentStatus() != null ? order.getPaymentStatus() : PaymentStatus.EN_ATTENTE;
        holder.txtPaymentBadge.setText("MVola : " + ps.name());
        if (ps == PaymentStatus.PAYE) {
            holder.txtPaymentBadge.setBackgroundResource(R.color.badge_3d_bg);
            holder.txtPaymentBadge.setTextColor(holder.itemView.getContext().getColor(R.color.status_green));
        } else if (ps == PaymentStatus.ECHOUE) {
            holder.txtPaymentBadge.setBackgroundResource(R.color.primary_light);
            holder.txtPaymentBadge.setTextColor(holder.itemView.getContext().getColor(R.color.status_red));
        } else {
            holder.txtPaymentBadge.setBackgroundResource(R.color.primary_light);
            holder.txtPaymentBadge.setTextColor(holder.itemView.getContext().getColor(R.color.primary_dark));
        }

        ImportTrackingStatus ts = order.getTrackingStatus() != null
                ? order.getTrackingStatus() : ImportTrackingStatus.PAIEMENT_EN_ATTENTE;
        holder.txtTrackingBadge.setText("Étape " + ts.getStepOrder() + "/6 : " + ts.getLabel());

        if (order.getProductImageUrl() != null && !order.getProductImageUrl().trim().isEmpty()) {
            Glide.with(holder.itemView.getContext())
                    .load(order.getProductImageUrl())
                    .placeholder(android.R.drawable.ic_menu_gallery)
                    .error(android.R.drawable.ic_menu_gallery)
                    .into(holder.imgProduct);
        } else {
            holder.imgProduct.setImageResource(android.R.drawable.ic_menu_gallery);
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onOrderClick(order);
            }
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final TextView txtOrderId;
        final TextView txtSimulatedBadge;
        final TextView txtOrderDate;
        final ImageView imgProduct;
        final TextView txtProductName;
        final TextView txtVariantQty;
        final TextView txtDualPrice;
        final TextView txtPaymentBadge;
        final TextView txtTrackingBadge;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            txtOrderId = itemView.findViewById(R.id.txt_order_id);
            txtSimulatedBadge = itemView.findViewById(R.id.txt_simulated_badge);
            txtOrderDate = itemView.findViewById(R.id.txt_order_date);
            imgProduct = itemView.findViewById(R.id.img_product);
            txtProductName = itemView.findViewById(R.id.txt_product_name);
            txtVariantQty = itemView.findViewById(R.id.txt_variant_qty);
            txtDualPrice = itemView.findViewById(R.id.txt_dual_price);
            txtPaymentBadge = itemView.findViewById(R.id.txt_payment_badge);
            txtTrackingBadge = itemView.findViewById(R.id.txt_tracking_badge);
        }
    }
}
