package ht.ia2.alamilouati.ghali.jsf;


import ht.ia2.alamilouati.ghali.llm.LlmClient;
import ht.ia2.alamilouati.ghali.llm.CompteurTokens;
import dev.langchain4j.model.output.TokenUsage;
import dev.langchain4j.service.TokenStream;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.faces.model.SelectItem;
import jakarta.faces.push.Push;
import jakarta.faces.push.PushContext;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Backing bean pour la page JSF index.xhtml.
 * Portée view pour conserver l'état de la conversation qui dure pendant plusieurs requêtes HTTP.
 * La portée view nécessite l'implémentation de Serializable (le backing bean peut être mis en mémoire secondaire).
 */
@Named
@ViewScoped
public class Bb implements Serializable {
    private static final Logger LOGGER = Logger.getLogger(Bb.class.getName());

    /**
     * Rôle "système" attribué au LLM.
     * Possible d'écrire un nouveau rôle dans la liste déroulante.
     */
    private String roleSysteme;

    /**
     * Quand le rôle est choisi par l'utilisateur dans la liste déroulante,
     * il n'est plus possible de le modifier (voir code de la page JSF), sauf si on veut un nouveau chat.
     */
    private boolean roleSystemeChangeable = true;

    /**
     * Liste de tous les rôles de l'API prédéfinis.
     */
    private List<SelectItem> listeRolesSysteme;

    /**
     * Dernière question posée par l'utilisateur.
     */
    private String question;
    /**
     * Dernière réponse du LLM.
     */
    private volatile String reponse;
    private volatile boolean generationEnCours;
    private volatile String erreurStreaming;
    /**
     * La conversation depuis le début.
     */
    private StringBuilder conversation = new StringBuilder();

    /**
     * Client pour envoyer la question au LLM et recevoir sa réponse.
     */
    @Inject
    private LlmClient llmClient;

    @Inject
    private CompteurTokens compteurTokens;

    @Inject
    @Push(channel = "chat")
    private PushContext webSocketPourChat;

    public long getNombreTokens() {
        return this.compteurTokens.getTotal();
    }

    public boolean isGenerationEnCours() {
        return this.generationEnCours;
    }

    public String getErreurStreaming() {
        return this.erreurStreaming;
    }

    /**
     * Contexte JSF. Utilisé pour qu'un message d'erreur s'affiche dans le formulaire.
     */
    @Inject
    private FacesContext facesContext;

    /**
     * Obligatoire pour un bean CDI (classe gérée par CDI), s'il y a un autre constructeur.
     */
    public Bb() {
    }

    public String getRoleSysteme() {
        return this.roleSysteme;
    }

    public void setRoleSysteme(String roleSysteme) {
        this.roleSysteme = roleSysteme;
    }

    public boolean isRoleSystemeChangeable() {
        return this.roleSystemeChangeable;
    }

    public String getQuestion() {
        return this.question;
    }

    public void setQuestion(String question) {
        this.question = question;
    }

    public String getReponse() {
        return this.reponse;
    }

    /**
     * setter indispensable pour le textarea.
     *
     * @param reponse la réponse à la question.
     */
    public void setReponse(String reponse) {
        this.reponse = reponse;
    }

    public synchronized String getConversation() {
        return conversation.toString();
    }

    public synchronized void setConversation(String conversation) {
        this.conversation = new StringBuilder(conversation);
    }

