#!/usr/bin/env bash
# Prépare l'environnement Docker du projet et lance l'application.
# Java, Gradle et Ollama ne sont pas installés sur la machine : la compilation et l'exécution
# se font dans les conteneurs (voir Dockerfile et docker-compose.yml).
#
# Usage : ./install.sh
#
# Ne pas lancer avec sudo : le script demande lui-même les droits quand c'est nécessaire.

set -euo pipefail

if [ "$(id -u)" -eq 0 ]; then
    echo "Ne lancez pas ce script avec sudo : il demande lui-même les droits si besoin." >&2
    exit 1
fi

RACINE="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$RACINE"

info()    { printf '\n==> %s\n' "$*"; }
present() { command -v "$1" >/dev/null 2>&1; }

# 1. Docker
info "Vérification de Docker"
if present docker && docker compose version >/dev/null 2>&1; then
    echo "Docker et Docker Compose déjà installés."
elif present apt-get; then
    info "Installation de Docker et de Docker Compose"
    sudo apt-get update
    sudo apt-get install -y docker.io docker-compose-v2
else
    echo "Docker ou Docker Compose est absent et ne peut pas être installé avec apt." >&2
    echo "Installez-le manuellement : https://docs.docker.com/engine/install/" >&2
    exit 1
fi

# 2. Démarrage du service Docker et accès sans sudo
if present systemctl; then
    sudo systemctl enable --now docker
fi

GROUPE_AJOUTE=0
if ! id -nG | tr ' ' '\n' | grep -qx docker; then
    info "Ajout de $(id -un) au groupe docker"
    sudo usermod -aG docker "$(id -un)"
    GROUPE_AJOUTE=1
fi

# Tant que la session n'a pas été rouverte, le groupe n'est pas actif : on passe par sudo
compose() {
    if docker info >/dev/null 2>&1; then
        docker compose "$@"
    else
        sudo docker compose "$@"
    fi
}

# 3. Construction et démarrage de la stack
info "Construction et démarrage de l'application"
compose up --build -d

info "Installation terminée"
echo "Application disponible sur http://localhost:${DESCRIPTEUR_PORT:-8080}"
echo "Arrêter la stack : docker compose down"
if [ "$GROUPE_AJOUTE" -eq 1 ]; then
    echo "Fermez puis rouvrez votre session pour utiliser docker sans sudo."
fi
