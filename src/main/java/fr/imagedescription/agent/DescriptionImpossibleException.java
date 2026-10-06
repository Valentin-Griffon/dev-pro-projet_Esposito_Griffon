package fr.imagedescription.agent;

/**
 * L'agent ne peut pas répondre (modèle arrêté, délai dépassé, réponse vide...).
 */
public class DescriptionImpossibleException extends Exception {

    public DescriptionImpossibleException(String message, Throwable cause) {
        super(message, cause);
    }

    public DescriptionImpossibleException(String message) {
        super(message);
    }
}
