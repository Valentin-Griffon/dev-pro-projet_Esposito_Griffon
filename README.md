# Décrire une image

Application web qui décrit à voix haute une image déposée par l'utilisateur, pour les personnes qui ne peuvent pas la voir.

La documentation (choix de conception, fonctionnement, diagrammes UML, conformité au sujet) est dans [docs/](docs/README.md).

## Lancer l'application

Prérequis : Java 17 ou plus.

```bash
./gradlew bootRun
```

Puis ouvrir http://localhost:8080.

## Lancer avec Docker

Sur une machine vierge, `./install.sh` installe Docker si besoin puis lance l'application.
Ensuite, `docker compose up --build` suffit.

Puis ouvrir http://localhost:8080. La stack lance deux conteneurs : l'application (profil `reel`, qui appelle le modèle) et Ollama, dans un conteneur séparé. Le conteneur Ollama utilise le GPU NVIDIA (`nvidia.com/gpu=all`) et garde ses modèles dans le volume `ollama_donnees`.

Au premier démarrage, il faut télécharger le modèle une fois :

```bash
docker compose exec ollama ollama pull llava
```

Sous WSL2 avec une carte NVIDIA, `bash demarrer-wsl.sh` (à lancer dans la distribution WSL) fait tout : configuration du GPU pour Docker, construction, démarrage et téléchargement du modèle choisi.

Pour lancer la stack sans modèle : `PROFIL=faux docker compose up --build`.

## Configuration

Les réglages se font par variables d'environnement, sans modifier le code :

| Variable | Valeur par défaut | Rôle |
| --- | --- | --- |
| `DESCRIPTEUR_PORT` | `8080` | Port HTTP de l'application |
| `DESCRIPTEUR_OLLAMA_HOTE` | `127.0.0.1` | Adresse IP ou nom d'hôte du serveur Ollama |
| `DESCRIPTEUR_OLLAMA_PORT` | `11434` | Port du serveur Ollama (port par défaut d'Ollama) |
| `DESCRIPTEUR_OLLAMA_MODELE` | `llava` | Modèle de vision utilisé (il doit accepter les images) |

Exemple : `DESCRIPTEUR_PORT=9000 ./gradlew bootRun`.

Avec Docker, le port et le modèle se règlent de la même façon : `DESCRIPTEUR_PORT=9000 docker compose up`. Pour garder un réglage d'un lancement à l'autre, l'écrire dans un fichier `.env` à la racine du projet (par exemple `DESCRIPTEUR_OLLAMA_MODELE=qwen2.5vl`) : Docker Compose le lit automatiquement.

## Fausse implémentation (sans modèle)

```bash
./gradlew bootRun --args='--spring.profiles.active=faux'
```

## Modèle réel

Le profil par défaut appelle Ollama avec le modèle `llava`
(réglages dans `src/main/resources/application.properties`).

Pour changer de modèle, le télécharger dans Ollama puis le désigner avec `DESCRIPTEUR_OLLAMA_MODELE`. Il doit s'agir d'un modèle de vision (par exemple `qwen2.5vl`, `llama3.2-vision`, `gemma3`) :

```bash
docker compose exec ollama ollama pull qwen2.5vl
DESCRIPTEUR_OLLAMA_MODELE=qwen2.5vl docker compose up -d application
```

L'adresse et le port d'Ollama se règlent avec les variables `DESCRIPTEUR_OLLAMA_HOTE` et `DESCRIPTEUR_OLLAMA_PORT` (voir la section Configuration). Exemple : `DESCRIPTEUR_OLLAMA_HOTE=192.168.1.20 ./gradlew bootRun`.

Ce réglage donne de la souplesse au système : Ollama peut tourner sur la même machine que l'application ou ailleurs, sans recompiler l'application.


*Ce README sera complété avant le rendu.*
