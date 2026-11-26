/* Copier le contenu d'un textarea dans le clipboard */
async function copyToClipboard(idTextArea) {
    var textArea = document.getElementById("form:" + idTextArea);
    if(textArea) {
        try {
            await navigator.clipboard.writeText(textArea.value);
        } catch (err) {
            console.error("Erreur lors de la copie : ", err)
        }
    }
    // Cet ancien code est deprecated :
    // textArea.select();
    // document.execCommand('copy');
}

/* Effacer la dernière question et la dernière réponse */
function toutEffacer() {
    if (etatChat.occupe) {
        return;
    }
    document.getElementById("form:question").value = "";
    document.getElementById("form:reponse").value = "";
}

const etatChat = { connecte: false, occupe: false, requeteId: null };

function afficherStatut(texte) {
    const zone = document.getElementById("streamStatus");
    if (zone) zone.textContent = texte;
}

function mettreAJourBoutons() {
    const envoyer = document.getElementById("form:envoyerQuestion");
    const nouveau = document.getElementById("form:nouveauChat");
    if (envoyer) envoyer.disabled = !etatChat.connecte || etatChat.occupe;
    if (nouveau) nouveau.disabled = etatChat.occupe;
}

function socketOuvert() {
    etatChat.connecte = true;
    afficherStatut("Prêt.");
    mettreAJourBoutons();
}

function socketFerme(code) {
    etatChat.connecte = false;
    afficherStatut(code === 1000 ? "Session ou vue expirée. Rechargez la page." : "Connexion interrompue. Rechargez la page.");
    mettreAJourBoutons();
}

function peutEnvoyerQuestion() {
    return etatChat.connecte && !etatChat.occupe;
}

function requeteAjax(data) {
    if (data.status === "begin") {
        etatChat.occupe = true;
        etatChat.requeteId = null;
        document.getElementById("form:reponse").value = "";
        afficherStatut("Réponse en cours…");
        mettreAJourBoutons();
    } else if (data.status === "success") {
        actualisationTerminee(data);
    }
}

function actualisationTerminee(data) {
    if (data.status !== "success") return;
    const etat = document.getElementById("form:etatGeneration");
    etatChat.occupe = etat && etat.textContent.trim() === "true";
    if (!etatChat.occupe) {
        const erreur = document.getElementById("form:erreurStreaming");
        afficherStatut(erreur && erreur.textContent ? erreur.textContent : "Prêt.");
    }
    mettreAJourBoutons();
}

function erreurAjax() {
    afficherStatut("Mise à jour impossible. Rechargez la page avant de réessayer.");
}

function socketListener(message) {
    if (!message || !message.type || !message.requestId) return;
    if (message.type === "start") {
        etatChat.requeteId = message.requestId;
        etatChat.occupe = true;
        document.getElementById("form:reponse").value = "";
    } else {
        if (message.requestId !== etatChat.requeteId) return;
        const reponse = document.getElementById("form:reponse");
        if (message.type === "partial") {
            reponse.value += message.text;
            reponse.scrollTop = reponse.scrollHeight;
        } else if (message.type === "complete" || message.type === "error") {
            etatChat.occupe = false;
            if (message.type === "complete") reponse.value = message.text;
            afficherStatut(message.type === "error" ? message.text : "Réponse terminée.");
            actualiserConversation();
        }
    }
    mettreAJourBoutons();
}

document.addEventListener("DOMContentLoaded", mettreAJourBoutons);
