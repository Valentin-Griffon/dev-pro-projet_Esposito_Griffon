# 04 - Conformité au sujet

Vérification point par point du sujet « Le projet, décrire une image à qui ne la voit pas ».

**Légende** : ✅ conforme et vérifié · 🟡 conforme mais à valider (navigateur, lecteur d'écran, modèle réel) · ❌ non conforme.

Les points marqués 🟡 ne peuvent pas être vérifiés depuis le terminal : ils demandent un vrai navigateur, un vrai lecteur d'écran ou le vrai modèle.

## 1. Le parcours minimal (cinq étapes)

| Étape du sujet | État | Preuve dans le code | Remarque |
| --- | --- | --- | --- |
| 1. choisir une image et écrire une question facultative | ✅ | `index.html` : `input type="file"`, `input type="text"` | |
| 2. la page envoie l'image et la question | ✅ | `app.js` : `FormData` et `fetch POST` | |
| 3. le serveur confie à l'agent, qui renvoie une description ou une réponse | ✅ | `DescriptionControleur` appelle `DescripteurImage.decrire` | vérifié en Docker avec la fausse implémentation (HTTP 200) |
| 4. la page affiche la réponse **et** la lit à voix haute | 🟡 | `afficherDescription` lit `texte` | **corrigé pendant cette vérification** : la lecture portait sur « Description prête. » au lieu de la description. À valider dans un navigateur. |
| 5. relire, arrêter, recommencer avec une autre image | 🟡 | boutons « Relire » et « Arrêter », champ fichier réinitialisable | à valider dans un navigateur |

## 2. Interface accessible

| Exigence | État | Preuve | Remarque |
| --- | --- | --- | --- |
| utilisable sans voyant, du début à la fin | 🟡 | parcours complet à la voix (lecture de la description et des erreurs) | à valider avec un lecteur d'écran |
| les contrôles sont utilisables au clavier | ✅ | éléments natifs `input`, `button`, `form` | |
| les contrôles ont des étiquettes | ✅ | `<label for="...">` sur le fichier et la question | |
| la description écrite est dans une zone `aria-live` | ✅ | `<div id="resultat" aria-live="polite">` | |
| le statut est annoncé | ✅ | `<p id="statut" role="status" aria-live="polite">` | |
| la langue de la page est déclarée | ✅ | `<html lang="fr">` | |
| le focus est visible | ✅ | `button:focus, input:focus { outline: 3px ... }` | |
| le focus ne se perd pas pendant l'analyse | ✅ | « Analyser » reste actif ; un drapeau `analyseEnCours` ignore les doubles envois | **corrigé** : « Arrêter » était désactivé et faisait perdre le focus |
| annonces doublées | 🟡 | statut et description sont tous deux annoncés, en plus de la voix | à vérifier avec un lecteur d'écran ; peut être trop bavard |

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
| l'application fonctionne de bout en bout sans modèle | 🟡 | la stack Docker répond en HTTP 200 et refuse un type incohérent (HTTP 400). La lecture vocale reste à valider dans un navigateur. |
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
| lecture bloquée par le navigateur | 🟡 | lecture déclenchée par l'envoi du formulaire (action utilisateur) ; bouton « Relire » en secours. Plusieurs navigateurs peuvent bloquer la lecture après l'attente réseau : à vérifier. |
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
| commits au nom de chaque auteur | ✅ | **vérifié le 20/10** : l'historique contient des commits sous les deux identités (`Valentin-Griffon`/`Valentin Griffon <griffonv@3il.fr>` et `Esposito Mathis`/`Mathis Esposito <mathis311005@gmail.com>`). |
| README complet | ✅ | les quatre points exigés par le sujet y sont : ce que fait l'application, comment l'installer/la lancer, quel modèle elle attend, comment passer à la fausse implémentation. La ligne « sera complété avant le rendu » peut être retirée une fois la relecture finale faite. |
| les deux implémentations rendues | ✅ | voir section 4 |

## 10. Répartition du binôme

Le sujet demande que chacun écrive du Java, avec une répartition (A : agent LangChain4J, consigne, fausse implémentation, configuration Ollama ; B : web, validation, traduction des erreurs, page et synthèse vocale). **Vérifié le 20/10** sur l'historique Git :

- `Mathis Esposito` (espositm@3il.fr / mathis311005@gmail.com) a écrit : la validation des images et des réponses de l'agent (`Image`), les tests unitaires et du contrôleur, le paramétrage Ollama (port, adresse, modèle) et Docker. Cela recoupe surtout la partie **A** (agent/Ollama), avec un peu de B (validation côté contrôleur).
- `Valentin-Griffon` (griffonv@3il.fr) a écrit : la base du projet (y compris l'agent et la fausse implémentation initiaux), la conteneurisation Docker, les corrections d'accessibilité (focus, lecture vocale) et la traduction des erreurs en messages lisibles. Cela recoupe surtout la partie **B** (web/accessibilité), avec la mise en place initiale de l'agent.

La répartition n'est donc pas une coupure stricte A/B mais les deux ont écrit du Java sur les deux parties. À l'oral, chacun doit pouvoir présenter ses propres commits sans l'assistant (N27) ; c'est à confirmer entre vous avant le 4 novembre.

## 11. Ce qui reste à faire avant le 4 novembre

1. Valider la lecture vocale et la navigation au clavier dans un navigateur (Chrome ou Firefox).
2. Tester un lecteur d'écran (NVDA, VoiceOver ou Orca).
3. Tester la consigne avec le vrai modèle (`llava` ou `qwen2.5vl`, déjà téléchargés dans le conteneur Ollama) sur des images dont on connaît le contenu.
4. Retirer la ligne « Ce README sera complété avant le rendu » une fois la relecture finale faite (le contenu exigé par le sujet y est déjà).
5. Convenir ensemble de qui présente quelle partie à l'oral (voir section 10), et préparer les réponses sans l'assistant (voir [01 - Choix](01-choix-conception.md)).
