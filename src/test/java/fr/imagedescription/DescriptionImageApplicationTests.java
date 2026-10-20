package fr.imagedescription;

import static org.assertj.core.api.Assertions.assertThat;

import fr.imagedescription.agent.DescripteurImage;
import fr.imagedescription.agent.FauxDescripteur;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("faux")
class DescriptionImageApplicationTests {

  @Autowired private DescripteurImage descripteur;

  @Test
  void leProfilFauxFourniraLaFausseImplementation() {
    assertThat(descripteur).isInstanceOf(FauxDescripteur.class);
  }
}
