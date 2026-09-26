import api.SearchApiServer;
import documents.document;
import engine.SearchEngine;
import datasets.CISILoader;

public class ApiServerMain {

    public static void main(String[] args) throws Exception {

        document[] documents;

        try {
            documents = CISILoader.load("data/CISI.ALL");

            System.out.println(
                "CISI dataset loaded: " + documents.length + " documents"
            );

        } catch (Exception e) {

            System.out.println("Failed to load CISI dataset.");
            e.printStackTrace();
            return;
        }

        SearchEngine searchEngine = new SearchEngine(documents);

        new SearchApiServer(searchEngine).start(8080);
    }
}
