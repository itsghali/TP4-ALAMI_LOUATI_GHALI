package ht.ia2.alamilouati.ghali.llm;

import dev.langchain4j.rag.DefaultRetrievalAugmentor;
import dev.langchain4j.rag.RetrievalAugmentor;
import dev.langchain4j.rag.query.router.LanguageModelQueryRouter;
import dev.langchain4j.rag.query.transformer.CompressingQueryTransformer;
import ht.ia2.alamilouati.ghali.tp4.CorpusRag;
import ht.ia2.alamilouati.ghali.tp4.Gemini;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.Map;

/** Index partagé. Chaque conversation conserve sa propre mémoire. */
@ApplicationScoped
public class RagWeb {
    private RetrievalAugmentor augmentor;

    public synchronized RetrievalAugmentor augmentor() {
        if (augmentor == null) {
            var corpus = new CorpusRag();
            var rag = corpus.indexer(CorpusRag.supportRag());
            var second = corpus.indexer(CorpusRag.secondPdf());
            var modele = Gemini.modele(false);
            var routeur = new LanguageModelQueryRouter(modele, Map.of(
                    rag, "Support de cours sur le fine-tuning, le RAG et LangChain4j.",
                    second, CorpusRag.secondDescription()));

            augmentor = DefaultRetrievalAugmentor.builder()
                    .queryTransformer(new CompressingQueryTransformer(modele))
                    .queryRouter(routeur)
                    .build();
        }
        return augmentor;
    }
}
