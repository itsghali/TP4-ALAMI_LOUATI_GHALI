package ht.ia2.alamilouati.ghali.tp4;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.listener.ChatModelListener;
import dev.langchain4j.model.chat.listener.ChatModelRequestContext;
import dev.langchain4j.model.chat.listener.ChatModelResponseContext;
import dev.langchain4j.model.google.genai.GoogleGenAiChatModel;

import java.util.List;
import java.util.logging.Logger;

public final class Gemini {
    private static final Logger LOG = Logger.getLogger(Gemini.class.getName());

    private Gemini() {
    }

    public static String cle(String variable) {
        String cle = System.getenv(variable);
        if (cle == null || cle.isBlank()) {
            throw new IllegalStateException("La variable " + variable + " est requise.");
        }
        return cle;
    }

    public static ChatModel modele(boolean traces) {
        var builder = GoogleGenAiChatModel.builder()
                .apiKey(cle("GEMINI_KEY"))
                .modelName(System.getProperty("tp4.gemini.model", "gemini-3.5-flash-lite"))
                .temperature(0.3);
        if (traces) {
            builder.listeners(List.of(new ChatModelListener() {
                @Override
                public void onRequest(ChatModelRequestContext contexte) {
                    LOG.info(() -> "Messages envoyés à Gemini : " + contexte.chatRequest().messages());
                }

                @Override
                public void onResponse(ChatModelResponseContext contexte) {
                    LOG.info(() -> "Réponse Gemini : " + contexte.chatResponse().aiMessage().text());
                }
            }));
        }
        return builder.build();
    }
}
