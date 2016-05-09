package smartseat.smartseat;

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
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.nio.charset.StandardCharsets;
import java.util.UUID;


public class MainActivity extends AppCompatActivity implements BluetoothAdapter.LeScanCallback {
    private final static String TAG = MainActivity.class.getSimpleName();

    public final static int NUM_SENSORS = 4;

    private BluetoothAdapter bluetoothAdapter;
    private BluetoothDevice bluetoothDevice;

    private RFduinoService rfduinoService;
    private DrawingPanel panel;

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


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        LinearLayout fv = (LinearLayout) findViewById(R.id.top_layout);
        panel = new DrawingPanel(this);
        fv.addView(panel);

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



        Log.i(TAG, String.format("PANEL: %s", String.valueOf(panel)));

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

        if(panel != null) {
            panel.updateSensor(sensorNum, sensorValue/1024.0);
        }

//        Log.i(TAG, String.format("data: %s", data));
    }

    private void connectDevice() {
        Intent rfduinoIntent = new Intent(MainActivity.this, RFduinoService.class);
        bindService(rfduinoIntent, rfduinoServiceConnection, BIND_AUTO_CREATE);
    }

    @Override
    public void onLeScan(BluetoothDevice device, final int rssi, final byte[] scanRecord) {
        bluetoothAdapter.stopLeScan(this);
        bluetoothDevice = device;
        Log.i(TAG, String.format("FOUND DEVICE: %s", device.getName()));

        connectDevice();

    }
}
