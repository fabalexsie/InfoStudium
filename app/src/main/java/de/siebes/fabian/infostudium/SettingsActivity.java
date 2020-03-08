package de.siebes.fabian.infostudium;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.constraint.ConstraintLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;


public class SettingsActivity extends AppCompatActivity {

    StorageHelper storageHelper;

    // Views
    Switch swiShowPoints, swiLogActivity;
    TextView tvMasterpassValue, tvPraefixValue, tvNoResultValue, tvLogActivity;
    CheckBox checkShowNoResultText;
    ConstraintLayout conMasterpass, conActiveModuls, conPraefixText, conNoResultText;
    ConstraintLayout conFeedback, conShareApp, conDisclaimer, conActivateModuleInstruction;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);

        storageHelper = new StorageHelper(this);

        findViews();

        setOnClickListeners();

        loadValues();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if(item.getItemId() == android.R.id.home){
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void findViews() {
        conMasterpass = findViewById(R.id.conMasterpass);
        tvMasterpassValue = findViewById(R.id.tvMasterpassValue);
        conActiveModuls = findViewById(R.id.conActiveModuls);
        swiShowPoints = findViewById(R.id.swiSetShowPoints);
        conPraefixText = findViewById(R.id.conPraefix);
        tvPraefixValue = findViewById(R.id.tvPraefixValue);
        conNoResultText = findViewById(R.id.conNoResultText);
        tvNoResultValue = findViewById(R.id.tvNoResultTextValue);
        checkShowNoResultText = findViewById(R.id.checkShowNoResult);
        swiLogActivity = findViewById(R.id.swiLogActivity);
        tvLogActivity = findViewById(R.id.tvLogActivity);
        conFeedback = findViewById(R.id.conFeedback);
        conShareApp = findViewById(R.id.conShareApp);
        conDisclaimer = findViewById(R.id.conDisclaimer);
        conActivateModuleInstruction = findViewById(R.id.conActivateModuleInstruction);
    }

    private void setOnClickListeners() {
        conMasterpass.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final StorageHelper storageHelper = new StorageHelper(SettingsActivity.this);
                if (storageHelper.isMasterpassSet()) {
                    new MasterPassDialog(SettingsActivity.this)
                            .setOnSubmitListener(new MasterPassDialog.OnSubmitListener() {
                                @Override
                                public boolean onSubmit(final String strOldMasterpass) {
                                    return changeMasterpassDialog(strOldMasterpass);
                                }
                            })
                            .setNeutralButton(R.string.delete, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    StorageHelper.showDialogDeleteAllPasswords(SettingsActivity.this,
                                            null,
                                            null);
                                }
                            })
                            .show();
                } else {
                    final String strOldMasterpass = "";
                    changeMasterpassDialog(strOldMasterpass);
                }
            }
        });

        conActiveModuls.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showSelectModulsActivity(SettingsActivity.this);
            }
        });

        swiShowPoints.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                storageHelper.saveSettings(StorageHelper.SHOW_POINTS, isChecked);
            }
        });

        conPraefixText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showTextInputDialog(tvPraefixValue.getText().toString(), new OnTextEnteredListener() {
                    @Override
                    public void onEntered(String newText) {
                        storageHelper.saveSettings(StorageHelper.PRAEFIX_NAME, newText.trim() + " ");
                        tvPraefixValue.setText(newText);
                    }
                });
            }
        });

        checkShowNoResultText.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                onCheckShowNoResult_UpdateOtherView(isChecked);
            }
        });

        tvLogActivity.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showActivityLogGraph();
            }
        });

        swiLogActivity.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                storageHelper.saveSettings(StorageHelper.ALLOWED_TO_LOG_ACTIVITY, isChecked);
            }
        });

        conFeedback.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showFeedbackForm();
            }
        });

        conShareApp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                shareApp();
            }
        });

        conDisclaimer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showDisclaimer();
            }
        });

        conActivateModuleInstruction.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showActivateModuleInstruction();
            }
        });
    }

    static void showSelectModulsActivity(Context context) {
        Intent intent = new Intent(context, SettingsEditModulsLoginsActivity.class);
        context.startActivity(intent);
    }


    /**
     * @param strOldMasterpass
     * @return if the oldMasterpass was correct
     */
    private boolean changeMasterpassDialog(final String strOldMasterpass) {
        final StorageHelper storageHelper = new StorageHelper(this);
        if (storageHelper.checkMasterpass(strOldMasterpass)) {
            showTextInputDialog("", new OnTextEnteredListener() {
                @Override
                public void onEntered(String strNewMasterpass) {
                    boolean sucessfulchanged = storageHelper.saveMasterpass(strOldMasterpass, strNewMasterpass);
                    if (sucessfulchanged) {
                        Toast.makeText(SettingsActivity.this, R.string.changed_masterpass, Toast.LENGTH_SHORT).show();
                        if(strNewMasterpass.equals("")) {
                            tvMasterpassValue.setText(R.string.masterpass_not_set);
                        } else {
                            tvMasterpassValue.setText(R.string.masterpass_set);
                        }
                    } else {
                        Toast.makeText(SettingsActivity.this, R.string.error_saving_masterpass, Toast.LENGTH_LONG).show();
                    }
                }
            });
            return true;
        }
        return false;
    }

    @NonNull
    private View.OnClickListener noResultText_ClickListener() {
        return new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showTextInputDialog(tvNoResultValue.getText().toString(), new OnTextEnteredListener() {
                    @Override
                    public void onEntered(String newText) {
                        storageHelper.saveSettings(StorageHelper.NO_RESULT_TEXT, newText);
                        tvNoResultValue.setText(newText);
                    }
                });
            }
        };
    }

    private void loadValues() {
        boolean booShowPoints = storageHelper.getBoolSettings(StorageHelper.SHOW_POINTS, StorageHelper.SHOW_POINTS_DEF_VALUE);
        swiShowPoints.setChecked(booShowPoints);

        String strMasterpass = storageHelper.getStringSettings(StorageHelper.MASTER_PASS, StorageHelper.MASTER_PASS_DEF_VALUE);
        if(strMasterpass.equals("")){
            tvMasterpassValue.setText(R.string.masterpass_not_set);
        }else {
            tvMasterpassValue.setText(R.string.masterpass_set);
        }

        String strPraefix = storageHelper.getStringSettings(StorageHelper.PRAEFIX_NAME, StorageHelper.PRAEFIX_NAME_DEF_VALUE);
        tvPraefixValue.setText(strPraefix);

        boolean showNoResults = storageHelper.getBoolSettings(StorageHelper.SHOW_NO_RESULT, StorageHelper.SHOW_NO_RESULT_DEF_VALUE);
        checkShowNoResultText.setChecked(showNoResults);
        onCheckShowNoResult_UpdateOtherView(showNoResults);

        String strNoResult = storageHelper.getStringSettings(StorageHelper.NO_RESULT_TEXT, StorageHelper.NO_RESULT_TEXT_DEF_VALUE);
        tvNoResultValue.setText(strNoResult);

        boolean booLogActivity = storageHelper.getBoolSettings(StorageHelper.ALLOWED_TO_LOG_ACTIVITY, StorageHelper.ALLOWED_TO_LOG_ACTIVITY_DEF_VALUE);
        swiLogActivity.setChecked(booLogActivity);

        if (storageHelper.isModuleActivated(Module.TYPE_MOODLE)) {
            conActivateModuleInstruction.setVisibility(View.VISIBLE);
        }else {
            conActivateModuleInstruction.setVisibility(View.GONE);
        }
    }

    private void onCheckShowNoResult_UpdateOtherView(boolean isChecked) {
        if (!isChecked) {
            // Disable other View
            for (int i = 0; i < conNoResultText.getChildCount(); i++) {
                View v = conNoResultText.getChildAt(i);
                if (v instanceof TextView) {
                    TextView tv = (TextView) v;
                    tv.setTextColor(Color.GRAY);
                }
            }
            conNoResultText.setOnClickListener(null);
        } else {
            // Enable Other View
            for (int i = 0; i < conNoResultText.getChildCount(); i++) {
                View v = conNoResultText.getChildAt(i);
                if (v instanceof TextView) {
                    TextView tv = (TextView) v;
                    tv.setTextColor(Color.LTGRAY);
                }
            }
            conNoResultText.setOnClickListener(noResultText_ClickListener());
        }

        storageHelper.saveSettings(StorageHelper.SHOW_NO_RESULT, isChecked);
    }

    private void showFeedbackForm() {
        showWebsiteInBrowser("https://goo.gl/forms/tKnzVxEjqIQ1PrPJ2");
    }

    private void showActivityLogGraph() {
        showWebsiteInBrowser("http://fabian.siebes.de/infostudium/log_activity_chart.php");
    }

    private void showWebsiteInBrowser(String strUrl) {
        Intent webIntent = new Intent(Intent.ACTION_VIEW);
        webIntent.setData(Uri.parse(strUrl));
        startActivity(Intent.createChooser(webIntent, getString(R.string.choose_browser)));
    }

    private void shareApp() {
        Intent intentShareApp = new Intent(Intent.ACTION_SEND);
        intentShareApp.setType("text/plain");
        intentShareApp.putExtra(Intent.EXTRA_TEXT, getString(R.string.share_app_text));
        startActivity(Intent.createChooser(intentShareApp, getString(R.string.choose_share_app)));
    }

    private void showDisclaimer() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.disclaimer)
                .setMessage(R.string.disclaimer_text)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                })
                .setNeutralButton(R.string.more_information, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        final StorageHelper storageHelper = new StorageHelper(SettingsActivity.this);
                        if(storageHelper.hasSecretMoodleLoginData()){
                            showTextInputDialog("", new OnTextEnteredListener() {
                                @Override
                                public void onEntered(String newText) {
                                    if(newText.toLowerCase().equals("true")){
                                        if(storageHelper.isModuleActivated(Module.TYPE_MOODLE)){
                                            storageHelper.deactivateModule(Module.TYPE_MOODLE);
                                            conActivateModuleInstruction.setVisibility(View.GONE);
                                            Toast.makeText(SettingsActivity.this, R.string.deactivated, Toast.LENGTH_SHORT).show();
                                        }else {
                                            storageHelper.activateModule(Module.TYPE_MOODLE);
                                            conActivateModuleInstruction.setVisibility(View.VISIBLE);
                                            Toast.makeText(SettingsActivity.this, R.string.activated, Toast.LENGTH_SHORT).show();
                                        }
                                    }else{
                                        showWebsiteInBrowser("http://fabian.siebes.de/infostudium/disclaimer.html");
                                    }
                                }
                            });
                        }else {
                            showWebsiteInBrowser("http://fabian.siebes.de/infostudium/disclaimer.html");
                        }
                    }
                })
                .show();
    }

    private void showActivateModuleInstruction() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.activateModuleInstruction)
                .setMessage(R.string.activateModuleInstruction_text)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                })
                .setNegativeButton(R.string.deactivateMoodleModule, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        storageHelper.deactivateModule(Module.TYPE_MOODLE);
                        conActivateModuleInstruction.setVisibility(View.GONE);
                    }
                })
                .show();
    }

    private void showTextInputDialog(String oldValue, final OnTextEnteredListener onTextEnteredListener) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View v = inflater.inflate(R.layout.dialog_change_setting, null);
        final EditText et = v.findViewById(R.id.etValue);
        et.setText(oldValue);

        builder.setView(v)
                .setCancelable(true)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        onTextEnteredListener.onEntered(et.getText().toString());
                    }
                }).show();
    }

    private interface OnTextEnteredListener {
        void onEntered(String newText);
    }
}
