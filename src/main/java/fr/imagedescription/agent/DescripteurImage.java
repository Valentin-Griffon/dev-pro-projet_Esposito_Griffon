package fr.imagedescription.agent;

/**
 * Contrat de l'agent : le contrôleur ne connaît que cette interface.
 */
public interface DescripteurImage {

    /**
     * Décrit le contenu d'une image en français, en quelques phrases,
     * ou répond à la question posée sur cette image.
     *
     * @param image les octets de l'image et son type MIME, jamais null
     * @param question la question de l'utilisateur, ou null pour une description générale
     * @return la description ou la réponse, jamais vide
     * @throws DescriptionImpossibleException si l'agent ne peut pas répondre
     */
    Description decrire(Image image, String question) throws DescriptionImpossibleException;
}
