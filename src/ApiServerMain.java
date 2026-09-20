import api.SearchApiServer;
import documents.document;
import engine.SearchEngine;

public class ApiServerMain {

    public static void main(String[] args) throws Exception {

        document[] documents = {
                new document(1, "Introduction to Machine Learning",
                        "Machine learning is a branch of artificial intelligence that allows computers to learn from data."),
                new document(2, "Deep Learning Fundamentals",
                        "Deep learning uses neural networks to learn complex patterns from large datasets."),
                new document(3, "Database Management Systems",
                        "A database management system stores, organizes, retrieves, and manages data efficiently."),
                new document(4, "Operating Systems",
                        "An operating system manages computer hardware, memory, processes, files, and software resources."),
                new document(5, "Computer Networks",
                        "Computer networks allow devices to communicate and exchange data using networking protocols."),
                new document(6, "Data Structures and Algorithms",
                        "Data structures organize data while algorithms provide efficient methods for solving computational problems."),
                new document(7, "Artificial Intelligence",
                        "Artificial intelligence enables machines to perform tasks that normally require human intelligence."),
                new document(8, "Cyber Security",
                        "Cyber security protects computer systems, networks, applications, and data from unauthorized access."),
                new document(9, "Cloud Computing",
                        "Cloud computing provides on-demand access to computing resources, storage, databases, and applications."),
                new document(10, "Software Engineering",
                        "Software engineering applies systematic methods to design, develop, test, deploy, and maintain software.")
        };

        SearchEngine searchEngine = new SearchEngine(documents);

        new SearchApiServer(searchEngine).start(8080);

        System.out.println("API running at http://localhost:8080/search?q=machine");
    }
}