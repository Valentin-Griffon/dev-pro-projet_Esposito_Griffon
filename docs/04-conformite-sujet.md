# 04 - Conformité au sujet

Vérification point par point du sujet « Le projet, décrire une image à qui ne la voit pas ».

**Légende** : ✅ conforme et vérifié · 🟡 conforme mais à valider (navigateur, lecteur d'écran, modèle réel) · ❌ non conforme.

Les points marqués 🟡 ne peuvent pas être vérifiés depuis le terminal : ils demandent un vrai navigateur, un vrai lecteur d'écran ou le vrai modèle. La lecture vocale, la navigation au clavier et le lecteur d'écran ont été validés à la main par l'équipe.

## 1. Le parcours minimal (cinq étapes)

| Étape du sujet | État | Preuve dans le code | Remarque |
| --- | --- | --- | --- |
| 1. choisir une image et écrire une question facultative | ✅ | `index.html` : `input type="file"`, `input type="text"` | |
| 2. la page envoie l'image et la question | ✅ | `app.js` : `FormData` et `fetch POST` | |
| 3. le serveur confie à l'agent, qui renvoie une description ou une réponse | ✅ | `DescriptionControleur` appelle `DescripteurImage.decrire` | vérifié en Docker avec la fausse implémentation (HTTP 200) |
| 4. la page affiche la réponse **et** la lit à voix haute | ✅ | `afficherDescription` lit `texte` | validé à la main dans un navigateur. La lecture portait d'abord sur « Description prête. » : corrigé. |
| 5. relire, arrêter, recommencer avec une autre image | ✅ | boutons « Relire » et « Arrêter » (et touche Échap), champ fichier réinitialisable | validé à la main |

## 2. Interface accessible

| Exigence | État | Preuve | Remarque |
| --- | --- | --- | --- |
| utilisable sans voyant, du début à la fin | ✅ | parcours complet à la voix : chaque contrôle annonce son rôle au focus (`data-voix`), la description et les erreurs sont lues | validé à la main |
| les contrôles sont utilisables au clavier | ✅ | éléments natifs `input`, `button`, `form` | |
| les contrôles ont des étiquettes | ✅ | `<label for="...">` sur le fichier et la question | |
| la description écrite est dans une zone `aria-live` | ✅ | `<div id="resultat" aria-live="polite">` | |
| le statut est annoncé | ✅ | `<p id="statut" role="status" aria-live="polite">` | |
| la langue de la page est déclarée | ✅ | `<html lang="fr">` | |
| le focus est visible | ✅ | `button:focus, input:focus { outline: 3px ... }` | |
| le focus ne se perd pas pendant l'analyse | ✅ | « Analyser » reste actif ; un drapeau `analyseEnCours` ignore les doubles envois | **corrigé** : « Arrêter » était désactivé et faisait perdre le focus |
| annonces doublées | ✅ | statut et description sont tous deux annoncés, en plus de la voix | vérifié à la main |
| style pour les personnes malvoyantes | ✅ | `style.css` : fort contraste (thèmes clair et sombre, `prefers-contrast`), texte agrandi, boutons larges, focus de 5 px, une seule colonne | la page est servie par `index.html` (`<link rel="stylesheet">`) |
| repères de page | ✅ | `<header>`, `<main>`, titre `h1`, étiquettes sur les champs | |
| arrêt de la voix au clavier | ✅ | touche Échap et bouton « Arrêter la lecture » | le muet de l'onglet ne coupe pas la voix (limite de la Web Speech API, acceptée) |

## 3. Agent derrière une interface

| Exigence | État | Preuve |
| --- | --- | --- |
| une interface, utilisant les types de l'application | ✅ | `DescripteurImage` n'utilise que `Image`, `Description`, `DescriptionImpossibleException` |
| le contrôleur ne connaît que l'interface | ✅ | aucun import de LangChain4J dans `web/` (recherche dans le code) |
| aucune classe LangChain4J hors de l'agent (N9) | ✅ | seul `OllamaDescripteur` importe LangChain4J (et son test) |
| le contrôleur reçoit l'agent par son constructeur | ✅ | `DescriptionControleur(DescripteurImage descripteur)` |
| le choix de l'implémentation se fait à un seul endroit | ✅ | `@Profile("faux")` / `@Profile("!faux")` |
| changer d'agent sans modifier le contrôleur | ✅ | conséquence de la règle ci-dessus |
| Javadoc des préconditions (N18) | ✅ | `DescripteurImage` documente la vérification faite par `Image` et le traitement de la question vide |

## 4. Deux implémentations, la vraie et la fausse

| Exigence | État | Preuve |
| --- | --- | --- |
| une implémentation qui appelle le modèle | ✅ | `OllamaDescripteur` |
| une fausse implémentation sans modèle | ✅ | `FauxDescripteur` |
| lancer l'une ou l'autre sans modifier le Java | ✅ | `--spring.profiles.active=faux` ou `SPRING_PROFILES_ACTIVE=faux` |
| l'application fonctionne de bout en bout sans modèle | ✅ | la page répond (200), l'API renvoie la fausse description (200) et refuse un type incohérent (400) ; lecture vocale validée à la main |
| le README indique comment lancer sans modèle | ✅ | section « Fausse implémentation » |

## 5. Les trois décisions à justifier à l'oral

| Décision | État | Où c'est justifié |
| --- | --- | --- |
| les types dans la signature (`Image`, `Description`) | ✅ | [01 - Choix](01-choix-conception.md#2-interface-descripteurimage-n10) |
| exception vérifiée ou non | ✅ | [01 - Choix](01-choix-conception.md#6-gestion-des-erreurs--exception-vérifiée-n14-n17) |
| qui vérifie les préconditions, et que fait-on d'une question vide | ✅ | [01 - Choix](01-choix-conception.md#7-préconditions-n18--qui-vérifie-quoi) |

## 6. Erreurs présentées à l'utilisateur

| Exigence | État | Preuve |
| --- | --- | --- |
| message compréhensible sans code HTTP ni trace | ✅ | messages en français dans `Image`, `DescriptionControleur` et `app.js` |
| message lu à voix haute | ✅ | `afficherErreur` appelle `lire` |
| fichier refusé, modèle arrêté, délai dépassé, réponse vide | ✅ | 400, 503 |
| erreur inattendue | ✅ | **ajouté pendant cette vérification** : message générique en 500 et `MESSAGE_INATTENDU` côté page. Avant, la voix lisait une chaîne vide. |
| erreur réseau | ✅ | « Impossible de joindre le serveur » |

## 7. Difficultés techniques signalées par le sujet

| Difficulté | État | Traitement |
| --- | --- | --- |
| modèle lent | ✅ | délai réglable (2 min), message « Analyse en cours », lecture de l'annonce |
| fichier de plus de 1 Mo | ✅ | limite fixée à 5 Mo et 6 Mo pour la requête, et non supprimée |
| le nom du fichier ne dit pas son type | ✅ | vérification de la signature (premiers octets) |
| lecture bloquée par le navigateur | ✅ | lecture déclenchée par l'envoi du formulaire (action utilisateur) ; bouton « Relire » en secours | validé à la main |
| voix fr-FR non disponible | ✅ | `lecture.lang = 'fr-FR'`, voix française choisie si elle existe, sinon voix par défaut |
| le modèle peut inventer | ❌ | la consigne n'a pas été testée sur des images connues. À faire avec le vrai modèle. |

## 8. Consigne donnée au modèle

| Exigence | État | Preuve |
| --- | --- | --- |
| en français | ✅ | `CONSIGNE_GENERALE` et `CONSIGNE_QUESTION` |
| l'essentiel d'abord, puis les détails | ✅ | « Donne d'abord l'essentiel, puis les détails utiles » |
| lire le texte présent dans l'image | ✅ | « lis le texte présent dans l'image » |
| la question oriente la réponse | ✅ | `CONSIGNE_QUESTION + question` |
| sans question, description générale | ✅ | `question == null` |

## 9. Technique et rendu

| Exigence | État | Preuve |
| --- | --- | --- |
| Java 17 ou plus | ✅ | `build.gradle` (`VERSION_17`), `Dockerfile` (`eclipse-temurin:17`) |
| Spring Boot et LangChain4J | ✅ | `build.gradle` (Spring Boot 3.5.16, LangChain4J 1.21.0) |
| se lance avec `./gradlew bootRun` | ✅ | exécuté avec succès pendant le développement |
| dépôt GitHub avec historique | ✅ | `origin/main` |
| commits au nom de chaque auteur | ✅ | deux auteurs dans l'historique, Mathis Esposito et Valentin Griffon. Un `.mailmap` regroupe leurs identités (adresses @3il.fr). | |
| README complet | ✅ | présentation, lancement avec Docker ou avec Gradle (dépendances distinctes), fausse implémentation, modèle attendu, configuration, tests |
| les deux implémentations rendues | ✅ | voir section 4 |

## 10. Répartition du binôme

Le sujet demande que chacun écrive du Java, avec une répartition (A : agent LangChain4J, consigne, fausse implémentation ; B : web, validation, traduction des erreurs). Les deux membres ont écrit du Java : l'historique Git montre des commits de chacun sur les classes de `agent/` et de `web/` et sur les tests. Valentin Griffon a écrit la base (interface, exception, `FauxDescripteur`, `DescriptionControleur`, page) ; Mathis Esposito a ajouté la validation de `Image`, `Description`, la configuration d'Ollama, les tests, Docker et la documentation. 🟡 **Chacun doit pouvoir présenter sa partie sans l'assistant (N27)** et répondre sur celle de l'autre.

## 11. Ce qui reste à faire avant le 4 novembre

1. Tester la consigne avec le vrai modèle (`llava` ou `qwen2.5vl`, déjà téléchargés dans le conteneur Ollama) sur des images dont on connaît le contenu : c'est le dernier point ❌.
2. Préparer les réponses orales (voir [01 - Choix](01-choix-conception.md)) et la démonstration.
