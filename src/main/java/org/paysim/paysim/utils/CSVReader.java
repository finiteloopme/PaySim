package org.paysim.paysim.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;

public class CSVReader {
    private static final String CSV_SEPARATOR = ",";

    private static CSVReader csvReader = new CSVReader();

    private CSVReader(){

    }

    public static CSVReader getInstance(){
        return csvReader;
    }

    public static ArrayList<String[]> read(String csvFile) {
        ArrayList<String[]> csvContent = new ArrayList<>();
        // try (BufferedReader br = new BufferedReader(new FileReader("/BOOT-INF/classes/src/main/resources" +csvFile))) {
           try (BufferedReader br = new BufferedReader(
            new InputStreamReader(csvReader.getClass().getResourceAsStream("/BOOT-INF/classes/src/main/resources/" +csvFile))
               )) {
                // Skip header
            br.readLine();
            String line;
            while ((line = br.readLine()) != null) {
                csvContent.add(line.split(CSV_SEPARATOR));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return csvContent;
    }
}
