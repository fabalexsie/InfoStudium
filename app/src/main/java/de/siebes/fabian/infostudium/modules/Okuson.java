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
import de.siebes.fabian.infostudium.R;
import de.siebes.fabian.infostudium.StorageHelper;
import de.siebes.fabian.infostudium.Test;
import de.siebes.fabian.infostudium.WebsiteLoadingUtils;

public class Okuson extends ModuleLoading {

    private String mStrUrl;
    private Module mModul;

    public Okuson(Activity activity, WebsiteLoadingUtils.OnFinishedListener onFinishedListener, Module modul) {
        super(activity, onFinishedListener);
        mStrUrl = activity.getString(R.string.url_okuson_results_1)
                + modul.getModulKursId()
                + activity.getString(R.string.url_okuson_results_2);
        mModul = modul;
    }

    @Override
    public void loadResults() {
        if (!isActivated(mModul)) {
            return;
        }
        // else
        new Thread() { // Internet in another Thread
            @Override
            public void run() {
                try {
                    StorageHelper storageHelper = new StorageHelper(mActivity);
                    LoginData loginData = storageHelper.getLogin(mModul);
                    String strPraefixName = storageHelper.getStringSettings(StorageHelper.PRAEFIX_NAME, StorageHelper.PRAEFIX_NAME_DEF_VALUE);

                    if (mStrUrl == null) return;
                    Connection con = Jsoup.connect(mStrUrl)
                            .data("id", loginData.getBenutzer())
                            .data("passwd", loginData.getPasswort())
                            .timeout(Const.TIMEOUT);

                    Document doc = con.post();

                    int col = 0;
                    String testName = "";

                    if (doc.getElementsByTag("td").size() <= 0) {
                        stopWithErrorCode(mModul, ErrorCode.NO_TESTS_FOUND);
                        return;
                    }

                    for (Element el : doc.getElementsByTag("td")) {
                        switch (col) {
                            case 0:
                                if (el.hasText())
                                    testName = el.text();
                                break;
                            case 1:
                            case 2:
                                double points, maxPoints;
                                if (el.hasText()) {
                                    String[] strTemp = el.text().split("\\(");
                                    if (strTemp[0].trim().equals("?")) {
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
                                    Test t = new Test(strPraefixName + testName, points, maxPoints);
                                    if (col == 1)
                                        mModul.addTestOnline(t);
                                    else
                                        mModul.addTestSchriftlich(t);
                                }
                                break;
                        }

                        col++;
                        col %= 3;
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                    Log.e(mModul.getModulTitle(), e.getMessage());
                    stopWithErrorCode(mModul, ErrorCode.OTHER_PROBLEMS);
                    return;
                }
                mActivity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (mOnFinishedListener != null) {
                            mOnFinishedListener.onFinished(mActivity, mModul);
                        }
                    }
                });
            }
        }.start();
    }
}
