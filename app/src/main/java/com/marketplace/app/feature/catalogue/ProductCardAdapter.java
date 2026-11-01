package com.marketplace.app.feature.catalogue;

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
import com.marketplace.app.model.Product;
import com.marketplace.app.ui.PriceFormatter;

/**
 * Rôle : Adaptateur RecyclerView uniforme pour les cartes produits sur deux colonnes style Deal.mg.
 * Entrée : Liste de Product.
 * Sortie : Affichage du visuel, prix en Ariary orange, pastille 3D/AR et interactions.
 */
public class ProductCardAdapter extends BaseAdapter<Product, ProductCardAdapter.ViewHolder> {

    public interface OnProductActionClickListener {
        void onProductClick(Product product);
        void onArClick(Product product);
    }

    private final OnProductActionClickListener actionListener;

    public ProductCardAdapter(OnProductActionClickListener listener) {
        this.actionListener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_product_card, parent, false);
        return new ViewHolder(view);
    }

    @Override
    protected void bind(@NonNull ViewHolder holder, Product product, int position) {
        holder.tvTitle.setText(product.getTitle());
        holder.tvPrice.setText(PriceFormatter.format(product.getPrice()));
        
        String sellerName = product.getSellerName() != null ? product.getSellerName() : "Vendeur";
        holder.tvSeller.setText("📍 " + sellerName);

        boolean has3d = product.hasValidGlbModel();
        if (holder.tvBadgeAr != null) {
            holder.tvBadgeAr.setText("3D");
            holder.tvBadgeAr.setVisibility(has3d ? View.VISIBLE : View.GONE);
        }

        if (has3d) {
            holder.btnQuickAr.setVisibility(View.VISIBLE);
            holder.btnQuickAr.setOnClickListener(v -> {
                if (actionListener != null) actionListener.onArClick(product);
            });
        } else {
            holder.btnQuickAr.setVisibility(View.GONE);
        }

        if (holder.tvStatus != null && product.getStatus() != null) {
            holder.tvStatus.setVisibility(View.VISIBLE);
            holder.tvStatus.setText(product.getStatus().name());
        }

        Glide.with(holder.itemView.getContext())
                .load(product.getThumbnailUrl())
                .placeholder(android.R.drawable.ic_menu_gallery)
                .error(android.R.drawable.ic_menu_report_image)
                .into(holder.ivThumbnail);

        holder.itemView.setOnClickListener(v -> {
            if (actionListener != null) actionListener.onProductClick(product);
        });
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivThumbnail;
        TextView tvTitle;
        TextView tvPrice;
        TextView tvSeller;
        TextView tvBadgeAr;
        TextView tvStatus;
        Button btnQuickAr;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivThumbnail = itemView.findViewById(R.id.iv_product_thumbnail);
            tvTitle = itemView.findViewById(R.id.tv_product_title);
            tvPrice = itemView.findViewById(R.id.tv_product_price);
            tvSeller = itemView.findViewById(R.id.tv_product_seller);
            tvBadgeAr = itemView.findViewById(R.id.tv_badge_ar_overlay);
            tvStatus = itemView.findViewById(R.id.tv_product_status);
            btnQuickAr = itemView.findViewById(R.id.btn_quick_ar);
        }
    }
}
