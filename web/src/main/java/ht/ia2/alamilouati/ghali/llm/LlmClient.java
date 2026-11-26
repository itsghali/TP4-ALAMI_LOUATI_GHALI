package ht.ia2.alamilouati.ghali.llm;

import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.ChatMessageDeserializer;
import dev.langchain4j.data.message.ChatMessageSerializer;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.google.genai.GoogleGenAiStreamingChatModel;
import dev.langchain4j.model.output.TokenUsage;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.TokenStream;
import ht.ia2.alamilouati.ghali.tp4.Gemini;
import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serial;
import java.io.Serializable;
import java.util.function.Consumer;

/** Client Gemini propre à la vue JSF dans laquelle il est injecté. */
@Dependent
public class LlmClient implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private String systemRole;
    private transient Assistant assistant;
    private transient ChatMemory chatMemory;

    @Inject
    private CompteurTokens compteurTokens;

    @Inject
    private RagWeb ragWeb;

    public LlmClient() {
        this.chatMemory = MessageWindowChatMemory.withMaxMessages(20);
    }

    private void initialiserAssistant() {
        StreamingChatModel model = GoogleGenAiStreamingChatModel.builder()
                .apiKey(Gemini.cle("GEMINI_KEY"))
                .modelName(System.getProperty("tp4.gemini.model", "gemini-3.5-flash-lite"))
                .build();

        this.assistant = AiServices.builder(Assistant.class)
                .streamingChatModel(model)
                .chatMemory(chatMemory)
                .retrievalAugmentor(ragWeb.augmentor())
                .build();
    }

    public void setSystemRole(String systemRole) {
        this.systemRole = systemRole;
        this.chatMemory.clear();
        this.chatMemory.add(SystemMessage.from(this.systemRole));
    }

    public TokenStream envoyerMessage(String question) {
        if (this.assistant == null) {
            initialiserAssistant();
        }
        return this.assistant.chat(question);
    }

    public Consumer<TokenUsage> enregistreurTokens() {
        return this.compteurTokens.enregistreur();
    }

    // La vue peut être sérialisée ; les objets techniques LangChain4j sont reconstruits.
    @Serial
    private void writeObject(ObjectOutputStream output) throws IOException {
        output.defaultWriteObject();
        output.writeObject(ChatMessageSerializer.messagesToJson(this.chatMemory.messages()));
    }

    @Serial
    private void readObject(ObjectInputStream input) throws IOException, ClassNotFoundException {
        input.defaultReadObject();
        String messages = (String) input.readObject();
        this.chatMemory = MessageWindowChatMemory.withMaxMessages(20);
        ChatMessageDeserializer.messagesFromJson(messages).forEach(this.chatMemory::add);
    }
}
