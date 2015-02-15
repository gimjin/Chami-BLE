package com.wind_mobi.chami;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ExpandableListView;
import android.widget.ExpandableListView.OnChildClickListener;
import android.widget.LinearLayout;
import android.widget.SimpleExpandableListAdapter;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * For a given BLE device, this Activity provides the user interface to connect, display data,
 * and display GATT services and characteristics supported by the device.  The Activity
 * communicates with {@code BluetoothLeService}, which in turn interacts with the
 * Bluetooth LE API.
 */
@TargetApi(18)
public class DeviceControlActivity extends Activity {
    private final static String TAG = DeviceControlActivity.class.getSimpleName();

    public static final String EXTRAS_DEVICE_NAME = "DEVICE_NAME";
    public static final String EXTRAS_DEVICE_ADDRESS = "DEVICE_ADDRESS";

    private static LinearLayout linlay_deviceinfo = null;
    private static LinearLayout linlay_fragment = null;

    private static String mSelectServiceName = null;
    private static String mSelectServiceUUID = null;
    private static String mSelectCharacteristicName = null;
    private static String mSelectCharacteristicUUID = null;
    private static BluetoothGattCharacteristic mSelectBluetoothGattCharacteristic = null;

    private TextView mConnectionState;
    private TextView txt_battery_value;
    private TextView txt_rssi_value;
    private TextView txt_txpower_value;
    private TextView kim_log;

    private TextView kim_now;
    private TextView kim_battery;
    private TextView kim_tea_count;
    private TextView kim_water_count;
    private TextView kim_temperature;
    private Button but_reset;

    private String kim_strValue;
    private String kim_temp;
    private Integer kim_WaterCount;
    private Integer kim_teaCount = 0;
    private Integer kim_reset;
    private Boolean kim_reset0 = true;
    private Boolean kim_reset1 = false;
    private SharedPreferences sp;
    private SharedPreferences.Editor spEdit;

    private String mDeviceName;
    private String mDeviceAddress;
    private ExpandableListView mGattServicesList;
    private static BluetoothLeService mBluetoothLeService;
    private ArrayList<ArrayList<BluetoothGattCharacteristic>> mGattCharacteristics = new ArrayList<ArrayList<BluetoothGattCharacteristic>>();
    private boolean mConnected = false;
    private BluetoothGattCharacteristic mNotifyCharacteristic;

    private ArrayList<HashMap<String, String>> mGattServiceData = null;
    private ArrayList<ArrayList<HashMap<String, String>>> mGattCharacteristicData = null;

    private final String LIST_NAME = "NAME";
    private final String LIST_UUID = "UUID";

    private FragmentManager mFragmentManager = getFragmentManager();
    private FragmentTransaction mFragmentTransaction = mFragmentManager.beginTransaction();

