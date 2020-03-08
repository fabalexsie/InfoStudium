package de.siebes.fabian.infostudium;

import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.crypto.spec.SecretKeySpec;

public class StorageHelper extends SQLiteOpenHelper {

    // public consts
    public static final String PRAEFIX_NAME = "praefix_name";
    public static final String PRAEFIX_NAME_DEF_VALUE = "Übung ";
    static final String SHOW_NO_RESULT = "show_no_results";
    static final boolean SHOW_NO_RESULT_DEF_VALUE = true;
    static final String LAST_NEWS_VERSION = "last_news_version";
    static final String LAST_NEWS_VERSION_DEF_VALUE = "7";
    static final String SHOW_POINTS = "show_points";
    static final boolean SHOW_POINTS_DEF_VALUE = true;
    static final String NO_RESULT_TEXT = "no_result";
    static final String NO_RESULT_TEXT_DEF_VALUE = "--";
    static final String MASTER_PASS = "master_pass";
    static final String MASTER_PASS_DEF_VALUE = "";
    static final String ALLOWED_TO_LOG_ACTIVITY = "allowed_to_log_activity";
    static final boolean ALLOWED_TO_LOG_ACTIVITY_DEF_VALUE = false;
    static final String LAST_PREFILLED_MODULES_VERSION = "last_prefilled_modules_version";
    static final String LAST_PREFILLED_MODULES_VERSION_DEF_VALUE = "0";
    private static final String MODULE_TYPE_ACTIVATED = "module_type_activated_";
    // MODULE_TYPE_ACTIVATED def Value depends on module_type
    private static final String MOODLE_SHORTEN_DATE = "shorten_date_in_moodle"; // Entfernt
    private static final String RESCUED_OLD_LOGINS = "rescued_old_logins"; // Zur Umstellung benötigt
    private static final boolean RESCUED_OLD_LOGINS_DEF_VALUE = false; // Zur Umstellung benötigt
    // private consts
    private static final String SHARED_PREFS = "prefs";

    // SQLite DB
    private static final String DB_NAME = "db";
    private static final int DB_VERISON = 3;

    private static final String DB_COLUMN_ID = "_id";
    private static final String DB_TABLE_MODULS = "db_moduls";
    private static final String DB_COLUMN_MODUL_POS = "pos";
    private static final String DB_COLUMN_MODUL_TITLE = "modul_name";
    private static final String DB_COLUMN_MODUL_DESC = "modul_desc";
    private static final String DB_COLUMN_MODUL_KURSID = "modul_kursid";
    private static final String DB_COLUMN_MODUL_TYPE = "login_type";
    private static final String DB_COLUMN_SEMESTER = "semester";
    private static final String DB_COLUMN_MODUL_LOGIN_ID = "login_id";
    private static final String DB_COLUMN_MODUL_ACTIVATED = "activated";
    private static final String DB_COLUMN_MODUL_GES_REACHED_POINTS = "ges_reached_points";
    private static final String DB_CREATE_TABLE_ACTIVE_MODULS = "CREATE TABLE " + DB_TABLE_MODULS + "(" +
            DB_COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
            DB_COLUMN_MODUL_POS + " INTEGER, " +
            DB_COLUMN_MODUL_TITLE + " TEXT, " +
            DB_COLUMN_MODUL_DESC + " TEXT, " +
            DB_COLUMN_MODUL_KURSID + " TEXT, " +
            DB_COLUMN_MODUL_TYPE + " INTEGER, " +
            DB_COLUMN_MODUL_LOGIN_ID + " INTEGER, " +
            DB_COLUMN_MODUL_ACTIVATED + " INTEGER," +
            DB_COLUMN_MODUL_GES_REACHED_POINTS + " REAL DEFAULT 0)";
    private static final String DB_TABLE_PREFILLED_MODULS = "db_prefilled_moduls";
    private static final String DB_CREATE_TABLE_PREFILLED_MODULS = "CREATE TABLE " + DB_TABLE_PREFILLED_MODULS + "(" +
            DB_COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
            DB_COLUMN_MODUL_TITLE + " TEXT, " +
            DB_COLUMN_MODUL_DESC + " TEXT, " +
            DB_COLUMN_MODUL_KURSID + " TEXT, " +
            DB_COLUMN_SEMESTER + " TEXT, " +
            DB_COLUMN_MODUL_TYPE + " INTEGER)";
    private static final String DB_TABLE_LOGIN_DATA = "db_login_data";
    private static final String DB_COLUMN_LOGIN_NAME = "name";
    private static final String DB_COLUMN_USERNAME = "username";
    private static final String DB_COLUMN_PASSWORD = "password";
    private static final String DB_COLUMN_SALT = "salt";
    private static final String DB_CREATE_TABLE_LOGIN_DATA = "CREATE TABLE " + DB_TABLE_LOGIN_DATA + "(" +
            DB_COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
            DB_COLUMN_LOGIN_NAME + " TEXT, " +
            DB_COLUMN_USERNAME + " TEXT, " +
            DB_COLUMN_PASSWORD + " TEXT, " +
            DB_COLUMN_SALT + " TEXT)";


