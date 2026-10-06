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

/**
 * Reçoit l'image et la question, les confie à l'agent et renvoie la réponse.
 * Ne connaît que l'interface DescripteurImage : jamais Ollama ni LangChain4J.
 */
@RestController
@RequestMapping("/api/descriptions")
public class DescriptionControleur {

    static final int QUESTION_MAX = 500;

    private final DescripteurImage descripteur;

    public DescriptionControleur(DescripteurImage descripteur) {
        this.descripteur = descripteur;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ReponseDescription> decrire(@RequestParam("image") MultipartFile fichier,
                                                      @RequestParam(value = "question", required = false) String question)
            throws IOException, DescriptionImpossibleException {

        Image image;
        try {
            image = new Image(fichier.getBytes(), fichier.getContentType());
        } catch (IllegalArgumentException e) {
            return erreur(HttpStatus.BAD_REQUEST, e.getMessage());
        }

        String questionNettoyee = question == null || question.isBlank() ? null : question.strip();
        if (questionNettoyee != null && questionNettoyee.length() > QUESTION_MAX) {
            return erreur(HttpStatus.BAD_REQUEST,
                    "La question est trop longue. Limitez-la à " + QUESTION_MAX + " caractères.");
        }

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
