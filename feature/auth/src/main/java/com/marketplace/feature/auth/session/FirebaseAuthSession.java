package com.marketplace.feature.auth.session;

import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GetTokenResult;
import com.marketplace.core.network.AuthSession;

/**
 * Session basée sur Firebase Auth : fournit au client réseau l'identifiant et le jeton de l'utilisateur
 * connecté. Firebase renouvelle lui-même le jeton quand il expire.
 */
public class FirebaseAuthSession implements AuthSession {

    @Override
    public String getUserId() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        return user != null ? user.getUid() : null;
    }

    /** Appelé depuis un thread réseau (intercepteur OkHttp) : l'attente bloquante y est autorisée. */
    @Override
    public String getIdToken() throws Exception {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return null;
        GetTokenResult result = Tasks.await(user.getIdToken(false));
        return result.getToken();
    }
}
