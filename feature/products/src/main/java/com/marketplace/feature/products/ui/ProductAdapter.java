package com.marketplace.feature.products.ui;

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
        holder.tvSeller.setText("Vendu par : " + product.getSellerName());

        if (product.isArSupported() && product.getModel3dUrl() != null && !product.getModel3dUrl().isEmpty()) {
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
        Button btnArBadge;

        ProductViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tv_product_title);
            tvPrice = itemView.findViewById(R.id.tv_product_price);
            tvSeller = itemView.findViewById(R.id.tv_product_seller);
            btnArBadge = itemView.findViewById(R.id.btn_ar_badge);
        }
    }
}
