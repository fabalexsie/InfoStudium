package de.siebes.fabian.infostudium.modules;

import android.app.Activity;
import android.util.Log;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.io.InputStream;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Map;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;

import de.siebes.fabian.infostudium.Const;
import de.siebes.fabian.infostudium.ErrorCode;
import de.siebes.fabian.infostudium.LoginData;
import de.siebes.fabian.infostudium.Module;
import de.siebes.fabian.infostudium.R;
import de.siebes.fabian.infostudium.StorageHelper;
import de.siebes.fabian.infostudium.Test;
import de.siebes.fabian.infostudium.WebsiteLoadingUtils;

public class Moodle extends ModuleLoading {
    private static Map<String, String> cookies;
    private static boolean booIsLogedIn = false;
    private Module mModul;
    private String mStrKursId;

    public Moodle(Activity activity, WebsiteLoadingUtils.OnFinishedListener onFinishedListener, Module modul) {
        super(activity, onFinishedListener);
        mModul = modul;
        mStrKursId = modul.getModulKursId();
    }

    /**
     * call only inside other Thread
     *
     * @param loginData
     * @return
     * @throws IOException
     * @throws InterruptedException
     */
    private static synchronized Map<String, String> getLoginCookies(LoginData loginData) throws IOException, InterruptedException {
        if (!booIsLogedIn) {
            Connection.Response resMoodleLogin = Jsoup
                    .connect("https://moodle.rwth-aachen.de/login/index.php")
                    .method(Connection.Method.GET)
                    .timeout(Const.TIMEOUT)
                    .execute();


            Connection.Response resLogin = Jsoup
                    .connect("https://moodle.rwth-aachen.de/auth/shibboleth/index.php")
                    .method(Connection.Method.POST)
                    .timeout(Const.TIMEOUT)
                    .cookies(resMoodleLogin.cookies())
                    .execute();

            Connection.Response resAccept = Jsoup
                    .connect("https://sso.rwth-aachen.de/idp/profile/SAML2/Redirect/SSO?execution=e1s1")
                    .cookies(resMoodleLogin.cookies())
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
                    .cookies(resMoodleLogin.cookies())
                    .cookies(resLogin.cookies())
                    .cookies(resAccept.cookies())
                    .method(Connection.Method.POST)
                    .timeout(Const.TIMEOUT)
                    .execute();

            Connection connection = Jsoup
                    .connect("https://moodle.rwth-aachen.de/Shibboleth.sso/SAML2/POST")
                    .method(Connection.Method.POST)
                    .timeout(Const.TIMEOUT)
                    .cookies(resMoodleLogin.cookies())
                    .cookies(resLogin.cookies())
                    .cookies(res.cookies());
            for (Element e : res.parse().getElementsByTag("input")) {
                if (!e.attr("name").equals("")) { // Solange das name attribute gefüllt ist
                    connection.data(e.attr("name"), e.attr("value"));
                }
            }
            Connection.Response resDashboard = connection.execute();

            cookies = resMoodleLogin.cookies();
            cookies.putAll(resLogin.cookies());
            cookies.putAll(resDashboard.cookies());

            booIsLogedIn = true;
        }
        return cookies;
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
                    String strPraefixName = storageHelper.getStringSettings(StorageHelper.PRAEFIX_NAME, StorageHelper.PRAEFIX_NAME_DEF_VALUE);

                    Map<String, String> cookies = getLoginCookies(loginData);

                    Connection.Response resErgebnisse = Jsoup
                            .connect("https://moodle.rwth-aachen.de/grade/report/user/index.php?id=" + String.valueOf(mStrKursId))
                            .method(Connection.Method.GET)
                            .cookies(cookies)
                            .timeout(Const.TIMEOUT)
                            .execute();

                    Document docErgebnisse = resErgebnisse.parse();

                    int col = 0;
                    String strName = "";
                    double dPoints = 0;
                    Elements elements;
                    if (mStrKursId.equals("8277")) {
                        // DatKom-Tests liegen in level3
                        elements = docErgebnisse.select("table th[id^=row_][class^=level3]," +
                                " table td[headers$=grade][class^=level3]," +
                                " table td[headers$=range][class^=level3]");
                    } else {
                        elements = docErgebnisse.select("table th[id^=row_].level2," +
                                " table td[headers$=grade].level2," +
                                " table td[headers$=range].level2");
                    }
                    for (Element el : elements) {
                        switch (col) {
                            case 0:
                                strName = el.text();
                                break;
                            case 1:
                                if (el.text().equals("-")) {
                                    dPoints = -1;
                                } else {
                                    String strPoints = el.text().replace(',', '.');
                                    try {
                                        dPoints = Double.parseDouble(strPoints);
                                    } catch (Exception e) {
                                        dPoints = -2;
                                    }
                                }
                                break;
                            case 2:
                                String strMaxPoints;
                                if (el.text().split("(–|&h;)").length > 1) { // - oder &h;
                                    strMaxPoints = el.text().split("(–|&h;)")[1];
                                } else {
                                    strMaxPoints = "0";
                                }
                                strMaxPoints = strMaxPoints.replace(',', '.');
                                double dMaxPoints;
                                try {
                                    dMaxPoints = Double.parseDouble(strMaxPoints);
                                } catch (Exception e) {
                                    dMaxPoints = -2;
                                }
                                if (strName.startsWith("Test")) {
                                    Test t = new Test(strName, dPoints, dMaxPoints);
                                    mModul.addTest("E-Tests", t);
                                } else if (strName.startsWith("Selbsttest")) {
                                    strName = strName.substring(12, strName.length() - 1);
                                    Test t = new Test(strName, dPoints, dMaxPoints);
                                    mModul.addTest("Selbsttest", t);
                                } else if (strName.startsWith("Quiz ")) {
                                    strName = strName.replace("Quiz ", "")
                                            .replace("zur Vorlesung am ", "");
                                    Test t = new Test(strName, dPoints, dMaxPoints);
                                    mModul.addTest("Quiz", t);
                                } else if (strName.startsWith("Bonus")) {
                                    Test t = new Test(strName, dPoints, dMaxPoints);
                                    mModul.addTest("Bonus-Stufen", t);
                                } else if (strName.startsWith("UNBEWERTET: ")) {
                                    strName = strName.replace("UNBEWERTET: ", "");
                                    Test t = new Test(strName, dPoints, dMaxPoints);
                                    mModul.addTest("UNBEWERTET", t);
                                } else if (strName.toLowerCase().contains("gesamt")
                                        || strName.toLowerCase().contains("summe")
                                        || strName.toLowerCase().contains("punkte")
                                        || strName.toLowerCase().contains("klausur")) {
                                    // Nicht hinzufügen
                                } else {
                                    strName = strName.replace("Übungsblatt ", strPraefixName); // TODO Post-Processor erstellen und auslagern
                                    strName = strName.replace("Uebungsblatt ", strPraefixName);
                                    strName = strName.replace("Blatt", strPraefixName);
                                    Test t = new Test(strName, dPoints, dMaxPoints);
                                    mModul.addTestSchriftlich(t);
                                }
                                break;
                        }

                        col++;
                        col %= 3;
                    }

                    Log.i("Moodle", "Fertig");

                } catch (IOException | InterruptedException e) {
                    e.printStackTrace();
                    Log.e("Moodle", e.getMessage());
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

    private SSLSocketFactory getRWTH_SSLSocketFactory() throws CertificateException, NoSuchAlgorithmException, KeyStoreException, KeyManagementException, IOException, NoSuchProviderException {
        return buildSslContext(mActivity.getResources().openRawResource(R.raw.moodle_cert)).getSocketFactory();
    }

    private SSLContext buildSslContext(InputStream... inputStreams) throws IOException, CertificateException, KeyStoreException, NoSuchAlgorithmException, KeyManagementException, NoSuchProviderException {
        X509Certificate cert;
        KeyStore trustStore = KeyStore.getInstance(KeyStore.getDefaultType());
        trustStore.load(null);

        for (InputStream inputStream : inputStreams) {
            try {
                CertificateFactory certificateFactory = CertificateFactory.getInstance("X.509");
                cert = (X509Certificate) certificateFactory.generateCertificate(inputStream);
            } finally {
                if (inputStream != null)
                    inputStream.close();
            }
            String alias = cert.getSubjectX500Principal().getName();
            trustStore.setCertificateEntry(alias, cert);
        }

        TrustManagerFactory tmf = TrustManagerFactory.getInstance("X509");
        tmf.init(trustStore);
        TrustManager[] trustManagers = tmf.getTrustManagers();
        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(null, trustManagers, null);

        return sslContext;
    }
}
