package ht.ia2.alamilouati.ghali.llm;

import dev.langchain4j.service.TokenStream;

public interface Assistant {
    TokenStream chat(String prompt);
}
