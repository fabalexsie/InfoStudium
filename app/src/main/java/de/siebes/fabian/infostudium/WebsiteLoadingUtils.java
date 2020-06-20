package de.siebes.fabian.infostudium;

import android.app.Activity;

import de.siebes.fabian.infostudium.modules.ExercisemanagementsystemI2;
import de.siebes.fabian.infostudium.modules.MaLo;
import de.siebes.fabian.infostudium.modules.Moodle;
import de.siebes.fabian.infostudium.modules.ORAbgabesystem;
import de.siebes.fabian.infostudium.modules.Okuson;
import de.siebes.fabian.infostudium.modules.TI;

public class WebsiteLoadingUtils {

    static void loadResults(final Activity activity, final Module modul, final OnFinishedListener onFinishedListener) {
        new Thread() {
            @Override
            public void run() {
                LoginData loginData = new StorageHelper(activity).getLogin(modul);
                if (loginData.getBenutzer().equals("")
                        || loginData.getPasswort().equals("")) {
                    modul.setErrorCode(ErrorCode.NO_LOGIN_DATA);
                    activity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            onFinishedListener.onFinished(activity, modul);
                        }
                    });
                    return;
                }

                modul.setErrorCode(ErrorCode.NO_ERROR);

                int modulType = modul.getModulType();
                switch (modulType) {
                    case Module.TYPE_MOODLE:
                        new Moodle(activity, onFinishedListener, modul).loadResults();
                        break;
                    case Module.TYPE_EXERCISEMANAGEMENT:
                        new ExercisemanagementsystemI2(activity, onFinishedListener, modul).loadResults();
                        break;
                    case Module.TYPE_OKUSON:
                        new Okuson(activity, onFinishedListener, modul).loadResults();
                        break;
                    case Module.TYPE_L2P:
                        new TI(activity, onFinishedListener, modul).loadResults();
                        break;
                    case Module.TYPE_MALO:
                        new MaLo(activity, onFinishedListener, modul).loadResults();
                        break;
                    case Module.TYPE_OR_ABGABESYSTEM:
                        new ORAbgabesystem(activity, onFinishedListener, modul).loadResults();
                        break;
                }
            }
        }.start();
    }

    public abstract static class OnFinishedListener {
        public abstract void onFinished(Activity activity, Module modul);
    }

}
