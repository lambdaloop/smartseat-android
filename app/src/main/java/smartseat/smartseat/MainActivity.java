package smartseat.smartseat;

import android.app.FragmentManager;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.IBinder;
import android.support.design.widget.TabLayout;
import android.app.Fragment;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.UUID;


public class MainActivity extends AppCompatActivity implements BluetoothAdapter.LeScanCallback {
    private final static String TAG = MainActivity.class.getSimpleName();

    public final static int NUM_SENSORS = 4;

    private BluetoothAdapter bluetoothAdapter;
    private BluetoothDevice bluetoothDevice;

    private RFduinoService rfduinoService;
//    private DrawingPanel panel;

    private ActionBarDrawerToggle mDrawerToggle;
    private DrawerLayout mDrawerLayout;


    private class ServiceConnectionHandler implements ServiceConnection {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            Log.i(TAG, "onServiceConnected");
            rfduinoService = ((RFduinoService.LocalBinder) service).getService();
            if (rfduinoService.initialize()) {
                if (rfduinoService.connect(bluetoothDevice.getAddress())) {
//                    upgradeState(STATE_CONNECTING);
                    Log.i(TAG, "CONNECTING...");
                }
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            rfduinoService = null;
            Log.i(TAG, "DISCONNECTED");
            bluetoothAdapter.startLeScan(
                    new UUID[]{RFduinoService.UUID_SERVICE},
                    MainActivity.this);
//            downgradeState(STATE_DISCONNECTED);
        }
    }

    private final ServiceConnection rfduinoServiceConnection = new ServiceConnection() {

    };


    private final BroadcastReceiver rfduinoReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (RFduinoService.ACTION_CONNECTED.equals(action)) {
                Log.i(TAG, "CONNECTED!");
//                upgradeState(STATE_CONNECTED);
            } else if (RFduinoService.ACTION_DISCONNECTED.equals(action)) {
                Log.i(TAG, "DISCONNECTED");
//                downgradeState(STATE_DISCONNECTED);
            } else if (RFduinoService.ACTION_DATA_AVAILABLE.equals(action)) {
                receiveData(intent.getByteArrayExtra(RFduinoService.EXTRA_DATA));
            }
        }
    };
    private TabsPagerAdapter tabsAdapter;
    ArrayList<NavItem> mNavItems = new ArrayList<NavItem>();
    private ListView mDrawerList;
    private RelativeLayout mDrawerPane;

    private ListView deviceList;
    private DeviceListAdapter deviceAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        TabLayout tabs = (TabLayout) findViewById(R.id.tabs);
        ViewPager pager = (ViewPager) findViewById(R.id.pager);
        tabsAdapter = new TabsPagerAdapter(getSupportFragmentManager());

        assert pager != null;
        assert tabs != null;

        pager.setAdapter(tabsAdapter);
        tabs.setupWithViewPager(pager);

        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawerLayout);
        Log.i(TAG, String.format("DRAWER: %s", String.valueOf(mDrawerLayout)));

        mNavItems.add(new NavItem("Home", "Meetup destination", R.drawable.ic_action_home));
        mNavItems.add(new NavItem("Preferences", "Change your preferences", R.drawable.ic_action_settings));
        mNavItems.add(new NavItem("About", "Get to know about us", R.drawable.ic_action_about));

        // DrawerLayout
        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawerLayout);

        // Populate the Navigtion Drawer with options
        mDrawerPane = (RelativeLayout) findViewById(R.id.drawerPane);
        mDrawerList = (ListView) findViewById(R.id.navList);
        DrawerListAdapter adapter = new DrawerListAdapter(this, mNavItems);
        mDrawerList.setAdapter(adapter);

        // Drawer Item click listeners
        mDrawerList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                selectItemFromDrawer(position);
            }
        });

        mDrawerToggle = new ActionBarDrawerToggle(this, mDrawerLayout, R.string.drawer_open, R.string.drawer_close) {
            @Override
            public void onDrawerOpened(View drawerView) {
                super.onDrawerOpened(drawerView);

                invalidateOptionsMenu();
            }

            @Override
            public void onDrawerClosed(View drawerView) {
                super.onDrawerClosed(drawerView);
                Log.d(TAG, "onDrawerClosed: " + getTitle());

                invalidateOptionsMenu();
            }
        };

        mDrawerLayout.setDrawerListener(mDrawerToggle);


        deviceList = (ListView) findViewById(R.id.deviceList);
        deviceAdapter = new DeviceListAdapter(this);
        deviceList.setAdapter(deviceAdapter);

        deviceList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                new AlertDialog.Builder(MainActivity.this)
                        .setTitle("Connect")
                        .setMessage("What should I connect as?")
                        .setPositiveButton("FSR", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {

                            }
                        })
                        .setNegativeButton("Yarn", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {

                            }
                        })
                        .setIcon(android.R.drawable.ic_dialog_info)
                        .show();
            }
        });


        if (this.checkSelfPermission(android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{android.Manifest.permission.ACCESS_COARSE_LOCATION}, 0);
        }

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();


        new android.os.Handler().postDelayed(
                new Runnable() {
                    public void run() {
                        Log.i(TAG, "starting scan...");

                        bluetoothAdapter.startLeScan(
                                new UUID[]{RFduinoService.UUID_SERVICE},
                                MainActivity.this);
                    }
                },
                400);



