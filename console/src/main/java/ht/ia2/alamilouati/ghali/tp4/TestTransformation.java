package ht.ia2.alamilouati.ghali.tp4;

import dev.langchain4j.rag.DefaultRetrievalAugmentor;
import dev.langchain4j.rag.query.router.DefaultQueryRouter;
import dev.langchain4j.rag.query.transformer.CompressingQueryTransformer;

/** Test 2, avec deux conversations indépendantes. */
public class TestTransformation {
    public static void main(String[] args) {
        var retriever = new CorpusRag().indexer(CorpusRag.supportRag());
        var modele = Gemini.modele(true);
        var routeur = new DefaultQueryRouter(retriever);

        var avant = DefaultRetrievalAugmentor.builder().queryRouter(routeur).build();
        var apres = DefaultRetrievalAugmentor.builder()
                .queryRouter(routeur)
                .queryTransformer(new CompressingQueryTransformer(modele))
                .build();

        comparer("Sans transformation", ConsoleRag.assistant(modele, avant));
        comparer("Avec transformation", ConsoleRag.assistant(modele, apres));
    }

    private static void comparer(String titre, Assistant assistant) {
        System.out.println("=== " + titre + " ===");
        System.out.println(assistant.chat("Qu'est-ce que le fine-tuning ?"));
        System.out.println(assistant.chat("Dans le support de cours fourni, à quoi c'est comparé ?"));
    }
}
