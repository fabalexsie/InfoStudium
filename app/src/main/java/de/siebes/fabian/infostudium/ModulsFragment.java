package de.siebes.fabian.infostudium;

import android.content.DialogInterface;
import android.os.Build;
import android.os.Bundle;
import androidx.annotation.NonNull;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import androidx.fragment.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SearchView;
import android.widget.Switch;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

/**
 * A placeholder fragment containing a simple view.
 */
public class ModulsFragment extends Fragment implements SearchView.OnQueryTextListener {

    List<View> mModulViewList = new ArrayList<>();
    private FloatingActionButton mFab;

    private View mRoot;

    public static ModulsFragment newInstance(FloatingActionButton fab) {
        ModulsFragment modulsFragment = new ModulsFragment();
        modulsFragment.setFab(fab);
        return modulsFragment;
    }

    private void setFab(FloatingActionButton fab) {
        mFab = fab;
    }

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        mRoot = inflater.inflate(R.layout.fragment_settings_list_moduls_logins, container, false);

        SearchView searchView = mRoot.findViewById(R.id.searchView);
        searchView.setOnQueryTextListener(this);

        final ScrollView scrollView = mRoot.findViewById(R.id.scrollView);
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

        initAllViews();

        return mRoot;
    }

    public void initAllViews() {
        LinearLayout linearLayout = mRoot.findViewById(R.id.linlayModulTypeViews);

        linearLayout.removeAllViews();

        StorageHelper storageHelper = new StorageHelper(getActivity());

        int count = 0;
        for (final Module modul : storageHelper.getModuls()) {
            final View view = View.inflate(getActivity(), R.layout.list_item_module, null);

            TextView tvModulTitle = view.findViewById(R.id.tvTitle);
            TextView tvModulDesc = view.findViewById(R.id.tvDesc);
            final Switch swiActivate = view.findViewById(R.id.swiActivate);
            ImageView imgSortUp = view.findViewById(R.id.imgSortUp);

            tvModulTitle.setText(modul.getModulTitle());

            LoginData loginData = new StorageHelper(getActivity()).getLogin(modul);
            String strModulInfo = loginData.getName()
                    + " (" + loginData.getBenutzer()
                    + ") @" + modul.getModulTypeReadable(getActivity());
            if (modul.getModulDesc() != null && modul.getModulDesc().trim().equals("")) {
                tvModulDesc.setText(strModulInfo);
            } else {
                tvModulDesc.setText(modul.getModulDesc() + "\n" + strModulInfo);
            }

            view.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    ModuleEditDialog dialog = new ModuleEditDialog(getActivity(), modul, false);
                    dialog.setOnCloseListener(new ModuleEditDialog.OnCloseListener() {
                        @Override
                        public void onClose(DialogInterface dialog) {
                            initAllViews();
                        }
                    });
                    dialog.show();
                }
            });
            swiActivate.setChecked(modul.isActivated());
            swiActivate.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    setModulActivatedAndUpdateView(modul, isChecked, view);
                }
            });
            setModulActivatedAndUpdateView(modul, modul.isActivated(), view);

            if (count == 0) {
                imgSortUp.setVisibility(View.INVISIBLE);
            } else {
                imgSortUp.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        new StorageHelper(getActivity()).bubbleModuleUp(modul);
                        initAllViews();
                    }
                });
            }

            if (count + 1 == storageHelper.getModuls().size()) { // Der letzte
                view.measure(0,0);
                view.setMinimumHeight(view.getMeasuredHeight() + 220);
            }

            view.setTag(modul);
            linearLayout.addView(view);
            mModulViewList.add(view);
            count++;
        }
    }

    private void setModulActivatedAndUpdateView(Module modul, boolean isChecked, View view) {
        modul.setActivated(isChecked);
        new StorageHelper(getActivity()).updateModul(modul);
        if(isChecked){
            view.findViewById(R.id.tvDesc).setVisibility(View.VISIBLE);
            view.findViewById(R.id.imgSortUp).setVisibility(View.VISIBLE);
        }else {
            view.findViewById(R.id.tvDesc).setVisibility(View.GONE);
            view.findViewById(R.id.imgSortUp).setVisibility(View.GONE);
        }
    }

    private void showSearchResults(String strSearch) {
        boolean wasFirstAlready = false;
        for (View modulViewItem : mModulViewList) {
            String strModulViewTitle = ((Module) modulViewItem.getTag()).getModulTitle();
            String strModulViewDesc = ((Module) modulViewItem.getTag()).getModulDesc();

            if (myStrContains(strModulViewTitle, strSearch)
                    || myStrContains(strModulViewDesc, strSearch)) {
                modulViewItem.setVisibility(View.VISIBLE);
            } else {
                modulViewItem.setVisibility(View.GONE);
            }

            if (strSearch.equals("")) {
                if (wasFirstAlready) {
                    modulViewItem.findViewById(R.id.imgSortUp).setVisibility(View.VISIBLE);
                } else {
                    wasFirstAlready = true;
                    modulViewItem.findViewById(R.id.imgSortUp).setVisibility(View.INVISIBLE);
                }
            } else {
                modulViewItem.findViewById(R.id.imgSortUp).setVisibility(View.INVISIBLE);
            }

        }
    }

    private boolean myStrContains(String strModulType, String strSearch) {
        return strModulType != null && strModulType.toLowerCase().contains(strSearch.toLowerCase());
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