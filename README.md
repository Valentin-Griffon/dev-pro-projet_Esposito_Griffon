# Décrire une image

Application web qui décrit à voix haute une image déposée par l'utilisateur, pour les personnes qui ne peuvent pas la voir.

## Lancer l'application

Prérequis : Java 17 ou plus.

```bash
./gradlew bootRun
```

Puis ouvrir http://localhost:8080.

## Lancer avec Docker

Sur une machine vierge, `./install.sh` installe Docker si besoin puis lance l'application.
Ensuite, `docker compose up --build` suffit.

Puis ouvrir http://localhost:8080. La stack lance l'application avec la fausse implémentation (profil `faux`). Ollama n'est pas encore intégré à Docker.

## Configuration

Les réglages se font par variables d'environnement, sans modifier le code :

| Variable | Valeur par défaut | Rôle |
| --- | --- | --- |
| `DESCRIPTEUR_PORT` | `8080` | Port HTTP de l'application |
| `DESCRIPTEUR_OLLAMA_HOTE` | `127.0.0.1` | Adresse IP ou nom d'hôte du serveur Ollama |
| `DESCRIPTEUR_OLLAMA_PORT` | `11434` | Port du serveur Ollama (port par défaut d'Ollama) |

Exemple : `DESCRIPTEUR_PORT=9000 ./gradlew bootRun`.

Avec Docker, le port de l'application se règle de la même façon : `DESCRIPTEUR_PORT=9000 docker compose up`.

## Fausse implémentation (sans modèle)

```bash
./gradlew bootRun --args='--spring.profiles.active=faux'
```

## Modèle réel

Le profil par défaut appelle Ollama avec le modèle `llava`
(réglages dans `src/main/resources/application.properties`).

L'adresse et le port d'Ollama se règlent avec les variables `DESCRIPTEUR_OLLAMA_HOTE` et `DESCRIPTEUR_OLLAMA_PORT` (voir la section Configuration). Exemple : `DESCRIPTEUR_OLLAMA_HOTE=192.168.1.20 ./gradlew bootRun`.

Ce réglage donne de la souplesse au système : Ollama peut tourner sur la même machine que l'application ou ailleurs, sans recompiler l'application.


*Ce README sera complété avant le rendu.*
