# Décrire une image

Application web d'audiodescription : elle décrit à voix haute une image déposée par l'utilisateur, pour les personnes qui ne peuvent pas la voir.

## Ce que fait l'application

1. L'utilisateur choisit une image sur son poste et, s'il le souhaite, écrit une question à son sujet (« Quelle est la date de péremption ? »).
2. La page envoie l'image et la question au serveur.
3. Le serveur confie l'image à un agent d'IA (un modèle de vision, via Ollama et LangChain4J). L'agent renvoie en français une description générale de l'image, ou la réponse à la question. Une bonne description donne d'abord l'essentiel, puis les détails utiles, et lit le texte présent dans l'image (panneau, légende, menu).
4. La page affiche la réponse et la lit à voix haute.
5. L'utilisateur peut relire la description, arrêter la lecture (bouton ou touche Échap) et recommencer avec une autre image.

La page est pensée pour être utilisée sans voir l'écran : tout se fait au clavier, chaque champ a son étiquette, chaque contrôle annonce à voix haute son rôle quand il reçoit le focus, et les messages d'état et d'erreur sont lus à voix haute et annoncés aux lecteurs d'écran. Le style est à fort contraste, avec un texte agrandi, pour les personnes malvoyantes. Quand quelque chose échoue (fichier refusé, modèle arrêté, délai dépassé), un message en français explique ce qui s'est passé et ce qu'il faut faire ensuite.

Côté code, l'agent est placé derrière l'interface `DescripteurImage` : le contrôleur web ne connaît ni Ollama ni LangChain4J. Le projet contient deux implémentations de cette interface : `OllamaDescripteur` (appelle le modèle) et `FauxDescripteur` (renvoie une réponse fixe, sans modèle). La documentation détaillée (choix de conception, fonctionnement, diagrammes UML, conformité au sujet) est dans [docs/](docs/README.md).

## Deux façons de lancer l'application

Les deux façons n'ont pas les mêmes dépendances. Choisissez-en une.

| | Avec Docker | Avec Gradle |
| --- | --- | --- |
| À installer sur la machine | Docker et Docker Compose, rien d'autre | Java 17 ou plus, et Ollama pour le vrai modèle |
| Compilation | dans un conteneur | par le wrapper `./gradlew` (aucune installation de Gradle) |
| Ollama | dans un conteneur séparé | à installer sur la machine |

L'application s'ouvre ensuite sur http://localhost:8080.

### Avec Docker

Seul Docker est nécessaire : Java, Gradle et Ollama tournent dans les conteneurs. Sur une machine Debian ou Ubuntu vierge, `./install.sh` installe Docker si besoin puis lance l'application. Si Docker est déjà installé :

```bash
docker compose up --build
```

La stack lance deux conteneurs : l'application et Ollama. Le conteneur Ollama utilise un GPU NVIDIA (`nvidia.com/gpu=all`) et garde ses modèles dans le volume `ollama_donnees`. Sur une machine sans GPU NVIDIA, retirer la ligne `devices` du service `ollama` dans `docker-compose.yml`.

Au premier démarrage, télécharger le modèle une fois :

```bash
docker compose exec ollama ollama pull llava
```

Sans modèle (fausse implémentation), l'application seule suffit :

```bash
PROFIL=faux docker compose up --build --no-deps application
```

### Avec Gradle

Prérequis : **Java 17 ou plus**. Gradle n'a pas besoin d'être installé, le wrapper `./gradlew` le télécharge.

Avec le vrai modèle, il faut aussi **Ollama** installé sur la machine ([ollama.com](https://ollama.com)), lancé, avec le modèle téléchargé :

```bash
ollama pull llava
./gradlew bootRun
```

Sans modèle (fausse implémentation), ni Ollama ni modèle ne sont nécessaires :

```bash
./gradlew bootRun --args='--spring.profiles.active=faux'
```

## Fausse implémentation (sans modèle)

La fausse implémentation n'appelle aucun modèle : elle renvoie une réponse fixe (« Fausse description : aucun modèle n'est appelé… » ou « Fausse réponse à la question… »). L'application fonctionne alors de bout en bout, page et synthèse vocale comprises, sur un poste où aucun modèle n'est installé. Elle s'active par le profil Spring `faux`, sans modifier le code : voir les commandes ci-dessus. Elle sert à développer la page, tester le contrôleur et faire la démonstration quand le modèle est trop lent.

## Modèle attendu

Par défaut, l'application appelle Ollama avec le modèle de vision **`llava`**. Le modèle doit accepter une image en entrée : par exemple `llava`, `llama3.2-vision`, `gemma3` ou `qwen2.5vl`. Un modèle de 7 milliards de paramètres pèse environ 5 Go et demande environ 8 Go de mémoire libre ; sans carte graphique, une description prend plusieurs dizaines de secondes.

Pour changer de modèle, le télécharger dans Ollama puis le désigner avec la variable `DESCRIPTEUR_OLLAMA_MODELE` :

```bash
# Docker
docker compose exec ollama ollama pull qwen2.5vl
DESCRIPTEUR_OLLAMA_MODELE=qwen2.5vl docker compose up -d application

# Gradle
ollama pull qwen2.5vl
DESCRIPTEUR_OLLAMA_MODELE=qwen2.5vl ./gradlew bootRun
```

## Configuration

Les réglages se font par variables d'environnement, sans modifier le code :

| Variable | Valeur par défaut | Rôle |
| --- | --- | --- |
| `DESCRIPTEUR_PORT` | `8080` | Port HTTP de l'application |
| `DESCRIPTEUR_OLLAMA_HOTE` | `127.0.0.1` | Adresse IP ou nom d'hôte du serveur Ollama |
| `DESCRIPTEUR_OLLAMA_PORT` | `11434` | Port du serveur Ollama (port par défaut d'Ollama) |
| `DESCRIPTEUR_OLLAMA_MODELE` | `llava` | Modèle de vision utilisé (il doit accepter les images) |

Exemple : `DESCRIPTEUR_PORT=9000 ./gradlew bootRun`. Ollama peut tourner sur la même machine que l'application ou ailleurs : `DESCRIPTEUR_OLLAMA_HOTE=192.168.1.20 ./gradlew bootRun`.

Avec Docker, les réglages se passent de la même façon : `DESCRIPTEUR_PORT=9000 docker compose up`. Pour garder un réglage d'un lancement à l'autre, l'écrire dans un fichier `.env` à la racine du projet (par exemple `DESCRIPTEUR_OLLAMA_MODELE=qwen2.5vl`) : Docker Compose le lit automatiquement.

Les images sont limitées à 5 Mo (formats JPEG, PNG, WebP et GIF), et le délai d'attente du modèle est de 2 minutes (réglages dans `src/main/resources/application.properties`).

## Tests

Les tests utilisent la fausse implémentation et un modèle simulé : ils n'ont besoin ni d'Ollama ni de modèle.

```bash
./gradlew test
```

Le rapport de couverture JaCoCo est écrit dans `report/jacoco`. Le format du code est vérifié avec `./gradlew spotlessCheck` (et corrigé avec `./gradlew spotlessApply`).