    // Code to manage Service lifecycle.
    private final ServiceConnection mServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            mBluetoothLeService = ((BluetoothLeService.LocalBinder) service).getService();
            if (!mBluetoothLeService.initialize()) {
                Log.e(TAG, "Unable to initialize Bluetooth");
                finish();
            }
            // Automatically connects to the device upon successful start-up initialization.
            mBluetoothLeService.connect(mDeviceAddress);

        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mBluetoothLeService = null;

        }
    };

    // Handles various events fired by the Service.
    // ACTION_GATT_CONNECTED: connected to a GATT server.
    // ACTION_GATT_DISCONNECTED: disconnected from a GATT server.
    // ACTION_GATT_SERVICES_DISCOVERED: discovered GATT services.
    // ACTION_DATA_AVAILABLE: received data from the device.  This can be a result of read
    //                        or notification operations.
    private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (Constants.ACTION_GATT_CONNECTED.equals(action)) {
                mConnected = true;
                updateConnectionState(R.string.connected);
                invalidateOptionsMenu();
            } else if (Constants.ACTION_GATT_DISCONNECTED.equals(action)) {
                mConnected = false;
                updateConnectionState(R.string.disconnected);
                invalidateOptionsMenu();
                clearUI();
            } else if (Constants.ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {
                // Show all the supported services and characteristics on the user interface.
                displayGattServices(mBluetoothLeService.getSupportedGattServices());
                SetListener();
            } else if (Constants.ACTION_DATA_AVAILABLE.equals(action)) {
                displayData(intent.getStringExtra(Constants.EXTRA_DATA));
            } else if (Constants.ACTION_RSSI_UPDATE.equals(action)) {
                displayData(intent.getStringExtra(Constants.EXTRA_DATA));
            }
        }
    };

    // If a given GATT characteristic is selected, check for supported features.  This sample
    // demonstrates 'Read' and 'Notify' features.  See
    // http://d.android.com/reference/android/bluetooth/BluetoothGatt.html for the complete
    // list of supported characteristic features.
    private final OnChildClickListener servicesListClickListner =
            new OnChildClickListener() {
                @TargetApi(18)
                @Override
                public boolean onChildClick(ExpandableListView parent, View v, int groupPosition,
                                            int childPosition, long id) {
                    if (mGattCharacteristics != null) {
                        final BluetoothGattCharacteristic characteristic = mGattCharacteristics.get(groupPosition).get(childPosition);
                        final int charaProp = characteristic.getProperties();
                        if ((charaProp | BluetoothGattCharacteristic.PROPERTY_READ) > 0) {
                            // If there is an active notification on a characteristic, clear
                            // it first so it doesn't update the data field on the user interface.
                            if (mNotifyCharacteristic != null) {
                                mBluetoothLeService.setCharacteristicNotification(mNotifyCharacteristic, false);
                                mNotifyCharacteristic = null;
                            }
                            mBluetoothLeService.readCharacteristic(characteristic);
                        }
                        if ((charaProp | BluetoothGattCharacteristic.PROPERTY_NOTIFY) > 0) {
                            mNotifyCharacteristic = characteristic;
                            mBluetoothLeService.setCharacteristicNotification(characteristic, true);
                        }
                        return true;
                    }
                    return false;
                }
            };

    private void clearUI() {
        mGattServicesList.setAdapter((SimpleExpandableListAdapter) null);
        txt_battery_value.setText(R.string.no_data);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.gatt_services_characteristics);

        final Intent intent = getIntent();
        mDeviceName = intent.getStringExtra(EXTRAS_DEVICE_NAME);
        mDeviceAddress = intent.getStringExtra(EXTRAS_DEVICE_ADDRESS);

        // Sets up UI references.
        linlay_deviceinfo = (LinearLayout) findViewById(R.id.linlay_deviceinfo);
        linlay_fragment = (LinearLayout) findViewById(R.id.linlay_fragment);
        ((TextView) findViewById(R.id.device_address)).setText(mDeviceAddress);
        mGattServicesList = (ExpandableListView) findViewById(R.id.gatt_services_list);
        mGattServicesList.setOnChildClickListener(servicesListClickListner);
        mConnectionState = (TextView) findViewById(R.id.connection_state);
        txt_battery_value = (TextView) findViewById(R.id.txt_battery_value);
        txt_rssi_value = (TextView) findViewById(R.id.txt_rssi_value);
        txt_txpower_value = (TextView) findViewById(R.id.txt_txpower_value);
        but_reset = (Button) findViewById(R.id.but_reset);
        kim_log = (TextView) findViewById(R.id.kim_log);

        kim_now = (TextView) findViewById(R.id.kim_now);
        kim_battery = (TextView) findViewById(R.id.kim_battery);
        kim_tea_count = (TextView) findViewById(R.id.kim_tea_count);
        kim_water_count = (TextView) findViewById(R.id.kim_water_count);
        kim_temperature = (TextView) findViewById(R.id.kim_temperature);

        but_reset.setOnClickListener(new Button.OnClickListener(){
            @Override
            public void onClick(View v)
            {
                spEdit.putInt("kim_sp_teaCount", 0);
                spEdit.commit();
            }
        });

