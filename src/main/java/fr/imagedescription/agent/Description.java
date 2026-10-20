package fr.imagedescription.agent;

/**
 * Texte en français renvoyé par l'agent : description générale ou réponse à la question. Un texte
 * vide n'est jamais une description valable.
 */
public record Description(String texte) {

  public Description {
    if (texte == null || texte.isBlank()) {
      throw new IllegalArgumentException("La description ne peut pas être vide");
    }
    texte = texte.strip();
  }
}
