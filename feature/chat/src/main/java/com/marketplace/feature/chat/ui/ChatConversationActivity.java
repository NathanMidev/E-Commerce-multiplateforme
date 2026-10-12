package com.marketplace.feature.chat.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.marketplace.core.firebase.FirebaseChatService;
import com.marketplace.core.model.ChatMessage;
import com.marketplace.feature.chat.R;

import java.util.ArrayList;
import java.util.List;

public class ChatConversationActivity extends AppCompatActivity {

    public static final String EXTRA_SELLER_ID = "EXTRA_SELLER_ID";
    public static final String EXTRA_SELLER_NAME = "EXTRA_SELLER_NAME";
    public static final String EXTRA_PRODUCT_ID = "EXTRA_PRODUCT_ID";
    public static final String EXTRA_PRODUCT_TITLE = "EXTRA_PRODUCT_TITLE";

    private TextView tvSellerHeader;
    private TextView tvProductHeader;
    private RecyclerView rvMessages;
    private View layoutEmptyChat;
    private EditText editMessageInput;
    private Button btnSendMessage;

    private String sellerId;
    private String sellerName;
    private String productId;
    private String conversationId;

    private final FirebaseChatService chatService = new FirebaseChatService();
    private MessageAdapter adapter;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat_conversation);

        sellerId = getIntent().getStringExtra(EXTRA_SELLER_ID);
        sellerName = getIntent().getStringExtra(EXTRA_SELLER_NAME);
        productId = getIntent().getStringExtra(EXTRA_PRODUCT_ID);
        String productTitle = getIntent().getStringExtra(EXTRA_PRODUCT_TITLE);

        if (sellerId == null) sellerId = "seller_default";
        if (sellerName == null) sellerName = "Vendeur";
        conversationId = "conv_" + sellerId + "_" + (productId != null ? productId : "gen");

        tvSellerHeader = findViewById(R.id.tv_seller_header);
        tvProductHeader = findViewById(R.id.tv_product_header);
        rvMessages = findViewById(R.id.rv_messages);
        layoutEmptyChat = findViewById(R.id.layout_empty_chat);
        editMessageInput = findViewById(R.id.edit_message_input);
        btnSendMessage = findViewById(R.id.btn_send_message);

        tvSellerHeader.setText("Discussion avec : " + sellerName);
        if (productTitle != null && !productTitle.isEmpty()) {
            tvProductHeader.setText("À propos de : " + productTitle);
            tvProductHeader.setVisibility(View.VISIBLE);
        } else {
            tvProductHeader.setVisibility(View.GONE);
        }

        setupRecyclerView();
        loadMessages();

        btnSendMessage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String text = editMessageInput.getText() != null ? editMessageInput.getText().toString().trim() : "";
                if (text.isEmpty()) return;

                chatService.sendMessage(conversationId, "buyer_me", sellerId, text, productId);
                editMessageInput.setText("");
                loadMessages();
            }
        });
    }

    private void setupRecyclerView() {
        rvMessages.setLayoutManager(new LinearLayoutManager(this));
        adapter = new MessageAdapter();
        rvMessages.setAdapter(adapter);
    }

    private void loadMessages() {
        List<ChatMessage> list = chatService.getMessages(conversationId);
        if (list.isEmpty()) {
            layoutEmptyChat.setVisibility(View.VISIBLE);
            rvMessages.setVisibility(View.GONE);
        } else {
            layoutEmptyChat.setVisibility(View.GONE);
            rvMessages.setVisibility(View.VISIBLE);
            adapter.setMessages(list);
            rvMessages.scrollToPosition(list.size() - 1);
        }
    }

    static class MessageAdapter extends RecyclerView.Adapter<MessageAdapter.MessageViewHolder> {
        private final List<ChatMessage> messages = new ArrayList<>();

        void setMessages(List<ChatMessage> list) {
            messages.clear();
            if (list != null) messages.addAll(list);
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public MessageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_chat_bubble, parent, false);
            return new MessageViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull MessageViewHolder holder, int position) {
            ChatMessage msg = messages.get(position);
            holder.tvContent.setText(msg.getContent());
            holder.tvSender.setText(msg.getSenderId().equals("buyer_me") ? "Moi" : "Vendeur");
        }

        @Override
        public int getItemCount() {
            return messages.size();
        }

        static class MessageViewHolder extends RecyclerView.ViewHolder {
            TextView tvContent;
            TextView tvSender;

            MessageViewHolder(@NonNull View itemView) {
                super(itemView);
                tvContent = itemView.findViewById(R.id.tv_bubble_content);
                tvSender = itemView.findViewById(R.id.tv_bubble_sender);
            }
        }
    }
}
