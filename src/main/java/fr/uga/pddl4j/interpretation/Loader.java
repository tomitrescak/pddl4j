package fr.uga.pddl4j.interpretation;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

interface Interpretation {

}

public class Loader {
    public Interpretation loadDefinition(String path) throws IOException {
        BufferedReader br = new BufferedReader(new FileReader("D:\\sampleJson.txt"));

        StringBuilder sb = new StringBuilder();
        String line = br.readLine();

        while (line != null) {
            sb.append(line);
            sb.append(System.lineSeparator());
            line = br.readLine();
        }
        br.close();
        String jsonInput = sb.toString();

        Object obj= JSONValue.parse(jsonInput);


        return null;
    }
}
