package datasets;

import documents.document;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class CISILoader {

    public static document[] load(String filePath) throws IOException {

        List<document> documents = new ArrayList<>();

        BufferedReader reader = new BufferedReader(new 
FileReader(filePath));

        String line;

        int id = -1;
        String title = "";
        StringBuilder content = new StringBuilder();

        String currentSection = "";

        while ((line = reader.readLine()) != null) {

            line = line.trim();

            if (line.startsWith(".I")) {

                if (id != -1) {
                    documents.add(
                        new document(
                            id,
                            title,
                            content.toString().trim()
                        )
                    );
                }

                String[] parts = line.split("\\s+");
                id = Integer.parseInt(parts[1]);

                title = "";
                content = new StringBuilder();
                currentSection = "";

            } else if (line.equals(".T")) {

                currentSection = "TITLE";

            } else if (line.equals(".A")) {

                currentSection = "AUTHOR";

            } else if (line.equals(".W")) {

                currentSection = "CONTENT";

            } else if (line.equals(".X")) {

                currentSection = "REFERENCES";

            } else {

                if (currentSection.equals("TITLE")) {

                    if (!line.isEmpty()) {
                        if (!title.isEmpty()) {
                            title += " ";
                        }

                        title += line;
                    }

                } else if (currentSection.equals("CONTENT")) {

                    if (!line.isEmpty()) {
                        if (content.length() > 0) {
                            content.append(" ");
                        }

                        content.append(line);
                    }
                }
            }
        }

        if (id != -1) {
            documents.add(
                new document(
                    id,
                    title,
                    content.toString().trim()
                )
            );
        }

        reader.close();

        return documents.toArray(new document[0]);
    }
}
