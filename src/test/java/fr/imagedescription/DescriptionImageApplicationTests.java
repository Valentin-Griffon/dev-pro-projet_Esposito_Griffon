package fr.imagedescription;

import fr.imagedescription.agent.DescripteurImage;
import fr.imagedescription.agent.FauxDescripteur;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("faux")
class DescriptionImageApplicationTests {

    @Autowired
    private DescripteurImage descripteur;

    @Test
    void leProfilFauxFourniraLaFausseImplementation() {
        assertThat(descripteur).isInstanceOf(FauxDescripteur.class);
    }
}
