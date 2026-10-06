package fr.imagedescription.web;

import fr.imagedescription.agent.Description;
import fr.imagedescription.agent.DescriptionImpossibleException;
import fr.imagedescription.agent.DescripteurImage;
import fr.imagedescription.agent.Image;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

/**
 * Reçoit l'image et la question, les confie à l'agent et renvoie la réponse.
 * Ne connaît que l'interface DescripteurImage : jamais Ollama ni LangChain4J.
 */
@RestController
@RequestMapping("/api/descriptions")
public class DescriptionControleur {

    private static final List<String> TYPES_ACCEPTES = List.of("image/jpeg", "image/png", "image/webp", "image/gif");

    private final DescripteurImage descripteur;

    public DescriptionControleur(DescripteurImage descripteur) {
        this.descripteur = descripteur;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ReponseDescription> decrire(@RequestParam("image") MultipartFile fichier,
                                                      @RequestParam(value = "question", required = false) String question)
            throws IOException, DescriptionImpossibleException {

        if (fichier.isEmpty()) {
            return erreur(HttpStatus.BAD_REQUEST, "Le fichier choisi est vide. Choisissez une autre image.");
        }
        if (!TYPES_ACCEPTES.contains(fichier.getContentType())) {
            return erreur(HttpStatus.BAD_REQUEST,
                    "Ce type de fichier n'est pas pris en charge. Choisissez une image JPEG, PNG, WebP ou GIF.");
        }

        String questionNettoyee = question == null || question.isBlank() ? null : question.strip();
        Image image = new Image(fichier.getBytes(), fichier.getContentType());
        Description description = descripteur.decrire(image, questionNettoyee);

        return ResponseEntity.ok(new ReponseDescription(description.texte()));
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ReponseDescription> fichierTropGros() {
        return erreur(HttpStatus.PAYLOAD_TOO_LARGE,
                "Cette image est trop lourde. Choisissez une image de moins de 5 Mo.");
    }

    @ExceptionHandler(DescriptionImpossibleException.class)
    public ResponseEntity<ReponseDescription> agentIndisponible() {
        return erreur(HttpStatus.SERVICE_UNAVAILABLE,
                "La description n'a pas pu être produite. Réessayez dans un instant.");
    }

    private ResponseEntity<ReponseDescription> erreur(HttpStatus statut, String message) {
        return ResponseEntity.status(statut).body(new ReponseDescription(null, message));
    }

    /**
     * Réponse JSON : soit la description, soit un message d'erreur destiné à être lu à voix haute.
     */
    public record ReponseDescription(String description, String message) {

        public ReponseDescription(String description) {
            this(description, null);
        }
    }
}
