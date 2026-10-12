package com.marketplace.feature.chat.ui;

import android.os.Bundle;
import android.widget.TextView;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import com.marketplace.feature.chat.R;

public class ChatListActivity extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat_list);

        TextView tvEmpty = findViewById(R.id.tv_chat_list_empty);
        if (tvEmpty != null) {
            tvEmpty.setText("Aucune conversation récente.\nCliquez sur un produit pour contacter son vendeur.");
        }
    }
}
