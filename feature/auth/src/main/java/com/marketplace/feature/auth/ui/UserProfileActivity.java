package com.marketplace.feature.auth.ui;

import android.os.Bundle;
import android.widget.TextView;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import com.marketplace.feature.auth.R;

public class UserProfileActivity extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_profile);

        String userId = getIntent().getStringExtra("EXTRA_USER_ID");
        TextView tvUserId = findViewById(R.id.tv_user_id);
        if (tvUserId != null) {
            tvUserId.setText("ID Utilisateur : " + (userId != null ? userId : "Session locale"));
        }
    }
}
