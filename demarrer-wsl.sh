#!/usr/bin/env bash
# Prépare et lance la stack Docker (application + Ollama) dans WSL2, avec accélération GPU NVIDIA.
#
# À lancer DANS la distro WSL (pas depuis PowerShell) :
#   wsl -d Ubuntu
#   bash demarrer-wsl.sh
#
# Le modèle de vision se choisit avec DESCRIPTEUR_OLLAMA_MODELE (llava par défaut), en variable
# ou dans un fichier .env à la racine du projet. Exemple :
#   DESCRIPTEUR_OLLAMA_MODELE=qwen2.5vl bash demarrer-wsl.sh
#
# Le script est idempotent : le relancer ne réinstalle/retélécharge rien d'inutile.
# Ne pas lancer avec sudo : il demande lui-même les droits quand c'est nécessaire.

set -euo pipefail

if [ "$(id -u)" -eq 0 ]; then
    echo "Ne lancez pas ce script avec sudo : il demande lui-même les droits si besoin." >&2
    exit 1
fi

info() { printf '\n==> %s\n' "$*"; }

# 1. Se placer sur une copie native WSL du projet (bien plus rapide que /mnt/... pour Docker)
RACINE_SCRIPT="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
case "$RACINE_SCRIPT" in
    /mnt/*)
        CIBLE="$HOME/projets/dev-pro-projet_Esposito_Griffon"
        info "Copie du projet vers $CIBLE (filesystem natif WSL)"
        mkdir -p "$HOME/projets"
        rsync -a --delete "$RACINE_SCRIPT"/ "$CIBLE"/ 2>/dev/null || cp -r "$RACINE_SCRIPT" "$CIBLE"
        cd "$CIBLE"
        ;;
    *)
        cd "$RACINE_SCRIPT"
        ;;
esac
info "Répertoire de travail : $(pwd)"

# 2. NVIDIA Container Toolkit (pour que Docker ait accès au GPU)
if dpkg -l nvidia-container-toolkit >/dev/null 2>&1; then
    echo "NVIDIA Container Toolkit déjà installé."
else
    info "Installation du NVIDIA Container Toolkit"
    curl -fsSL https://nvidia.github.io/libnvidia-container/gpgkey \
        | sudo gpg --dearmor -o /usr/share/keyrings/nvidia-container-toolkit-keyring.gpg
    curl -s -L https://nvidia.github.io/libnvidia-container/stable/deb/nvidia-container-toolkit.list \
        | sed 's#deb https://#deb [signed-by=/usr/share/keyrings/nvidia-container-toolkit-keyring.gpg] https://#g' \
        | sudo tee /etc/apt/sources.list.d/nvidia-container-toolkit.list >/dev/null
    sudo apt-get update
    sudo apt-get install -y nvidia-container-toolkit
fi

if docker info 2>/dev/null | grep -qi 'nvidia'; then
    echo "Runtime Docker NVIDIA déjà configuré."
else
    info "Configuration du runtime Docker pour NVIDIA"
    sudo nvidia-ctk runtime configure --runtime=docker
    sudo systemctl restart docker
fi

info "Vérification de l'accès GPU depuis un conteneur"
# --gpus all plante sur Docker 29 ("AMD CDI spec not found") : on utilise la syntaxe CDI explicite.
docker run --rm --device nvidia.com/gpu=all nvidia/cuda:12.6.0-base-ubuntu24.04 nvidia-smi \
    || { echo "Le GPU n'est pas accessible depuis Docker, voir le message ci-dessus." >&2; exit 1; }

# 3. Construction et démarrage de la stack (application + ollama)
info "Construction et démarrage de la stack"
docker compose up --build -d

# 4. Téléchargement du modèle dans le conteneur ollama (une seule fois)
# Même source que docker compose : variable d'environnement, sinon fichier .env, sinon llava.
if [ -z "${DESCRIPTEUR_OLLAMA_MODELE:-}" ] && [ -f .env ]; then
    DESCRIPTEUR_OLLAMA_MODELE="$(sed -n 's/^DESCRIPTEUR_OLLAMA_MODELE=//p' .env | tail -n 1 | tr -d "\r\"'")"
fi
MODELE="${DESCRIPTEUR_OLLAMA_MODELE:-llava}"

info "Vérification du modèle $MODELE"
if docker compose exec -T ollama ollama list 2>/dev/null | awk 'NR > 1 { print $1 }' \
        | grep -qxF -e "$MODELE" -e "$MODELE:latest"; then
    echo "Modèle $MODELE déjà présent."
else
    info "Téléchargement du modèle $MODELE (peut prendre plusieurs minutes)"
    docker compose exec -T ollama ollama pull "$MODELE"
fi

info "Terminé"
echo "Application disponible sur http://localhost:${DESCRIPTEUR_PORT:-8080} (modèle $MODELE)"
echo "Arrêter la stack : docker compose down"
