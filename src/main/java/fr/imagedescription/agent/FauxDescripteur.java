package fr.imagedescription.agent;

import java.util.List;
import java.util.Objects;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * Fausse implémentation : n'appelle aucun modèle et renvoie une réponse fixe. Activée avec le
 * profil "faux" (ex. ./gradlew bootRun --args='--spring.profiles.active=faux').
 */
@Component
@Profile("faux")
public class FauxDescripteur implements DescripteurImage {

  @Override
  public Description decrire(Image image, String question, List<Echange> historique) {
    Objects.requireNonNull(historique, "L'historique est absent");
    if (question == null) {
      return new Description(
          "Fausse description : aucun modèle n'est appelé. "
              + "Une image de "
              + image.octets().length
              + " octets a été reçue.");
    }
    return new Description("Fausse réponse à la question « " + question + " ».");
  }
}
