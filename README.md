# Décrire une image

Application web qui décrit à voix haute une image déposée par l'utilisateur, pour les personnes qui ne peuvent pas la voir.

## Lancer l'application

Prérequis : Java 17 ou plus.

```bash
./gradlew bootRun
```

Puis ouvrir http://localhost:8080.

## Fausse implémentation (sans modèle)

```bash
./gradlew bootRun --args='--spring.profiles.active=faux'
```

## Modèle réel

Le profil par défaut appelle Ollama sur `http://localhost:11434` avec le modèle `llava`
(réglages dans `src/main/resources/application.properties`).

*Ce README sera complété avant le rendu.*
