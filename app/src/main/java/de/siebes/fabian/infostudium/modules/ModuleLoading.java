package de.siebes.fabian.infostudium.modules;

import android.app.Activity;

import de.siebes.fabian.infostudium.ErrorCode;
import de.siebes.fabian.infostudium.Module;
import de.siebes.fabian.infostudium.StorageHelper;
import de.siebes.fabian.infostudium.WebsiteLoadingUtils;

// Neue Module müssen diese Klasse erweitern und in einem Thread die Ergebnisse laden.
// Der Thread muss nacher auf dem UI Thread onFinishedListener.onFinished(activity, modul); aufrufen.
//  - Die Klasse muss in den WebsiteLoadingUtils als switch Option ergänzt werden
//  - Der Klassenname muss in strings.xml im array eingefügt werden

public abstract class ModuleLoading {

    Activity mActivity;
    WebsiteLoadingUtils.OnFinishedListener mOnFinishedListener;

    ModuleLoading(Activity activity, WebsiteLoadingUtils.OnFinishedListener onFinishedListener) {
        mActivity = activity;
        mOnFinishedListener = onFinishedListener;
    }

    public abstract void loadResults();

    boolean isActivated(Module mModul){
        StorageHelper storageHelper = new StorageHelper(mActivity);
        if(!storageHelper.isModuleActivated(mModul.getModulType())){
            stopWithErrorCode(mModul, ErrorCode.NOT_ACTIVATED);
            return false;
        }
        return true;
    }

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
