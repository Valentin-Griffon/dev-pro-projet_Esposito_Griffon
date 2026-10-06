#!/usr/bin/env bash
# Installe tout le nécessaire au projet : Java 17, Git, Ollama et le modèle de vision,
# puis compile le projet avec le wrapper Gradle.
#
# Usage : ./install.sh [--sans-ollama] [--sans-modele]
#   --sans-ollama   n'installe pas Ollama (utiliser le profil "faux")
#   --sans-modele   installe Ollama mais ne télécharge pas le modèle (environ 4,7 Go)
#
# Variable d'environnement : MODELE (par défaut : llava)

set -euo pipefail

# Lancé avec sudo, le JDK, Ollama et Gradle seraient installés pour root
# (dans /root et avec des fichiers appartenant à root dans le projet)
if [ "$(id -u)" -eq 0 ]; then
    echo "Ne lancez pas ce script avec sudo : il demande lui-même les droits si besoin." >&2
    exit 1
fi

MODELE="${MODELE:-llava}"
AVEC_OLLAMA=1
AVEC_MODELE=1
for argument in "$@"; do
    case "$argument" in
        --sans-ollama) AVEC_OLLAMA=0; AVEC_MODELE=0 ;;
        --sans-modele) AVEC_MODELE=0 ;;
        *) echo "Option inconnue : $argument" >&2; exit 1 ;;
    esac
done

RACINE="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
DOSSIER_LOCAL="$HOME/.local/opt"

info()    { printf '\n==> %s\n' "$*"; }
present() { command -v "$1" >/dev/null 2>&1; }
apt_disponible() { present apt-get && present sudo; }

# Renvoie la version majeure de Java installée, ou 0 si absent
version_java() {
    if present java; then
        java -version 2>&1 | sed -n 's/.*version "\([0-9]*\).*/\1/p' | head -1
    else
        echo 0
    fi
}

installer_java_17() {
    info "Installation de Java 17"
    # Index apt périmé : les paquets peuvent renvoyer 404 sans cette mise à jour
    if apt_disponible && sudo apt-get update && sudo apt-get install -y openjdk-17-jdk-headless; then
        return
    fi

    # Repli sans droits administrateur : JDK Temurin téléchargé dans ~/.local/opt
    echo "Installation par apt impossible, téléchargement du JDK 17 dans $DOSSIER_LOCAL"
    mkdir -p "$DOSSIER_LOCAL"
    curl -fsSL -o "$DOSSIER_LOCAL/jdk17.tar.gz" \
        "https://api.adoptium.net/v3/binary/latest/17/ga/linux/x64/jdk/hotspot/normal/eclipse"
    tar xzf "$DOSSIER_LOCAL/jdk17.tar.gz" -C "$DOSSIER_LOCAL"
    rm "$DOSSIER_LOCAL/jdk17.tar.gz"
}

trouver_java_home() {
    if [ -n "${JAVA_HOME:-}" ] && [ -x "$JAVA_HOME/bin/java" ]; then
        echo "$JAVA_HOME"
        return
    fi
    local candidat
    candidat=$(ls -d "$DOSSIER_LOCAL"/jdk-17* 2>/dev/null | head -1 || true)
    [ -n "$candidat" ] && echo "$candidat"
}

# 1. Java 17
info "Vérification de Java"
if [ "$(version_java)" -ge 17 ] 2>/dev/null; then
    echo "Java $(version_java) déjà installé."
elif JAVA_HOME_TROUVE=$(trouver_java_home) && [ -n "$JAVA_HOME_TROUVE" ] && [ -x "$JAVA_HOME_TROUVE/bin/java" ]; then
    echo "JDK 17 trouvé dans $JAVA_HOME_TROUVE."
else
    installer_java_17
fi

JAVA_HOME="$(trouver_java_home)"
if [ -z "$JAVA_HOME" ]; then
    # apt a installé le JDK dans le système : JAVA_HOME est déduit de java
    JAVA_HOME="$(dirname "$(dirname "$(readlink -f "$(command -v java)")")")"
fi
export JAVA_HOME
export PATH="$JAVA_HOME/bin:$PATH"
echo "JAVA_HOME=$JAVA_HOME"
java -version

# 2. Git
info "Vérification de Git"
if present git; then
    echo "Git déjà installé."
elif apt_disponible; then
    sudo apt-get install -y git
else
    echo "Git est absent et ne peut pas être installé sans apt/sudo. Installez-le manuellement." >&2
    exit 1
fi

# 3. Ollama et le modèle de vision
if [ "$AVEC_OLLAMA" -eq 1 ]; then
    info "Vérification d'Ollama"
    if present ollama; then
        echo "Ollama déjà installé."
    else
        curl -fsSL https://ollama.com/install.sh | sh
    fi

    if [ "$AVEC_MODELE" -eq 1 ]; then
        info "Téléchargement du modèle $MODELE"
        if ! curl -s -m 3 http://localhost:11434 >/dev/null; then
            echo "Démarrage du serveur Ollama en arrière-plan"
            nohup ollama serve >"$RACINE/ollama.log" 2>&1 &
            for _ in $(seq 1 30); do
                curl -s -m 2 http://localhost:11434 >/dev/null && break
                sleep 1
            done
        fi
        ollama pull "$MODELE"
    fi
fi

# 4. Compilation et tests du projet
info "Compilation du projet avec le wrapper Gradle"
cd "$RACINE"
chmod +x gradlew
./gradlew build

info "Installation terminée"
cat <<EOF
Pour lancer l'application :
  ./gradlew bootRun                                          (avec Ollama et le modèle $MODELE)
  ./gradlew bootRun --args='--spring.profiles.active=faux'   (sans modèle)
Puis ouvrir http://localhost:8080

Pour utiliser Java 17 dans un nouveau terminal, ajoutez à ~/.bashrc :
  export JAVA_HOME=$JAVA_HOME
  export PATH="\$JAVA_HOME/bin:\$PATH"
EOF
