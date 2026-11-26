package ht.ia2.alamilouati.ghali.tp4;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.loader.ClassPathDocumentLoader;
import dev.langchain4j.data.document.parser.apache.tika.ApacheTikaDocumentParser;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.embedding.onnx.bgesmallenv15q.BgeSmallEnV15QuantizedEmbeddingModel;
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;

import java.util.List;

public final class CorpusRag {
    private static final String PDF_GESTION_EAU = "gestion-eau.pdf";
    private final EmbeddingModel embeddingModel = new BgeSmallEnV15QuantizedEmbeddingModel();

    public static String supportRag() {
        return System.getProperty("tp4.rag.pdf", "rag.pdf");
    }

    public static String secondPdf() {
        return System.getProperty("tp4.second.pdf", PDF_GESTION_EAU);
    }

    public static String secondDescription() {
        String description = System.getProperty("tp4.second.description", "").trim();
        if (description.isEmpty()) {
            if (PDF_GESTION_EAU.equals(secondPdf())) {
                return "Gestion de l'eau et irrigation goutte-à-goutte.";
            }
            throw new IllegalStateException("Indiquez le sujet du deuxième PDF avec -Dtp4.second.description.");
        }
        return description;
    }

    public EmbeddingStoreContentRetriever indexer(String fichier) {
        String ressource = fichier;
        ClassLoader chargeur = CorpusRag.class.getClassLoader();
        if (chargeur.getResource(ressource) == null) {
            throw new IllegalStateException("PDF absent du classpath : " + ressource);
        }
        Document document = ClassPathDocumentLoader.loadDocument(
                ressource, new ApacheTikaDocumentParser(), chargeur);
        List<TextSegment> segments = DocumentSplitters.recursive(900, 90).split(document);
        var magasin = new InMemoryEmbeddingStore<TextSegment>();
        magasin.addAll(embeddingModel.embedAll(segments).content(), segments);

        return EmbeddingStoreContentRetriever.builder()
                .embeddingStore(magasin)
                .embeddingModel(embeddingModel)
                .maxResults(2)
                .minScore(0.5)
                .build();
    }
}
