package de.siebes.fabian.infostudium.modules;

public class DataProcessing {
    static double getDouble(String strDouble) {
        try {
            return Double.parseDouble(strDouble.replace(',', '.').trim());
        } catch (Exception e) {
            return -2;
        }
    }

    // TODO Post-Processor erstellen und templates auslagern auf server: "orig" -> "replace" : [newCategorie]
    public static String parseTestName(String strName) {
        return strName.replace("Quiz", "")
                .replace("zur Vorlesung am", "")
                .replace("zu Kapitel", "")
                .replace("zum Kapitel", "")
                .replace("UNBEWERTET:", "")
                .replace("Python Coding", "")
                .replace("Übungsblatt", "") //strPraefixName)
                .replace("Uebungsblatt", "") //strPraefixName)
                .replace("Blatt", "")
                .replace("Minitest", "")
                .trim();
    }
}