//        Log.i(TAG, String.format("PANEL: %s", String.valueOf(panel)));

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        Log.i(TAG, String.format("TABS onCreate: %s", String.valueOf(tabsAdapter)));


    }


    /*
* Called when a particular item from the navigation drawer
* is selected.
* */
    private void selectItemFromDrawer(int position) {
//        Fragment fragment = new PreferencesFragment();
//
//        FragmentManager fragmentManager = getFragmentManager();
//        fragmentManager.beginTransaction()
//                .replace(R.id.mainContent, fragment)
//                .commit();

        String title = mNavItems.get(position).mTitle;

        LinearLayout main = (LinearLayout) findViewById(R.id.mainContent);
        LinearLayout prefs = (LinearLayout) findViewById(R.id.preferences);

        if(title == "Preferences") {
            main.setVisibility(LinearLayout.GONE);
            prefs.setVisibility(LinearLayout.VISIBLE);
        } else if(title == "Home") {
            main.setVisibility(LinearLayout.VISIBLE);
            prefs.setVisibility(LinearLayout.GONE);
        }

        mDrawerList.setItemChecked(position, true);
        setTitle(mNavItems.get(position).mTitle);

        // Close the drawer
        mDrawerLayout.closeDrawer(mDrawerPane);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Log.i(TAG, String.format("optionsSelected DRAWER: %s", String.valueOf(mDrawerLayout)));

        // Pass the event to ActionBarDrawerToggle
        // If it returns true, then it has handled
        // the nav drawer indicator touch event
        if (mDrawerToggle.onOptionsItemSelected(item)) {
            return true;
        }

        // Handle your other action bar items...

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        Log.i(TAG, String.format("PostCreate DRAWER: %s", String.valueOf(mDrawerLayout)));

        super.onPostCreate(savedInstanceState);
        mDrawerToggle.syncState();
    }

    @Override
    protected void onStart() {
        super.onStart();

//        registerReceiver(scanModeReceiver, new IntentFilter(BluetoothAdapter.ACTION_SCAN_MODE_CHANGED));
//        registerReceiver(bluetoothStateReceiver, new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED));
        registerReceiver(rfduinoReceiver, RFduinoService.getIntentFilter());

//        updateState(bluetoothAdapter.isEnabled() ? STATE_DISCONNECTED : STATE_BLUETOOTH_OFF);
    }

    @Override
    protected void onStop() {
        super.onStop();

        bluetoothAdapter.stopLeScan(this);

//        unregisterReceiver(scanModeReceiver);
//        unregisterReceiver(bluetoothStateReceiver);
        unregisterReceiver(rfduinoReceiver);
    }

    private void receiveData(byte[] bytes) {
        String data = new String(bytes, StandardCharsets.UTF_8);
        String[] arr = data.split(",");
        int sensorNum = Integer.parseInt(arr[0]);
        int sensorValue = Integer.parseInt(arr[1]);

//        Log.i(TAG, String.format("TABS receive: %s", String.valueOf(tabsAdapter)));
//        Log.i(TAG, String.format("%d %.3f", sensorNum, 1-sensorValue/1024.0));

        tabsAdapter.updateSensor(sensorNum, 1-sensorValue/1024.0);
 
    }

    private void connectDevice() {
        Intent rfduinoIntent = new Intent(MainActivity.this, RFduinoService.class);
        bindService(rfduinoIntent, rfduinoServiceConnection, BIND_AUTO_CREATE);
    }

    @Override
    public void onLeScan(BluetoothDevice device, final int rssi, final byte[] scanRecord) {
        Log.i(TAG, String.format("FOUND DEVICE: %s", device.getName()));

        deviceAdapter.addDevice(device);
//
//        if(device.getName().equals("SmartAss V1")) {
//            bluetoothAdapter.stopLeScan(this);
//            bluetoothDevice = device;
//            connectDevice();
//        }

    }

    class NavItem {
        String mTitle;
        String mSubtitle;
        int mIcon;

        public NavItem(String title, String subtitle, int icon) {
            mTitle = title;
            mSubtitle = subtitle;
            mIcon = icon;
        }
    }

    class DrawerListAdapter extends BaseAdapter {

        Context mContext;
        ArrayList<NavItem> mNavItems;

        public DrawerListAdapter(Context context, ArrayList<NavItem> navItems) {
            mContext = context;
            mNavItems = navItems;
        }

        @Override
        public int getCount() {
            return mNavItems.size();
        }

        @Override
        public Object getItem(int position) {
            return mNavItems.get(position);
        }

        @Override
        public long getItemId(int position) {
            return 0;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View view;

            if (convertView == null) {
                LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                view = inflater.inflate(R.layout.drawer_item, null);
            }
            else {
                view = convertView;
            }

            TextView titleView = (TextView) view.findViewById(R.id.title);
            TextView subtitleView = (TextView) view.findViewById(R.id.subTitle);
            ImageView iconView = (ImageView) view.findViewById(R.id.icon);

            titleView.setText( mNavItems.get(position).mTitle );
            subtitleView.setText( mNavItems.get(position).mSubtitle );
            iconView.setImageResource(mNavItems.get(position).mIcon);

            return view;
        }
    }


    class DeviceListAdapter extends BaseAdapter {

        Context mContext;
        ArrayList<BluetoothDevice> devices;

        public DeviceListAdapter(Context context) {
            mContext = context;
            devices = new ArrayList<BluetoothDevice>();
        }

        @Override
        public int getCount() {
            return devices.size();
        }

        @Override
        public Object getItem(int position) {
            return devices.get(position);
        }

        @Override
        public long getItemId(int position) {
            return 0;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View view;

            if (convertView == null) {
                LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                view = inflater.inflate(R.layout.device_item, null);
            }
            else {
                view = convertView;
            }

            TextView titleView = (TextView) view.findViewById(R.id.deviceName);

            BluetoothDevice device = devices.get(position);
            String title = String.format("%s (%s)", device.getName(), device.getAddress());
            titleView.setText(title);

            return view;
        }

        public void addDevice(BluetoothDevice device) {
            if(!devices.contains(device)) {
                devices.add(device);
                this.notifyDataSetChanged();
            }
        }
    }
    }
