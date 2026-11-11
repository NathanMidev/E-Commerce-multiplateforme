package com.marketplace.app.transaction.panier;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.marketplace.app.R;
import com.marketplace.app.base.BaseAdapter;
import com.marketplace.app.model.CartItem;
import com.marketplace.app.ui.PriceFormatter;
import com.marketplace.app.ui.QuantityStepper;

/**
 * Rôle : Adaptateur RecyclerView pour le panier groupé par vendeur avec QuantityStepper.
 * Entrée : Liste ordonnée d'articles CartItem.
 * Sortie : Callbacks de mise à jour de quantité et bouton Discuter avec le vendeur.
 */
public class CartAdapter extends BaseAdapter<CartItem, CartAdapter.ViewHolder> {

    public interface OnCartItemActionListener {
        void onQuantityChanged(CartItem item, int newQuantity);
        void onChatSeller(String sellerId, String sellerName);
    }

    private final OnCartItemActionListener actionListener;

    public CartAdapter(OnCartItemActionListener listener) {
        this.actionListener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_cart, parent, false);
        return new ViewHolder(view);
    }

    @Override
    protected void bind(@NonNull ViewHolder holder, CartItem item, int position) {
        // En-tête vendeur affiché uniquement pour le premier article du vendeur
        String currentSeller = item.getSellerId() != null ? item.getSellerId() : "";
        String prevSeller = (position > 0 && getItem(position - 1) != null && getItem(position - 1).getSellerId() != null)
                ? getItem(position - 1).getSellerId() : "";
        boolean showSellerHeader = (position == 0) || !currentSeller.equals(prevSeller);
        if (showSellerHeader) {
            holder.layoutSellerHeader.setVisibility(View.VISIBLE);
            holder.tvSellerName.setText("Boutique : " + (item.getSellerName() != null ? item.getSellerName() : item.getSellerId()));
            holder.btnChatSeller.setOnClickListener(v -> {
                if (actionListener != null) actionListener.onChatSeller(item.getSellerId(), item.getSellerName());
            });
        } else {
            holder.layoutSellerHeader.setVisibility(View.GONE);
        }

        holder.tvTitle.setText(item.getTitle());
        holder.tvPrice.setText(PriceFormatter.format(item.getUnitPrice()));

        if (!item.isAvailable()) {
            holder.tvUnavailable.setVisibility(View.VISIBLE);
        } else {
            holder.tvUnavailable.setVisibility(View.GONE);
        }

        holder.stepper.setMaxStock(Math.max(1, item.getStock()));
        holder.stepper.setQuantity(item.getQuantity());
        holder.stepper.setOnQuantityChangeListener(newQuantity -> {
            if (actionListener != null) actionListener.onQuantityChanged(item, newQuantity);
        });

        Glide.with(holder.itemView.getContext())
                .load(item.getThumbnailUrl())
                .placeholder(android.R.drawable.ic_menu_gallery)
                .into(holder.ivThumb);
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        View layoutSellerHeader;
        TextView tvSellerName;
        Button btnChatSeller;
        ImageView ivThumb;
        TextView tvTitle;
        TextView tvPrice;
        TextView tvUnavailable;
        QuantityStepper stepper;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            layoutSellerHeader = itemView.findViewById(R.id.layout_seller_header);
            tvSellerName = itemView.findViewById(R.id.tv_cart_seller_name);
            btnChatSeller = itemView.findViewById(R.id.btn_cart_chat_seller);
            ivThumb = itemView.findViewById(R.id.iv_cart_thumb);
            tvTitle = itemView.findViewById(R.id.tv_cart_item_title);
            tvPrice = itemView.findViewById(R.id.tv_cart_item_price);
            tvUnavailable = itemView.findViewById(R.id.tv_cart_item_unavailable);
            stepper = itemView.findViewById(R.id.stepper_cart_item);
        }
    }
}