    /**
     * Envoie la question au serveur.
     * La question est transmise au LLM par LlmClient.
     *
     * @return null pour rester sur la même page.
     */
    public synchronized String envoyer() {
        if (this.generationEnCours) {
            return null;
        }
        if (this.question == null || this.question.isBlank()) {
            // Erreur ! Le formulaire va être réaffiché en réponse à la requête POST, avec un message d'erreur.
            FacesMessage message = new FacesMessage(FacesMessage.SEVERITY_ERROR,
                    "Texte question vide", "Il manque le texte de la question");
            facesContext.addMessage(null, message);
            return null;
        }

        // Traite la question pour construire la réponse.
        if (this.conversation.isEmpty()) { // Si la conversation n'a pas encore commencé
            // Fige la liste déroulante qui permet de choisir le rôle système
            this.llmClient.setSystemRole(this.roleSysteme);
            this.roleSystemeChangeable = false;
        }
        final String questionEnvoyee = this.question;
        final String requeteId = UUID.randomUUID().toString();
        final Consumer<TokenUsage> compterTokens = this.llmClient.enregistreurTokens();
        final AtomicBoolean termine = new AtomicBoolean();
        this.reponse = "";
        this.erreurStreaming = null;
        this.generationEnCours = true;
        pousser("start", "", requeteId);

        try {
            TokenStream tokenStream = this.llmClient.envoyerMessage(questionEnvoyee);
            tokenStream
                    .onPartialResponse(tokens -> {
                        if (!termine.get()) {
                            pousser("partial", tokens, requeteId);
                        }
                    })
                    .onCompleteResponse(response -> {
                        if (!termine.compareAndSet(false, true)) {
                            return;
                        }
                        compterTokens.accept(response.tokenUsage());
                        String texte = response.aiMessage().text();
                        synchronized (this) {
                            this.reponse = texte;
                            afficherConversation(questionEnvoyee, texte);
                            this.generationEnCours = false;
                        }
                        pousser("complete", texte, requeteId);
                    })
                    .onError(erreur -> terminerAvecErreur(erreur, requeteId, termine))
                    .start();
        } catch (RuntimeException erreur) {
            terminerAvecErreur(erreur, requeteId, termine);
        }
        return null;
    }

    private void terminerAvecErreur(Throwable erreur, String requeteId, AtomicBoolean termine) {
        if (!termine.compareAndSet(false, true)) {
            return;
        }
        this.erreurStreaming = "La réponse n'a pas pu être reçue. Réessayez ou démarrez un nouveau chat.";
        this.generationEnCours = false;
        LOGGER.log(Level.WARNING, "Échec de la génération en streaming", erreur);
        pousser("error", this.erreurStreaming, requeteId);
    }

    private void pousser(String type, String texte, String requeteId) {
        this.webSocketPourChat.send(Map.of("type", type, "text", texte, "requestId", requeteId));
    }

    /**
     * Pour un nouveau chat.
     * Termine la portée view en retournant "index" (la page index.xhtml sera affichée après le traitement
     * effectué pour construire la réponse) et pas null. null aurait indiqué de rester dans la même page (index.xhtml)
     * sans changer de vue.
     * Le fait de changer de vue va faire supprimer l'instance en cours du backing bean par CDI et donc on reprend
     * tout comme au début puisqu'une nouvelle instance du backing va être utilisée par la page index.xhtml.
     * @return "index"
     */
    public String nouveauChat() {
        return this.generationEnCours ? null : "index";
    }

    /**
     * Pour afficher la conversation dans le textArea de la page JSF.
     */
    private void afficherConversation(String question, String reponse) {
        this.conversation.append("== User:\n").append(question).append("\n== Serveur:\n").append(reponse).append("\n");
    }

    public List<SelectItem> getRolesSysteme() {
        if (this.listeRolesSysteme == null) {
            // Génère les rôles de l'API prédéfinis
            this.listeRolesSysteme = new ArrayList<>();
            // Vous pouvez évidemment écrire ces rôles dans la langue que vous voulez.
            String role = """
                    You are a helpful assistant. You help the user to find the information they need.
                    If the user type a question, you answer it.
                    """;
            // 1er argument : la valeur du rôle (envoyé au serveur)
            // 2ème argument : le libellé du rôle dans la liste déroulante.
            this.listeRolesSysteme.add(new SelectItem(role, "Assistant"));

            role = """
                    You are an interpreter. You translate from English to French and from French to English.
                    If the user type a French text, you translate it into English.
                    If the user type an English text, you translate it into French.
                    If the text contains only one to three words, give some examples of usage of these words in English.
                    """;
            this.listeRolesSysteme.add(new SelectItem(role, "Traducteur Anglais-Français"));

            role = """
                    Your are a travel guide. If the user type the name of a country or of a town,
                    you tell them what are the main places to visit in the country or the town
                    are you tell them the average price of a meal.
                    """;
            this.listeRolesSysteme.add(new SelectItem(role, "Guide touristique"));
        }

        return this.listeRolesSysteme;
    }

}
