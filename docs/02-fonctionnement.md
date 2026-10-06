# 02 - Fonctionnement

## 1. Vue d'ensemble

L'application décrit à voix haute une image déposée par l'utilisateur. Elle se compose de trois parties :

1. **La page** (`src/main/resources/static/`) : choix du fichier, question facultative, affichage et lecture de la réponse.
2. **Le contrôleur** (`web/DescriptionControleur`) : reçoit la requête, valide la saisie, appelle l'agent, traduit les erreurs en réponses.
3. **L'agent** (`agent/`) : une interface `DescripteurImage` et deux implémentations, la vraie (Ollama via LangChain4J) et la fausse.

Le diagramme de composants est dans [03 - Diagrammes UML](03-diagrammes-uml.md#7-composants-et-déploiement).

## 2. Parcours utilisateur

Le parcours minimal du sujet tient en cinq étapes :

1. l'utilisateur choisit une image et, s'il le souhaite, écrit une question ;
2. la page envoie l'image et la question au serveur ;
3. le serveur les confie à l'agent, qui renvoie une description ou la réponse à la question ;
4. la page affiche la réponse et la lit à voix haute ;
5. l'utilisateur peut relire, arrêter, ou recommencer avec une autre image.

| Action | Effet |
| --- | --- |
| « Analyser l'image » | envoie le formulaire, annonce « Analyse en cours », lit l'annonce |
| « Relire la description » | relit la dernière description (disponible après une réponse réussie) |
| « Arrêter la lecture » | coupe la lecture en cours |
| Choix d'un autre fichier, puis « Analyser » | nouvelle analyse |

Les boutons sont natifs, donc utilisables au clavier avec Tab et Entrée.

## 3. API REST

Une seule route : `POST /api/descriptions`, en `multipart/form-data`.

| Champ | Obligatoire | Contenu |
| --- | --- | --- |
| `image` | oui | fichier JPEG, PNG, WebP ou GIF, 5 Mo maximum |
| `question` | non | texte, 500 caractères maximum ; vide équivaut à absent |

**Réponses** (corps JSON) :

| Code | Cas | Corps |
| --- | --- | --- |
| 200 | description produite | `{"description": "...", "message": null}` |
| 400 | fichier vide, type refusé, contenu ne correspondant pas au type, question trop longue, image absente, envoi illisible | `{"description": null, "message": "..."}` |
| 413 | fichier de plus de 5 Mo | `{"description": null, "message": "..."}` |
| 503 | l'agent ne répond pas (modèle arrêté, délai dépassé, réponse vide) | `{"description": null, "message": "..."}` |
| 500 | erreur inattendue | `{"description": null, "message": "Une erreur inattendue..."}` |

Le champ `message` est toujours en français et prêt à être lu à voix haute.

## 4. Vérifications, dans l'ordre

Le diagramme d'activité détaille ce cheminement. En résumé :

1. Spring refuse un fichier de plus de 5 Mo (413).
2. `Image` refuse un fichier vide, un type non pris en charge ou une signature incohérente (400).
3. Le contrôleur normalise la question (suppression des espaces, blanc = absent) et refuse plus de 500 caractères (400).
4. L'agent produit la description. Si le modèle échoue, `DescriptionImpossibleException` donne 503.
5. Toute autre erreur donne 500, avec un message générique et une trace dans le journal serveur.

## 5. Configuration

Les réglages se trouvent dans `src/main/resources/application.properties`. Chacun peut être remplacé par une variable d'environnement.

| Propriété | Variable d'environnement | Défaut | Rôle |
| --- | --- | --- | --- |
| `server.port` | `DESCRIPTEUR_PORT` | `8080` | port HTTP de l'application |
| `descripteur.ollama.hote` | `DESCRIPTEUR_OLLAMA_HOTE` | `127.0.0.1` | adresse d'Ollama |
| `descripteur.ollama.port` | `DESCRIPTEUR_OLLAMA_PORT` | `11434` | port d'Ollama (port par défaut) |
| `descripteur.ollama.url` | (composée des deux précédentes) | `http://127.0.0.1:11434` | URL d'Ollama |
| `descripteur.ollama.modele` | `DESCRIPTEUR_OLLAMA_MODELE` | `llava` | modèle de vision |
| `descripteur.ollama.delai` | `DESCRIPTEUR_OLLAMA_DELAI` | `2m` | délai maximal d'une réponse |
| `spring.profiles.active` | (profil) | aucun | `faux` active la fausse implémentation |

**Activer la fausse implémentation** : `./gradlew bootRun --args='--spring.profiles.active=faux'`, ou `SPRING_PROFILES_ACTIVE=faux`.

## 6. Lancer l'application

| Méthode | Commande | Ollama nécessaire |
| --- | --- | --- |
| Développement, sans modèle | `./gradlew bootRun --args='--spring.profiles.active=faux'` | non |
| Développement, avec modèle | `./gradlew bootRun` (Ollama lancé, modèle `llava` téléchargé) | oui |
| Docker | `docker compose up --build` (profil `faux` par défaut) | non |
| Installation Docker sur une machine vierge | `./install.sh` | non |

## 7. Démarrage et choix de l'agent

Au démarrage, Spring crée un seul bean `DescripteurImage`, selon le profil actif, puis l'injecte dans le contrôleur. Le diagramme de séquence est dans [03](03-diagrammes-uml.md#9-choix-de-limplémentation-au-démarrage).

## 8. Erreurs côté utilisateur

Une erreur n'affiche jamais de code HTTP ni de trace. La page :

1. affiche le message dans la zone de statut (annoncé par le lecteur d'écran) ;
2. le lit à voix haute ;
3. désactive « Relire », car il n'y a plus de description.

Si le serveur est injoignable, la page affiche : « Impossible de joindre le serveur. Vérifiez que l'application est lancée. »

## 9. Tests

```bash
./gradlew test
```

Les 36 tests vérifient la validation des images, la fausse et la vraie implémentation (avec un modèle simulé), le contrôleur (codes HTTP et messages) et le démarrage du contexte avec le profil `faux`.

## 10. Structure du projet

```
ProjetJavaPro/
├── Dockerfile, docker-compose.yml, .dockerignore   conteneurisation
├── install.sh                                       installation Docker
├── build.gradle, settings.gradle, gradlew           construction
├── CLAUDE.md                                        règles du projet (Git, versions, conteneurs)
├── docs/                                            cette documentation
├── src/main/java/fr/imagedescription/
│   ├── DescriptionImageApplication.java
│   ├── agent/        interface, valeurs, Ollama et fausse implémentation
│   └── web/          contrôleur REST
├── src/main/resources/
│   ├── application.properties
│   └── static/       index.html, app.js
└── src/test/java/    tests unitaires et de contrôleur
```

## 11. Versions

Le projet suit le versionnage sémantique (`MAJEUR.MINEUR.PATCH`), défini dans `build.gradle`. Les règles complètes sont dans [CLAUDE.md](../CLAUDE.md).
