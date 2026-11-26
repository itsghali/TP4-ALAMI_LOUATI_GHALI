# TP4 RAG — ALAMI LOUATI Ghali

Le dossier contient un projet Maven Java pour les tests console et un projet Jakarta EE 11 pour l’application Web. Le Web reprend l’interface JSF et la génération en streaming du TP2.

## Préparer le projet

1. Utiliser le JDK 25 et Maven. Pour le Web, utiliser Payara 7.
2. Placer le [support officiel rag.pdf](http://richard.grin.free.fr/emsi/casablanca-ia/supports/rag.pdf) dans `console/src/main/resources/rag.pdf`. `TestRoutage` et le Web utilisent `gestion-eau.pdf` comme deuxième document. Pour essayer un autre PDF placé dans le même dossier, indiquer son nom et son sujet dans les options JVM avec `-Dtp4.second.pdf=nom-du-document.pdf` et `"-Dtp4.second.description=Description du sujet"`.
3. Configurer la variable d’environnement `GEMINI_KEY` pour la console et le processus Payara. Pour le test 5, configurer aussi `TAVILY_API_KEY` pour la console.
4. Lancer `mvn package` à la racine. Le profil Maven `sans-tavily` permet de compiler la console et le Web depuis un cache Maven dépourvu de l’intégration Tavily. Il exclut uniquement la classe du test 5.

Le modèle Gemini vient du TP2 Web. Pour le remplacer, utiliser l’option JVM `-Dtp4.gemini.model=NOM_DU_MODELE`. Le programme ne consigne aucun en-tête d’authentification. Les traces console contiennent les questions, les extraits ajoutés au prompt et les réponses.

## Tests console

Exécuter la méthode `main` de chaque classe du package `ht.ia2.alamilouati.ghali.tp4` dans IntelliJ. En PowerShell, après la compilation, on peut aussi utiliser Maven pour construire le classpath, puis lancer Java.

```powershell
mvn -pl console dependency:build-classpath "-Dmdep.outputFile=target/classpath.txt"
$cp = "console/target/classes;" + (Get-Content console/target/classpath.txt -Raw).Trim()
java -cp $cp ht.ia2.alamilouati.ghali.tp4.RagNaif --logs
```

| Énoncé | Classe | Vérification |
| --- | --- | --- |
| 1. RAG explicite | `RagNaif` | L’option `--question-initiale` pose la question de définition. En mode interactif, poser plusieurs questions. Vérifier avec `--logs` les extraits ajoutés au prompt. |
| 2. Transformation | `TestTransformation` | Comparer les deux réponses à la question de suivi et les messages finaux envoyés au modèle. Chaque variante démarre avec sa propre mémoire. |
| 3. Routage | `TestRoutage` | Demander à quoi sert le RAG, puis poser une question sur le sujet du deuxième PDF. Vérifier dans les traces que le modèle choisit la source attendue pour chaque question. |
| 4. Désactivation du RAG | `Test3` | Saisir « Bonjour », puis « À quoi sert le fine-tuning ? ». Vérifier que la première demande reçoit une décision négative et que le prompt final ne contient pas d’extrait du PDF. |
| 5. Récupération Web | `Test4` | Lancer un premier essai avec les deux sources actives. Pour le bonus, relancer avec `--routage` et comparer les traces sur la même question d’actualité. |

Saisir `fin` pour quitter les conversations interactives. Pour les preuves demandées au rendu, conserver les réponses et les prompts observés lors des exécutions.

## Application Web

Dans IntelliJ, ouvrir la racine `TP4-ALAMI_LOUATI_Ghali`, sélectionner **TP4 Web (Payara)** dans le menu Run et cliquer sur ▶. Cette configuration de type **Application** utilise le module Web et lance `run-web-payara.bat`. Si IntelliJ ne reconnaît pas les sources du lanceur, recharger les projets Maven.

Le script compile le projet avec le JDK 25 et déploie le WAR sur Payara 7. Il crée au premier démarrage le domaine `tp4-local`, utilise le port d'administration `10048` et configure le port HTTP `9080` pour l'application. Après le déploiement, ouvrir [http://127.0.0.1:9080/tp4/](http://127.0.0.1:9080/tp4/).

Le script utilise les installations JDK, Maven et Payara du dossier de cours parent. La clé `GEMINI_KEY` doit être disponible pour le processus qui démarre Payara.

Pour utiliser une autre installation de Payara 7, définir `TP4_PAYARA_HOME` dans les variables d'environnement de la configuration IntelliJ **TP4 Web (Payara)**. Indiquer le dossier `payara7` qui contient `bin/asadmin.bat`.

L’index en mémoire est partagé entre les vues. La mémoire conversationnelle est propre à chaque vue. Une question de suivi est d’abord reformulée à partir de l’historique. Le modèle sélectionne ensuite les sources pertinentes parmi les deux index PDF, puis le modèle de streaming répond avec les extraits récupérés.

Pour vérifier les deux fonctionnalités, poser une question sur le sujet du deuxième PDF, puis une question de suivi qui reprend ce sujet par un pronom. Poser ensuite « Qu'est-ce que le fine-tuning ? » et vérifier que la réponse traite du support de cours. Démarrer un nouveau chat pour vérifier que le suivi conversationnel a été réinitialisé.

## Références

- Richard Grin, [énoncé du TP4](http://richard.grin.free.fr/emsi/casablanca-ia/tp/tp4-rag/index.html) et [copie consultée](https://chatgpt.com/library?file_id=libfile_db71d9927d008191b64e351aa9cc58d4).
- LangChain4j, [parseur Apache Tika](https://docs.langchain4j.dev/apidocs/dev/langchain4j/data/document/parser/apache/tika/ApacheTikaDocumentParser.html), [récupération vectorielle](https://docs.langchain4j.dev/apidocs/dev/langchain4j/rag/content/retriever/EmbeddingStoreContentRetriever.html), [reformulation conversationnelle](https://docs.langchain4j.dev/apidocs/dev/langchain4j/rag/query/transformer/CompressingQueryTransformer.html), [routage par modèle](https://docs.langchain4j.dev/apidocs/dev/langchain4j/rag/query/router/LanguageModelQueryRouter.html) et [intégration Tavily](https://docs.langchain4j.dev/integrations/web-search-engines/tavily/).
