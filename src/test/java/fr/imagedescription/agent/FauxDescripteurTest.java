package fr.imagedescription.agent;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

class FauxDescripteurTest {

  private final FauxDescripteur descripteur = new FauxDescripteur();
  private final Image image =
      new Image(new byte[] {(byte) 0x89, 'P', 'N', 'G', 1, 2, 3}, "image/png");

  @Test
  void renvoieUneDescriptionGeneraleQuiMentionLaTailleDeLImage() {
    Description description = descripteur.decrire(image, null, List.of());

    assertThat(description.texte()).contains("Fausse description").contains("7 octets");
  }

  @Test
  void renvoieUneReponseQuiReprendLaQuestion() {
    Description description = descripteur.decrire(image, "Quelle couleur ?", List.of());

    assertThat(description.texte()).contains("Fausse réponse").contains("Quelle couleur ?");
  }

  @Test
  void repondAussiAvecUnHistorique() {
    Description description =
        descripteur.decrire(
            image, "Et derrière ?", List.of(new Echange(null, "Une première description.")));

    assertThat(description.texte()).contains("Et derrière ?");
  }
}
