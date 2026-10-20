package fr.imagedescription.web;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import fr.imagedescription.agent.DescripteurImage;
import fr.imagedescription.agent.Description;
import fr.imagedescription.agent.DescriptionImpossibleException;
import fr.imagedescription.agent.Image;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder;

@WebMvcTest(DescriptionControleur.class)
class DescriptionControleurTest {

  private static final byte[] PNG = {(byte) 0x89, 'P', 'N', 'G', '\r', '\n', 0x1A, '\n', 0};

  @Autowired private MockMvc mvc;

  @MockitoBean private DescripteurImage descripteur;

  private MockMultipartFile image(String type, byte[] contenu) {
    return new MockMultipartFile("image", "photo", type, contenu);
  }

  private MockMultipartHttpServletRequestBuilder requete(MockMultipartFile fichier) {
    return multipart("/api/descriptions").file(fichier);
  }

  @Test
  void renvoieLaDescriptionDeLImage() throws Exception {
    when(descripteur.decrire(any(Image.class), isNull())).thenReturn(new Description("Un chat."));

    mvc.perform(requete(image("image/png", PNG)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.description").value("Un chat."))
        .andExpect(jsonPath("$.message").doesNotExist());
  }

  @Test
  void transmetLaQuestionNettoyeeOuNullSiElleEstVide() throws Exception {
    when(descripteur.decrire(any(Image.class), anyString())).thenReturn(new Description("Rouge."));

    mvc.perform(requete(image("image/png", PNG)).param("question", "  De quelle couleur ?  "))
        .andExpect(status().isOk());
    verify(descripteur).decrire(any(Image.class), eq("De quelle couleur ?"));

    when(descripteur.decrire(any(Image.class), isNull())).thenReturn(new Description("Un chat."));
    mvc.perform(requete(image("image/png", PNG)).param("question", "   "))
        .andExpect(status().isOk());
    verify(descripteur).decrire(any(Image.class), isNull());
  }

  @Test
  void refuseUnFichierVide() throws Exception {
    mvc.perform(requete(image("image/png", new byte[0])))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message").value(containsString("vide")));
    verify(descripteur, never()).decrire(any(), any());
  }

  @Test
  void refuseUnTypeNonPrisEnCharge() throws Exception {
    mvc.perform(requete(image("application/pdf", PNG)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message").value(containsString("pas pris en charge")));
  }

  @Test
  void refuseUnFichierDontLEnTeteNeCorrespondPasAuType() throws Exception {
    mvc.perform(requete(image("image/jpeg", "texte brut".getBytes(StandardCharsets.UTF_8))))
        .andExpect(status().isBadRequest());
    verify(descripteur, never()).decrire(any(), any());
  }

  @Test
  void refuseUneQuestionTropLongue() throws Exception {
    String question = "a".repeat(DescriptionControleur.QUESTION_MAX + 1);

    mvc.perform(requete(image("image/png", PNG)).param("question", question))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message").value(containsString("trop longue")));
    verify(descripteur, never()).decrire(any(), any());
  }

  @Test
  void repondSansErreurSiLaDescriptionEstImpossible() throws Exception {
    when(descripteur.decrire(any(Image.class), isNull()))
        .thenThrow(new DescriptionImpossibleException("modèle arrêté"));

    mvc.perform(requete(image("image/png", PNG)))
        .andExpect(status().isServiceUnavailable())
        .andExpect(jsonPath("$.message").value(containsString("Réessayez")));
  }

  @Test
  void refuseUneRequeteSansFichierAvecUnMessageLisible() throws Exception {
    mvc.perform(multipart("/api/descriptions").param("question", "Quelle couleur ?"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message").value(containsString("Choisissez d'abord une image")));
  }

  @Test
  void refuseUnFichierSansTypeMime() throws Exception {
    mvc.perform(requete(image(null, PNG)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message").value(containsString("pas pris en charge")));
  }

  @Test
  void repondParUnMessageLisibleSiUneErreurInattendueSurvient() throws Exception {
    when(descripteur.decrire(any(Image.class), isNull()))
        .thenThrow(new IllegalStateException("bug"));

    mvc.perform(requete(image("image/png", PNG)))
        .andExpect(status().isInternalServerError())
        .andExpect(jsonPath("$.message").value(containsString("erreur inattendue")));
  }
}
