package fr.imagedescription.agent;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.ImageContent;
import dev.langchain4j.data.message.TextContent;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.ollama.OllamaChatModel;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Objects;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * Agent réel : appelle un modèle de vision via Ollama, à travers LangChain4J. Toute la connaissance
 * de LangChain4J reste dans cette classe.
 */
@Component
@Profile("!faux")
public class OllamaDescripteur implements DescripteurImage {

  private static final String CONSIGNE_GENERALE =
      "Décris cette image en français, en trois phrases, pour une personne qui ne la voit pas. "
          + "Donne d'abord l'essentiel, puis les détails utiles, et lis le texte présent dans l'image.";

  private static final String CONSIGNE_QUESTION =
      "Réponds en français, en quelques phrases, à la question suivante à propos de cette image, "
          + "pour une personne qui ne la voit pas : ";

  private final ChatModel modele;

  @Autowired
  public OllamaDescripteur(
      @Value("${descripteur.ollama.url}") String url,
      @Value("${descripteur.ollama.modele}") String nomModele,
      @Value("${descripteur.ollama.delai}") Duration delai) {
    this(OllamaChatModel.builder().baseUrl(url).modelName(nomModele).timeout(delai).build());
  }

  /** Constructeur destiné aux tests : permet de fournir un modèle simulé. */
  OllamaDescripteur(ChatModel modele) {
    this.modele = Objects.requireNonNull(modele, "Le modèle de chat est absent");
  }

  @Override
  public Description decrire(Image image, String question, List<Echange> historique)
      throws DescriptionImpossibleException {
    Objects.requireNonNull(historique, "L'historique est absent");
    String base64 = Base64.getEncoder().encodeToString(image.octets());

    // L'image n'est jointe qu'au premier message de la conversation : les tours suivants la
    // retrouvent dans le contexte envoyé au modèle, sans la ré-encoder à chaque question.
    List<ChatMessage> messages = new ArrayList<>();
    for (Echange echange : historique) {
      messages.add(
          messageUtilisateur(echange.question(), base64, image.typeMime(), messages.isEmpty()));
      messages.add(AiMessage.from(echange.reponse()));
    }
    messages.add(messageUtilisateur(question, base64, image.typeMime(), messages.isEmpty()));

    String texte;
    try {
      ChatResponse reponse = modele.chat(ChatRequest.builder().messages(messages).build());
      texte = reponse.aiMessage().text();
    } catch (RuntimeException e) {
      throw new DescriptionImpossibleException("Le modèle n'a pas répondu", e);
    }

    if (texte == null || texte.isBlank()) {
      throw new DescriptionImpossibleException("Le modèle a renvoyé une réponse vide");
    }
    return new Description(texte);
  }

  private static UserMessage messageUtilisateur(
      String question, String imageBase64, String typeMime, boolean premierMessage) {
    String consigne = question == null ? CONSIGNE_GENERALE : CONSIGNE_QUESTION + question;
    return premierMessage
        ? UserMessage.from(TextContent.from(consigne), ImageContent.from(imageBase64, typeMime))
        : UserMessage.from(consigne);
  }
}
