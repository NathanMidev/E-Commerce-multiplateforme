package com.marketplace.app.base;

import android.os.Handler;
import android.os.Looper;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.util.List;

import okhttp3.ResponseBody;
import retrofit2.Response;

/**
 * Rôle : Classe de base pour tous les repositories d'accès aux données.
 * Entrée : Appels réseau Retrofit et callbacks unifiés.
 * Sortie : Parsing centralisé des erreurs HTTP (401, format JSON {"error":{"message"}}).
 */
public abstract class BaseRepository<T extends BaseModel> {

    public interface Callback<R> {
        void onSuccess(R result);
        void onError(String message);
    }

    public abstract void list(Object filter, Callback<List<T>> callback);
    public abstract void getById(String id, Callback<T> callback);
    public abstract void create(T item, Callback<T> callback);
    public abstract void update(T item, Callback<T> callback);
    public abstract void delete(String id, Callback<Boolean> callback);

    private static void dispatch(Runnable runnable) {
        try {
            Looper looper = Looper.getMainLooper();
            if (looper != null && Looper.myLooper() != looper) {
                new Handler(looper).post(runnable);
                return;
            }
        } catch (Throwable ignored) {
            // Environnement de test unitaire JVM sans stub Android Looper
        }
        runnable.run();
    }

    protected void postSuccess(final Callback<?> callback, final Object result) {
        if (callback == null) return;
        dispatch(() -> ((Callback<Object>) callback).onSuccess(result));
    }

    protected void postError(final Callback<?> callback, final String message) {
        if (callback == null) return;
        dispatch(() -> callback.onError(message));
    }

    /**
     * Analyse la réponse HTTP pour extraire le message d'erreur du backend.
     */
    public static String parseHttpError(Response<?> response) {
        if (response == null) return "Erreur inconnue";
        if (response.code() == 401) {
            return "Session expirée ou connexion requise (401)";
        }
        ResponseBody errorBody = response.errorBody();
        if (errorBody != null) {
            try {
                String raw = errorBody.string();
                JsonObject obj = JsonParser.parseString(raw).getAsJsonObject();
                if (obj.has("error") && obj.get("error").isJsonObject()) {
                    JsonObject err = obj.getAsJsonObject("error");
                    if (err.has("message")) {
                        return err.get("message").getAsString();
                    }
                }
            } catch (Exception ignored) {
            }
        }
        return "Erreur serveur (" + response.code() + ")";
    }

    /**
     * Analyse les erreurs d'exception réseau (IOException, déconnexion).
     */
    public static String parseNetworkError(Throwable t) {
        if (t instanceof IOException) {
            return "Impossible de joindre le serveur. Vérifiez votre connexion.";
        }
        return t != null && t.getMessage() != null ? t.getMessage() : "Erreur inattendue";
    }
}
