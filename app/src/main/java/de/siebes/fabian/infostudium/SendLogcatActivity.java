package de.siebes.fabian.infostudium;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.text.Html;
import android.view.View;
import android.view.Window;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import org.jsoup.Jsoup;

import java.io.IOException;
import java.io.InputStreamReader;

public class SendLogcatActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE); // make a dialog without a titlebar
        setFinishOnTouchOutside(false); // prevent users from dismissing the dialog by tapping outside
        setContentView(R.layout.background_send_log);

        final String strLog = extractLog();
        final String strAppInfo = extractDeviceAppInfo();
        final String strDeviceName = extractDeviceName();

        final View vDialog = View.inflate(this, R.layout.dia_send_log, null);

        TextView tvLog = vDialog.findViewById(R.id.tvLog);
        TextView tvAppInfo = vDialog.findViewById(R.id.tvAppInfo);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.this_shouldnt_happen)
                .setView(vDialog)
                .setNegativeButton(R.string.close, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        finish();
                    }
                })
                .setPositiveButton(R.string.send_logcat, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(SendLogcatActivity.this, R.string.sending_logcat, Toast.LENGTH_SHORT).show();
                            }
                        });
                        new Thread() {
                            @Override
                            public void run() {
                                try {
                                    EditText et = vDialog.findViewById(R.id.etUserProblemDesc);
                                    String strUserDesc = et.getText().toString();
                                    Jsoup.connect("http://fabian.siebes.de/infostudium/logcat.php")
                                            .data("logcat", strAppInfo + "\n\n" +
                                                    "Beschreibung des Nutzers:\n"
                                                    + strUserDesc + "\n\n"
                                                    + strLog)
                                            .data("device", strDeviceName)
                                            .post();
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                                finish();
                            }
                        }.start();
                    }
                });
        builder.show();


        String strLogHtml;
        if (strLog == null) {
            strLogHtml = "Logcat konnte nicht erfasst werden.";
        } else {
            strLogHtml = strLog;
        }
        strLogHtml = markImportantInformationWithHTML(strLogHtml);
        tvLog.setText(Html.fromHtml(strLogHtml));
        tvAppInfo.setText(strAppInfo);


        final ScrollView scrollView = vDialog.findViewById(R.id.scrollView);
        scrollView.post(new Runnable() {
            @Override
            public void run() {
                scrollView.fullScroll(ScrollView.FOCUS_DOWN);
            }
        });
    }

    private String extractDeviceName() {
        String model = Build.MODEL;
        if (!model.startsWith(Build.MANUFACTURER))
            model = Build.MANUFACTURER + " " + model;

        return model;
    }

    private String extractDeviceAppInfo() {
        PackageManager manager = this.getPackageManager();
        PackageInfo info = null;
        try {
            info = manager.getPackageInfo(this.getPackageName(), 0);
        } catch (PackageManager.NameNotFoundException ignored) {
        }
        String model = extractDeviceName();

        StringBuilder stringBuilder;
        stringBuilder = new StringBuilder();
        stringBuilder.append("Android version: ").append(Build.VERSION.SDK_INT).append("\n");
        stringBuilder.append("Device: ").append(model).append("\n");
        stringBuilder.append("App version: ").append(info == null ? "(null)" : info.versionCode).append("");

        return stringBuilder.toString();
    }

    private String extractLog() {
        InputStreamReader reader = null;
        StringBuilder stringBuilder;
        try {
            /*
             * -d         - logcat is printed and closed
             * -v time    - the time is printed in the logcat
             * -t {count} - Only the last {count} entrys from logcat
             * *:d        - logcat is filtered to all tags with error level d or above (Log.d -> Log.i -> Log.w -> Log.e)
             */
            String cmd = "logcat -d -v time -t 200 *:d";

            // get input stream
            Process process = Runtime.getRuntime().exec(cmd);
            reader = new InputStreamReader(process.getInputStream());

            // write output to StringBuilder
            stringBuilder = new StringBuilder();

            char[] buffer = new char[10000];
            do {
                int n = reader.read(buffer, 0, buffer.length);
                if (n == -1)
                    break;
                stringBuilder.append(buffer, 0, n);
            } while (true);

            reader.close();

            return stringBuilder.toString();

        } catch (IOException e) {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException ignored) {
                }
            }

            // You might want to write a failure message to the log here.
            Toast.makeText(this, "Destroyed complete, so it can't save the log.", Toast.LENGTH_LONG).show();
            return null;
        }
    }

    private String markImportantInformationWithHTML(String strLog) {
        // Save the line breaks
        strLog = strLog.replace("\n", "<br \\>");

        // mark my Application in bold and ColorPrimary
        strLog = strLog.replace("de.siebes.fabian.infostudium",
                "<b><font color='#008577'>de.siebes.fabian.infostudium</font></b>");
        // mark system errors in bold and red
        strLog = strLog.replace("System.err",
                "<b><font color='#BD221B'>System.err</font></b>");
        return strLog;
    }


}
