package fr.imagedescription.agent;

/**
 * Un échange passé dans la conversation sur une image : la question posée (ou {@code null} pour la
 * description générale) et la réponse du modèle à ce moment-là. Permet de transmettre le contexte
 * d'une conversation à l'agent pour qu'il reste cohérent avec ce qui a déjà été dit sur l'image.
 */
public record Echange(String question, String reponse) {

  public Echange {
    if (reponse == null || reponse.isBlank()) {
      throw new IllegalArgumentException("La réponse d'un échange ne peut pas être vide");
    }
    reponse = reponse.strip();
    if (question != null) {
      question = question.isBlank() ? null : question.strip();
    }
  }
}
