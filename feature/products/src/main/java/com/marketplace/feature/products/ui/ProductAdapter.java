package com.marketplace.feature.products.ui;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.marketplace.core.model.Product;
import com.marketplace.feature.products.R;

import java.util.ArrayList;
import java.util.List;

public class ProductAdapter extends RecyclerView.Adapter<ProductAdapter.ProductViewHolder> {

    public interface OnProductClickListener {
        void onProductClick(Product product);
        void onArClick(Product product);
    }

    private final List<Product> items = new ArrayList<>();
    private final OnProductClickListener listener;

    public ProductAdapter(OnProductClickListener listener) {
        this.listener = listener;
    }

    public void submitList(List<Product> newItems) {
        items.clear();
        if (newItems != null) {
            items.addAll(newItems);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ProductViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_product_card, parent, false);
        return new ProductViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ProductViewHolder holder, int position) {
        final Product product = items.get(position);
        holder.tvTitle.setText(product.getTitle());
        holder.tvPrice.setText(String.format("%.2f €", product.getPrice()));
        holder.tvSeller.setText("Boutique : " + product.getSellerName());
        holder.tvStock.setText("En stock : " + product.getStock());

        // Affichage du statut
        if (product.getStatus() != null) {
            holder.tvStatus.setText(product.getStatus().name());
            switch (product.getStatus()) {
                case NOUVEAU:
                    holder.tvStatus.setBackgroundColor(Color.parseColor("#E3F2FD"));
                    holder.tvStatus.setTextColor(Color.parseColor("#1565C0"));
                    break;
                case DISPONIBLE:
                    holder.tvStatus.setBackgroundColor(Color.parseColor("#E8F5E9"));
                    holder.tvStatus.setTextColor(Color.parseColor("#2E7D32"));
                    break;
                case INDISPONIBLE:
                    holder.tvStatus.setBackgroundColor(Color.parseColor("#FFEBEE"));
                    holder.tvStatus.setTextColor(Color.parseColor("#C62828"));
                    break;
                case A_VENDRE:
                    holder.tvStatus.setBackgroundColor(Color.parseColor("#FFF3E0"));
                    holder.tvStatus.setTextColor(Color.parseColor("#E65100"));
                    break;
            }
        }

        // Badge et action AR 3D si le modèle 3D existe
        if (product.hasModele3d() && product.getModel3dUrl() != null && !product.getModel3dUrl().isEmpty()) {
            holder.btnArBadge.setVisibility(View.VISIBLE);
            holder.btnArBadge.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (listener != null) listener.onArClick(product);
                }
            });
        } else {
            holder.btnArBadge.setVisibility(View.GONE);
        }

        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (listener != null) listener.onProductClick(product);
            }
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ProductViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle;
        TextView tvPrice;
        TextView tvSeller;
        TextView tvStock;
        TextView tvStatus;
        Button btnArBadge;

        ProductViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tv_product_title);
            tvPrice = itemView.findViewById(R.id.tv_product_price);
            tvSeller = itemView.findViewById(R.id.tv_product_seller);
            tvStock = itemView.findViewById(R.id.tv_product_stock);
            tvStatus = itemView.findViewById(R.id.tv_product_status);
            btnArBadge = itemView.findViewById(R.id.btn_ar_badge);
        }
    }
}
