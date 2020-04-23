package de.siebes.fabian.infostudium;

import android.animation.ValueAnimator;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.animation.DecelerateInterpolator;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;

import java.util.List;
import java.util.Locale;

import static de.siebes.fabian.infostudium.Const.decimalFormat;

public class ModulView extends ConstraintLayout {

    boolean mBooShouldShowNoResult;
    private Module mModul;
    private Activity mActivity;
    private boolean mBooExpandedView = false;
    private boolean mIsLoading = false;
    private SHOWABLE_SECTION mCurSection = SHOWABLE_SECTION.LOADING;

    public ModulView(Activity activity, Module modul) {
        super(activity);
        inflate(activity, R.layout.modul_view, this);

        mModul = modul;
        mActivity = activity;

        ((TextView) findViewById(R.id.tvTitle)).setText(modul.getModulTitle());

        reloadData();

        setOnClickListeners();
    }

    public void animateHeightChange(final View v, int duration, int targetHeight) {
        int prevHeight = v.getHeight();
        ValueAnimator valueAnimator = ValueAnimator.ofInt(prevHeight, targetHeight);
        valueAnimator.setInterpolator(new DecelerateInterpolator());
        valueAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                v.getLayoutParams().height = (int) animation.getAnimatedValue();
                v.requestLayout();
            }
        });
        valueAnimator.setInterpolator(new DecelerateInterpolator());
        valueAnimator.setDuration(duration);
        valueAnimator.start();
    }

    public void reloadData() {
        if (mIsLoading) return;

        showLoading(true, null);
        ((LinearLayout) findViewById(R.id.linlayTestList)).removeAllViews();
        mModul.clearTestLists();

        reloadView();

        StorageHelper storageHelper = new StorageHelper(mActivity);
        mBooShouldShowNoResult = storageHelper.getBoolSettings(StorageHelper.SHOW_NO_RESULT, StorageHelper.SHOW_NO_RESULT_DEF_VALUE);

        WebsiteLoadingUtils
                .loadResults(mActivity, mModul,
                        new WebsiteLoadingUtils.OnFinishedListener() {
                            @Override
                            public void onFinished(Activity activity, Module modul) {
                                if (modul == null) {

                                } else {
                                    mModul = modul;
                                }
                                showLoading(false, modul);
                                reloadView();
                            }
                        });
    }

    private void reloadView() {
        final LinearLayout linlayTests = findViewById(R.id.linlayTestList);
        linlayTests.removeAllViews();

        if (!mIsLoading)
            showErrorWhenTherIsOne(mModul);

        double dOldGesPoints = mModul.getGesReachedPoints();
        double dNewGesPoints = mModul.calcNewGesReachedPoints();
        // SHOW NEW-Data Info
        if (mCurSection == SHOWABLE_SECTION.RESULTS
                && Double.compare(dOldGesPoints, dNewGesPoints) != 0) {
            // Eine Punktänderung
            findViewById(R.id.imgNew).setVisibility(VISIBLE);
            TextView tvNewPointsDiff = findViewById(R.id.tvNewPointsDiff);
            tvNewPointsDiff.setVisibility(VISIBLE);
            if (Double.compare(dOldGesPoints, dNewGesPoints) > 0) {
                tvNewPointsDiff.setText(getResources().getString(R.string.points_diff_negative, decimalFormat.format(dOldGesPoints - dNewGesPoints)));
            } else {
                tvNewPointsDiff.setText(getResources().getString(R.string.points_diff_positive, decimalFormat.format(dNewGesPoints - dOldGesPoints)));
            }

        } else {
            // Keine Punktänderung
            findViewById(R.id.imgNew).setVisibility(GONE);
            findViewById(R.id.tvNewPointsDiff).setVisibility(GONE);
        }

        // Nur die hinteren Ergebniss ohne Punkte entfernen, nicht aus der Mitte
        if (!mBooShouldShowNoResult) {
            for (TestList testList : mModul.getTestLists()) {
                int pos = 0;
                while (pos < testList.getTests().size()
                        && testList.getTests().get(pos) != null
                        && testList.getTests().get(pos).getPoints() <= 0) {
                    testList.getTests().remove(pos);
                }

            }
        }


        if (mBooExpandedView) {
            findViewById(R.id.imgExpand).setRotation(0);
            if (mModul != null) {
                for (TestList testList : mModul.getTestLists()) {
                    addViewsForTestList(testList);
                }
            }
        } else {
            findViewById(R.id.imgExpand).setRotation(270);
            if (mModul != null) {
                for (TestList testList : mModul.getTestLists()) {
                    addViewForSummary(testList);
                }
            }
        }

        linlayTests.measure(MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED),
                MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED));
        int newHeight = linlayTests.getMeasuredHeight();

        final LinearLayout linLayMsgs = findViewById(R.id.linLayMsgs);
        animateHeightChange(linlayTests, 300, newHeight);

        // Bessere Variante da Listener erst aufgerufen wird wenn Höhe bestimmt ist
        linLayMsgs.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                int newHeightLinLayMsg = 0;

                TextView tvError = findViewById(R.id.tvError);
                TextView tvLoading = findViewById(R.id.tvLoading);
                if (tvError.getVisibility() == VISIBLE)
                    newHeightLinLayMsg += tvError.getLineHeight() * tvError.getLineCount() + 2 * 16;
                if (tvLoading.getVisibility() == VISIBLE)
                    newHeightLinLayMsg += tvLoading.getLineHeight() * tvLoading.getLineCount() + 2 * 16;

                linLayMsgs.getViewTreeObserver().removeGlobalOnLayoutListener(this);

                animateHeightChange(linLayMsgs, 300, newHeightLinLayMsg);
            }
        });

    }

    private void addViewsForTestList(TestList testList) {
        StorageHelper storageHelper = new StorageHelper(mActivity);
        boolean showPoints = storageHelper.getBoolSettings(StorageHelper.SHOW_POINTS, StorageHelper.SHOW_POINTS_DEF_VALUE);
        String strNoResult = storageHelper.getStringSettings(StorageHelper.NO_RESULT_TEXT, StorageHelper.NO_RESULT_TEXT_DEF_VALUE);

        int maxWidthName = 0;
        int maxWidthProzent = 0;
        // Schleife über alle Elemente um die maximale Breite zu bestimmen
        for (int i = 0; i < testList.getTests().size(); i++) {
            LayoutInflater inflater = (LayoutInflater) mActivity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            View v = inflater.inflate(R.layout.test_view, null);
            TextView tvProzent = v.findViewById(R.id.tvProzent);
            TextView tvName = v.findViewById(R.id.tvName);

            String strName = testList.getTests().get(i).getName();
            if (testList.getTests().size() > 9) {
                strName = strName.concat("0");
            }
            tvName.setText(strName);

            String strProzent = "100%";
            if (showPoints) {
                strProzent = strProzent.concat(" (20.0/20.0)");
            }
            tvProzent.setText(strProzent);


            tvName.measure(0, 0);
            tvProzent.measure(0, 0);
            maxWidthName = Math.max(maxWidthName, tvName.getMeasuredWidth());
            maxWidthProzent = Math.max(maxWidthProzent, tvProzent.getMeasuredWidth());
        }


        // Only the header
        TestListSummaryResult testListSummaryResult = getSummaryResult(testList.getTests());
        TextView tvTitle = new TextView(mActivity);
        LinearLayout.LayoutParams llp = new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
        llp.setMargins(8, 8, 8, 8); // llp.setMargins(left, top, right, bottom);
        tvTitle.setGravity(Gravity.CENTER);
        // tvTitle.setText(String.format("%s (%d%%)", testList.getListName(), testListSummaryResult.gesProzent));
        tvTitle.setText(String.format(Locale.GERMANY, "%s %d%% (%s/%s)",
                testList.getListName(),
                testListSummaryResult.gesProzent,
                decimalFormat.format(testListSummaryResult.gesPoints),
                decimalFormat.format(testListSummaryResult.gesMaxPoints)));
        ((ViewGroup) findViewById(R.id.linlayTestList)).addView(tvTitle);

        for (Test t : testList.getTests()) {
            LayoutInflater inflater = (LayoutInflater) mActivity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            View v = inflater.inflate(R.layout.test_view, null);

            int prozent = (int) (((float) t.getPoints() / t.getMaxPoints()) * 100);

            if (t.getMaxPoints() == 0) prozent = 0;
            if (prozent < 0) prozent = 0; // Um nicht bearbeitete Tests mit 0% anzuzeigen
            String strProzent = Integer.toString(prozent) + "%";

            String strPoints;
            if (t.getPoints() < 0) {
                strPoints = strNoResult;
            } else {
                strPoints = decimalFormat.format(t.getPoints());
            }

            if (showPoints)
                strProzent = strProzent.concat(" (" + strPoints + "/" + decimalFormat.format(t.getMaxPoints()) + ")");


            TextView tvName = v.findViewById(R.id.tvName);
            TextView tvProzent = v.findViewById(R.id.tvProzent);
            ProgressBar progressPoints = v.findViewById(R.id.progressPoints);

            tvName.setText(t.getName());
            tvName.setWidth(maxWidthName);

            tvProzent.setText(strProzent);
            tvProzent.setWidth(maxWidthProzent);

            progressPoints.setMax(round(t.getMaxPoints()));
            progressPoints.setProgress(round(t.getPoints())); // Negative Zahlen sind egal, kein Unterschied in der Grafik

            if (round(t.getMaxPoints()) <= 0) {
                tvProzent.setTextColor(Color.GRAY);
                tvName.setTextColor(Color.GRAY);
            }


            ((ViewGroup) findViewById(R.id.linlayTestList)).addView(v);
        }
    }

    private void addViewForSummary(TestList testList) {
        // TestList-Title
        TestListSummaryResult testListSummaryResult = getSummaryResult(testList.getTests());
        TextView tvTitle = new TextView(mActivity);
        LinearLayout.LayoutParams llp = new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
        llp.setMargins(16, 8, 8, 8); // llp.setMargins(left, top, right, bottom);
        tvTitle.setGravity(Gravity.LEFT);
        tvTitle.setText(String.format(Locale.GERMANY, "%s %d%% (%s/%s)",
                testList.getListName(),
                testListSummaryResult.gesProzent,
                decimalFormat.format(testListSummaryResult.gesPoints),
                decimalFormat.format(testListSummaryResult.gesMaxPoints)));
        ((ViewGroup) findViewById(R.id.linlayTestList)).addView(tvTitle);
    }

    private void showLoading(boolean shouldShowLoading, @Nullable Module modul) {
        if (shouldShowLoading) {
            mIsLoading = true;
            // Lade-Ansicht
            showSection(SHOWABLE_SECTION.LOADING);
        } else {
            mIsLoading = false;
            // Ergebnis-Ansicht
            showSection(SHOWABLE_SECTION.RESULTS);
            // Tests werden nur angezeigt, wenn sie auch vorhanden sind
            showErrorWhenTherIsOne(modul);
        }
    }

    private void showErrorWhenTherIsOne(Module modul) {
        int nStrErrorText = R.string.error_showing_error_should_not_happen;
        boolean booError = false;

        if (modul == null) {
            // Fehler-Ansicht
            nStrErrorText = R.string.error;
        } else {
            switch (modul.getErrorCode()) {
                case NO_LOGIN_DATA:
                    booError = true;
                    nStrErrorText = R.string.error_no_username_pw;
                    break;
                case SERVER_NOT_AVAILABLE:
                    booError = true;
                    nStrErrorText = R.string.error_server_not_available;
                    break;
                case MAINTENANCE:
                    booError = true;
                    nStrErrorText = R.string.error_maintenance;
                    break;
                case OTHER_PROBLEMS:
                    booError = true;
                    nStrErrorText = R.string.error;
                    break;
                case NO_TESTS_FOUND:
                    if (modul.getTestLists().size() <= 0) {
                        booError = true;
                        nStrErrorText = R.string.error_no_tests;
                    }
                    break;
                case NOT_ACTIVATED:
                    booError = true;
                    nStrErrorText = R.string.error_module_not_activated;
                    break;
                case NO_ERROR:
                    booError = false;
                    break;
            }
        }

        if (booError) {
            ((TextView) findViewById(R.id.tvError)).setText(nStrErrorText);
            showSection(SHOWABLE_SECTION.ERROR);

            // Nur wenn ein anderer Fehler als keine Nutzerdaten oder keine Tests vorhanden auftritt
            if (!modul.getErrorCode().equals(ErrorCode.NO_LOGIN_DATA)
                    && !modul.getErrorCode().equals(ErrorCode.NO_TESTS_FOUND)
                    && !modul.getErrorCode().equals(ErrorCode.NOT_ACTIVATED)) {
                showSection(SHOWABLE_SECTION.FATAL_ERROR);
            }
        } else {
            showSection(SHOWABLE_SECTION.RESULTS);
        }
    }

    public void saveGesReachedPoints() {
        StorageHelper storageHelper = new StorageHelper(mActivity);
        storageHelper.updateModul_GesReachedPoints(mModul);
    }

    private void showSection(SHOWABLE_SECTION section) {
        mCurSection = section;
        findViewById(R.id.linlayTestList).setVisibility(GONE);
        findViewById(R.id.tvLoading).setVisibility(GONE);
        findViewById(R.id.imgReload).setVisibility(VISIBLE);
        findViewById(R.id.tvError).setVisibility(GONE);
        findViewById(R.id.imgShowLogcat).setVisibility(GONE);
        switch (section) {
            case RESULTS:
                findViewById(R.id.linlayTestList).setVisibility(VISIBLE);
                break;
            case LOADING:
                findViewById(R.id.tvLoading).setVisibility(VISIBLE);
                findViewById(R.id.imgReload).setVisibility(GONE);
                break;
            case FATAL_ERROR:
                findViewById(R.id.imgShowLogcat).setVisibility(VISIBLE);
            case ERROR:
                findViewById(R.id.tvError).setVisibility(VISIBLE);
                break;
        }
    }

    private void setOnClickListeners() {
        OnClickListener expandOnClickListener = new OnClickListener() {
            @Override
            public void onClick(View v) {
                mBooExpandedView = !mBooExpandedView;
                reloadView();
            }
        };
        findViewById(R.id.imgExpand).setOnClickListener(expandOnClickListener);
        findViewById(R.id.tvTitle).setOnClickListener(expandOnClickListener);

        findViewById(R.id.imgShowLogcat).setOnClickListener(showLogcatOnClickListener());

        findViewById(R.id.imgReload).setOnClickListener(reloadOnClickListener());
    }

    private View.OnClickListener showLogcatOnClickListener() {
        return new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intentLogcat = new Intent(mActivity, SendLogcatActivity.class);
                intentLogcat.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                mActivity.startActivity(intentLogcat);
            }
        };
    }

    private View.OnClickListener reloadOnClickListener() {
        return new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                reloadData();
            }
        };
    }

    private TestListSummaryResult getSummaryResult(List<Test> tests) {
        double sum = 0;
        double sumMax = 0;
        for (Test t : tests) {
            // Addiere nur positive Werte um nicht bearbeitete Tests nicht falsch zu verrechnen
            if (t.getPoints() >= 0)
                sum += t.getPoints();
            sumMax += t.getMaxPoints();
        }
        // Durchschnitt berechnen
        int gesProzent = (int) ((float) sum / (float) sumMax * 100); // In Prozent

        if (sumMax == 0) gesProzent = 0;

        return new TestListSummaryResult(gesProzent, sum, sumMax);
    }

    private int round(double zahl) {
        return (int) (zahl + 0.5);
    }

    enum SHOWABLE_SECTION {RESULTS, LOADING, ERROR, FATAL_ERROR}

    private class TestListSummaryResult {
        int gesProzent;
        double gesPoints, gesMaxPoints;

        TestListSummaryResult(int gesProzent, double gesPoints, double gesMaxPoints) {
            this.gesProzent = gesProzent;
            this.gesPoints = gesPoints;
            this.gesMaxPoints = gesMaxPoints;
        }
    }
}
