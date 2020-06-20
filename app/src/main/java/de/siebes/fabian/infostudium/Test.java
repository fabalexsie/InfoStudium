package de.siebes.fabian.infostudium;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Test implements Comparable<Test> {
    private static Pattern p = Pattern.compile("\\d{1,2}\\.\\d{1,2}\\.\\d{2,4}");

    private String name;
    private double points;
    private double maxPoints;
    private long sortValue;

    public Test(String name, double points, double maxPoints) {
        setName(name);
        this.points = points;
        this.maxPoints = maxPoints;
    }

    public Test(String name, String strSortValue, double points, double maxPoints) {
        setName(name/*, strSortValue*/);
        this.points = points;
        this.maxPoints = maxPoints;
    }

    /**
     * konvertiert alle ziffern in eine Zahl unabhängig von Zeichen dazwischen,
     * wenn ein Datum im Format dd.mm.yy erkannt wird, dann wird der wert yyyymmdd zurück geliefert
     */
    private static long toLong(String string) {
        long res = 0;

        // converts all available numbers in string in one long
        for (int i : string.toCharArray()) {
            if (i >= 48 && i <= 57) { // 48 = '0' und 57 = '9'
                res = res * 10 + i - 48;
            }
        }

        Matcher m = p.matcher(string);
        if (m.find()) {  // Pattern.matches("[0-9]{1,2}.[0-9]{1,2}.[0-9]{2,4}", string)
            /*int day = (int) (res / 100000000);
            int month = (int) (res / 1000000 - day * 100);
            int year = (int) (res - day * 10000 - month * 100);*/
            String strDate = m.group();
            String[] strSplit = strDate.split("\\.");
            if (strSplit.length > 2) {
                String day = strSplit[0];
                String month = strSplit[1];
                String year = strSplit[2];
                res = toLong((year.length() > 2 ? year : "20" + year)
                        + (month.length() > 1 ? month : "0" + month)
                        + (day.length() > 1 ? day : "0" + day));
            }
        }

        return res;
    }

    double getMaxPoints() {
        return maxPoints;
    }

    double getPoints() {
        return points;
    }

    private void setName(String strName) {
        this.sortValue = toLong(strName);
        this.name = strName.trim();
    }

    private void setName(String strName, String strSortValue) {
        this.sortValue = toLong(strSortValue);
        this.name = strName.trim();
    }

    String getName() {
        return name;
    }

    private long getSortValue() {
        return sortValue;
    }

    @Override
    public int compareTo(Test other) {
        if (other == null) return 0;
        if (other.getSortValue() > this.getSortValue())
            return 1;
        else if (other.getSortValue() < this.getSortValue())
            return -1;
        else
            return 0;
    }
}
