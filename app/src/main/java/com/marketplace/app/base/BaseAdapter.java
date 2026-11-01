package com.marketplace.app.base;

import android.view.View;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

/**
 * Rôle : Classe de base pour tous les adaptateurs RecyclerView.
 * Entrée : Liste générique d'éléments de type T.
 * Sortie : setItems, gestion de clic uniforme et état vide.
 */
public abstract class BaseAdapter<T, VH extends RecyclerView.ViewHolder> extends RecyclerView.Adapter<VH> {

    protected final List<T> items = new ArrayList<>();
    protected OnItemClickListener<T> clickListener;

    public interface OnItemClickListener<T> {
        void onItemClick(T item, int position);
    }

    public void setOnItemClickListener(OnItemClickListener<T> listener) {
        this.clickListener = listener;
    }

    public void setItems(List<T> newItems) {
        this.items.clear();
        if (newItems != null) {
            this.items.addAll(newItems);
        }
        notifyDataSetChanged();
    }

    public List<T> getItems() {
        return items;
    }

    public T getItem(int position) {
        if (position >= 0 && position < items.size()) {
            return items.get(position);
        }
        return null;
    }

    public boolean isEmpty() {
        return items.isEmpty();
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        final T item = getItem(position);
        if (clickListener != null) {
            holder.itemView.setOnClickListener(v -> {
                int adapterPos = holder.getAdapterPosition();
                if (adapterPos != RecyclerView.NO_POSITION) {
                    clickListener.onItemClick(getItem(adapterPos), adapterPos);
                }
            });
        }
        bind(holder, item, position);
    }

    protected abstract void bind(@NonNull VH holder, T item, int position);
}
