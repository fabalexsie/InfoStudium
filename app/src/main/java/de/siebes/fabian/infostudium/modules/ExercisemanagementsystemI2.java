package de.siebes.fabian.infostudium.modules;

import android.app.Activity;
import android.util.Log;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.io.IOException;
import java.net.NoRouteToHostException;

import de.siebes.fabian.infostudium.Const;
import de.siebes.fabian.infostudium.ErrorCode;
import de.siebes.fabian.infostudium.LoginData;
import de.siebes.fabian.infostudium.Module;
import de.siebes.fabian.infostudium.R;
import de.siebes.fabian.infostudium.StorageHelper;
import de.siebes.fabian.infostudium.Test;
import de.siebes.fabian.infostudium.WebsiteLoadingUtils;

public class ExercisemanagementsystemI2 extends ModuleLoading {

    private String mStrKursName;
    private Module mModul;

    public ExercisemanagementsystemI2(Activity activity, WebsiteLoadingUtils.OnFinishedListener onFinishedListener, Module modul) {
        super(activity, onFinishedListener);
        this.mModul = modul;
        this.mStrKursName = modul.getModulKursId();
    }

    @Override
    public void loadResults() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    StorageHelper storageHelper = new StorageHelper(mActivity);
                    LoginData loginData = storageHelper.getLogin(mModul);
                    String strPraefixName = storageHelper.getStringSettings(StorageHelper.PRAEFIX_NAME, StorageHelper.PRAEFIX_NAME_DEF_VALUE);

                    Connection conLogin = Jsoup.connect(mActivity.getString(R.string.url_exmanag_login, mStrKursName))
                            .method(Connection.Method.POST)
                            .timeout(Const.TIMEOUT);
                    Connection.Response resLogin = conLogin.execute();
                    Document docLogin = resLogin.parse();

                    Element eSessionNumber = docLogin.select("input[name=javax.faces.ViewState]").get(0);
                    Element eForm = docLogin.select("form").get(0);
                    Element eButton = docLogin.select("button").get(0);

                    String strSessValue = eSessionNumber.attr("value");
                    String strLoginUrl = eForm.attr("action");
                    String strBtnNameAttr = eButton.attr("name");

                    Connection conIndex = Jsoup.connect(mActivity.getString(R.string.url_exmanag_index_base) + strLoginUrl)
                            .method(Connection.Method.POST)
                            .data("form", "form")
                            .data("form:username", loginData.getBenutzer())
                            .data("form:password", loginData.getPasswort())
                            .data(strBtnNameAttr, "")
                            .data("javax.faces.ViewState", strSessValue)
                            .cookie("JSESSIONID", resLogin.cookie("JSESSIONID"))
                            .timeout(Const.TIMEOUT);

                    conIndex.execute(); // Um sich beim Server anzumelden

                    Connection conPoints = Jsoup.connect(mActivity.getString(R.string.url_exmanag_points, mStrKursName))
                            .cookie("JSESSIONID", resLogin.cookie("JSESSIONID"))
                            .timeout(Const.TIMEOUT);

                    Document docPoints = conPoints.get();

                    int col = 0;
                    String strTestName = "";
                    forTdElemente:
                    for (Element el : docPoints.getElementsByTag("td")) {
                        switch (col) {
                            case 0: // Link zum Maximieren der Details
                                break;
                            case 1: // Name des Übungsblattes (neuestes zu erst)
                                if (el.hasText())
                                    strTestName = el.text();
                                break;
                            case 2: // Grafikbalken (erzeugt die App selbstständig
                            case 3: // Erreicht in Prozent, wird selbstständig berechnet
                                break;
                            case 4: // Punkte (Möglich)
                                double points, maxPoints;
                                if (el.hasText()) {
                                    String[] strTemp = el.text().split("\\(");
                                    if (strTemp[0].trim().equals("--")) {
                                        points = -1;
                                    } else {
                                        try {
                                            points = Double.parseDouble(strTemp[0].trim());
                                        } catch (Exception e) {
                                            points = -2;
                                        }
                                    }
                                    // Zahl nach "(" ohne das letzte Zeichen ")"
                                    try {
                                        maxPoints = Double.parseDouble(strTemp[1].substring(0, strTemp[1].length() - 1).trim());
                                    } catch (Exception e) {
                                        maxPoints = 0;
                                    }

                                    if (strTestName.startsWith("Minitest ")) {
                                        strTestName = strTestName.replace("Minitest ", strPraefixName);
                                        Test t = new Test(strTestName, points, maxPoints);
                                        mModul.addTest("Minitest", t);
                                    } else {
                                        strTestName = strTestName.replace("Übungsblatt ", strPraefixName);
                                        Test t = new Test(strTestName, points, maxPoints);
                                        mModul.addTestSchriftlich(t);
                                    }
                                }
                                if (strTestName.equals("Übungsblatt 1"))
                                    break forTdElemente;
                                break;
                        }
                        col++;
                        if (el.hasAttr("onclick")) // 1. Spalte einer neuen Zeile
                            col = 1;
                    }
                } catch (IOException e) {
                    if (e instanceof NoRouteToHostException) {
                        stopWithErrorCode(mModul, ErrorCode.SERVER_NOT_AVAILABLE);
                    } else {
                        e.printStackTrace();
                        Log.e("ExcersiseMS", e.getMessage());
                        stopWithErrorCode(mModul, ErrorCode.OTHER_PROBLEMS);
                    }
                    return;
                }
                mActivity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (mOnFinishedListener != null)
                            mOnFinishedListener.onFinished(mActivity, mModul);
                    }
                });
            }
        }).start();
    }
}
