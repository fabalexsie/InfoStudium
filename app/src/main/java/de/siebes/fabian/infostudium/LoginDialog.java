package de.siebes.fabian.infostudium;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

public class LoginDialog extends AlertDialog.Builder {

    Context c;
    private OnCloseListener onCloseListener;

    LoginDialog(Context context, final LoginData loginData) {
        super(context);
        c = context;
        final View v = getLoginView();
        preFill(v, loginData); // alte Eingaben abspeichern
        setView(v); // Anzeigen in Dialog
        setCancelable(true);
        setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if(loginData.isEmpty()) {
                    deleteLogin(loginData);
                }
                dialog.dismiss();
            }
        });
        setNeutralButton(R.string.delete, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                deleteLogin(loginData);
                if (onCloseListener != null)
                    onCloseListener.onClose(dialog);
                dialog.dismiss();
            }
        });
        setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String strName = ((EditText) v.findViewById(R.id.etName)).getText().toString();
                String strBenutzer = ((EditText) v.findViewById(R.id.etBenutzer)).getText().toString();
                String strPasswort = ((EditText) v.findViewById(R.id.etPasswort)).getText().toString();
                loginData.setName(strName);
                loginData.setBenutzer(strBenutzer);
                loginData.setPasswort(strPasswort);
                if(loginData.isEmpty()) {
                    deleteLogin(loginData);
                } else {
                    saveLogin(loginData);
                }
                if (onCloseListener != null)
                    onCloseListener.onClose(dialog);
            }
        });
        setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                if(loginData.isEmpty()) {
                    deleteLogin(loginData);
                }
                if (onCloseListener != null)
                    onCloseListener.onClose(dialog);
            }
        });
    }

    private void preFill(View v, LoginData loginData) {
        String strName = loginData.getName();
        String strBenutzer = loginData.getBenutzer();
        String strPasswort = loginData.getPasswort();

        ((TextView) v.findViewById(R.id.tvTitle)).setText(c.getResources().getString(R.string.logindata));
        ((EditText) v.findViewById(R.id.etName)).setText(strName);
        ((EditText) v.findViewById(R.id.etBenutzer)).setText(strBenutzer);
        ((EditText) v.findViewById(R.id.etPasswort)).setText(strPasswort);
    }

    private void saveLogin(LoginData loginData) {
        if (!new StorageHelper(c).updateLoginData(loginData)) {
            Toast.makeText(c, R.string.error_saving_password, Toast.LENGTH_SHORT).show();
        }
    }

    private void deleteLogin(LoginData loginData) {
        new StorageHelper(c).deleteLoginData(loginData.getId());
    }

    private View getLoginView() {
        LayoutInflater inflater = (LayoutInflater) c.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        return inflater.inflate(R.layout.dialog_login, null);
    }

    public AlertDialog.Builder setOnCloseListener(OnCloseListener onCloseListener) {
        this.onCloseListener = onCloseListener;
        return this;
    }

    interface OnCloseListener {
        void onClose(DialogInterface dialog);
    }
}
