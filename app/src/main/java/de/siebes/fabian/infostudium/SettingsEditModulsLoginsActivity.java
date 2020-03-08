package de.siebes.fabian.infostudium;

import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.SearchView;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.viewpager.widget.PagerAdapter;
import androidx.viewpager.widget.ViewPager;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.tabs.TabLayout;

import java.util.ArrayList;
import java.util.List;

public class SettingsEditModulsLoginsActivity extends AppCompatActivity implements ViewPager.OnPageChangeListener {

    private FloatingActionButton mFab;

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings_edit_moduls_logins);

        mFab = findViewById(R.id.fab);

        final SectionsPagerAdapter sectionsPagerAdapterEditModuls = new SectionsPagerAdapter(this, getSupportFragmentManager(), mFab);
        final ViewPager viewPager = findViewById(R.id.view_pager);
        viewPager.setAdapter(sectionsPagerAdapterEditModuls);
        viewPager.addOnPageChangeListener(this);
        TabLayout tabs = findViewById(R.id.tabs);
        tabs.setupWithViewPager(viewPager);

        mFab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View view) {
                switch (viewPager.getCurrentItem()) {
                    case 0:
                        List<LoginData> loginDatas = new StorageHelper(SettingsEditModulsLoginsActivity.this).getLogins();
                        if (loginDatas.size() <= 0) {
                            Snackbar.make(view, R.string.add_login_first, Snackbar.LENGTH_LONG)
                                    .addCallback(new Snackbar.Callback() {
                                        @Override
                                        public void onDismissed(Snackbar transientBottomBar, int event) {
                                            super.onDismissed(transientBottomBar, event);
                                            // todo nicht schön aber funktioniert
                                            reloadFragmentPage(viewPager, 1);
                                            onClick(view);
                                        }
                                    })
                                    .show();
                        } else {
                            showPrefilledModulesDialog(viewPager);
                        }
                        break;
                    case 1:
                        LoginData loginData = new LoginData();
                        long loginId = new StorageHelper(SettingsEditModulsLoginsActivity.this).addLoginData(loginData);
                        loginData.setId(loginId);
                        LoginDialog loginDialog = new LoginDialog(SettingsEditModulsLoginsActivity.this, loginData);
                        loginDialog.setOnCloseListener(new LoginDialog.OnCloseListener() {
                            @Override
                            public void onClose(DialogInterface dialog) {
                                reloadFragmentPage(viewPager, 1);
                            }
                        });
                        loginDialog.show();
                        break;
                }
            }
        });

        findViewById(R.id.imgBack).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }

    private void showPrefilledModulesDialog(final ViewPager viewPager) {
        final AlertDialog.Builder builder = new AlertDialog.Builder(this)
                .setTitle(R.string.modules)
                .setCancelable(true);

        LayoutInflater inflater = (LayoutInflater) this.getSystemService(LAYOUT_INFLATER_SERVICE);
        View viewDialog = inflater.inflate(R.layout.dia_choose_prefilled_module, null);

        ListView lvPrefilledModules = viewDialog.findViewById(R.id.lvPrefilledModules);
        StorageHelper storageHelper = new StorageHelper(this);

        final List<Module> moduleList_forFiltering = storageHelper.getPrefilledModules();
        final MyPrefilledModuleAdapter myPrefilledModuleAdapter
                = new MyPrefilledModuleAdapter(this, moduleList_forFiltering);
        lvPrefilledModules.setAdapter(myPrefilledModuleAdapter);

        Spinner spinSemester = viewDialog.findViewById(R.id.spinSemester);
        final String[] strPrefilledSemesters = storageHelper.getPrefilledSemesters().toArray(new String[]{});
        ArrayAdapter prefilledSemestersAdapter
                = new ArrayAdapter(this, android.R.layout.simple_spinner_item, strPrefilledSemesters);
        spinSemester.setAdapter(prefilledSemestersAdapter);
        spinSemester.setSelection(1);
        spinSemester.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                myPrefilledModuleAdapter.filterSemester(strPrefilledSemesters[position]);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        final SearchView searchView = viewDialog.findViewById(R.id.searchView);
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                myPrefilledModuleAdapter.filter(query);
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                myPrefilledModuleAdapter.filter(newText);
                return true;
            }
        });

        builder.setView(viewDialog);
        final AlertDialog alertDialog = builder.show();

        lvPrefilledModules.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, final View view, int position, long id) {
                Module modul = moduleList_forFiltering.get(position);

                modul.setActivated(true);

                long modulId = new StorageHelper(SettingsEditModulsLoginsActivity.this).addModule(modul);
                modul.setId(modulId);

                if (modul.getModulTitle().equals("Benutzerdefiniert")) { // Wenn "Benutzerdefiniert" oder "Custom"
                    modul.setModulTitle(""); // Benutzerdefiniert ohne Titel
                    modul.setModulDesc(""); // Benutzerdefiniert ohne Desc
                }

                ModuleEditDialog dialog = new ModuleEditDialog(SettingsEditModulsLoginsActivity.this, modul, true);
                dialog.setOnCloseListener(new ModuleEditDialog.OnCloseListener() {
                    @Override
                    public void onClose(DialogInterface dialog) {
                        //((ModulsFragment)((SectionsPagerAdapter)viewPager.getAdapter()).getFragment(0)).initAllViews();
                        // das gleiche wie oben mit null-Checks
                        PagerAdapter adapter = viewPager.getAdapter();
                        if (adapter instanceof SectionsPagerAdapter) {
                            SectionsPagerAdapter sectionPageAdapter = ((SectionsPagerAdapter) adapter);
                            if (sectionPageAdapter.getCount() > 0) {
                                Fragment fragment = sectionPageAdapter.getFragment(0);
                                if (fragment instanceof ModulsFragment) {
                                    ((ModulsFragment) fragment).initAllViews();
                                }
                            }
                        }
                    }
                });
                dialog.show();

                alertDialog.dismiss();
            }
        });
    }

    void reloadFragmentPage(ViewPager viewPager, int position) {
        SectionsPagerAdapter adapter = new SectionsPagerAdapter(SettingsEditModulsLoginsActivity.this,
                getSupportFragmentManager(), mFab);
        viewPager.setAdapter(adapter);
        viewPager.setCurrentItem(position);
    }

    @Override
    public void onPageScrolled(int position, float v, int i1) {

    }

    @Override
    public void onPageSelected(int position) {
        mFab.show();
    }

    @Override
    public void onPageScrollStateChanged(int i) {

    }

    class MyPrefilledModuleAdapter extends ArrayAdapter<Module> {
        private LayoutInflater inflater;
        private List<Module> modulList;
        private List<Module> filteredModulList;

        private String mFilter = "", mFilterSemester = "";

        /**
         * Constructor
         *
         * @param context                The current context.
         * @param modulList_forFiltering The modulList to represent in the ListView.
         */
        public MyPrefilledModuleAdapter(@NonNull Context context, @NonNull List<Module> modulList_forFiltering) {
            super(context, 0, modulList_forFiltering);
            inflater = getLayoutInflater();
            filteredModulList = modulList_forFiltering;
            this.modulList = new ArrayList<>();
            this.modulList.addAll(modulList_forFiltering);
        }

        @NonNull
        @Override
        public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
            final ViewHolder holder;
            if (convertView == null) {
                holder = new ViewHolder();
                convertView = inflater.inflate(R.layout.list_item_prefilled_modules, null);
                // Locate the TextViews in listview_item.xml
                holder.tvModuleTitle = convertView.findViewById(R.id.moduleTitle);
                holder.tvModuleDesc = convertView.findViewById(R.id.moduleDesc);
                convertView.setTag(holder);
            } else {
                holder = (ViewHolder) convertView.getTag();
            }
            // Set the results into TextViews
            holder.tvModuleTitle.setText(filteredModulList.get(position).getModulTitle());
            holder.tvModuleDesc.setText(filteredModulList.get(position).getModulDesc());

            return convertView;
        }

        void filter(String filter) {
            mFilter = filter;
            filteredModulList.clear();
            if (filter.equals("")
                    && (mFilterSemester.equals("") || mFilterSemester.equals(getString(R.string.all_semesters)))) {
                filteredModulList.addAll(modulList);
            } else {
                for (Module module : modulList) {
                    if (myContains(module.getSemester(), mFilterSemester)) { // Im richtigen Semester
                        if (myContains(module.getModulTitle(), filter) // Und Suchkriterien passen zu einem der Felder
                                || myContains(module.getModulDesc(), filter)
                                || myContains(module.getSemester(), filter)) {
                            filteredModulList.add(module);
                        }
                    }
                }
                // Benutzerdefiniert immer anzeigen
                Module modBenutzerdef = modulList.get(modulList.size() - 1);
                if (!filteredModulList.contains(modBenutzerdef)) {
                    filteredModulList.add(modBenutzerdef);
                }
            }
            notifyDataSetChanged();
        }

        public void filterSemester(String strSemester) {
            mFilterSemester = strSemester;
            filter(mFilter);
        }

        @Override
        public int getCount() {
            return filteredModulList.size();
        }

        private boolean myContains(String strModulType, String strSearch) {
            return strModulType != null && strModulType.toLowerCase().contains(strSearch.toLowerCase());
        }

        class ViewHolder {
            TextView tvModuleTitle;
            TextView tvModuleDesc;
        }
    }
}