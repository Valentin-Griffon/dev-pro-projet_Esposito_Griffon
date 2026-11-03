# 01 - Choix de conception

Chaque choix est présenté avec sa raison et, quand il y en a une, l'alternative écartée. Les numéros de notion (N9, N10…) renvoient au cours du module.

## 1. Architecture générale

**Choix** : une application Spring Boot qui sert une page statique, expose une seule route REST et délègue la description à un agent derrière une interface.

**Pourquoi** : le sujet impose une séparation nette entre le contrôleur web et l'agent. Le contrôleur ne doit jamais savoir qu'Ollama ou LangChain4J existent.

**Règle de dépendance** : `web` dépend de `agent`, jamais l'inverse. Seule la classe `OllamaDescripteur` importe LangChain4J (vérifié par recherche dans le code, voir [04 - Conformité](04-conformite-sujet.md)).

## 2. Interface `DescripteurImage` (N10)

**Choix** : l'interface décrit ce dont le contrôleur a besoin (« décris-moi cette image »), avec ses propres types `Image` et `Description`.

```java
Description decrire(Image image, String question) throws DescriptionImpossibleException;
```

**Pourquoi** : si la signature mentionnait `ImageContent` ou `ChatResponse`, changer de bibliothèque obligerait à modifier le contrôleur. Le sujet signale ce piège explicitement.

**Alternative écartée** : une interface avec une méthode par appel LangChain4J. Elle n'aurait fait que recopier la bibliothèque.

## 3. Patron Adaptateur autour de LangChain4J (N12)

**Choix** : `OllamaDescripteur` adapte `ChatModel` à l'interface `DescripteurImage`. Il construit le message (consigne et image en base64), appelle le modèle et traduit le résultat en `Description`.

**Pourquoi** : c'est le cas d'usage typique du patron Adaptateur : une bibliothèque externe, une interface attendue par le code.

**Autres patrons** : l'injection de dépendances (voir 5) et le choix d'une implémentation parmi plusieurs (proche du patron Stratégie). Le patron Valeur (*Value Object*) est appliqué à `Image` et `Description`.

## 4. Deux implémentations, choisies par profil

**Choix** :

| Classe | Condition d'activation | Rôle |
| --- | --- | --- |
| `OllamaDescripteur` | `@Profile("!faux")` (par défaut) | appelle le modèle |
| `FauxDescripteur` | `@Profile("faux")` | répond une phrase prévisible, sans modèle |

**Pourquoi** : le sujet demande les deux implémentations, activables sans toucher au Java. Un profil Spring répond à ce besoin, et il est documenté dans le README.

**Alternative écartée** : une classe `@Configuration` avec un `if` sur une propriété. Elle aurait été plus lourde pour le même résultat.

## 5. Injection par constructeur (N9, N10)

**Choix** : `DescriptionControleur` reçoit `DescripteurImage` dans son constructeur. Spring fournit l'implémentation.

**Pourquoi** : le contrôleur n'appelle jamais `new OllamaDescripteur()`. Il reste testable avec un simulateur (`@MockitoBean`).

## 6. Gestion des erreurs : exception vérifiée (N14, N17)

**Choix** : `DescriptionImpossibleException` est une exception **vérifiée** (elle étend `Exception`), déclarée par l'interface.

**Pourquoi** : un modèle arrêté, un délai dépassé ou une réponse vide sont des événements prévisibles, extérieurs au code. Une exception vérifiée oblige l'appelant à les traiter. Le contrôleur les transforme en réponse 503 avec un message lisible.

**Coût accepté** : la signature de l'interface porte un `throws`. C'est le prix d'une erreur qu'on ne peut pas ignorer.

**Erreurs de préconditions** : elles sont des `IllegalArgumentException` (non vérifiées), car elles viennent d'une saisie invalide, traitée immédiatement par le contrôleur.

## 7. Préconditions (N18) : qui vérifie quoi

| Précondition | Où elle est vérifiée | Résultat |
| --- | --- | --- |
| Image nulle | `Image` (`requireNonNull`) | erreur de programmation |
| Fichier vide | `Image` | `IllegalArgumentException`, message lu, HTTP 400 |
| Type MIME absent ou non pris en charge | `Image` | `IllegalArgumentException`, HTTP 400 |
| En-tête du fichier ne correspondant pas au type | `Image` | `IllegalArgumentException`, HTTP 400 |
| Fichier de plus de 5 Mo | Spring (`spring.servlet.multipart.max-file-size`) | HTTP 413 |
| Question vide ou blanche | contrôleur, transformée en `null` | équivaut à une question absente |
| Question de plus de 500 caractères | contrôleur | HTTP 400 |

**Pourquoi dans `Image`** : l'agent ne doit jamais recevoir une image invalide. En plaçant la validation dans le type, toute image construite est valide, quel que soit l'appelant.

**Pourquoi le type MIME n'est pas suffisant** : le type est annoncé par le navigateur, donc fourni par le client. `Image` vérifie aussi la signature du fichier (les premiers octets : `FF D8 FF` pour JPEG, `89 50 4E 47` pour PNG, etc.). Le sujet rappelle que l'extension ne dit rien du contenu.

**Copie défensive** : `Image` copie le tableau d'octets à la construction et à la lecture. Un appelant ne peut donc pas modifier l'image après validation.

**Question vide** : le sujet demande de la documenter. C'est fait dans la Javadoc de `DescripteurImage`. Le contrôleur normalise, et l'agent ne reçoit jamais de question blanche.

## 8. Messages d'erreur destinés à la voix (sujet, « difficultés fréquentes »)

**Choix** : chaque erreur renvoie `{"description": null, "message": "..."}`. Le message est en français, sans code HTTP ni trace. La page le lit à voix haute.

