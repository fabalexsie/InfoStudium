package de.siebes.fabian.infostudium.modules;

import android.app.Activity;
import android.util.Log;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.util.Map;

import de.siebes.fabian.infostudium.Const;
import de.siebes.fabian.infostudium.ErrorCode;
import de.siebes.fabian.infostudium.LoginData;
import de.siebes.fabian.infostudium.Module;
import de.siebes.fabian.infostudium.StorageHelper;
import de.siebes.fabian.infostudium.Test;
import de.siebes.fabian.infostudium.WebsiteLoadingUtils;

public class MaLo extends ModuleLoading {
    private Module mModul;

    public MaLo(Activity activity, WebsiteLoadingUtils.OnFinishedListener onFinishedListener, Module modul) {
        super(activity, onFinishedListener);
        mModul = modul;
    }

    @Override
    public void loadResults() {
        if (!isActivated(mModul)) {
            return;
        }
        // else
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    StorageHelper storageHelper = new StorageHelper(mActivity);
                    LoginData loginData = storageHelper.getLogin(mModul);

                    Connection.Response resLogin = Jsoup
                            .connect("https://logic.rwth-aachen.de/malo-shib/index.php")
                            .method(Connection.Method.GET)
                            .timeout(Const.TIMEOUT)
                            .execute();

                   /*Connection.Response resAccept = Jsoup
                            .connect("https://sso.rwth-aachen.de/idp/profile/SAML2/Redirect/SSO?execution=e1s1")
                            .cookies(resLogin.cookies())
                            .data("j_username", loginData.getBenutzer())
                            .data("j_password", loginData.getPasswort())
                            .data("donotcache", "1")
                            .data("_eventId_proceed", "Anmeldung")
                            .data("_shib_idp_revokeConsent", "true")
                            .timeout(Const.TIMEOUT)
                            .execute();

                    Connection.Response res = Jsoup.connect("https://sso.rwth-aachen.de/idp/profile/SAML2/Redirect/SSO?execution=e1s2")
                            .timeout(Const.TIMEOUT)
                            .data("_shib_idp_consentIds", "rwthSystemIDs")
                            .data("_shib_idp_consentOptions", "_shib_idp_rememberConsent")
                            .data("_eventId_proceed", "Akzeptieren")
                            .cookies(resLogin.cookies())
                            .cookies(resAccept.cookies())
                            .method(Connection.Method.POST)
                            .timeout(Const.TIMEOUT)
                            .execute();

                    Connection con = Jsoup.connect("https://logic.rwth-aachen.de/malo-shib/Shibboleth.sso/SAML2/POST")
                            .method(Connection.Method.POST)
                            .timeout(Const.TIMEOUT);*/

                    Connection.Response resErgebnisse = Jsoup
                            .connect("https://logic.rwth-aachen.de/malo-shib/index.php")
                            .method(Connection.Method.GET)
                            .cookies(resLogin.cookies())
                            //.cookies(resAccept.cookies())
                            //.cookies(res.cookies())
                            .timeout(Const.TIMEOUT)
                            .execute();

                    Document docErgebnisse = resErgebnisse.parse();

                    int col = 0;
                    String strName = "";
                    //Elements elements = docErgebnisse.select("table td:not(.head):not(.sum)");
                    Elements elements = docErgebnisse.select("table td:not(.head)");
                    for (Element el : elements) {
                        switch (col) {
                            case 0:
                                strName = el.text();
                                break;
                            case 1:
                            case 2:
                                String[] strPointSplit = el.text().split(" / ");
                                String strPoints = strPointSplit[0].replace(',', '.');
                                String strGesPoints = strPointSplit[1].replace(',', '.');
                                double dPoints, dGesPoints;
                                try {
                                    dPoints = Double.parseDouble(strPoints);
                                    dGesPoints = Double.parseDouble(strGesPoints);
                                } catch (Exception e) {
                                    dPoints = -2;
                                    dGesPoints = -2;
                                }
                                if (col == 1) {
                                    mModul.addTestSchriftlich(new Test(strName, dPoints, dGesPoints));
                                } else {
                                    mModul.addTestOnline(new Test(strName, dPoints, dGesPoints));
                                }
                                break;
                        }

                        col++;
                        col %= 4;
                    }

                    Log.i("MaLo", "Fertig");

                } catch (IOException e) {
                    e.printStackTrace();
                    Log.e("MaLo", e.getMessage());
                    stopWithErrorCode(mModul, ErrorCode.OTHER_PROBLEMS);
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
