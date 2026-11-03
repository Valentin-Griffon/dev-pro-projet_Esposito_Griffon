const formulaire = document.getElementById('formulaire');
const statut = document.getElementById('statut');
const resultat = document.getElementById('resultat');
const boutonAnalyser = document.getElementById('analyser');
const boutonRelire = document.getElementById('relire');
const boutonArreter = document.getElementById('arreter');

const MESSAGE_INATTENDU = 'Une erreur inattendue s\'est produite. Réessayez dans un instant.';

let dernierTexte = null;
let analyseEnCours = false;

/** Lit le texte à voix haute. Sans synthèse vocale, la page reste utilisable par son texte. */
function lire(texte) {
    if (!('speechSynthesis' in window) || !texte) {
        return;
    }
    speechSynthesis.cancel();
    const lecture = new SpeechSynthesisUtterance(texte);
    lecture.lang = 'fr-FR';
    const voixFrancaise = speechSynthesis.getVoices().find(voix => voix.lang.startsWith('fr'));
    if (voixFrancaise) {
        lecture.voice = voixFrancaise;
    }
    speechSynthesis.speak(lecture);
}

function afficherDescription(texte) {
    dernierTexte = texte;
    boutonRelire.disabled = false;
    resultat.textContent = texte;
    statut.textContent = 'Description prête.';
    lire(texte);
}

function afficherErreur(message) {
    dernierTexte = null;
    boutonRelire.disabled = true;
    resultat.textContent = '';
    statut.textContent = message;
    lire(message);
}

formulaire.addEventListener('submit', async (evenement) => {
    evenement.preventDefault();
    if (analyseEnCours) {
        return;
    }

    const fichier = document.getElementById('image').files[0];
    if (!fichier) {
        afficherErreur('Choisissez d\'abord une image.');
        return;
    }

    const donnees = new FormData();
    donnees.append('image', fichier);
    const question = document.getElementById('question').value.trim();
    if (question) {
        donnees.append('question', question);
    }

    analyseEnCours = true;
    resultat.textContent = '';
    const attente = 'Analyse en cours, cela peut prendre quelques dizaines de secondes.';
    statut.textContent = attente + '…';
    lire(attente);

    try {
        const reponse = await fetch('/api/descriptions', { method: 'POST', body: donnees });
        let corps = null;
        try {
            corps = await reponse.json();
        } catch (erreurLecture) {
            corps = null;
        }

        if (reponse.ok && corps && corps.description) {
            afficherDescription(corps.description);
        } else {
            afficherErreur((corps && corps.message) || MESSAGE_INATTENDU);
        }
    } catch (erreur) {
        afficherErreur('Impossible de joindre le serveur. Vérifiez que l\'application est lancée.');
    } finally {
        analyseEnCours = false;
    }
});

boutonRelire.addEventListener('click', () => {
    lire(dernierTexte);
});

function arreterLecture() {
    if ('speechSynthesis' in window) {
        speechSynthesis.cancel();
    }
}

boutonArreter.addEventListener('click', arreterLecture);

// Échap coupe la voix depuis n'importe où dans la page, sans chercher le bouton
document.addEventListener('keydown', (evenement) => {
    if (evenement.key === 'Escape') {
        arreterLecture();
    }
});

// Confirme le fichier choisi : une personne qui ne voit pas l'écran ne peut pas le vérifier autrement
document.getElementById('image').addEventListener('change', (evenement) => {
    const fichier = evenement.target.files[0];
    if (fichier) {
        const annonce = 'Image choisie : ' + fichier.name + '.';
        statut.textContent = annonce;
        lire(annonce);
        resultat.textContent = '';
        dernierTexte = null;
        boutonRelire.disabled = true;
    }
});

// Dit à voix haute où l'on se trouve : chaque contrôle annonce son rôle quand il reçoit le focus
document.addEventListener('focusin', (evenement) => {
    const phrase = evenement.target.dataset && evenement.target.dataset.voix;
    if (phrase) {
        lire(phrase);
    }
});
