package fr.imagedescription.agent;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.ImageContent;
import dev.langchain4j.data.message.TextContent;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class OllamaDescripteurTest {

  private final Image image = new Image(new byte[] {(byte) 0x89, 'P', 'N', 'G', 1}, "image/png");

  private ChatModel modele;
  private OllamaDescripteur descripteur;

  @BeforeEach
  void preparer() {
    modele = mock(ChatModel.class);
    descripteur = new OllamaDescripteur(modele);
  }

  private void repondre(String texte) {
    when(modele.chat(any(ChatRequest.class)))
        .thenReturn(ChatResponse.builder().aiMessage(AiMessage.from(texte)).build());
  }

  /** Texte de la consigne envoyée au modèle (sans l'image). */
  private String consigneEnvoyee() {
    return texteDuMessage(0);
  }

  private List<ChatMessage> messagesEnvoyes() {
    ArgumentCaptor<ChatRequest> captation = ArgumentCaptor.forClass(ChatRequest.class);
    verify(modele).chat(captation.capture());
    return captation.getValue().messages();
  }

  private String texteDuMessage(int index) {
    ChatMessage message = messagesEnvoyes().get(index);
    return ((UserMessage) message)
        .contents().stream()
            .filter(TextContent.class::isInstance)
            .map(contenu -> ((TextContent) contenu).text())
            .findFirst()
            .orElseThrow();
  }

  @Test
  void renvoieLaReponseDuModeleSansEspacesSuperflus() throws DescriptionImpossibleException {
    repondre("  Un paysage de montagne.  ");

    Description description = descripteur.decrire(image, null, List.of());

    assertThat(description.texte()).isEqualTo("Un paysage de montagne.");
  }

  @Test
  void demandeUneDescriptionGeneraleSansQuestion() throws DescriptionImpossibleException {
    repondre("Une description.");

    descripteur.decrire(image, null, List.of());

    assertThat(consigneEnvoyee()).startsWith("Décris cette image");
  }

  @Test
  void transmetLaQuestionAuModele() throws DescriptionImpossibleException {
    repondre("Une réponse.");

    descripteur.decrire(image, "Combien de personnes ?", List.of());

    assertThat(consigneEnvoyee()).endsWith("Combien de personnes ?");
  }

  @Test
  void joinLImageAuPremierMessageSeulement() throws DescriptionImpossibleException {
    repondre("Il y a deux chats.");

    descripteur.decrire(
        image,
        "Et le second ?",
        List.of(new Echange(null, "Une description."), new Echange("Combien ?", "Deux chats.")));

    List<ChatMessage> messages = messagesEnvoyes();
    assertThat(messages).hasSize(5);
    assertThat(((UserMessage) messages.get(0)).contents()).anyMatch(ImageContent.class::isInstance);
    assertThat(((UserMessage) messages.get(2)).contents())
        .noneMatch(ImageContent.class::isInstance);
    assertThat(((UserMessage) messages.get(4)).contents())
        .noneMatch(ImageContent.class::isInstance);
    assertThat(((AiMessage) messages.get(1)).text()).isEqualTo("Une description.");
    assertThat(((AiMessage) messages.get(3)).text()).isEqualTo("Deux chats.");
    assertThat(texteDuMessage(4)).endsWith("Et le second ?");
  }

  @Test
  void signaleLErreurQuandLeModeleEchoue() {
    when(modele.chat(any(ChatRequest.class))).thenThrow(new IllegalStateException("Ollama arrêté"));

    assertThatThrownBy(() -> descripteur.decrire(image, null, List.of()))
        .isInstanceOf(DescriptionImpossibleException.class)
        .hasCauseInstanceOf(IllegalStateException.class);
  }

  @Test
  void signaleUneReponseVide() {
    repondre("   ");

    assertThatThrownBy(() -> descripteur.decrire(image, null, List.of()))
        .isInstanceOf(DescriptionImpossibleException.class)
        .hasMessageContaining("vide");
  }

  @Test
  void signaleUneReponseSansMessage() {
    ChatResponse reponseSansMessage = mock(ChatResponse.class);
    when(reponseSansMessage.aiMessage()).thenReturn(null);
    when(modele.chat(any(ChatRequest.class))).thenReturn(reponseSansMessage);

    assertThatThrownBy(() -> descripteur.decrire(image, null, List.of()))
        .isInstanceOf(DescriptionImpossibleException.class);
  }

  @Test
  void refuseUnHistoriqueAbsent() {
    assertThatThrownBy(() -> descripteur.decrire(image, null, null))
        .isInstanceOf(NullPointerException.class);
  }

  @Test
  void refuseUnModeleAbsent() {
    assertThatThrownBy(() -> new OllamaDescripteur(null)).isInstanceOf(NullPointerException.class);
  }
}
