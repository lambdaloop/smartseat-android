package smartseat.smartseat;

import android.app.ActionBar;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.IBinder;
import android.support.design.widget.TabLayout;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Objects;
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


    private final ServiceConnection rfduinoServiceConnection = new ServiceConnection() {
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

        if(device.getName().equals("SmartAss V1")) {
            bluetoothAdapter.stopLeScan(this);
            bluetoothDevice = device;
            connectDevice();
        }

    }
}
