package com.marketplace.app.feature.ar;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

/**
 * Rôle : Validation stricte du format, de la taille et de l'entête binaire (glTF 2.0) des fichiers .glb.
 * Entrée : Nom de fichier, taille en octets et flux binaire (12 premiers octets d'entête GLB).
 * Sortie : Validation d'intégrité ou message d'erreur explicite.
 */
public final class GlbValidator {

    /** Magic number officiel glTF Binary : ASCII "glTF" = 0x46546C67 en Little-Endian. */
    public static final int GLB_HEADER_MAGIC = 0x46546C67;
    public static final int GLB_HEADER_LENGTH = 12;
    public static final long DEFAULT_MAX_GLB_SIZE_BYTES = 50L * 1024L * 1024L; // 50 Mo par défaut

    private static volatile long maxGlbSizeBytes = DEFAULT_MAX_GLB_SIZE_BYTES;

    private GlbValidator() {}

    public static long getMaxGlbSizeBytes() {
        return maxGlbSizeBytes;
    }

    public static void setMaxGlbSizeBytes(long maxBytes) {
        if (maxBytes < GLB_HEADER_LENGTH) {
            throw new IllegalArgumentException("La limite de taille GLB doit être au moins de 12 octets");
        }
        maxGlbSizeBytes = maxBytes;
    }

    public static void resetMaxGlbSizeBytes() {
        maxGlbSizeBytes = DEFAULT_MAX_GLB_SIZE_BYTES;
    }

    /**
     * Vérifie que le nom de fichier ou l'URL correspond bien exclusivement à l'extension .glb.
     */
    public static boolean isValidGlbExtension(String fileNameOrUrl) {
        if (fileNameOrUrl == null || fileNameOrUrl.trim().isEmpty()) {
            return false;
        }
        String clean = fileNameOrUrl.trim().toLowerCase(Locale.ROOT);
        int queryIdx = clean.indexOf('?');
        if (queryIdx >= 0) {
            clean = clean.substring(0, queryIdx);
        }
        return clean.endsWith(".glb");
    }

    /**
     * Valide le nom, la taille et l'entête binaire 12 octets d'un fichier .glb.
     *
     * @return null si le fichier est valide, sinon le message d'erreur explicite.
     */
    public static String validateGlbHeader(String fileName, long fileSize, byte[] firstBytes) {
        if (!isValidGlbExtension(fileName)) {
            return "Format non supporté : seuls les fichiers .glb sont acceptés (" + fileName + ")";
        }
        if (fileSize <= 0) {
            return "Le fichier .glb est vide (0 octet)";
        }
        if (fileSize < GLB_HEADER_LENGTH) {
            return "Fichier .glb corrompu : taille inférieure à l'entête minimale (12 octets)";
        }
        if (fileSize > maxGlbSizeBytes) {
            return "Le fichier .glb dépasse la taille maximale autorisée ("
                    + formatFileSize(fileSize) + " > " + formatFileSize(maxGlbSizeBytes) + ")";
        }
        if (firstBytes == null || firstBytes.length < GLB_HEADER_LENGTH) {
            return "Impossible de vérifier l'intégrité de l'entête du fichier .glb";
        }

        ByteBuffer buffer = ByteBuffer.wrap(firstBytes, 0, GLB_HEADER_LENGTH).order(ByteOrder.LITTLE_ENDIAN);
        int magic = buffer.getInt();
        int version = buffer.getInt();
        long declaredLength = Integer.toUnsignedLong(buffer.getInt());

        if (magic != GLB_HEADER_MAGIC) {
            return "Fichier .glb invalide ou corrompu : signature binaire 'glTF' absente";
        }
        if (version != 2 && version != 1) {
            return "Version glTF binaire non supportée (version " + version + ", attendu : 2)";
        }
        if (declaredLength < GLB_HEADER_LENGTH || declaredLength > fileSize) {
            return "Intégrité du fichier .glb compromise : longueur déclarée (" + declaredLength
                    + " octets) incohérente avec la taille réelle (" + fileSize + " octets)";
        }
        return null;
    }

    /**
     * Valide un flux InputStream d'un fichier .glb.
     */
    public static String validateGlbStream(String fileName, long fileSize, InputStream inputStream) {
        if (inputStream == null) {
            return "Flux de lecture du fichier .glb introuvable";
        }
        byte[] header = new byte[GLB_HEADER_LENGTH];
        try {
            int totalRead = 0;
            while (totalRead < GLB_HEADER_LENGTH) {
                int r = inputStream.read(header, totalRead, GLB_HEADER_LENGTH - totalRead);
                if (r == -1) break;
                totalRead += r;
            }
            if (totalRead < GLB_HEADER_LENGTH) {
                return "Fichier .glb tronqué ou incomplet (" + totalRead + " octets lus)";
            }
            return validateGlbHeader(fileName, fileSize, header);
        } catch (IOException e) {
            return "Erreur de lecture lors de la vérification du fichier .glb : " + e.getMessage();
        }
    }

    /**
     * Génère un buffer binaire .glb (glTF 2.0) minimal et valide pour les tests et la prévisualisation locale.
     */
    public static byte[] createMinimalValidGlbBytes() {
        byte[] jsonBytes = "{\"asset\":{\"version\":\"2.0\"}} ".getBytes(StandardCharsets.UTF_8); // 28 octets (multiple de 4)
        int totalLength = GLB_HEADER_LENGTH + 8 + jsonBytes.length;
        ByteBuffer buffer = ByteBuffer.allocate(totalLength).order(ByteOrder.LITTLE_ENDIAN);
        buffer.putInt(GLB_HEADER_MAGIC);
        buffer.putInt(2); // version 2.0
        buffer.putInt(totalLength);
        buffer.putInt(jsonBytes.length);
        buffer.putInt(0x4E4F534A); // "JSON" chunk type
        buffer.put(jsonBytes);
        return buffer.array();
    }

    /**
     * Formate une taille en octets en chaîne lisible (Octets, Ko, Mo).
     */
    public static String formatFileSize(long bytes) {
        if (bytes < 0) return "0 o";
        if (bytes < 1024) return bytes + " o";
        double kb = bytes / 1024.0;
        if (kb < 1024) {
            return String.format(Locale.FRANCE, "%.1f Ko", kb);
        }
        double mb = kb / 1024.0;
        return String.format(Locale.FRANCE, "%.2f Mo", mb);
    }
}
