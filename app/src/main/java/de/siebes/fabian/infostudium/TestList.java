package de.siebes.fabian.infostudium;

import java.util.ArrayList;
import java.util.List;

public class TestList implements Comparable<TestList> {
    private String listName;
    private List<Test> tests = new ArrayList<>();

    TestList(String listName) {
        this.listName = listName;
    }

    String getListName() {
        return listName;
    }

    List<Test> getTests() {
        return tests;
    }

    @Override
    public int compareTo(TestList other) {
        if (other == null) return 0;
        return this.getListName().compareTo(other.getListName());
    }
}
