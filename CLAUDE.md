# Règles du projet

Ces règles s'appliquent à tout travail fait dans ce dépôt, par Claude comme par l'équipe.

## Git

- **Aucun `Co-Authored-By`** dans les messages de commit, ni dans les descriptions de PR.
- **Pas de commit ni de push sans accord explicite.** Claude propose le message de commit et attend la validation avant d'exécuter `git commit` ou `git push`.
- **Messages de commit au format Conventional Commits**, en français, sur une seule ligne :
  - `feat:` nouvelle fonctionnalité
  - `fix:` correction de bug
  - `docs:` documentation seule
  - `refactor:` réorganisation sans changement de comportement
  - `test:` ajout ou modification de tests
  - `chore:` build, scripts, dépendances, configuration
- Un commit = un changement cohérent. Éviter les commits qui mélangent plusieurs sujets.

## Versionnage

- Le projet suit le [versionnage sémantique](https://semver.org/lang/fr/) `MAJEUR.MINEUR.PATCH`.
- La version est définie dans `build.gradle` (`version = '...'`). Elle est actuellement `0.1.0`.
- Incrémenter le PATCH pour une correction, le MINEUR pour une fonctionnalité compatible, le MAJEUR pour un changement incompatible.
- Chaque version publiée est marquée par un tag Git `vX.Y.Z`. Le tag n'est créé et poussé qu'avec l'accord explicite de l'utilisateur.
