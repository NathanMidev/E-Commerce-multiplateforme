package com.marketplace.app.model;

import com.marketplace.app.base.BaseModel;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Rôle : Entrée d'historique d'un changement d'état de suivi Import 1688.
 * Entrée : Statut, horodatage, note facultative et identifiant administrateur.
 * Sortie : Enregistrement immuable dans la chronologie de la commande.
 */
public class ImportStatusHistoryEntry extends BaseModel {

    private ImportTrackingStatus status;
    private long timestamp;
    private String date;
    private String time;
    private String note;
    private String updatedByAdmin;

    public ImportStatusHistoryEntry() {}

    public ImportStatusHistoryEntry(ImportTrackingStatus status, long timestamp, String note, String updatedByAdmin) {
        this.status = status;
        this.timestamp = timestamp;
        Date d = new Date(timestamp);
        this.date = new SimpleDateFormat("yyyy-MM-dd", Locale.FRANCE).format(d);
        this.time = new SimpleDateFormat("HH:mm", Locale.FRANCE).format(d);
        this.note = note;
        this.updatedByAdmin = updatedByAdmin;
    }

    @Override
    public List<String> validate() {
        List<String> errors = new ArrayList<>();
        if (status == null) errors.add("Statut requis");
        if (updatedByAdmin == null || updatedByAdmin.trim().isEmpty()) {
            errors.add("Identifiant administrateur requis");
        }
        return errors;
    }

    public ImportTrackingStatus getStatus() { return status; }
    public void setStatus(ImportTrackingStatus status) { this.status = status; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }

    public String getTime() { return time; }
    public void setTime(String time) { this.time = time; }

    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }

    public String getUpdatedByAdmin() { return updatedByAdmin; }
    public void setUpdatedByAdmin(String updatedByAdmin) { this.updatedByAdmin = updatedByAdmin; }
}
