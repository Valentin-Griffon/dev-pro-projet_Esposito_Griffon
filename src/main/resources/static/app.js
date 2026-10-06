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

boutonArreter.addEventListener('click', () => {
    if ('speechSynthesis' in window) {
        speechSynthesis.cancel();
    }
});
