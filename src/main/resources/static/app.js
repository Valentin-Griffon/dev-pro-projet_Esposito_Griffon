const formulaire = document.getElementById('formulaire');
const statut = document.getElementById('statut');
const resultat = document.getElementById('resultat');
const transcript = document.getElementById('transcript');
const boutonAnalyser = document.getElementById('analyser');
const boutonRelire = document.getElementById('relire');
const boutonArreter = document.getElementById('arreter');
const formulaireSuite = document.getElementById('formulaireSuite');
const champQuestionSuite = document.getElementById('questionSuite');
const boutonEnvoyerSuite = document.getElementById('envoyerSuite');

const MESSAGE_INATTENDU = 'Une erreur inattendue s\'est produite. Réessayez dans un instant.';

let dernierTexte = null;
let analyseEnCours = false;
let fichierCourant = null;
// Historique de la conversation sur l'image en cours : la page le fait grossir elle-même et le
// renvoie à chaque question, car le serveur ne garde aucun état entre les requêtes.
let historique = [];

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

function ajouterAuTranscript(question, reponse) {
    const bloc = document.createElement('p');
    bloc.textContent = question ? `Question : ${question}\nRéponse : ${reponse}` : `Description : ${reponse}`;
    transcript.appendChild(bloc);
}

function activerConversation() {
    champQuestionSuite.disabled = false;
    boutonEnvoyerSuite.disabled = false;
}

function desactiverConversation() {
    champQuestionSuite.disabled = true;
    boutonEnvoyerSuite.disabled = true;
}

function nouvelleConversation() {
    historique = [];
    transcript.textContent = '';
    dernierTexte = null;
    boutonRelire.disabled = true;
    resultat.textContent = '';
    desactiverConversation();
}

function afficherReponse(question, texte) {
    dernierTexte = texte;
    boutonRelire.disabled = false;
    resultat.textContent = texte;
    statut.textContent = question ? 'Réponse prête.' : 'Description prête.';
    historique.push({ question, reponse: texte });
    ajouterAuTranscript(question, texte);
    activerConversation();
    lire(texte);
}

function afficherErreur(message) {
    statut.textContent = message;
    lire(message);
}

function donneesHistorique(donnees) {
    if (historique.length > 0) {
        donnees.append('historique', JSON.stringify(historique));
    }
}

async function envoyer(donnees, question) {
    analyseEnCours = true;
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
            afficherReponse(question, corps.description);
        } else {
            afficherErreur((corps && corps.message) || MESSAGE_INATTENDU);
        }
    } catch (erreur) {
        afficherErreur('Impossible de joindre le serveur. Vérifiez que l\'application est lancée.');
    } finally {
        analyseEnCours = false;
    }
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

    fichierCourant = fichier;
    nouvelleConversation();

    const donnees = new FormData();
    donnees.append('image', fichier);
    const question = document.getElementById('question').value.trim() || null;
    if (question) {
        donnees.append('question', question);
    }

    await envoyer(donnees, question);
});

formulaireSuite.addEventListener('submit', async (evenement) => {
    evenement.preventDefault();
    if (analyseEnCours || !fichierCourant) {
        return;
    }

    const question = champQuestionSuite.value.trim();
    if (!question) {
        return;
    }

    const donnees = new FormData();
    donnees.append('image', fichierCourant);
    donnees.append('question', question);
    donneesHistorique(donnees);

    await envoyer(donnees, question);
    champQuestionSuite.value = '';
    champQuestionSuite.focus();
});

boutonRelire.addEventListener('click', () => {
    lire(dernierTexte);
});

boutonArreter.addEventListener('click', () => {
    if ('speechSynthesis' in window) {
        speechSynthesis.cancel();
    }
});
