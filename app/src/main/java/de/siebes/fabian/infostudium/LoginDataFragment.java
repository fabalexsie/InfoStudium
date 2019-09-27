package de.siebes.fabian.infostudium;

import android.content.DialogInterface;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SearchView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

/**
 * A placeholder fragment containing a simple view.
 */
public class LoginDataFragment extends Fragment implements SearchView.OnQueryTextListener {

    List<View> mModulViewList = new ArrayList<>();
    private FloatingActionButton mFab;

    public static LoginDataFragment newInstance(FloatingActionButton fab) {
        LoginDataFragment loginDataFragment = new LoginDataFragment();
        loginDataFragment.setFab(fab);
        return loginDataFragment;
    }

    private void setFab(FloatingActionButton fab) {
        mFab = fab;
    }


    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_settings_list_moduls_logins, container, false);

        SearchView searchView = root.findViewById(R.id.searchView);
        searchView.setOnQueryTextListener(this);

        final ScrollView scrollView = root.findViewById(R.id.scrollView);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            scrollView.setOnScrollChangeListener(new View.OnScrollChangeListener() {
                @Override
                public void onScrollChange(View v, int scrollX, int scrollY, int oldScrollX, int oldScrollY) {
                    int untereKante = scrollY + scrollView.getHeight();
                    int scrollListHeight = scrollView.getChildAt(0).getHeight();

                    if (scrollView.getHeight() < scrollListHeight // Wenn die Liste länger ist als die ScrollView
                            && oldScrollY > scrollY // und nach unten scrollen
                            && (untereKante) < scrollListHeight - 220 // und nicht auf den letzten 220 pixeln
                            || scrollY < 220) { // Oder immer auf den ersten 220 pixeln
                        mFab.show();
                    } else {
                        mFab.hide();
                    }
                }
            });
        }

        initAllViews(root);

        return root;
    }

    void initAllViews(final View root) {
        LinearLayout linearLayout = root.findViewById(R.id.linlayModulTypeViews);

        linearLayout.removeAllViews();

        StorageHelper storageHelper = new StorageHelper(getActivity());

        int count = 0;
        for (final LoginData loginData : storageHelper.getLogins()) {

            View view = View.inflate(getActivity(), R.layout.list_item_logindata, null);

            TextView tvTitle = view.findViewById(R.id.tvTitle);
            TextView tvDesc = view.findViewById(R.id.tvDesc);

            tvTitle.setText(loginData.getName());
            tvDesc.setText(loginData.getBenutzer());

            view.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    LoginDialog dialog = new LoginDialog(getActivity(), loginData);
                    dialog.setOnCloseListener(new LoginDialog.OnCloseListener() {
                        @Override
                        public void onClose(DialogInterface dialog) {
                            initAllViews(root);
                        }
                    });
                    dialog.show();
                }
            });

            if (count + 1 == storageHelper.getLogins().size()) { // Der letzte
                view.measure(0, 0);
                view.setMinimumHeight(view.getMeasuredHeight() + 220);
            }

            view.setTag(loginData);
            linearLayout.addView(view);
            mModulViewList.add(view);
            count++;
        }
    }

    private void showSearchResults(String strSearch) {
        for (View loginDataView : mModulViewList) {
            String strName = ((LoginData) loginDataView.getTag()).getName();
            String strBenutzer = ((LoginData) loginDataView.getTag()).getBenutzer();

            if (myStrContains(strName, strSearch)
                    || myStrContains(strBenutzer, strSearch)) {
                loginDataView.setVisibility(View.VISIBLE);
            } else {
                loginDataView.setVisibility(View.GONE);
            }

        }
    }

    private boolean myStrContains(String strKey, String strSearch) {
        return strKey != null && strKey.toLowerCase().contains(strSearch.toLowerCase());
    }

    /**
     * Called when the user submits the query. This could be due to a key press on the
     * keyboard or due to pressing a submit button.
     * The listener can override the standard behavior by returning true
     * to indicate that it has handled the submit request. Otherwise return false to
     * let the SearchView handle the submission by launching any associated intent.
     *
     * @param query the query text that is to be submitted
     * @return true if the query has been handled by the listener, false to let the
     * SearchView perform the default action.
     */
    @Override
    public boolean onQueryTextSubmit(String query) {
        showSearchResults(query);
        return true;
    }

    /**
     * Called when the query text is changed by the user.
     *
     * @param newText the new content of the query text field.
     * @return false if the SearchView should perform the default action of showing any
     * suggestions if available, true if the action was handled by the listener.
     */
    @Override
    public boolean onQueryTextChange(String newText) {
        showSearchResults(newText);
        return true;
    }
}