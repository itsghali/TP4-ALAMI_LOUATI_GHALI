package ht.ia2.alamilouati.ghali.tp4;

import dev.langchain4j.rag.DefaultRetrievalAugmentor;
import dev.langchain4j.rag.content.retriever.WebSearchContentRetriever;
import dev.langchain4j.rag.query.router.DefaultQueryRouter;
import dev.langchain4j.rag.query.router.LanguageModelQueryRouter;
import dev.langchain4j.rag.query.router.QueryRouter;
import dev.langchain4j.web.search.WebSearchEngine;
import dev.langchain4j.web.search.tavily.TavilyWebSearchEngine;

import java.util.List;
import java.util.Map;

/** Test 5 de l'énoncé : comparaison PDF + Web et routage sélectif. */
public class Test4 {
    public static void main(String[] args) {
        var modele = Gemini.modele(true);
        var pdf = new CorpusRag().indexer(CorpusRag.supportRag());
        WebSearchEngine moteur = TavilyWebSearchEngine.builder()
                .apiKey(Gemini.cle("TAVILY_API_KEY"))
                .build();
        var web = WebSearchContentRetriever.builder().webSearchEngine(moteur).build();

        // Version demandée avant l'exercice bonus : les deux sources sont toujours interrogées.
        QueryRouter routeur = new DefaultQueryRouter(pdf, web);
        if (List.of(args).contains("--routage")) {
            routeur = new LanguageModelQueryRouter(modele, Map.of(
                    pdf, "Support de cours : fine-tuning, RAG et LangChain4j. À utiliser pour les questions sur ce support.",
                    web, "Recherche Web : actualité, événements récents et faits externes au support de cours."));
        }

        var augmentor = DefaultRetrievalAugmentor.builder().queryRouter(routeur).build();
        ConsoleRag.conversation(ConsoleRag.assistant(modele, augmentor));
    }
}
