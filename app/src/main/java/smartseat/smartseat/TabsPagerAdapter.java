package smartseat.smartseat;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.util.Log;


public class TabsPagerAdapter extends FragmentPagerAdapter {
    private final static String TAG = TabsPagerAdapter.class.getSimpleName();

    private final FSRFragment fsrFragment;

    public TabsPagerAdapter(FragmentManager fm) {
        super(fm);
        fsrFragment = new FSRFragment();
    }

    @Override
    public Fragment getItem(int position) {
        // getItem is called to instantiate the fragment for the given page.
        String title = getPageTitle(position).toString();
        if(title == "FSR") {
            return fsrFragment;
        } else {
            return PageFragment.newInstance(position + 1);
        }
    }

    @Override
    public int getCount() {
        return 3;
    }

    private String[] pageTitles = new String[]{"FSR", "Yarn", "IR"};

    @Override
    public CharSequence getPageTitle(int position) {
        return pageTitles[position];
    }

    public void updateSensor(int sensor, double value) {
        Log.i(TAG, String.format("%d %.3f", sensor, value));

        fsrFragment.updateSensor(sensor, value);
    }
}
