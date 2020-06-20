package de.siebes.fabian.infostudium.modules;

import android.app.Activity;
import android.util.Log;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.io.IOException;

import de.siebes.fabian.infostudium.Const;
import de.siebes.fabian.infostudium.ErrorCode;
import de.siebes.fabian.infostudium.LoginData;
import de.siebes.fabian.infostudium.Module;
import de.siebes.fabian.infostudium.StorageHelper;
import de.siebes.fabian.infostudium.Test;
import de.siebes.fabian.infostudium.WebsiteLoadingUtils;

public class ORAbgabesystem extends ModuleLoading {
    private final Module mModule;

    public ORAbgabesystem(Activity activity, WebsiteLoadingUtils.OnFinishedListener onFinishedListener, Module module) {
        super(activity, onFinishedListener);
        mModule = module;
    }

    @Override
    public void loadResults() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    StorageHelper storageHelper = new StorageHelper(mActivity);
                    LoginData loginData = storageHelper.getLogin(mModule);

                    Connection.Response resLogin = Jsoup
                            .connect("https://tutor.or.rwth-aachen.de/accounts/login/")
                            .timeout(Const.TIMEOUT)
                            .execute();

                    Connection connection = Jsoup
                            .connect("https://tutor.or.rwth-aachen.de/accounts/login/")
                            .cookies(resLogin.cookies())
                            .timeout(Const.TIMEOUT);

                    for (Element e : resLogin.parse().select("input[type=hidden]")) {
                        if (!e.attr("name").equals("")) { // Solange das name Attribut gefüllt ist
                            connection.data(e.attr("name"), e.attr("value"));
                        }
                    }
                    connection.data("username", loginData.getBenutzer());
                    connection.data("password", loginData.getPasswort());
                    connection.header("Referer", "https://tutor.or.rwth-aachen.de/accounts/login/");
                    connection.header("Accept-Language","de");
                    Document doc = connection.post();

                    String strName = "", strDate = "";
                    double dMaxPoints, dPoints;
                    int colCount = 0;
                    for (Element el : doc.select(".row h4, .row .card .card-text")) {
                        switch (colCount) {
                            case 0:
                                strName = el.text();
                                break;
                            case 1:
                                if (el.text().contains("Maximal erreichbar")) {
                                    dPoints = 0;
                                    String maxPoints = Const.substrBetween(el.text(), "Maximal erreichbar:", null);
                                    try {
                                        dMaxPoints = Double.parseDouble(maxPoints);
                                    } catch (Exception e) {
                                        dMaxPoints = -2;
                                    }
                                    strDate = Const.substrBetween(el.html(), "Fällig bis:", "<br>");
                                } else if (el.text().contains("Bewertung")) {
                                    String strPoints = Const.substrBetween(el.text(), "Bewertung:", "/");
                                    try {
                                        dPoints = Double.parseDouble(strPoints);
                                    } catch (Exception e) {
                                        dPoints = -2;
                                    }
                                    String strMaxPoints = Const.substrBetween(el.text(), "/", null);
                                    try {
                                        dMaxPoints = Double.parseDouble(strMaxPoints);
                                    } catch (Exception e) {
                                        dMaxPoints = -2;
                                    }
                                    strDate = Const.substrBetween(el.html(), "Beendet:", "<br>");
                                } else {
                                    dPoints = dMaxPoints = -3;
                                }
                                mModule.addTest("Python & Gurobi", new Test(strName, strDate, dPoints, dMaxPoints));
                                strName = strDate = "";
                                break;
                        }

                        colCount++;
                        colCount %= 2;
                    }

                } catch (IOException e) {
                    e.printStackTrace();
                    Log.e("ORAbgabesystem", e.getMessage());
                    stopWithErrorCode(mModule, ErrorCode.OTHER_PROBLEMS);
                    return;
                }

                mActivity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (mOnFinishedListener != null)
                            mOnFinishedListener.onFinished(mActivity, mModule);
                    }
                });
            }
        }).start();
    }
}
