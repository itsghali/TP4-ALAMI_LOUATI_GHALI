package ht.ia2.alamilouati.ghali.tp4;

import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.rag.RetrievalAugmentor;
import dev.langchain4j.service.AiServices;

import java.util.Scanner;

final class ConsoleRag {
    private ConsoleRag() {
    }

    static Assistant assistant(ChatModel modele, RetrievalAugmentor augmentor) {
        return AiServices.builder(Assistant.class)
                .chatModel(modele)
                .chatMemory(MessageWindowChatMemory.withMaxMessages(10))
                .retrievalAugmentor(augmentor)
                .build();
    }

    static void conversation(Assistant assistant) {
        try (Scanner scanner = new Scanner(System.in)) {
            System.out.println("Posez une question. Saisissez « fin » pour quitter.");
            while (scanner.hasNextLine()) {
                String question = scanner.nextLine().strip();
                if ("fin".equalsIgnoreCase(question)) {
                    return;
                }
                if (!question.isEmpty()) {
                    System.out.println("Assistant : " + assistant.chat(question));
                }
            }
        }
    }
}
