package fr.imagedescription.agent;

/**
 * Contrat de l'agent : le contrôleur ne connaît que cette interface.
 *
 * <p>Préconditions : la vérification du fichier est faite par le type {@link Image} lui-même. Une
 * image fournie ici est donc toujours non vide, dans un format pris en charge, et son en-tête
 * correspond à son type. Une question vide ou blanche équivaut à une question absente : le
 * contrôleur la transforme en {@code null} avant d'appeler l'agent.
 */
public interface DescripteurImage {

  /**
   * Décrit le contenu d'une image en français, en quelques phrases, ou répond à la question posée
   * sur cette image.
   *
   * @param image les octets de l'image et son type MIME, jamais null
   * @param question la question de l'utilisateur, ou null pour une description générale
   * @return la description ou la réponse, jamais vide
   * @throws DescriptionImpossibleException si l'agent ne peut pas répondre (modèle arrêté, délai
   *     dépassé, réponse vide)
   */
  Description decrire(Image image, String question) throws DescriptionImpossibleException;
}