//        getActionBar().setTitle(mDeviceName);
        getActionBar().setTitle(R.string.app_name);
        getActionBar().setDisplayHomeAsUpEnabled(true);
        Intent gattServiceIntent = new Intent(this, BluetoothLeService.class);
        bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);

        sp = getSharedPreferences("kim_sp", Context.MODE_PRIVATE);
        spEdit = sp.edit();
    }

    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());
        if (mBluetoothLeService != null) {
            final boolean result = mBluetoothLeService.connect(mDeviceAddress);
            Log.d(TAG, "Connect request result=" + result);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(mGattUpdateReceiver);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbindService(mServiceConnection);
        mBluetoothLeService = null;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.gatt_services, menu);
        if (mConnected) {
            menu.findItem(R.id.menu_connect).setVisible(false);
            menu.findItem(R.id.menu_disconnect).setVisible(true);
        } else {
            menu.findItem(R.id.menu_connect).setVisible(true);
            menu.findItem(R.id.menu_disconnect).setVisible(false);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()) {
            case R.id.menu_connect:
                mBluetoothLeService.connect(mDeviceAddress);
                return true;
            case R.id.menu_disconnect:
                mBluetoothLeService.disconnect();
                return true;
            case android.R.id.home:
                onBackPressed();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void updateConnectionState(final int resourceId) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mConnectionState.setText(resourceId);
                kim_now.setText(resourceId);
            }
        });
    }

    private void displayData(String data) {
        if (data != null) {
            if(data.contains(Constants.BATTERY_LEVEL_VALUE)) {
//                txt_battery_value.setText(data.substring(data.indexOf("_") + 1) + " %");
                kim_battery.setText("剩余电量：" + data.substring(data.indexOf("_") + 1) + " %");
            }

            if(data.contains(Constants.RSSI_VALUE))
                txt_rssi_value.setText(data.substring(data.indexOf("_")+1)+" dBm");

            if(data.contains(Constants.TXPOWER_LEVEL_VALUE))
                txt_txpower_value.setText(data.substring(data.indexOf("_")+1));
            // kim 这里是自定义数据获取口，来更新界面，！！！！ 在这里setText 温度，泡等信息
            if(data.contains(Constants.SELFDEFINENOTIFY_VALUE)){
//                Fragment mFragment = mFragmentManager.findFragmentByTag(Constants.CharacteristicsDetailFragment);
//                if (mFragment != null){
//                    ((CharacteristicsDetailFragment)mFragment).UpdateNotifyValue(data.substring(data.indexOf("_")+1));
//                }


                kim_strValue = data.substring(data.indexOf("_")+1);
                kim_WaterCount = Integer.valueOf(kim_strValue.substring(0, kim_strValue.indexOf("|", 1)));
                kim_temp = kim_strValue.substring(kim_strValue.indexOf("|", 1) + 1, kim_strValue.indexOf("|", 2));
                kim_reset = Integer.valueOf(kim_strValue.substring(kim_strValue.indexOf("|", 2) + 1));
//                Log.i("kim_test11", String.valueOf(kim_WaterCount) + "|" + kim_temp + "|" + String.valueOf(kim_reset));

                if (kim_reset == 1) {
                    if(kim_reset1) {
                        spEdit.putInt("kim_sp_teaCount", kim_teaCount + 1);
                        spEdit.commit();
                    }
                    kim_reset0 = true;
                    kim_reset1 = false;
                } else if (kim_reset == 0) {
                    if(kim_reset0) {
                        spEdit.putInt("kim_sp_teaCount", kim_teaCount + 1);
                        spEdit.commit();
                    }
                    kim_reset0 = false;
                    kim_reset1 = true;
                } else {
                    //无效数据
                }


                kim_teaCount = sp.getInt("kim_sp_teaCount", 0);

                kim_tea_count.setText("泡茶次数：" + kim_teaCount + "包");
                kim_water_count.setText("泡水次数：" + kim_WaterCount + "次");
                kim_temperature.setText("当前温度：" + kim_temp + "度");

            }
        }
    }

    // Demonstrates how to iterate through the supported GATT Services/Characteristics.
    // In this sample, we populate the data structure that is bound to the ExpandableListView
    // on the UI.
    private void displayGattServices(List<BluetoothGattService> gattServices) {
        if (gattServices == null) return;
        String uuid = null;
        String unknownServiceString = getResources().getString(R.string.unknown_service);
        String unknownCharaString = getResources().getString(R.string.unknown_characteristic);
        mGattServiceData = new ArrayList<HashMap<String, String>>();
        mGattCharacteristicData = new ArrayList<ArrayList<HashMap<String, String>>>();
        mGattCharacteristics = new ArrayList<ArrayList<BluetoothGattCharacteristic>>();

        // Loops through available GATT Services.
        for (BluetoothGattService gattService : gattServices) {
            HashMap<String, String> currentServiceData = new HashMap<String, String>();
            uuid = gattService.getUuid().toString();
            currentServiceData.put(LIST_NAME, BLEGattAttributes.lookup(uuid, unknownServiceString));
            currentServiceData.put(LIST_UUID, uuid);
            mGattServiceData.add(currentServiceData);

            ArrayList<HashMap<String, String>> gattCharacteristicGroupData = new ArrayList<HashMap<String, String>>();
            List<BluetoothGattCharacteristic> gattCharacteristics = gattService.getCharacteristics();
            ArrayList<BluetoothGattCharacteristic> charas = new ArrayList<BluetoothGattCharacteristic>();

            // Loops through available Characteristics.
            for (BluetoothGattCharacteristic gattCharacteristic : gattCharacteristics) {
                charas.add(gattCharacteristic);
                HashMap<String, String> currentCharaData = new HashMap<String, String>();
                uuid = gattCharacteristic.getUuid().toString();
                currentCharaData.put(LIST_NAME, BLEGattAttributes.lookup(uuid, unknownCharaString));
                currentCharaData.put(LIST_UUID, uuid);
                gattCharacteristicGroupData.add(currentCharaData);

                //kim add StartNotification
                if (uuid.equals(UUIDConstants.SELFDEFINE_CHARACTERISTIC_NOFITY_UUID)) {
                    mBluetoothLeService.StartNofitication(gattCharacteristic);
                    Log.i("kim:", "StartNotification");
                }
            }
            mGattCharacteristics.add(charas);
            mGattCharacteristicData.add(gattCharacteristicGroupData);
        }

        SimpleExpandableListAdapter gattServiceAdapter = new SimpleExpandableListAdapter(
                this,
                mGattServiceData,
                android.R.layout.simple_expandable_list_item_2,
                new String[] {LIST_NAME, LIST_UUID},
                new int[] { android.R.id.text1, android.R.id.text2 },
                mGattCharacteristicData,
                android.R.layout.simple_expandable_list_item_2,
                new String[] {LIST_NAME, LIST_UUID},
                new int[] { android.R.id.text1, android.R.id.text2 }
        );
        mGattServicesList.setAdapter(gattServiceAdapter);
    }

    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(Constants.ACTION_GATT_CONNECTED);
        intentFilter.addAction(Constants.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(Constants.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(Constants.ACTION_DATA_AVAILABLE);
        intentFilter.addAction(Constants.ACTION_RSSI_UPDATE);
        return intentFilter;
    }

    private void SetListener(){
        mGattServicesList.setOnChildClickListener(new OnChildClickListener() {

            @Override
            public boolean onChildClick(ExpandableListView parent, View v, int groupPosition, int childPosition, long id) {
                linlay_deviceinfo.setVisibility(View.GONE);
                linlay_fragment.setVisibility(View.VISIBLE);

                mSelectServiceName = mGattServiceData.get(groupPosition).get(LIST_NAME);
                mSelectServiceUUID = mGattServiceData.get(groupPosition).get(LIST_UUID);
                mSelectCharacteristicName = mGattCharacteristicData.get(groupPosition).get(childPosition).get(LIST_NAME);
                mSelectCharacteristicUUID = mGattCharacteristicData.get(groupPosition).get(childPosition).get(LIST_UUID);
                mSelectBluetoothGattCharacteristic = mGattCharacteristics.get(groupPosition).get(childPosition);

                ChangetoCharacteristicsDetailFragment();
                return true;
            }
        });
    }

    public static void ShowDeviceInfo(){
        linlay_deviceinfo.setVisibility(View.VISIBLE);
        linlay_fragment.setVisibility(View.GONE);
    }

    private void ChangetoCharacteristicsDetailFragment(){
        FragmentManager fm = getFragmentManager();
        FragmentTransaction ft = fm.beginTransaction();

        Fragment mFragment = fm.findFragmentByTag(Constants.CharacteristicsDetailFragment);
        if (mFragment == null){
            CharacteristicsDetailFragment characteristicsdetailfragment = new CharacteristicsDetailFragment();
            ft.add(R.id.linlay_fragment, characteristicsdetailfragment, Constants.CharacteristicsDetailFragment);
            ft.show(characteristicsdetailfragment);
        }else{
            ((CharacteristicsDetailFragment)mFragment).SetCharacteristicInfo();
            ft.show(mFragment);
        }

        ft.commit();
    }

    public static String getServiceName(){
        return mSelectServiceName;
    }

    public static String getServiceUUID(){
        return mSelectServiceUUID;
    }

    public static String getCharacteristicName(){
        return mSelectCharacteristicName;
    }

    public static String getCharacteristicUUID(){
        return mSelectCharacteristicUUID;
    }

    public static int getSelectBluetoothGattCharacteristicProperties(){
        return mSelectBluetoothGattCharacteristic.getProperties();
    }

    public static void StartNotify(){
        mBluetoothLeService.StartNofitication(mSelectBluetoothGattCharacteristic);
    }


    public static void WriteValue(int value){
        mBluetoothLeService.WriteCharacteristic(mSelectBluetoothGattCharacteristic, value);
    }
}
