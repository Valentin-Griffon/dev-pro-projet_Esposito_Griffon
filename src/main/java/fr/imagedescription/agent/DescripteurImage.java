package fr.imagedescription.agent;

import java.util.List;

/**
 * Contrat de l'agent : le contrôleur ne connaît que cette interface.
 *
 * <p>Préconditions : la vérification du fichier est faite par le type {@link Image} lui-même. Une
 * image fournie ici est donc toujours non vide, dans un format pris en charge, et son en-tête
 * correspond à son type. Une question vide ou blanche équivaut à une question absente : le
 * contrôleur la transforme en {@code null} avant d'appeler l'agent. L'historique n'est jamais null
 * (vide pour le premier échange de la conversation) et chaque {@link Echange} qu'il contient a déjà
 * vérifié sa propre validité.
 */
public interface DescripteurImage {

  /**
   * Décrit le contenu d'une image en français, en quelques phrases, ou répond à la question posée
   * sur cette image, en tenant compte des échanges précédents de la conversation.
   *
   * @param image les octets de l'image et son type MIME, jamais null
   * @param question la question de l'utilisateur, ou null pour une description générale
   * @param historique les échanges précédents de la conversation sur cette image, du plus ancien au
   *     plus récent ; jamais null, vide pour le premier échange
   * @return la description ou la réponse, jamais vide
   * @throws DescriptionImpossibleException si l'agent ne peut pas répondre (modèle arrêté, délai
   *     dépassé, réponse vide)
   */
  Description decrire(Image image, String question, List<Echange> historique)
      throws DescriptionImpossibleException;
}