**Pourquoi** : le sujet demande un message compréhensible par un utilisateur non technicien qui ne voit pas l'écran.

**Filet de sécurité** : une erreur inattendue renvoie HTTP 500 avec un message générique, et elle est journalisée côté serveur. Sans cela, la page recevrait une réponse brute sans texte à lire.

## 9. Interface web sans framework

**Choix** : HTML, CSS et JavaScript simples, sans bibliothèque. La lecture utilise l'API Web Speech (`speechSynthesis`), dans le navigateur.

**Pourquoi** :
- le sujet cite ces outils comme suffisants ;
- la page reste lisible et facile à auditer pour l'accessibilité ;
- aucun serveur supplémentaire n'est nécessaire pour la voix.

**Accessibilité** :
- contrôles natifs (`input type="file"`, `button`), donc utilisables au clavier ;
- chaque champ a un `<label>` ;
- `lang="fr"` sur la page ;
- zones `aria-live` pour le statut et la description ;
- contour de focus visible (5 px) ;
- repères de page (`<header>`, `<main>`) et titre `h1` ;
- chaque contrôle annonce à voix haute son rôle quand il reçoit le focus (attribut `data-voix`, lu par `app.js`), pour qu'une personne qui ne voit pas l'écran sache où elle se trouve et ce que fait Entrée ;
- touche Échap pour couper la voix depuis n'importe où ;
- le choix d'un fichier est annoncé (« Image choisie : nom.jpg »).

**Style** : `style.css`, séparé de la page. Fort contraste (thème clair, thème sombre selon le système, `prefers-contrast`), texte agrandi (125 %), boutons larges et séparés, une seule colonne, bouton désactivé reconnaissable sans la couleur (bordure en pointillé), pas d'animation. Pour les personnes malvoyantes.

**Limite connue** : le muet de l'onglet du navigateur ne coupe pas la voix, car la Web Speech API ne passe pas par l'audio de l'onglet et la page ne peut pas le détecter. L'arrêt se fait avec Échap ou le bouton.

**Lecture** : déclenchée par l'envoi du formulaire, donc par une action de l'utilisateur, car les navigateurs bloquent la voix sans interaction. Un bouton « Relire » et un bouton « Arrêter la lecture » sont toujours disponibles. La voix `fr-FR` est choisie si elle existe.

**Correction apportée** : le bouton « Arrêter la lecture » était désactivé tant qu'aucune lecture ne tournait. Un utilisateur au clavier perdait alors le focus. Il reste maintenant actif en permanence.

## 10. Limites et délais

| Réglage | Valeur | Raison |
| --- | --- | --- |
| Taille d'un fichier | 5 Mo | une photo de téléphone dépasse 1 Mo (valeur par défaut de Spring) |
| Taille de la requête | 6 Mo | marge pour la question et les en-têtes |
| Question | 500 caractères | protège le modèle et la page |
| Délai du modèle | 2 minutes, réglable par `DESCRIPTEUR_OLLAMA_DELAI` ou `descripteur.ollama.delai` | un modèle de vision sans GPU est lent |

Le sujet demande un délai réglable et un message d'attente : le délai est dans `application.properties`, et la page affiche « Analyse en cours » pendant le traitement.

## 11. Configuration par variables d'environnement

**Choix** : chaque réglage utile a une valeur par défaut et peut être remplacé par une variable d'environnement (`DESCRIPTEUR_PORT`, `DESCRIPTEUR_OLLAMA_HOTE`, `DESCRIPTEUR_OLLAMA_PORT`, `DESCRIPTEUR_OLLAMA_MODELE`).

**Pourquoi** : l'application n'a pas forcément le même emplacement d'un poste à l'autre. Elle peut tourner en conteneur, et Ollama peut être ailleurs, sans recompiler.

## 12. Docker

**Choix** : une image de l'application construite en deux étapes (compilation avec le JDK, exécution avec le JRE), et un `docker-compose.yml` qui lance deux conteneurs : l'application et Ollama. Ollama a son propre conteneur (image `ollama/ollama`, accès au GPU NVIDIA, modèles gardés dans un volume), séparé de l'image de l'application. L'application le joint par le nom du service (`DESCRIPTEUR_OLLAMA_HOTE=ollama`).

**Pourquoi** : le sujet demande que l'application se lance sur un poste. Docker rend l'environnement reproductible, sans installer Java à la main.

## 13. Tests (N26, N27)

**Choix** : JUnit 5, AssertJ, Mockito, et `@WebMvcTest` pour le contrôleur.

| Classe de test | Ce qu'elle vérifie |
| --- | --- |
| `ImageTest` | formats acceptés et refusés, signature, copie défensive |
| `DescriptionTest` | texte vide refusé, espaces retirés |
| `FauxDescripteurTest` | réponses prévisibles |
| `OllamaDescripteurTest` | appel du modèle simulé, erreurs, réponses vides |
| `DescriptionControleurTest` | codes HTTP, messages, transmission de la question |
| `DescriptionImageApplicationTests` | démarrage du contexte avec le profil `faux` |

**Limite** : aucun test ne lance un vrai modèle Ollama. Le modèle est simulé. Ollama est maintenant dans la stack Docker, mais aucun test d'intégration automatique ne l'appelle encore.

## 14. Versionnage et Git

**Choix** : SemVer (`0.1.0` actuellement) et Conventional Commits en français (`feat`, `fix`, `docs`, `refactor`, `test`, `chore`). Pas de `Co-Authored-By`. Pas de commit ni de push sans accord.

**Pourquoi** : le sujet demande un historique qui montre qui a écrit quoi, et des commits réguliers.
