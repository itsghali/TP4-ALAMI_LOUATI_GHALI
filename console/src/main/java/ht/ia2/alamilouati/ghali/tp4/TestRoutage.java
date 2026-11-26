package ht.ia2.alamilouati.ghali.tp4;

import dev.langchain4j.rag.DefaultRetrievalAugmentor;
import dev.langchain4j.rag.query.router.LanguageModelQueryRouter;

import java.util.Map;

/** Test 3, avec deux sources et deux magasins. */
public class TestRoutage {
    public static void main(String[] args) {
        var corpus = new CorpusRag();
        var rag = corpus.indexer(CorpusRag.supportRag());
        var second = corpus.indexer(CorpusRag.secondPdf());
        var modele = Gemini.modele(true);
        var routeur = new LanguageModelQueryRouter(modele, Map.of(
                rag, "Support du cours sur le fine-tuning, le RAG et LangChain4j.",
                second, CorpusRag.secondDescription()));
        var augmentor = DefaultRetrievalAugmentor.builder().queryRouter(routeur).build();
        ConsoleRag.conversation(ConsoleRag.assistant(modele, augmentor));
    }
}
