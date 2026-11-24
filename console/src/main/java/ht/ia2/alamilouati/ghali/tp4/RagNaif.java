package ht.ia2.alamilouati.ghali.tp4;

import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.service.AiServices;

import java.util.List;

/** Test 1, avec activation des traces via --logs. */
public class RagNaif {
    public static void main(String[] args) {
        List<String> options = List.of(args);
        var retriever = new CorpusRag().indexer(CorpusRag.supportRag());
        Assistant assistant = AiServices.builder(Assistant.class)
                .chatModel(Gemini.modele(options.contains("--logs")))
                .chatMemory(MessageWindowChatMemory.withMaxMessages(10))
                .contentRetriever(retriever)
                .build();

        if (options.contains("--question-initiale")) {
            System.out.println(assistant.chat("Quelle est la signification de RAG et à quoi ça sert ?"));
            return;
        }
        ConsoleRag.conversation(assistant);
    }
}
