package de.siebes.fabian.infostudium.modules;

import android.app.Activity;

import de.siebes.fabian.infostudium.ErrorCode;
import de.siebes.fabian.infostudium.Module;
import de.siebes.fabian.infostudium.WebsiteLoadingUtils;

// Neue Module müssen diese Klasse erweitern und in einem Thread die Ergebnisse laden.
// Der Thread muss nacher auf dem UI Thread onFinishedListener.onFinished(activity, modul); aufrufen.
//  - Die Klasse des neuen Moduls muss in der MainActivity als ModulView erzeugt werden
//  - Die View in der MainActivity muss bei onRefresh aktualisiert werden
//  - Die Klasse muss in den WebsiteLoadingUtils als switch Option ergänzt werden

public abstract class ModuleLoading {

    Activity mActivity;
    WebsiteLoadingUtils.OnFinishedListener mOnFinishedListener;

    ModuleLoading(Activity activity, WebsiteLoadingUtils.OnFinishedListener onFinishedListener) {
        mActivity = activity;
        mOnFinishedListener = onFinishedListener;
    }

    public abstract void loadResults();

    void stopWithErrorCode(final Module modul, final ErrorCode errorCode) {
        mActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (mOnFinishedListener != null) {
                    modul.setErrorCode(errorCode);
                    mOnFinishedListener.onFinished(mActivity, modul);
                }
            }
        });
    }

}
