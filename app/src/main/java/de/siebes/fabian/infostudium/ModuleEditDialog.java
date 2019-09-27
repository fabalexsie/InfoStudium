package de.siebes.fabian.infostudium;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import java.util.Arrays;

public class ModuleEditDialog extends AlertDialog.Builder {

    Context c;
    private OnCloseListener onCloseListener;
    private int mAnzLogins;

    ModuleEditDialog(Context context, final Module modul, final boolean booNewModul) {
        super(context);
        c = context;
        final View v = getEditModulView();
        preFill(v, modul); // alte Eingaben abspeichern
        setView(v); // Anzeigen in Dialog
        setCancelable(true);
        setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                cancelDialog(dialog, modul, booNewModul);
            }
        });
        setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                cancelDialog(dialog, modul, booNewModul);
            }
        });
        setNeutralButton(R.string.delete, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                deleteModul(modul);
                if (onCloseListener != null)
                    onCloseListener.onClose(dialog);
                dialog.dismiss();
            }
        });
        setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String strTitle = ((EditText) v.findViewById(R.id.etTitle)).getText().toString();
                String strDesc = ((EditText) v.findViewById(R.id.etDesc)).getText().toString();
                String strKursId = ((EditText) v.findViewById(R.id.etKursId)).getText().toString();
                int nType = ((Spinner) v.findViewById(R.id.spinType)).getSelectedItemPosition();
                Spinner spinLogin = v.findViewById(R.id.spinLoginId);

                if (mAnzLogins <= 0) {
                    Toast.makeText(c, R.string.error_saving_module, Toast.LENGTH_SHORT).show();
                    deleteModul(modul);
                } else {
                    int loginPos = spinLogin.getSelectedItemPosition();
                    LoginData loginData = (LoginData) spinLogin.getItemAtPosition(loginPos);
                    modul.setModulTitle(strTitle);
                    modul.setModulDesc(strDesc);
                    modul.setModulType(nType);
                    modul.setModulKursId(strKursId);
                    modul.setLoginId(loginData.getId());
                    if (modul.isEmpty()) {
                        deleteModul(modul);
                    } else {
                        saveModul(modul);
                    }
                }
                if (onCloseListener != null)
                    onCloseListener.onClose(dialog);
            }
        });
    }

    private void cancelDialog(DialogInterface dialog, Module modul, boolean booNewModul) {
        if (modul.isEmpty() || booNewModul) { // Wenn Modul leer oder neu ausgewählt, dann lösche es (wieder) beim abbrechen
            deleteModul(modul);
        }
        if (onCloseListener != null)
            onCloseListener.onClose(dialog);
        dialog.dismiss();
    }

    private void preFill(View v, Module modul) {
        String strTitle = modul.getModulTitle();
        String strDesc = modul.getModulDesc();
        int nType = modul.getModulType();
        String strKursId = modul.getModulKursId();
        long nLoginDataId = modul.getLoginId();

        Spinner spinLoginId = v.findViewById(R.id.spinLoginId);
        Spinner spinType = v.findViewById(R.id.spinType);

        LoginData[] loginDatas = new StorageHelper(c).getLogins().toArray(new LoginData[]{});
        mAnzLogins = loginDatas.length;
        ArrayAdapter loginAdapter = new ArrayAdapter(c, android.R.layout.simple_spinner_item, loginDatas);
        spinLoginId.setAdapter(loginAdapter);

        String[] strArrTypes = c.getResources().getStringArray(R.array.modul_types);
        ArrayAdapter typeAdapter = new ArrayAdapter(c, android.R.layout.simple_spinner_item, strArrTypes);
        spinType.setAdapter(typeAdapter);

        ((EditText) v.findViewById(R.id.etTitle)).setText(strTitle);
        ((EditText) v.findViewById(R.id.etDesc)).setText(strDesc);
        ((Spinner) v.findViewById(R.id.spinType)).setSelection(nType);
        ((EditText) v.findViewById(R.id.etKursId)).setText(strKursId);
        int logindataPos = 0, counter = 0;
        for (LoginData loginData : loginDatas) {
            if (loginData.getId() == nLoginDataId) logindataPos = counter;
            counter++;
        }
        spinLoginId.setSelection(logindataPos);
    }

    private void saveModul(Module modul) {
        new StorageHelper(c).updateModul(modul);
    }

    private void deleteModul(Module modul) {
        new StorageHelper(c).deleteModul(modul);
    }

    private View getEditModulView() {
        LayoutInflater inflater = (LayoutInflater) c.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        return inflater.inflate(R.layout.dialog_edit_modul, null);
    }

    public AlertDialog.Builder setOnCloseListener(OnCloseListener onCloseListener) {
        this.onCloseListener = onCloseListener;
        return this;
    }

    interface OnCloseListener {
        void onClose(DialogInterface dialog);
    }
}
