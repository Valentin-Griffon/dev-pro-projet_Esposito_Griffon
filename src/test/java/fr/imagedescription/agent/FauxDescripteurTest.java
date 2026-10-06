package fr.imagedescription.agent;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class FauxDescripteurTest {

    private final FauxDescripteur descripteur = new FauxDescripteur();
    private final Image image = new Image(new byte[]{(byte) 0x89, 'P', 'N', 'G', 1, 2, 3}, "image/png");

    @Test
    void renvoieUneDescriptionGeneraleQuiMentionLaTailleDeLImage() {
        Description description = descripteur.decrire(image, null);

        assertThat(description.texte()).contains("Fausse description").contains("7 octets");
    }

    @Test
    void renvoieUneReponseQuiReprendLaQuestion() {
        Description description = descripteur.decrire(image, "Quelle couleur ?");

        assertThat(description.texte()).contains("Fausse réponse").contains("Quelle couleur ?");
    }
}
