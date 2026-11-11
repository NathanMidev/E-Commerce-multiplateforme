package com.marketplace.app.transaction.commande;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.marketplace.app.R;
import com.marketplace.app.base.BaseAdapter;
import com.marketplace.app.model.Order;
import com.marketplace.app.model.OrderStatus;
import com.marketplace.app.ui.PriceFormatter;

/**
 * Rôle : Adaptateur RecyclerView pour les commandes avec actions d'avancement de statut.
 * Entrée : Liste ordonnée d'Order et booléen isSellerMode.
 * Sortie : Affichage des détails et boutons contextuels (Confirmer, Expédier, Réceptionner, Annuler).
 */
public class OrdersAdapter extends BaseAdapter<Order, OrdersAdapter.ViewHolder> {

    public interface OnOrderActionListener {
        void onUpdateStatus(Order order, OrderStatus newStatus);
    }

    private final boolean isSellerMode;
    private final OnOrderActionListener listener;

    public OrdersAdapter(boolean isSellerMode, OnOrderActionListener listener) {
        this.isSellerMode = isSellerMode;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_order, parent, false);
        return new ViewHolder(view);
    }

    @Override
    protected void bind(@NonNull ViewHolder holder, Order order, int position) {
        holder.tvRef.setText(order.getReference() != null ? order.getReference() : ("CMD #" + order.getId()));
        holder.tvStatus.setText(order.getStatus() != null ? order.getStatus().name() : "EN_ATTENTE");

        if (isSellerMode) {
            holder.tvParty.setText("Acheteur : " + (order.getBuyerName() != null ? order.getBuyerName() : (order.getBuyerId() != null ? order.getBuyerId() : "-"))
                    + " | Livrer à : " + (order.getDeliveryAddress() != null ? order.getDeliveryAddress() : "-"));
        } else {
            holder.tvParty.setText("Vendeur : " + (order.getSellerName() != null ? order.getSellerName() : (order.getSellerId() != null ? order.getSellerId() : "-")));
        }

        int count = order.getItems() != null ? order.getItems().size() : 0;
        holder.tvSummary.setText(count + " article(s) • Total : " + PriceFormatter.format(order.getTotal()));

        // Actions contextuelles
        holder.btnPrimary.setVisibility(View.GONE);
        holder.btnSecondary.setVisibility(View.GONE);

        if (isSellerMode) {
            if (order.canSellerConfirm()) {
                holder.btnPrimary.setVisibility(View.VISIBLE);
                holder.btnPrimary.setText("Confirmer la commande");
                holder.btnPrimary.setOnClickListener(v -> listener.onUpdateStatus(order, OrderStatus.CONFIRMEE));

                holder.btnSecondary.setVisibility(View.VISIBLE);
                holder.btnSecondary.setText("Refuser / Annuler");
                holder.btnSecondary.setOnClickListener(v -> listener.onUpdateStatus(order, OrderStatus.ANNULEE));
            } else if (order.canSellerShip()) {
                holder.btnPrimary.setVisibility(View.VISIBLE);
                holder.btnPrimary.setText("Marquer Expédiée");
                holder.btnPrimary.setOnClickListener(v -> listener.onUpdateStatus(order, OrderStatus.EXPEDIEE));

                holder.btnSecondary.setVisibility(View.VISIBLE);
                holder.btnSecondary.setText("Annuler");
                holder.btnSecondary.setOnClickListener(v -> listener.onUpdateStatus(order, OrderStatus.ANNULEE));
            }
        } else {
            if (order.canBuyerCancel()) {
                holder.btnSecondary.setVisibility(View.VISIBLE);
                holder.btnSecondary.setText("Annuler la commande");
                holder.btnSecondary.setOnClickListener(v -> listener.onUpdateStatus(order, OrderStatus.ANNULEE));
            } else if (order.canBuyerConfirmDelivery()) {
                holder.btnPrimary.setVisibility(View.VISIBLE);
                holder.btnPrimary.setText("Confirmer la réception (Livrée)");
                holder.btnPrimary.setOnClickListener(v -> listener.onUpdateStatus(order, OrderStatus.LIVREE));
            }
        }
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvRef;
        TextView tvStatus;
        TextView tvParty;
        TextView tvSummary;
        Button btnPrimary;
        Button btnSecondary;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvRef = itemView.findViewById(R.id.tv_order_ref);
            tvStatus = itemView.findViewById(R.id.tv_order_status);
            tvParty = itemView.findViewById(R.id.tv_order_party);
            tvSummary = itemView.findViewById(R.id.tv_order_summary);
            btnPrimary = itemView.findViewById(R.id.btn_order_action_primary);
            btnSecondary = itemView.findViewById(R.id.btn_order_action_secondary);
        }
    }
}
