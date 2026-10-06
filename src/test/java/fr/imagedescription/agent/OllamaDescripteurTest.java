package fr.imagedescription.agent;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.TextContent;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OllamaDescripteurTest {

    private final Image image = new Image(new byte[]{(byte) 0x89, 'P', 'N', 'G', 1}, "image/png");

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
        ArgumentCaptor<ChatRequest> captation = ArgumentCaptor.forClass(ChatRequest.class);
        verify(modele).chat(captation.capture());
        ChatMessage message = captation.getValue().messages().get(0);
        return ((UserMessage) message).contents().stream()
                .filter(TextContent.class::isInstance)
                .map(contenu -> ((TextContent) contenu).text())
                .findFirst()
                .orElseThrow();
    }

    @Test
    void renvoieLaReponseDuModeleSansEspacesSuperflus() throws DescriptionImpossibleException {
        repondre("  Un paysage de montagne.  ");

        Description description = descripteur.decrire(image, null);

        assertThat(description.texte()).isEqualTo("Un paysage de montagne.");
    }

    @Test
    void demandeUneDescriptionGeneraleSansQuestion() throws DescriptionImpossibleException {
        repondre("Une description.");

        descripteur.decrire(image, null);

        assertThat(consigneEnvoyee()).startsWith("Décris cette image");
    }

    @Test
    void transmetLaQuestionAuModele() throws DescriptionImpossibleException {
        repondre("Une réponse.");

        descripteur.decrire(image, "Combien de personnes ?");

        assertThat(consigneEnvoyee()).endsWith("Combien de personnes ?");
    }

    @Test
    void signaleLErreurQuandLeModeleEchoue() {
        when(modele.chat(any(ChatRequest.class))).thenThrow(new IllegalStateException("Ollama arrêté"));

        assertThatThrownBy(() -> descripteur.decrire(image, null))
                .isInstanceOf(DescriptionImpossibleException.class)
                .hasCauseInstanceOf(IllegalStateException.class);
    }

    @Test
    void signaleUneReponseVide() {
        repondre("   ");

        assertThatThrownBy(() -> descripteur.decrire(image, null))
                .isInstanceOf(DescriptionImpossibleException.class)
                .hasMessageContaining("vide");
    }

    @Test
    void signaleUneReponseSansMessage() {
        ChatResponse reponseSansMessage = mock(ChatResponse.class);
        when(reponseSansMessage.aiMessage()).thenReturn(null);
        when(modele.chat(any(ChatRequest.class))).thenReturn(reponseSansMessage);

        assertThatThrownBy(() -> descripteur.decrire(image, null))
                .isInstanceOf(DescriptionImpossibleException.class);
    }

    @Test
    void refuseUnModeleAbsent() {
        assertThatThrownBy(() -> new OllamaDescripteur(null)).isInstanceOf(NullPointerException.class);
    }
}
