package de.siebes.fabian.infostudium;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;
import android.view.ViewGroup;

import java.util.HashMap;

/**
 * A [FragmentPagerAdapter] that returns a fragment corresponding to
 * one of the sections/tabs/pages.
 */
public class SectionsPagerAdapter extends FragmentPagerAdapter {

    @StringRes
    private static final int[] TAB_TITLES = new int[]{R.string.modules, R.string.logindata};
    private final Context mContext;
    private final FloatingActionButton mFab;

    private HashMap<Integer, Fragment> mPageReferenceMap = new HashMap<>();

    public SectionsPagerAdapter(Context context, FragmentManager fm, FloatingActionButton fab) {
        super(fm);
        mContext = context;
        mFab = fab;
    }

    @Override
    public Fragment getItem(int position) {
        // getItem is called to instantiate the fragment for the given page.
        // Return a ModulsFragment (defined as a static inner class below).
        Fragment fragment;
        switch (position) {
            case 0:
                fragment = ModulsFragment.newInstance(mFab);
                break;
            case 1:
            default:
                fragment = LoginDataFragment.newInstance(mFab);
                break;
        }
        mPageReferenceMap.put(position, fragment);
        return fragment;
    }

    @Override
    public void destroyItem(@NonNull ViewGroup container, int position, @NonNull Object object) {
        super.destroyItem(container, position, object);
        mPageReferenceMap.remove(position);
    }

    Fragment getFragment(int position){
        return mPageReferenceMap.get(position);
    }

    @Nullable
    @Override
    public CharSequence getPageTitle(int position) {
        return mContext.getResources().getString(TAB_TITLES[position]);
    }

    @Override
    public int getCount() {
        // Show 2 total pages.
        return TAB_TITLES.length;
    }

}