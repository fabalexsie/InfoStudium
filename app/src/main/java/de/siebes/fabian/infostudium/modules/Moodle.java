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
    private Module mModul;
    private String mStrKursId;

    public Moodle(Activity activity, WebsiteLoadingUtils.OnFinishedListener onFinishedListener, Module modul) {
        super(activity, onFinishedListener);
        mModul = modul;
        mStrKursId = modul.getModulKursId();
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

                    Map<String, String> cookies = RWTHonlineLogin.login(loginData);

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
                    /*Elements elements = docErgebnisse.select("table th[id^=row_][class^=level3]," +
                            " table td[headers$=grade][class^=level3]," +
                            " table td[headers$=range][class^=level3]");*/
                    Elements elements = docErgebnisse.select("table th[id^=row_].level2," +
                            " table td[headers$=grade].level2," +
                            " table td[headers$=range].level2");
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
                                double dMaxPoints;
                                try {
                                    dMaxPoints = Double.parseDouble(strMaxPoints);
                                } catch (Exception e) {
                                    dMaxPoints = -2;
                                }
                                if (strName.startsWith("Test")) {
                                    Test t = new Test(strName, dPoints, dMaxPoints);
                                    mModul.addTest("E-Tests", t);
                                } else if (strName.startsWith("Quiz ")) {
                                    strName = strName.replace("Quiz ", "");
                                    Test t = new Test(strName, dPoints, dMaxPoints);
                                    mModul.addTest("Quiz", t);
                                } else if (strName.startsWith("Gesamt") || strName.startsWith("Summe") || strName.startsWith("Punkte") || strName.startsWith("Klausur")) {
                                    // Nicht hinzufügen
                                } else if (strName.startsWith("Bonus")) {
                                    Test t = new Test(strName, dPoints, dMaxPoints);
                                    mModul.addTest("Bonus-Stufen", t);
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

                } catch (IOException e) {
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
