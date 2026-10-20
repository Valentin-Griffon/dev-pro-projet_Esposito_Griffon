package fr.imagedescription.web;

import fr.imagedescription.agent.DescripteurImage;
import fr.imagedescription.agent.Description;
import fr.imagedescription.agent.DescriptionImpossibleException;
import fr.imagedescription.agent.Image;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.support.MissingServletRequestPartException;

/**
 * Reçoit l'image et la question, les confie à l'agent et renvoie la réponse. Ne connaît que
 * l'interface DescripteurImage : jamais Ollama ni LangChain4J. Chaque erreur renvoie un message en
 * français, destiné à être lu à voix haute.
 */
@RestController
@RequestMapping("/api/descriptions")
public class DescriptionControleur {

  static final int QUESTION_MAX = 500;

  private static final Logger JOURNAL = LoggerFactory.getLogger(DescriptionControleur.class);

  private final DescripteurImage descripteur;

  public DescriptionControleur(DescripteurImage descripteur) {
    this.descripteur = descripteur;
  }

  @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public ResponseEntity<ReponseDescription> decrire(
      @RequestParam("image") MultipartFile fichier,
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
      return erreur(
          HttpStatus.BAD_REQUEST,
          "La question est trop longue. Limitez-la à " + QUESTION_MAX + " caractères.");
    }

    Description description = descripteur.decrire(image, questionNettoyee);
    return ResponseEntity.ok(new ReponseDescription(description.texte()));
  }

  @ExceptionHandler(MaxUploadSizeExceededException.class)
  public ResponseEntity<ReponseDescription> fichierTropGros() {
    return erreur(
        HttpStatus.PAYLOAD_TOO_LARGE,
        "Cette image est trop lourde. Choisissez une image de moins de 5 Mo.");
  }

  @ExceptionHandler({
    MissingServletRequestPartException.class,
    MissingServletRequestParameterException.class
  })
  public ResponseEntity<ReponseDescription> imageAbsente() {
    return erreur(HttpStatus.BAD_REQUEST, "Choisissez d'abord une image.");
  }

  @ExceptionHandler(MultipartException.class)
  public ResponseEntity<ReponseDescription> envoiIllisible() {
    return erreur(
        HttpStatus.BAD_REQUEST,
        "Le fichier envoyé n'a pas pu être lu. Choisissez une autre image.");
  }

  @ExceptionHandler(DescriptionImpossibleException.class)
  public ResponseEntity<ReponseDescription> agentIndisponible(DescriptionImpossibleException e) {
    JOURNAL.warn("Agent indisponible : {}", e.getMessage());
    return erreur(
        HttpStatus.SERVICE_UNAVAILABLE,
        "La description n'a pas pu être produite. Réessayez dans un instant.");
  }

  /**
   * Filet de sécurité : toute autre erreur reçoit un message lisible au lieu d'une réponse brute.
   */
  @ExceptionHandler({IOException.class, RuntimeException.class})
  public ResponseEntity<ReponseDescription> erreurInattendue(Exception e) {
    JOURNAL.error("Erreur inattendue pendant l'analyse", e);
    return erreur(
        HttpStatus.INTERNAL_SERVER_ERROR,
        "Une erreur inattendue s'est produite. Réessayez dans un instant.");
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
