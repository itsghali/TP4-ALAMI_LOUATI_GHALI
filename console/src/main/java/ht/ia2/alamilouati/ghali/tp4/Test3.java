package ht.ia2.alamilouati.ghali.tp4;

import dev.langchain4j.model.input.PromptTemplate;
import dev.langchain4j.rag.DefaultRetrievalAugmentor;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.query.router.QueryRouter;

import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Test 4 de l'énoncé : activer ou désactiver le RAG selon la question. */
public class Test3 {
    public static void main(String[] args) {
        ContentRetriever retriever = new CorpusRag().indexer(CorpusRag.supportRag());
        var modele = Gemini.modele(true);
        var prompt = PromptTemplate.from("""
                Est-ce que cette question porte sur l'intelligence artificielle ?
                Réponds uniquement par oui, non ou peut-être.
                Question : {{question}}
                """);

        QueryRouter routeur = query -> {
            String decision = modele.chat(prompt.apply(Map.of("question", query.text())).text())
                    .strip().toLowerCase(Locale.ROOT).replaceAll("[.!?]+$", "");
            return "non".equals(decision) ? List.of() : List.of(retriever);
        };

        var augmentor = DefaultRetrievalAugmentor.builder().queryRouter(routeur).build();
        ConsoleRag.conversation(ConsoleRag.assistant(modele, augmentor));
    }
}
