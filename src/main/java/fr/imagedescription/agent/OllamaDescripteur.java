package fr.imagedescription.agent;

import dev.langchain4j.data.message.ImageContent;
import dev.langchain4j.data.message.TextContent;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.ollama.OllamaChatModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Base64;

/**
 * Agent réel : appelle un modèle de vision via Ollama, à travers LangChain4J.
 * Toute la connaissance de LangChain4J reste dans cette classe.
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

    public OllamaDescripteur(@Value("${descripteur.ollama.url}") String url,
                             @Value("${descripteur.ollama.modele}") String nomModele,
                             @Value("${descripteur.ollama.delai}") Duration delai) {
        this.modele = OllamaChatModel.builder()
                .baseUrl(url)
                .modelName(nomModele)
                .timeout(delai)
                .build();
    }

    @Override
    public Description decrire(Image image, String question) throws DescriptionImpossibleException {
        String consigne = question == null ? CONSIGNE_GENERALE : CONSIGNE_QUESTION + question;
        String base64 = Base64.getEncoder().encodeToString(image.octets());

        String texte;
        try {
            UserMessage message = UserMessage.from(
                    TextContent.from(consigne),
                    ImageContent.from(base64, image.typeMime()));
            texte = modele.chat(message).aiMessage().text();
        } catch (RuntimeException e) {
            throw new DescriptionImpossibleException("Le modèle n'a pas répondu", e);
        }

        if (texte == null || texte.isBlank()) {
            throw new DescriptionImpossibleException("Le modèle a renvoyé une réponse vide");
        }
        return new Description(texte.strip());
    }
}
