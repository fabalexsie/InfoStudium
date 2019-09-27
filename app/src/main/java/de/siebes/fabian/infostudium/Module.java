package de.siebes.fabian.infostudium;

import android.content.Context;
import android.util.Log;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Module {

    public static final int TYPE_MOODLE = 0;
    public static final int TYPE_OKUSON = 1;
    public static final int TYPE_EXERCISEMANAGEMENT = 2;
    public static final int TYPE_L2P = 3;

    private long id = -1;
    private int pos = -1;
    private String modulTitle;
    private String modulDesc;
    private int modulType = -1;
    private String modulKursId;
    private String semester;
    private long loginId = -1;
    private boolean mActivated = true;
    private double gesReachedPoints = 0;

    private List<TestList> testLists = new ArrayList<>();
    private ErrorCode errorCode = ErrorCode.NO_ERROR;


    public Module() {
        this("");
    }

    public Module(String name) {
        modulTitle = name;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public int getPos() {
        return pos;
    }

    public void setPos(int pos) {
        this.pos = pos;
    }

    public String getModulTitle() {
        return modulTitle;
    }

    public void setModulTitle(String modulTitle) {
        this.modulTitle = modulTitle;
    }

    public String getModulDesc() {
        return modulDesc;
    }

    public void setModulDesc(String modulDesc) {
        this.modulDesc = modulDesc;
    }

    public String getSemester() {
        return semester;
    }

    public void setSemester(String semester) {
        this.semester = semester;
    }

    public int getModulType() {
        return modulType;
    }

    public void setModulType(int modulType) {
        this.modulType = modulType;
    }

    public String getModulTypeReadable(Context c) {
        if (getModulType() >= 0 && getModulType() < c.getResources().getStringArray(R.array.modul_types).length) {
            return c.getResources().getStringArray(R.array.modul_types)[getModulType()];
        } else {
            return "";
        }
    }

    public long getLoginId() {
        return loginId;
    }

    public void setLoginId(long loginId) {
        this.loginId = loginId;
    }

    public String getModulKursId() {
        return modulKursId;
    }

    public void setModulKursId(String modulId) {
        this.modulKursId = modulId;
    }

    public boolean isActivated() {
        return mActivated;
    }

    public void setActivated(boolean activated) {
        this.mActivated = activated;
    }

    public double getGesReachedPoints() {
        return gesReachedPoints;
    }

    public void setGesReachedPoints(double gesReachedPoints) {
        this.gesReachedPoints = gesReachedPoints;
    }

    public void addTestOnline(Test test) {
        addTest("online", test);
    }

    public void addTestSchriftlich(Test test) {
        addTest("schriftlich", test);
    }

    public void addTest(String groupName, Test test) {
        TestList testList = findListByName(groupName);
        testList.getTests().add(test);
    }

    List<TestList> getTestLists() {
        Collections.sort(testLists);
        for (TestList tl : testLists) {
            Collections.sort(tl.getTests());
        }
        return testLists;
    }

    private TestList findListByName(String name) {
        for (TestList list : testLists) {
            if (list != null && list.getListName().equals(name)) return list;
        }
        TestList testList = new TestList(name);
        testLists.add(testList);
        return testList;
    }

    double calcNewGesReachedPoints() {
        double gesPoints = 0;
        for (TestList testList : getTestLists()) {
            for (Test test : testList.getTests()) {
                if (test.getPoints() > 0) // Because of error codes (-1,-2,...) saved in test points
                    gesPoints += test.getPoints();
            }
        }
        return gesPoints;
    }

    public void clearTestLists() {
        testLists.clear();
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }

    public void setErrorCode(ErrorCode errorCode) {
        this.errorCode = errorCode;
    }

    public boolean isEmpty() {
        return getModulTitle().equals("");
    }
}
