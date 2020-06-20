package de.siebes.fabian.infostudium;

import android.content.Context;

import androidx.annotation.Nullable;

import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.List;

public class Const {
    public static final int TIMEOUT = 10 * 1000; // 10s

    static DecimalFormat decimalFormat;

    static {
        DecimalFormatSymbols symbols = DecimalFormatSymbols.getInstance();
        symbols.setDecimalSeparator('.');
        decimalFormat = new DecimalFormat("0.0#", symbols);
        decimalFormat.setRoundingMode(RoundingMode.HALF_UP);
    }

    public static String[] getPrefilledModuleNames(@Nullable Context c) {
        return new String[]{c != null ? c.getResources().getString(R.string.custom_module) : "Custom",
                "AfI",
                "DS",
                "Progra",
                "TI",
                "BuS",
                "DsAl",
                "FoSAP",
                "LA",
                "Stocha",
        };
    }

    public static Module getPrefilledModul(@Nullable Context c, int position) {
        Module modul = new Module();
        modul.setModulTitle(getPrefilledModuleNames(c)[position]);

        switch (position) {
            case 0:
                modul.setModulDesc("Füge Module hinzu, die nicht in der App enthalten sind.");
                break;
            case 1: // AFI
                modul.setModulDesc("1. Semester (2018)\nAnalysis für Informatiker");
                modul.setModulType(Module.TYPE_MOODLE);
                modul.setModulKursId("148");
                break;
            case 2: // DS
                modul.setModulDesc("1. Semester (2018)\nDiskrete Strukturen");
                modul.setModulType(Module.TYPE_OKUSON);
                modul.setModulKursId("DS18");
                break;
            case 3: // Progra
                modul.setModulDesc("1. Semester (2018)\nProgrammierung");
                modul.setModulType(Module.TYPE_EXERCISEMANAGEMENT);
                modul.setModulKursId("info18");
                break;
            case 4: // TI
                modul.setModulDesc("1. Semester (2018)\nTechnische Informatik");
                modul.setModulType(Module.TYPE_L2P);
                modul.setModulKursId("");
                break;
            case 5: // BuS
                modul.setModulDesc("2. Semester (2019)\nBetriebssysteme und Systemsoftware");
                modul.setModulType(Module.TYPE_MOODLE);
                modul.setModulKursId("732");
                break;
            case 6: // DsAl
                modul.setModulDesc("2. Semester (2019)\nDatenstrukturen und Algorithmen");
                modul.setModulType(Module.TYPE_EXERCISEMANAGEMENT);
                modul.setModulKursId("dsal19");
                break;
            case 7: // FoSAP
                modul.setModulDesc("2. Semester (2019)\nFormale Systeme, Automaten und Prozesse");
                modul.setModulType(Module.TYPE_EXERCISEMANAGEMENT);
                modul.setModulKursId("fosap19");
                break;
            case 8: // LA
                modul.setModulDesc("2. Semester (2019)\nLineare Algebra");
                modul.setModulType(Module.TYPE_OKUSON);
                modul.setModulKursId("LAInf19");
                break;
            case 9: // Stocha
                modul.setModulDesc("4. Semester (2019)\nEinführung in die angewandte Stochastik");
                modul.setModulType(Module.TYPE_MOODLE);
                modul.setModulKursId("693");
                break;
        }

        return modul;
    }

    public static String getPrefilledModules2Json(Context c) {
        List<Module> moduleList = new ArrayList<>();

        StringBuilder builder = new StringBuilder();

        for (int i = 0; i <= 9; i++) {
            Module module = getPrefilledModul(c, i);
            if (module != null) {
                moduleList.add(module);

                builder.append("{\n");
                builder.append("\"title\": \"\n" + module.getModulTitle() + "\",\n");
                builder.append("\"desc\": \"\n" + module.getModulDesc() + "\",\n");
                builder.append("\"type\": \"\n" + module.getModulType() + "\",\n");
                builder.append("\"kursid\": \"\n" + module.getModulKursId() + "\",\n");
                builder.append("},\n");

            }
        }

        return builder.toString();
    }

    public static String substrBetween(String string, String strStart, String strEnd) {
        int iStart = 0, iEnd = string.length();
        if (strStart != null) {
            iStart = string.indexOf(strStart) + strStart.length();
            if (iStart < strStart.length())
                iStart = 0;
        }
        if (strEnd != null) {
            iEnd = string.indexOf(strEnd, iStart);
            if (iEnd < 0)
                iEnd = string.length();
        }
        return string.substring(iStart, iEnd).trim();
    }
}
