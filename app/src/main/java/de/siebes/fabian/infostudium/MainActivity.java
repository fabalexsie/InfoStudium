package de.siebes.fabian.infostudium;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.text.Html;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Anleitung zum Hinzufügen neuer Module siehe ModuleLoading im package modules
 */
public class MainActivity extends AppCompatActivity implements SwipeRefreshLayout.OnRefreshListener, View.OnClickListener {

    private SwipeRefreshLayout mSwipeRefreshLayout;
    private List<ModulView> mModulViews = new ArrayList<>();

    private boolean mReloadOnResume = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
            @Override
            public void uncaughtException(Thread t, Throwable e) {
                handleUncaughtException(t, e);
            }
        });

        setContentView(R.layout.activity_main);
        findViewById(R.id.btn_enter_masterpass).setOnClickListener(this);
        findViewById(R.id.btn_select_moduls).setOnClickListener(this);

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.INTERNET) == PackageManager.PERMISSION_GRANTED)
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.INTERNET}, 1);

        mSwipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);
        mSwipeRefreshLayout.setOnRefreshListener(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mReloadOnResume) {
            mReloadOnResume = false;
            StorageHelper storageHelper = new StorageHelper(this);
            if (!storageHelper.hasRescuedOldLogins()) {
                storageHelper.rescueOldLogins();
            }
            if (storageHelper.knowsMasterpass()) {
                addModulViews();
                onRefresh(); // onRefresh checksForNews()
            } else {
                showMasterpassDialog();
                checkForNews();
            }
        }
    }

    private void addModulViews() {
        LinearLayout linlayModuls = findViewById(R.id.linlayModuls);
        linlayModuls.removeAllViews();
        mModulViews.clear();

        StorageHelper storageHelper = new StorageHelper(this);

        List<Module> moduleList = storageHelper.getModuls();
        if (moduleList.size() > 0) {
            showMainInfo(MainInfoType.MODULS);
            for (Module modul : moduleList) {
                if (modul.isActivated()) {
                    ModulView modulView = new ModulView(this, modul);
                    linlayModuls.addView(modulView);
                    mModulViews.add(modulView);
                }
            }
        } else {
            showMainInfo(MainInfoType.NO_MODULS_SELECTED);
        }
    }

    private void showMainInfo(MainInfoType infoType) {
        LinearLayout linlayModuls = findViewById(R.id.linlayModuls);
        LinearLayout linlayNoMasterpassEntered = findViewById(R.id.linlayNoMasterpassEntered);
        LinearLayout linlayNoModulsSelected = findViewById(R.id.linlayNoModulSelected);
        switch (infoType) {
            case MODULS:
                linlayModuls.setVisibility(View.VISIBLE);
                linlayNoMasterpassEntered.setVisibility(View.GONE);
                linlayNoModulsSelected.setVisibility(View.GONE);
                break;
            case NO_MASTERPASS_ENTERED:
                linlayModuls.setVisibility(View.GONE);
                linlayNoMasterpassEntered.setVisibility(View.VISIBLE);
                linlayNoModulsSelected.setVisibility(View.GONE);
                break;
            case NO_MODULS_SELECTED:
                linlayModuls.setVisibility(View.GONE);
                linlayNoMasterpassEntered.setVisibility(View.GONE);
                linlayNoModulsSelected.setVisibility(View.VISIBLE);
                break;
        }
    }

    private void checkForNews() {
        new Thread() {
            @Override
            public void run() {
                StorageHelper storageHelper = new StorageHelper(MainActivity.this);
                String strLastNewsDate = storageHelper.getStringSettings(StorageHelper.LAST_NEWS_VERSION, StorageHelper.LAST_NEWS_VERSION_DEF_VALUE);
                String strLastPrefilledModulesVersion =
                        storageHelper.getStringSettings(StorageHelper.LAST_PREFILLED_MODULES_VERSION,
                                StorageHelper.LAST_PREFILLED_MODULES_VERSION_DEF_VALUE);
                boolean booAllowedToLogActivity = storageHelper.getBoolSettings(StorageHelper.ALLOWED_TO_LOG_ACTIVITY, StorageHelper.ALLOWED_TO_LOG_ACTIVITY_DEF_VALUE);

                Document document;
                try {
                    Connection connection = Jsoup.connect("http://fabian.siebes.de/infostudium/news.php")
                            .data("last_version", strLastNewsDate);

                    if (booAllowedToLogActivity) {
                        String versionName = "-1";
                        try {
                            PackageInfo pInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
                            versionName = pInfo.versionName;
                        } catch (PackageManager.NameNotFoundException e) {
                            e.printStackTrace();
                        }

                        connection.data("app_version", versionName);
                        connection.data("android_version", String.valueOf(Build.VERSION.RELEASE));
                    }

                    document = connection.data("allowed_to_log", Boolean.toString(booAllowedToLogActivity))
                            .post();
                    final String strNews = document.body().toString();

                    String strNewestNewsDate = document.getElementById("versionnr").attr("version");
                    storageHelper.saveSettings(StorageHelper.LAST_NEWS_VERSION, strNewestNewsDate);
                    String strNewestPrefilledModulesVersion = document.getElementById("modules_versionnr").attr("version");
                    storageHelper.saveSettings(StorageHelper.LAST_PREFILLED_MODULES_VERSION, strNewestPrefilledModulesVersion);

                    if (!strLastNewsDate.equals(strNewestNewsDate)) { // Wenn es eine neuere (andere) News-Version gibt
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                createNewsDialog(strNews);
                            }
                        });
                    }

                    if(!strLastPrefilledModulesVersion.equals(strNewestPrefilledModulesVersion)){
                        loadNewPrefilledModules();
                    }

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            mSwipeRefreshLayout.setRefreshing(false);
                        }
                    });
                } catch (final IOException e) {
                    e.printStackTrace();
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(MainActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
                            mSwipeRefreshLayout.setRefreshing(false);
                        }
                    });
                }
            }

            private void createNewsDialog(String strNews) {
                AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                builder.setTitle(R.string.news)
                        .setMessage(Html.fromHtml(strNews))
                        .setIcon(R.mipmap.ic_launcher)
                        .setPositiveButton(R.string.close, null);
                builder.show();
            }
        }.start();
    }

    /* nicht aus main thread aufrufen */
    private void loadNewPrefilledModules() {
        try {
            Document doc = Jsoup.connect("http://fabian.siebes.de/infostudium/prefilled_modules.json").ignoreContentType(true).get();
            String strJson = doc.text();
            JSONArray jsonArray = new JSONArray(strJson);

            List<Module> prefilledModuleList =new ArrayList<>();

            for(int i=0; i< jsonArray.length(); i++){
                JSONObject jsonObjectModule = jsonArray.getJSONObject(i);
                Module module = new Module();
                module.setModulTitle(jsonObjectModule.getString("title"));
                module.setModulDesc(jsonObjectModule.getString("desc"));
                module.setModulType(jsonObjectModule.getInt("type"));
                module.setModulKursId(jsonObjectModule.getString("kursid"));
                module.setSemester(jsonObjectModule.getString("semester"));
                prefilledModuleList.add(module);
            }

            StorageHelper storageHelper = new StorageHelper(this);
            storageHelper.setPrefilledModules(prefilledModuleList);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (JSONException e) {
            e.printStackTrace();
        }

    }

    private void showMasterpassDialog() {
        final StorageHelper storageHelper = new StorageHelper(this);
        new MasterPassDialog(this)
                .setOnSubmitListener(new MasterPassDialog.OnSubmitListener() {
                    @Override
                    public boolean onSubmit(String strPass) {
                        if (storageHelper.checkMasterpass(strPass)) {
                            addModulViews();
                            return true;
                        } else {
                            return false;
                        }
                    }
                })
                .setNeutralButton(R.string.delete, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        StorageHelper.showDialogDeleteAllPasswords(MainActivity.this,
                                new StorageHelper.OnSubmitListener() {
                                    @Override
                                    public void onSubmit() {
                                        onResume();
                                    }
                                },
                                new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        showMainInfo(MainInfoType.NO_MASTERPASS_ENTERED);
                                    }
                                });
                    }
                })
                .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        showMainInfo(MainInfoType.NO_MASTERPASS_ENTERED);
                    }
                })
                .setOnCancelListener(new DialogInterface.OnCancelListener() {
                    @Override
                    public void onCancel(DialogInterface dialog) {
                        showMainInfo(MainInfoType.NO_MASTERPASS_ENTERED);
                    }
                })
                .show();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.item_settings:
                mReloadOnResume = true;
                Intent settingsIntent = new Intent(this, SettingsActivity.class);
                startActivity(settingsIntent);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onRefresh() {
        for (ModulView modulView : mModulViews) {
            reload(modulView);
        }

        checkForNews();
        // mSwipeRefreshLayout.setRefreshing(false); // in checkForNews
    }

    void reload(ModulView modulViewOld) {
        if (modulViewOld != null) {
            modulViewOld.reloadData();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        for(ModulView moduleView : mModulViews){
            moduleView.saveGesReachedPoints();
        }
    }

    /**
     * Called when a view has been clicked.
     *
     * @param v The view that was clicked.
     */
    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_enter_masterpass:
                showMasterpassDialog();
                break;
            case R.id.btn_select_moduls:
                SettingsActivity.showSelectModulsActivity(this);
                mReloadOnResume = true;
                break;
        }
    }

    private void handleUncaughtException(Thread t, Throwable e) {
        e.printStackTrace();

        Intent i = new Intent();
        i.setAction("de.siebes.fabian.infostudium.SEND_LOG");
        i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(i);

        finish();
        System.exit(1);  //kills the crashed app
    }

    private enum MainInfoType {MODULS, NO_MASTERPASS_ENTERED, NO_MODULS_SELECTED}
}
