package fr.imagedescription.agent;

import java.util.List;
import java.util.Objects;

/**
 * Image transmise à l'agent : ses octets et son type MIME.
 * Le contenu est vérifié à la construction : fichier vide, type non pris en charge
 * ou en-tête ne correspondant pas au type annoncé sont refusés.
 */
public record Image(byte[] octets, String typeMime) {

    private static final List<String> TYPES_ACCEPTES = List.of("image/jpeg", "image/png", "image/webp", "image/gif");

    public Image {
        Objects.requireNonNull(octets, "Les octets de l'image sont absents");
        Objects.requireNonNull(typeMime, "Le type de l'image est absent");
        if (octets.length == 0) {
            throw new IllegalArgumentException("Le fichier choisi est vide. Choisissez une autre image.");
        }
        if (!TYPES_ACCEPTES.contains(typeMime)) {
            throw new IllegalArgumentException(
                    "Ce type de fichier n'est pas pris en charge. Choisissez une image JPEG, PNG, WebP ou GIF.");
        }
        if (!correspondAuType(octets, typeMime)) {
            throw new IllegalArgumentException(
                    "Le contenu du fichier ne correspond pas à son type. Choisissez une autre image.");
        }
        // Copie défensive : l'appelant ne peut pas modifier les octets après coup
        octets = octets.clone();
    }

    /**
     * Renvoie une copie des octets, pour que l'image reste inchangée.
     */
    @Override
    public byte[] octets() {
        return octets.clone();
    }

    /**
     * Vérifie la signature (en-tête) du fichier : le type MIME vient du client et ne suffit pas.
     */
    private static boolean correspondAuType(byte[] octets, String typeMime) {
        return switch (typeMime) {
            case "image/jpeg" -> commencePar(octets, 0xFF, 0xD8, 0xFF);
            case "image/png" -> commencePar(octets, 0x89, 'P', 'N', 'G');
            case "image/gif" -> commencePar(octets, 'G', 'I', 'F', '8');
            case "image/webp" -> commencePar(octets, 'R', 'I', 'F', 'F')
                    && octets.length >= 12 && octets[8] == 'W' && octets[9] == 'E' && octets[10] == 'B' && octets[11] == 'P';
            default -> false;
        };
    }

    private static boolean commencePar(byte[] octets, int... signature) {
        if (octets.length < signature.length) {
            return false;
        }
        for (int i = 0; i < signature.length; i++) {
            if ((octets[i] & 0xFF) != signature[i]) {
                return false;
            }
        }
        return true;
    }
}
