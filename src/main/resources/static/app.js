const formulaire = document.getElementById('formulaire');
const statut = document.getElementById('statut');
const resultat = document.getElementById('resultat');
const boutonRelire = document.getElementById('relire');
const boutonArreter = document.getElementById('arreter');
const boutonAnalyser = document.getElementById('analyser');

let dernierTexte = null;

function dire(texte) {
    if (!('speechSynthesis' in window)) {
        return;
    }
    speechSynthesis.cancel();
    const lecture = new SpeechSynthesisUtterance(texte);
    lecture.lang = 'fr-FR';
    const voixFrancaise = speechSynthesis.getVoices().find(v => v.lang.startsWith('fr'));
    if (voixFrancaise) {
        lecture.voice = voixFrancaise;
    }
    boutonArreter.disabled = false;
    lecture.onend = () => { boutonArreter.disabled = true; };
    speechSynthesis.speak(lecture);
}

function afficher(texte, message) {
    resultat.textContent = texte;
    statut.textContent = message;
    dire(message || texte);
}

formulaire.addEventListener('submit', async (evenement) => {
    evenement.preventDefault();
    const fichier = document.getElementById('image').files[0];
    if (!fichier) {
        afficher('', 'Choisissez d\'abord une image.');
        return;
    }

    const donnees = new FormData();
    donnees.append('image', fichier);
    const question = document.getElementById('question').value;
    if (question.trim()) {
        donnees.append('question', question);
    }

    boutonAnalyser.disabled = true;
    resultat.textContent = '';
    statut.textContent = 'Analyse en cours, cela peut prendre quelques dizaines de secondes…';

    try {
        const reponse = await fetch('/api/descriptions', { method: 'POST', body: donnees });
        const corps = await reponse.json();
        if (reponse.ok) {
            dernierTexte = corps.description;
            boutonRelire.disabled = false;
            afficher(corps.description, 'Description prête.');
        } else {
            dernierTexte = null;
            boutonRelire.disabled = true;
            afficher('', corps.message);
        }
    } catch (erreur) {
        afficher('', 'Impossible de joindre le serveur. Vérifiez que l\'application est lancée.');
    } finally {
        boutonAnalyser.disabled = false;
    }
});

boutonRelire.addEventListener('click', () => {
    if (dernierTexte) {
        dire(dernierTexte);
    }
});

boutonArreter.addEventListener('click', () => {
    speechSynthesis.cancel();
    boutonArreter.disabled = true;
});
