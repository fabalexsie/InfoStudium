package de.siebes.fabian.infostudium.modules;

import android.app.Activity;
import android.util.Base64;
import android.util.Log;

import com.luigidragone.net.ntlm.NTLM;

import org.bouncycastle.crypto.digests.MD4Digest;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.InetAddress;
import java.util.Collections;

import javax.net.SocketFactory;
import javax.net.ssl.SSLSocket;

import de.siebes.fabian.infostudium.Const;
import de.siebes.fabian.infostudium.ErrorCode;
import de.siebes.fabian.infostudium.LoginData;
import de.siebes.fabian.infostudium.Module;
import de.siebes.fabian.infostudium.StorageHelper;
import de.siebes.fabian.infostudium.Test;
import de.siebes.fabian.infostudium.WebsiteLoadingUtils;
import okhttp3.CipherSuite;
import okhttp3.ConnectionSpec;
import okhttp3.OkHttpClient;
import okhttp3.TlsVersion;

public class TI extends ModuleLoading {

    private Module mModul;

    public TI(Activity activity, WebsiteLoadingUtils.OnFinishedListener onFinishedListener, Module modul) {
        super(activity, onFinishedListener);
        mModul = modul;
    }

    @Override
    public void loadResults() {
        if (!isActivated(mModul)) {
            return;
        }
        // else
        new Thread() {
            @Override
            public void run() {
                StorageHelper storageHelper = new StorageHelper(mActivity);
                LoginData loginData = storageHelper.getLogin(mModul);
                String strPraefixName = storageHelper.getStringSettings(StorageHelper.PRAEFIX_NAME, StorageHelper.PRAEFIX_NAME_DEF_VALUE);

                String strUser = loginData.getBenutzer();
                String strPassword = loginData.getPasswort();

                String strAccessToken;
                try {
                    strAccessToken = getAccessToken(strUser, strPassword);
                    if (strAccessToken.equals("")) // "" Fehler wurde in getAccessToken schon behandelt
                        return;
                } catch (Exception e) {
                    e.printStackTrace();
                    stopWithErrorCode(mModul, ErrorCode.OTHER_PROBLEMS);
                    return;
                }

                String strUrlLoginWithAccessToken = "https://www3.elearning.rwth-aachen.de/moodle2/login/index.php?courseId=18ws-186353&accessToken=" + strAccessToken;
                String strUrlBewertung = "https://www3.elearning.rwth-aachen.de/moodle2/grade/report/user/index.php?id=564";

                try {
                    Connection.Response resMoodleLogin = Jsoup.connect(strUrlLoginWithAccessToken)
                            .method(Connection.Method.GET)
                            .timeout(Const.TIMEOUT)
                            .execute();

                    Connection.Response resErgebnisse = Jsoup
                            .connect(strUrlBewertung)
                            .cookies(resMoodleLogin.cookies())
                            .method(Connection.Method.GET)
                            .timeout(Const.TIMEOUT)
                            .execute();

                    Document docErgebnisse = resErgebnisse.parse();

                    int col = 0;
                    String strName = "";
                    double dPoints = 0;
                    checkAllTests:
                    for (Element el : docErgebnisse.select("table th[id^=row_], table td[headers$=grade], table td[headers$=range]")) {
                        switch (col) {
                            case 0:
                                if (el.text().startsWith("Summe für den Kurs")) break checkAllTests;
                                strName = el.text().replace("Übungsblatt ", strPraefixName);
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
                                String strMaxPoints = el.text().split("–")[1];
                                double dMaxPoints;
                                try {
                                    dMaxPoints = Double.parseDouble(strMaxPoints);
                                } catch (Exception e) {
                                    dMaxPoints = -2;
                                }
                                Test t = new Test(strName, dPoints, dMaxPoints);
                                if (strName.contains("Bonus")) {
                                    mModul.addTest("Bonus-Tests", t);
                                } else {
                                    mModul.addTestOnline(t);
                                }
                                break;
                        }

                        col++;
                        col %= 3;
                    }

                    System.out.print("Fertig TI");

                } catch (IOException e) {
                    e.printStackTrace();
                    Log.e("TI", e.getMessage());
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
        }.start();
    }


    private String getAccessToken(String strUser, String strPassword) throws Exception {
        ConnectionSpec spec = new ConnectionSpec.Builder(ConnectionSpec.MODERN_TLS)
                .tlsVersions(TlsVersion.TLS_1_0)
                .cipherSuites(CipherSuite.TLS_ECDHE_RSA_WITH_AES_256_CBC_SHA)
                .build();

        OkHttpClient okHttpClient = new OkHttpClient.Builder()
                .connectionSpecs(Collections.singletonList(spec))
                .build();

        SocketFactory sslSocketFactory = okHttpClient.sslSocketFactory();// getRWTH_SSLSocketFactory(context);
        SSLSocket s = (SSLSocket) sslSocketFactory.createSocket(InetAddress.getByName("www3.elearning.rwth-aachen.de"), 443);
        s.setEnabledProtocols(new String[]{"TLSv1"});
        s.setKeepAlive(true);
        s.setReuseAddress(true);
        s.setSoTimeout(Const.TIMEOUT);
        InputStream is = s.getInputStream();
        OutputStream os = s.getOutputStream();
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(os));


        // first request
        writer.write("GET /ws18/18ws-186353/SitePages/eTests_IFrame.aspx HTTP/1.1\n");
        writer.write("Host: www3.elearning.rwth-aachen.de\n");
        writer.write("User-Agent: insomnia/6.3.2\n");
        writer.write("Accept: */*\n\n");
        writer.flush();

        // first response
        String firstLine;
        String strCookie1 = "";
        while ((firstLine = reader.readLine()) != null) {
            Log.d("First", firstLine);
            if (firstLine.startsWith("Set-Cookie")) {
                strCookie1 = firstLine.split(": ")[1].split(";")[0];
            }
            if (firstLine.contains("RequireReadOnly")) {
                break;
            }
            if (firstLine.contains("l2p-maintenance")) {
                //  http://www.l2p-maintenance.itc.rwth-aachen.de/
                stopWithErrorCode(new Module("TI"), ErrorCode.MAINTENANCE);
                return "";
            }
        }


        //Process
        byte[] secondMsg = NTLM.formatRequest("www3.elearning.rwth-aachen.de", "");
        secondMsg[13] = (byte) 0x00; // Type-1
        String secondNtlmMsg = Base64.encodeToString(secondMsg, Base64.DEFAULT);
        secondNtlmMsg = secondNtlmMsg.replace("\n", "");

        // second request
        writer.write("GET /ws18/18ws-186353/SitePages/eTests_IFrame.aspx HTTP/1.1\n");
        writer.write("Host: www3.elearning.rwth-aachen.de\n");
        writer.write("Authorization: NTLM " + secondNtlmMsg + "\n");
        writer.write("User-Agent: insomnia/6.3.2\n");
        writer.write("Cookie: " + strCookie1 + "\n");
        writer.write("Accept: */*\n\n");
        writer.flush();

        // second response
        String secondLine;
        String strWwwAuthenticate = "";
        while ((secondLine = reader.readLine()) != null) {
            Log.d("Second", secondLine);
            if (secondLine.startsWith("WWW-Authenticate:")) {
                strWwwAuthenticate = secondLine.split(": NTLM ")[1];
            }
            if (secondLine.contains("RequireReadOnly")) {
                break;
            }
        }

        // Process
        byte[] secondResponseMessage = Base64.decode(strWwwAuthenticate, Base64.DEFAULT);
        byte[] nonce = NTLM.getNonce(secondResponseMessage);
        byte[] thirdMsg = NTLM.formatResponse("www3.elearning.rwth-aachen.de",
                strUser,
                "",
                NTLM.computeLMPassword(strPassword),
                computeNTPassword(strPassword),
                nonce);
        thirdMsg[60] = (byte) 0x01; // Type-3
        thirdMsg[61] = (byte) 0x82;
        String thirdNtlmMsg = Base64.encodeToString(thirdMsg, Base64.DEFAULT);
        thirdNtlmMsg = thirdNtlmMsg.replace("\n", "");


        // third request
        writer.write("GET /ws18/18ws-186353/SitePages/eTests_IFrame.aspx HTTP/1.1\n");
        writer.write("Host: www3.elearning.rwth-aachen.de\n");
        writer.write("Authorization: NTLM " + thirdNtlmMsg + "\n");
        writer.write("User-Agent: insomnia/6.3.2\n");
        writer.write("Cookie: " + strCookie1 + "\n");
        writer.write("Accept: */*\n\n");
        writer.flush();

        // third response
        String thirdLine;
        String strAccessToken = "";
        while ((thirdLine = reader.readLine()) != null) {
            //Log.d("Third", thirdLine);
            if (thirdLine.contains("<iframe")) {
                strAccessToken = thirdLine.split("accessToken=")[1]; // Alles nach dem 'accessToken='
                break;
            }
        }

        // Process
        strAccessToken = strAccessToken.split("\"")[0]; // Alles vor dem '"'
        return strAccessToken;
    }

    /**
     * Computes the NT hashed version of a password.
     *
     * @param password the user password
     * @return the NT hashed version of the password in a 16-bytes array
     * @throws IllegalArgumentException if the supplied password is null
     */
    private static byte[] computeNTPassword(String password) throws IllegalArgumentException {
        if (password == null)
            throw new IllegalArgumentException("password : null value not allowed");
        //Gets the first 14-bytes of the UNICODE password
        int len = password.length();
        if (len > 14)
            len = 14;
        byte[] nt_pw = new byte[2 * len];
        for (int i = 0; i < len; i++) {
            char ch = password.charAt(i);
            nt_pw[2 * i] = getLoByte(ch);
            nt_pw[2 * i + 1] = getHiByte(ch);
        }

        //Return its MD4 digest as the hashed version
        MD4Digest md4 = new MD4Digest();
        md4.update(nt_pw, 0, nt_pw.length);
        byte[] md4_pw = new byte[md4.getDigestSize()];
        md4.doFinal(md4_pw, 0);
        return md4_pw;
    }

    private static byte getHiByte(char c) {
        return (byte) ((c >>> 8) & 0xFF);
    }

    private static byte getLoByte(char c) {
        return (byte) c;
    }
}