    private static String mStrMasterpass = "";
    private SharedPreferences sp;
    private Context mContext;

    public StorageHelper(Context c) {
        super(c, DB_NAME, null, DB_VERISON);
        mContext = c;
        sp = c.getSharedPreferences(SHARED_PREFS, Context.MODE_PRIVATE);
    }

    static void showDialogDeleteAllPasswords(final Context context,
                                             final OnSubmitListener onSubmitListener,
                                             final DialogInterface.OnClickListener onNegativeButtonClickedListener) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(R.string.delete_passwords_title)
                .setMessage(R.string.delete_passwords_desc)
                .setPositiveButton(R.string.delete_passwords, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        StorageHelper storageHelper = new StorageHelper(context);
                        storageHelper.deleteAllPasswords();
                        if (onSubmitListener != null) {
                            onSubmitListener.onSubmit();
                        }
                    }
                })
                .setNegativeButton(android.R.string.cancel, onNegativeButtonClickedListener)
                .setCancelable(false)
                .show();
    }

    /**
     * @return if the storageHelper knows the Masterpass so it can decrypt the passwords
     */
    boolean knowsMasterpass() {
        return !isMasterpassSet() || !mStrMasterpass.equals("");
    }

    /**
     * @return if the user has safed a masterpassword for de/encrypting the other passwords
     */
    boolean isMasterpassSet() {
        String strHash = getStringSettings(MASTER_PASS, MASTER_PASS_DEF_VALUE);
        return !strHash.equals("");
    }

    boolean checkMasterpass(String strMasterpass) {
        if (!isMasterpassSet()) return true;
        String strHashOrigMasterpass = getStringSettings(MASTER_PASS, MASTER_PASS_DEF_VALUE);
        String strHashToCheckMasterpass = PasswordSecurity.hash(strMasterpass);
        if (strHashOrigMasterpass.equals(strHashToCheckMasterpass)) {
            mStrMasterpass = strMasterpass;
            return true;
        } else {
            return false;
        }

    }

    boolean saveMasterpass(String strOldMasterpass, String strNewMasterpass) {
        if (checkMasterpass(strOldMasterpass)) {
            // Alle Passwörter auslesen
            List<LoginData> loginDatas = getLogins();

            // Masterpasswort speichern
            if (strNewMasterpass.equals("")) {
                saveSettings(MASTER_PASS, "");
            } else {
                saveSettings(MASTER_PASS, PasswordSecurity.hash(strNewMasterpass));
            }
            mStrMasterpass = strNewMasterpass;

            // Alle Passwörter speichern
            boolean booSucces = true;
            for (LoginData loginData : loginDatas) {
                booSucces = updateLoginData(loginData);
                if (!booSucces) break;
            }

            if (!booSucces) {// Passwortändern rückgängig machen
                saveMasterpass(mStrMasterpass, "");
                return false;
            }
            return true;
        } else {
            return false;
        }
    }

    List<Module> getPrefilledModules() {
        List<Module> list = new ArrayList<>();

        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.query(DB_TABLE_PREFILLED_MODULS, null, null,
                null, null, null, null);
        if (cursor.moveToFirst()) {
            do {
                list.add(cursorToPrefilledModul(cursor));
            } while (cursor.moveToNext());
        }

        cursor.close();
        return list;
    }

    void setPrefilledModules(List<Module> moduleList) {
        SQLiteDatabase db = getWritableDatabase();
        db.delete(DB_TABLE_PREFILLED_MODULS, null, null);
        for (Module module : moduleList) {
            ContentValues cv = getPrefilledModuleCV(module);
            db.insert(DB_TABLE_PREFILLED_MODULS, null, cv);
        }
    }

    List<String> getPrefilledSemesters() {
        List<String> list = new ArrayList<>();

        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.query(DB_TABLE_PREFILLED_MODULS, null, null,
                null, null, null, null);
        if (cursor.moveToLast()) {
            do {
                String strSemester = cursor.getString(cursor.getColumnIndex(DB_COLUMN_SEMESTER));
                if (strSemester.equals(""))
                    strSemester = mContext.getResources().getString(R.string.all_semesters);
                if (!list.contains(strSemester)) { // SQLite group by
                    list.add(strSemester);
                }
            } while (cursor.moveToPrevious());
        }

        cursor.close();
        return list;
    }

    public LoginData getLogin(Module modul) {
        Cursor cursor = getReadableDatabase().query(DB_TABLE_LOGIN_DATA, null, DB_COLUMN_ID + "=" + modul.getLoginId(), null, null, null, null);
        LoginData loginData = new LoginData();
        if (cursor.moveToFirst()) {
            loginData = cursorToLogin(cursor);
        }
        cursor.close();
        return loginData;
    }

    private void deleteAllPasswords() {
        // Masterpasswort entfernen
        saveSettings(MASTER_PASS, "");
        mStrMasterpass = "";

        // Alle Accounts entfernen
        getWritableDatabase().delete(DB_TABLE_LOGIN_DATA, null, null);
    }


    public List<Module> getModuls() {
        List<Module> list = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.query(DB_TABLE_MODULS, null, null,
                null, null, null, DB_COLUMN_MODUL_POS + " ASC");
        if (cursor.moveToFirst()) {
            do {
                list.add(cursorToModul(cursor));
            } while (cursor.moveToNext());
        }

        cursor.close();
        return list;
    }

    public long addModule(Module module) {
        module.setPos(getModuls().size());

        ContentValues cv = getModuleCV(module);

        return getWritableDatabase().insert(DB_TABLE_MODULS, null, cv);
    }

    public void updateModul(Module modul) {
        ContentValues cv = getModuleCV(modul);

        getWritableDatabase().update(DB_TABLE_MODULS, cv, DB_COLUMN_ID + "=" + modul.getId(), null);
    }

    public void updateModul_GesReachedPoints(Module modul) {
        if (modul.calcNewGesReachedPoints() <= 0)
            return; // Nur Speichern, wenn Punkte erreicht und geladen

        ContentValues cv = new ContentValues();
        cv.put(DB_COLUMN_MODUL_GES_REACHED_POINTS, modul.calcNewGesReachedPoints());
        //cv.put(DB_COLUMN_MODUL_GES_REACHED_POINTS, 219);

        getWritableDatabase().update(DB_TABLE_MODULS, cv, DB_COLUMN_ID + "=" + modul.getId(), null);
    }

    public void bubbleModuleUp(Module moduleGoingUp) {
        Module moduleGoingDown = getModuleForPos(moduleGoingUp.getPos() - 1);
        if (moduleGoingDown == null) return;

        moduleGoingUp.setPos(moduleGoingUp.getPos() - 1);
        moduleGoingDown.setPos(moduleGoingDown.getPos() + 1);
        updateModul(moduleGoingDown);
        updateModul(moduleGoingUp);
    }

    public Module getModuleForPos(int pos) {
        Module modul = null;

        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.query(DB_TABLE_MODULS, null, DB_COLUMN_MODUL_POS + "=" + String.valueOf(pos),
                null, null, null, null);
        if (cursor.moveToFirst()) {
            modul = cursorToModul(cursor);
        }
        cursor.close();

        return modul;

    }

    public Module getModule(long id) {
        Module modul = null;

        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.query(DB_TABLE_MODULS, null, DB_COLUMN_ID + "=" + String.valueOf(id),
                null, null, null, null);
        if (cursor.moveToFirst()) {
            modul = cursorToModul(cursor);
        }
        cursor.close();

        return modul;

    }

    public void deleteModul(Module module) {
        getWritableDatabase().delete(DB_TABLE_MODULS, DB_COLUMN_ID + "=" + module.getId(), null);
        getWritableDatabase().execSQL("UPDATE " + DB_TABLE_MODULS
                + " SET " + DB_COLUMN_MODUL_POS + "=" + DB_COLUMN_MODUL_POS + "-1"
                + " WHERE " + DB_COLUMN_MODUL_POS + ">" + module.getPos());
    }

    @NonNull
    private ContentValues getModuleCV(Module module) {
        ContentValues cv = new ContentValues();
        cv.put(DB_COLUMN_MODUL_POS, module.getPos());
        cv.put(DB_COLUMN_MODUL_TITLE, module.getModulTitle());
        cv.put(DB_COLUMN_MODUL_DESC, module.getModulDesc());
        cv.put(DB_COLUMN_MODUL_TYPE, String.valueOf(module.getModulType()));
        cv.put(DB_COLUMN_MODUL_KURSID, module.getModulKursId());
        cv.put(DB_COLUMN_MODUL_LOGIN_ID, module.getLoginId());
        cv.put(DB_COLUMN_MODUL_ACTIVATED, module.isActivated() ? 1 : 0);
        return cv;
    }

    @NonNull
    private ContentValues getPrefilledModuleCV(Module module) {
        ContentValues cv = new ContentValues();
        cv.put(DB_COLUMN_MODUL_TITLE, module.getModulTitle());
        cv.put(DB_COLUMN_MODUL_DESC, module.getModulDesc());
        cv.put(DB_COLUMN_MODUL_TYPE, String.valueOf(module.getModulType()));
        cv.put(DB_COLUMN_MODUL_KURSID, module.getModulKursId());
        cv.put(DB_COLUMN_SEMESTER, module.getSemester());
        return cv;
    }

    @NonNull
    private Module cursorToModul(Cursor cursor) {
        Module modul = new Module(cursor.getString(cursor.getColumnIndex(DB_COLUMN_MODUL_TITLE)));

        modul.setId(cursor.getInt(cursor.getColumnIndex(DB_COLUMN_ID)));
        modul.setPos(cursor.getInt(cursor.getColumnIndex(DB_COLUMN_MODUL_POS)));
        modul.setModulDesc(cursor.getString(cursor.getColumnIndex(DB_COLUMN_MODUL_DESC)));
        modul.setModulType(cursor.getInt(cursor.getColumnIndex(DB_COLUMN_MODUL_TYPE)));
        modul.setModulKursId(cursor.getString(cursor.getColumnIndex(DB_COLUMN_MODUL_KURSID)));
        modul.setLoginId(cursor.getInt(cursor.getColumnIndex(DB_COLUMN_MODUL_LOGIN_ID)));
        modul.setActivated(cursor.getInt(cursor.getColumnIndex(DB_COLUMN_MODUL_ACTIVATED)) == 1);
        modul.setGesReachedPoints(cursor.getDouble(cursor.getColumnIndex(DB_COLUMN_MODUL_GES_REACHED_POINTS)));
        return modul;
    }

    @NonNull
    private Module cursorToPrefilledModul(Cursor cursor) {
        Module modul = new Module();

        modul.setId(cursor.getInt(cursor.getColumnIndex(DB_COLUMN_ID)));
        modul.setModulTitle(cursor.getString(cursor.getColumnIndex(DB_COLUMN_MODUL_TITLE)));
        modul.setModulDesc(cursor.getString(cursor.getColumnIndex(DB_COLUMN_MODUL_DESC)));
        modul.setModulType(cursor.getInt(cursor.getColumnIndex(DB_COLUMN_MODUL_TYPE)));
        modul.setModulKursId(cursor.getString(cursor.getColumnIndex(DB_COLUMN_MODUL_KURSID)));
        modul.setSemester(cursor.getString(cursor.getColumnIndex(DB_COLUMN_SEMESTER)));
        return modul;
    }


    List<LoginData> getLogins() {
        List<LoginData> list = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.query(DB_TABLE_LOGIN_DATA, null, null,
                null, null, null, null);
        if (cursor.moveToFirst()) {
            do {
                list.add(cursorToLogin(cursor));
            } while (cursor.moveToNext());
        }

        cursor.close();
        return list;
    }

    public long addLoginData(LoginData loginData) {
        ContentValues cv = null;
        try {
            cv = getLoginCV(loginData);
        } catch (GeneralSecurityException | UnsupportedEncodingException e) {
            e.printStackTrace();
            return -1;
        }

        return getWritableDatabase().insert(DB_TABLE_LOGIN_DATA, null, cv);
    }

    public boolean updateLoginData(LoginData loginData) {

        ContentValues cv = null;
        try {
            cv = getLoginCV(loginData);
        } catch (GeneralSecurityException | UnsupportedEncodingException e) {
            e.printStackTrace();
            return false;
        }

        getWritableDatabase().update(DB_TABLE_LOGIN_DATA, cv, DB_COLUMN_ID + "=" + loginData.getId(), null);
        return true;
    }

    public LoginData getLoginData(long id) {
        LoginData LoginData = null;

        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.query(DB_TABLE_LOGIN_DATA, null, DB_COLUMN_ID + "=" + String.valueOf(id),
                null, null, null, null);
        if (cursor.moveToFirst()) {
            LoginData = cursorToLogin(cursor);
        }
        cursor.close();

        return LoginData;

    }

    public void deleteLoginData(long id) {
        getWritableDatabase().delete(DB_TABLE_LOGIN_DATA, DB_COLUMN_ID + "=" + id, null);
    }

    @NonNull
    ContentValues getLoginCV(LoginData loginData) throws GeneralSecurityException, UnsupportedEncodingException {
        ContentValues cv = new ContentValues();
        cv.put(DB_COLUMN_LOGIN_NAME, loginData.getName());
        cv.put(DB_COLUMN_USERNAME, loginData.getBenutzer());


        String strSalt = PasswordSecurity.generateSalt(6);
        String strPasswortCrypt;
        if (mStrMasterpass.equals("")) {
            strSalt = "";
            strPasswortCrypt = loginData.getPasswort();
        } else {
            SecretKeySpec secretKey = PasswordSecurity.createSecretKey(mStrMasterpass.toCharArray(), strSalt.getBytes());
            strPasswortCrypt = PasswordSecurity.encrypt(loginData.getPasswort(), secretKey);
        }


        cv.put(DB_COLUMN_PASSWORD, strPasswortCrypt);
        cv.put(DB_COLUMN_SALT, strSalt);
        return cv;
    }

    @NonNull
    private LoginData cursorToLogin(Cursor cursor) {
        int id = cursor.getInt(cursor.getColumnIndex(DB_COLUMN_ID));
        String strName = cursor.getString(cursor.getColumnIndex(DB_COLUMN_LOGIN_NAME));
        String strBenutzer = cursor.getString(cursor.getColumnIndex(DB_COLUMN_USERNAME));
        String strPasswordCrypt = cursor.getString(cursor.getColumnIndex(DB_COLUMN_PASSWORD));
        String strSalt = cursor.getString(cursor.getColumnIndex(DB_COLUMN_SALT));

        String strPassword = decryptPassword(strPasswordCrypt, strSalt);

        return new LoginData(id, strName, strBenutzer, strPassword);
    }

    private String decryptPassword(String strPasswordCrypt, String strSalt) {
        String strPassword = "";
        try {
            if (!strSalt.equals("") && !mStrMasterpass.equals("")) {
                SecretKeySpec secretKey = PasswordSecurity.createSecretKey(mStrMasterpass.toCharArray(), strSalt.getBytes());
                strPassword = PasswordSecurity.decrypt(strPasswordCrypt, secretKey);
            } else {
                strPassword = strPasswordCrypt;
            }
        } catch (IOException | GeneralSecurityException e) {
            e.printStackTrace();
        }
        return strPassword;
    }


    void removeSettings(String strKey) {
        SharedPreferences.Editor e = sp.edit();
        e.remove(strKey);
        e.apply();
    }

    void saveSettings(String strKey, boolean booValue) {
        SharedPreferences.Editor e = sp.edit();
        e.putBoolean(strKey, booValue);
        e.apply();
    }

    public boolean getBoolSettings(String strKey, boolean booDefValue) {
        return sp.getBoolean(strKey, booDefValue);
    }

    void saveSettings(String strKey, String strValue) {
        SharedPreferences.Editor e = sp.edit();
        e.putString(strKey, strValue);
        e.apply();
    }

    public String getStringSettings(String strKey, String strDefValue) {
        return sp.getString(strKey, strDefValue);
    }


    public String getTableModulsAsString() {
        StringBuilder builder = new StringBuilder();
        builder.append(strToLength("id", 4))
                .append(strToLength("pos", 4))
                .append(strToLength("name", 15))
                .append(strToLength("type", 4))
                .append(strToLength("kursid", 10))
                .append(strToLength("loginid", 7))
                .append(strToLength("activated", 10))
                .append(strToLength("ges_reached", 13))
                .append("\n");
        for (Module modul : getModuls()) {
            builder.append(strToLength(String.valueOf(modul.getId()), 4))
                    .append(strToLength(String.valueOf(modul.getPos()), 4))
                    .append(strToLength(modul.getModulTitle(), 15))
                    .append(strToLength(String.valueOf(modul.getModulType()), 4))
                    .append(strToLength(modul.getModulKursId(), 10))
                    .append(strToLength(String.valueOf(modul.getLoginId()), 7))
                    .append(strToLength(String.valueOf(modul.isActivated()), 10))
                    .append(strToLength(String.valueOf(modul.getGesReachedPoints()), 13))
                    .append("\n");
        }
        System.out.print("\n\n" + builder.toString());
        return builder.toString();
    }

    public String getTableLoginDataAsString() {
        StringBuilder builder = new StringBuilder();
        builder.append(strToLength("name", 20))
                .append(strToLength("username", 10))
                .append(strToLength("password", 15))
                .append("\n");
        for (LoginData loginData : getLogins()) {
            builder.append(strToLength(loginData.getName(), 20))
                    .append(strToLength(loginData.getBenutzer(), 10))
                    .append(strToLength(loginData.getPasswort(), 15))
                    .append("\n");
        }
        System.out.print("\n\n" + builder.toString());
        return builder.toString();
    }

    private String strToLength(String str, int length) {
        if (str == null) str = "<null>";
        StringBuilder builder = new StringBuilder(str);
        while (builder.length() < length) {
            builder.append(" ");
        }
        builder.append(" | ");
        return builder.toString();
    }

    /**
     * Called when the database is created for the first time. This is where the
     * creation of tables and the initial population of the tables should happen.
     *
     * @param db The database.
     */
    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(DB_CREATE_TABLE_ACTIVE_MODULS);
        db.execSQL(DB_CREATE_TABLE_LOGIN_DATA);
        db.execSQL(DB_CREATE_TABLE_PREFILLED_MODULS);
    }

    public void rescueOldLogins() {
        // RESCUE OLD VARIABLES
        String[] strModules_Old = {"AfI", "DS", "Progra", "TI", "BuS", "DsAl", "FoSAP", "LA", "Stocha"};
        SharedPreferences.Editor e = sp.edit();
        for (String str_old_module : strModules_Old) {
            // COPY
            rescue_login(str_old_module,
                    sp.getString(str_old_module + "_benutzer", ""),
                    sp.getString(str_old_module + "_passwort", ""),
                    sp.getString(str_old_module + "_salt", ""),
                    sp.getBoolean(str_old_module + "_activated", false));

            // DELETE
            e.remove(str_old_module + "_benutzer");
            e.remove(str_old_module + "_passwort");
            e.remove(str_old_module + "_salt");
            e.remove(str_old_module + "_activated");
        }
        e.putBoolean(RESCUED_OLD_LOGINS, true);

        e.apply();
    }

    boolean hasRescuedOldLogins() {
        return sp.getBoolean(RESCUED_OLD_LOGINS, RESCUED_OLD_LOGINS_DEF_VALUE);
    }

    private void rescue_login(String str_old_module, String strBenutzer, String strPassCrypt, String strSalt, boolean strActivated) {
        if (strBenutzer != null && !strBenutzer.equals("")
                && strPassCrypt != null && !strPassCrypt.equals("")) {
            LoginData loginData = new LoginData();
            loginData.setName("Login: " + str_old_module);
            loginData.setBenutzer(strBenutzer);
            loginData.setPasswort(decryptPassword(strPassCrypt, strSalt));
            long loginId = addLoginData(loginData);

            int posForModule = Arrays.asList(Const.getPrefilledModuleNames(null)).indexOf(str_old_module);
            if (posForModule >= 0) {
                Module module = Const.getPrefilledModul(null, posForModule);
                if (strActivated) {
                    module.setActivated(true);
                } else {
                    module.setActivated(false);
                }
                module.setLoginId(loginId);
                addModule(module);
            }
        }
    }

    /**
     * Called when the database needs to be upgraded. The implementation
     * should use this method to drop tables, add tables, or do anything else it
     * needs to upgrade to the new schema version.
     *
     * <p>
     * The SQLite ALTER TABLE documentation can be found
     * <a href="http://sqlite.org/lang_altertable.html">here</a>. If you add new columns
     * you can use ALTER TABLE to insert them into a live table. If you rename or remove columns
     * you can use ALTER TABLE to rename the old table, then create the new table and then
     * populate the new table with the contents of the old table.
     * </p><p>
     * This method executes within a transaction.  If an exception is thrown, all changes
     * will automatically be rolled back.
     * </p>
     *
     * @param db         The database.
     * @param oldVersion The old database version.
     * @param newVersion The new database version.
     */
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 2) {
            addColumnToTable(db, DB_TABLE_MODULS, DB_COLUMN_MODUL_GES_REACHED_POINTS, "REAL DEFAULT 0");
        }
        if (oldVersion < 3) {
            db.execSQL(DB_CREATE_TABLE_PREFILLED_MODULS);
            removeSettings(StorageHelper.MOODLE_SHORTEN_DATE);
        }
    }

    private void addColumnToTable(SQLiteDatabase db, String strTable, String strColName, String strTypeAndDefault) {
        Cursor cursor = db.rawQuery("SELECT * FROM " + strTable, null);
        if (cursor.getColumnIndex(strColName) < 0) {
            // Wenn die Spalte noch nicht existiert
            db.execSQL("ALTER TABLE " + strTable + " ADD COLUMN " + strColName + " " + strTypeAndDefault);
        }
        cursor.close();
    }

    public boolean isModuleActivated(int typeMoodle) {
        if(typeMoodle == Module.TYPE_MOODLE) { // Moodle standardmäßig nicht aktiviert
            return getBoolSettings(MODULE_TYPE_ACTIVATED + typeMoodle, false);
        } else {
            return getBoolSettings(MODULE_TYPE_ACTIVATED + typeMoodle, true);
        }
    }

    void activateModule(int typeMoodle) {
        saveSettings(MODULE_TYPE_ACTIVATED + typeMoodle, true);
    }

    void deactivateModule(int typeMoodle) {
        saveSettings(MODULE_TYPE_ACTIVATED + typeMoodle, false);
    }

    boolean hasSecretMoodleLoginData() {
        List<LoginData> loginList = getLogins();
        for (LoginData loginData : loginList) {
            if (loginData.getBenutzer().equals("Moodle")
                    && loginData.getPasswort().equals("ActivateSecretMoodleAsDeveloper"))
                return true;
        }
        return false;
    }

    public interface OnSubmitListener {
        void onSubmit();
    }

}
