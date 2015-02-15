package com.wind_mobi.chami;

import android.annotation.TargetApi;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;

/**
 * Service for managing connection and data communication with a GATT server hosted on a
 * given Bluetooth LE device.
 */
@TargetApi(18)
public class BluetoothLeService extends Service {
    private final static String TAG = BluetoothLeService.class.getSimpleName();
 
    private BluetoothManager mBluetoothManager;
    private BluetoothAdapter mBluetoothAdapter;
    private String mBluetoothDeviceAddress;
    private BluetoothGatt mBluetoothGatt;
    private int mConnectionState = STATE_DISCONNECTED;
    
    private Timer mBatteryTimer = null;
    private Timer mRSSITimer = null;
    private Timer mSinglekeyTimer = null; 
    private Timer mTxPowerTimer = null;
    private Timer mNotifyTimer = null;
 
    private static final int STATE_DISCONNECTED = 0;
    private static final int STATE_CONNECTING = 1;
    private static final int STATE_CONNECTED = 2;
  
    public final static UUID UUID_HEART_RATE_MEASUREMENT = UUID.fromString(UUIDConstants.HEART_RATE_MEASUREMENT_UUID);
    public final static UUID UUID_BATTERY_LEVEL = UUID.fromString(UUIDConstants.BATTERY_LEVEL_UUID);
    public final static UUID UUID_TX_POWER_LEVEL = UUID.fromString(UUIDConstants.TX_POWER_LEVEL_UUID);
    public final static UUID UUID_SELF_DEFINE_NOTIFY = UUID.fromString(UUIDConstants.SELFDEFINE_CHARACTERISTIC_NOFITY_UUID);
    
    private BluetoothGattCharacteristic mNotifyCharacteristic = null;

    // Implements callback methods for GATT events that the app cares about.  For example,
    // connection change and services discovered.
    private final BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            String intentAction;
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                intentAction = Constants.ACTION_GATT_CONNECTED;
                mConnectionState = STATE_CONNECTED;
                broadcastUpdate(intentAction);
                Log.i(TAG, "Connected to GATT server.");
                // Attempts to discover services after successful connection.
                Log.i(TAG, "Attempting to start service discovery:" + mBluetoothGatt.discoverServices());
                
                // Battery Level
                GetBatteryLevel();
                
                // RSSI Value
                ReadRSSIValue();      
                
                // Single Key Service()
                GetSingelKey(); 
                
                // TxPower
                GetTxPower();
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                intentAction = Constants.ACTION_GATT_DISCONNECTED;
                mConnectionState = STATE_DISCONNECTED;
                Log.i(TAG, "Disconnected from GATT server.");
                broadcastUpdate(intentAction);
            }
        }
 
        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            Log.i(TAG, "status:" + status + "|");
            if (status == BluetoothGatt.GATT_SUCCESS) {
                broadcastUpdate(Constants.ACTION_GATT_SERVICES_DISCOVERED);
            } else {
//                Log.w("kim", "onServicesDiscovered received: " + status);
            }
        }
 
        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                broadcastUpdate(Constants.ACTION_DATA_AVAILABLE, characteristic);
            }
//            Log.i("kim", "GATT Read status:" + BluetoothGatt.GATT_SUCCESS);
        }
 
        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            broadcastUpdate(Constants.ACTION_DATA_AVAILABLE, characteristic);
//            Log.i("kim", "GATT Changed status:" + BluetoothGatt.GATT_SUCCESS);
        }


        @Override
		public void onReadRemoteRssi(BluetoothGatt gatt, int rssi, int status) {
			if (status == BluetoothGatt.GATT_SUCCESS) {
				broadcastUpdate(Constants.ACTION_RSSI_UPDATE, rssi);
            }
			super.onReadRemoteRssi(gatt, rssi, status);
//            Log.i("kim", "GATT Rssi status:" + status);
		}
    };
 
    private void broadcastUpdate(final String action) {
        final Intent intent = new Intent(action);
        sendBroadcast(intent);
    }
    
    private void broadcastUpdate(final String action, final int rssi) {
        final Intent intent = new Intent(action);
        //intent.putExtra(Constants.RSSI_DATA, rssi);
        intent.putExtra(Constants.EXTRA_DATA, Constants.RSSI_VALUE +"_"+ String.valueOf(rssi));
        sendBroadcast(intent);
    }
 
    private void broadcastUpdate(final String action, final BluetoothGattCharacteristic characteristic) {
        final Intent intent = new Intent(action);
 
        // This is special handling for the Heart Rate Measurement profile.  Data parsing is
        // carried out as per profile specifications:
        // http://developer.bluetooth.org/gatt/characteristics/Pages/CharacteristicViewer.aspx?u=org.bluetooth.characteristic.heart_rate_measurement.xml
        if (UUID_HEART_RATE_MEASUREMENT.equals(characteristic.getUuid())) {
            int flag = characteristic.getProperties();
            int format = -1;
            if ((flag & 0x01) != 0) {
                format = BluetoothGattCharacteristic.FORMAT_UINT16;
                Log.d(TAG, "Heart rate format UINT16.");
            } else {
                format = BluetoothGattCharacteristic.FORMAT_UINT8;
                Log.d(TAG, "Heart rate format UINT8.");
            }
            final int heartRate = characteristic.getIntValue(format, 1);
            Log.d(TAG, String.format("Received heart rate: %d", heartRate));
            intent.putExtra(Constants.EXTRA_DATA, String.valueOf(heartRate));
        } 
        /*
        else {
            // For all other profiles, writes the data formatted in HEX.
            final byte[] data = characteristic.getValue();
            if (data != null && data.length > 0) {
                final StringBuilder stringBuilder = new StringBuilder(data.length);
                for(byte byteChar : data)
                    stringBuilder.append(String.format("%02X ", byteChar));
                intent.putExtra(EXTRA_DATA, new String(data) + "\n" + stringBuilder.toString());
            }
        }
        */
        
        // Battery Data
        if (UUID_BATTERY_LEVEL.equals(characteristic.getUuid())) {
        	final byte[] data = characteristic.getValue();
        	
        	if (data != null && data.length > 0) {
        		intent.putExtra(Constants.EXTRA_DATA, Constants.BATTERY_LEVEL_VALUE +"_"+ String.valueOf(data[0]));        		
            }
        }
        
        // TxPower Data
        if (UUID_TX_POWER_LEVEL.equals(characteristic.getUuid())) {
        	final byte[] data = characteristic.getValue();
        	
        	if (data != null && data.length > 0) {
        		intent.putExtra(Constants.EXTRA_DATA, Constants.TXPOWER_LEVEL_VALUE +"_"+ String.valueOf(data[0]));        		
            }
        }
        
        // Notify
        if(UUID_SELF_DEFINE_NOTIFY.equals(characteristic.getUuid())){
            //01 泡数 23 温度 4{127无效, 0&255有效值} 新茶包
        	final byte[] data = characteristic.getValue();
        	
        	if (data != null && data.length > 0) {

                int mCount = data[0]|(data[1]<<8); //泡数
                int mTemp = data[2]|(data[3]<<8); //温度
                int mReset = data[4]; // 新茶包 reset
        		intent.putExtra(Constants.EXTRA_DATA, Constants.SELFDEFINENOTIFY_VALUE +"_"+ String.valueOf(mCount) + "|" + String.valueOf(mTemp) + "|" + String.valueOf(mReset));
//
//                for(int i =0 ; i <= data.length; i++) {
//                    Log.i("kim byte", String.valueOf(data[i]) + "(" + i + ")");
//                }
            }
        }
        
        sendBroadcast(intent);
    }
 
    public class LocalBinder extends Binder {
        BluetoothLeService getService() {
            return BluetoothLeService.this;
        }
    }
 
    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }
 
    @Override
    public boolean onUnbind(Intent intent) {
        // After using a given device, you should make sure that BluetoothGatt.close() is called
        // such that resources are cleaned up properly.  In this particular example, close() is
        // invoked when the UI is disconnected from the Service.
        close();
        return super.onUnbind(intent);
    }
 
    private final IBinder mBinder = new LocalBinder();
 
    /**
     * Initializes a reference to the local Bluetooth adapter.
     *
     * @return Return true if the initialization is successful.
     */
    public boolean initialize() {
        // For API level 18 and above, get a reference to BluetoothAdapter through
        // BluetoothManager.
        if (mBluetoothManager == null) {
            mBluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
            if (mBluetoothManager == null) {
                Log.e(TAG, "Unable to initialize BluetoothManager.");
                return false;
            }
        }
 
        mBluetoothAdapter = mBluetoothManager.getAdapter();
        if (mBluetoothAdapter == null) {
            Log.e(TAG, "Unable to obtain a BluetoothAdapter.");
            return false;
        }
 
        return true;
    }
 
    /**
     * Connects to the GATT server hosted on the Bluetooth LE device.
     *
     * @param address The device address of the destination device.
     *
     * @return Return true if the connection is initiated successfully. The connection result
     *         is reported asynchronously through the
     *         {@code BluetoothGattCallback#onConnectionStateChange(android.bluetooth.BluetoothGatt, int, int)}
     *         callback.
     */
    public boolean connect(final String address) {
        if (mBluetoothAdapter == null || address == null) {
            Log.w(TAG, "BluetoothAdapter not initialized or unspecified address.");
            return false;
        }
 
        // Previously connected device.  Try to reconnect.
        if (mBluetoothDeviceAddress != null && address.equals(mBluetoothDeviceAddress)
                && mBluetoothGatt != null) {
            Log.d(TAG, "Trying to use an existing mBluetoothGatt for connection.");
            if (mBluetoothGatt.connect()) {
                mConnectionState = STATE_CONNECTING;
                return true;
            } else {
                return false;
            }
        }
 
        final BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
        if (device == null) {
            Log.w(TAG, "Device not found.  Unable to connect.");
            return false;
        }
        // We want to directly connect to the device, so we are setting the autoConnect
        // parameter to false.
        mBluetoothGatt = device.connectGatt(this, false, mGattCallback);
        Log.d(TAG, "Trying to create a new connection.");
        mBluetoothDeviceAddress = address;
        mConnectionState = STATE_CONNECTING;
        return true;
    }
 
    /**
     * Disconnects an existing connection or cancel a pending connection. The disconnection result
     * is reported asynchronously through the
     * {@code BluetoothGattCallback#onConnectionStateChange(android.bluetooth.BluetoothGatt, int, int)}
     * callback.
     */
    public void disconnect() {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return;
        }
        mBluetoothGatt.disconnect();
    }
 
    /**
     * After using a given BLE device, the app must call this method to ensure resources are
     * released properly.
     */
    public void close() {
    	if(mBatteryTimer != null)
    		mBatteryTimer.cancel();
    	
    	if(mRSSITimer != null)
    		mRSSITimer.cancel();
    	
    	if(mSinglekeyTimer != null)
    		mSinglekeyTimer.cancel();
    	
    	if(mTxPowerTimer != null)
    		mTxPowerTimer.cancel();
    	
        if (mBluetoothGatt == null)
            return;
        
        mBluetoothGatt.close();
        mBluetoothGatt = null;
    }
 
    /**
     * Request a read on a given {@code BluetoothGattCharacteristic}. The read result is reported
     * asynchronously through the {@code BluetoothGattCallback#onCharacteristicRead(android.bluetooth.BluetoothGatt, android.bluetooth.BluetoothGattCharacteristic, int)}
     * callback.
     *
     * @param characteristic The characteristic to read from.
     */
    public void readCharacteristic(BluetoothGattCharacteristic characteristic) {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return;
        }
        mBluetoothGatt.readCharacteristic(characteristic);
    }
 
    /**
     * Enables or disables notification on a give characteristic.
     *
     * @param characteristic Characteristic to act on.
     * @param enabled If true, enable notification.  False otherwise.
     */
    public void setCharacteristicNotification(BluetoothGattCharacteristic characteristic, boolean enabled) {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return;
        }
        
        mBluetoothGatt.setCharacteristicNotification(characteristic, enabled);
 
        // This is specific to Heart Rate Measurement.
        if (UUID_HEART_RATE_MEASUREMENT.equals(characteristic.getUuid())) {
            BluetoothGattDescriptor descriptor = characteristic.getDescriptor(UUID.fromString(UUIDConstants.CLIENT_CHARACTERISTIC_CONFIG));
            descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
            mBluetoothGatt.writeDescriptor(descriptor);
        }
        
        if (UUID_SELF_DEFINE_NOTIFY.equals(characteristic.getUuid())) {
            BluetoothGattDescriptor descriptor = characteristic.getDescriptor(UUID.fromString(UUIDConstants.CLIENT_CHARACTERISTIC_CONFIG));
            descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
            mBluetoothGatt.writeDescriptor(descriptor);
        }
    }
 
    /**
     * Retrieves a list of supported GATT services on the connected device. This should be
     * invoked only after {@code BluetoothGatt#discoverServices()} completes successfully.
     *
     * @return A {@code List} of supported services.
     */
    public List<BluetoothGattService> getSupportedGattServices() {
        if (mBluetoothGatt == null) return null;
 
        return mBluetoothGatt.getServices();
    }
    
    // Battery
    private void GetBatteryLevel(){
        mBatteryTimer = new Timer("BatteryTimer");
        TimerTask task = new TimerTask() {
        @Override
        public void run() {
        		getBattery();
        	}
        };
        mBatteryTimer.schedule(task, 3000, 10000);
    }
    
    public void getBattery() {
    	if (mBluetoothGatt == null) {
    		//Log.e(TAG, "lost connection");
    		return;
    	} 
    	
    	if(mBluetoothGatt != null){
	    	BluetoothGattService batteryService = null;
	    	
	    	for (BluetoothGattService gattService : mBluetoothGatt.getServices()) {
	    		if(gattService.getUuid().equals(UUID.fromString(UUIDConstants.BATTERY_SERVICE_UUID)))
	    			batteryService = gattService;
	    	}
	    	
	    	if(batteryService == null) {
	    		Log.d(TAG, "Battery Service not found!");
	    		mBatteryTimer.cancel();
	    		return;
	    	}
	
	    	BluetoothGattCharacteristic batteryLevel = batteryService.getCharacteristic(UUID.fromString(UUIDConstants.BATTERY_LEVEL_UUID));
	    	if(batteryLevel == null) {
	    		Log.d(TAG, "Battery Level not found!");
	    		return;
	    	}
	
	    	readCharacteristic(batteryLevel);
    	}
    }
    
    // RSSI
    private void ReadRSSIValue(){
    	mRSSITimer = new Timer("BatteryTimer");
        TimerTask task = new TimerTask(){
           @Override
           public void run(){
        	   getRSSI();
           }
        };
        mRSSITimer.schedule(task, 1000, 5000);
    }
    
    private void getRSSI(){
    	if (mBluetoothGatt == null) {
    		//Log.e(TAG, "lost connection");
    		return;
    	} 
    	
    	if(mBluetoothGatt != null){
    		mBluetoothGatt.readRemoteRssi();
    	}
    }
    
    // Single Key
    private void GetSingelKey(){
    	mSinglekeyTimer = new Timer("SingleKeyTimer");
        TimerTask task = new TimerTask() {
        @Override
        public void run() {
        		setSingleKeyPressNotification();
        	}
        };
        mSinglekeyTimer.schedule(task, 5000);
    }
    
    private void setSingleKeyPressNotification(){
    	if (mBluetoothGatt == null) {
    		//Log.e(TAG, "lost connection");
    		return;
    	} 
    	
    	if(mBluetoothGatt != null){
    		BluetoothGattService singlekeyservice = null;
	    	
	    	for (BluetoothGattService gattService : mBluetoothGatt.getServices()) {
	    		if(gattService.getUuid().equals(UUID.fromString(UUIDConstants.SIMPLE_KEY_SERVICE_UUID)))
	    			singlekeyservice = gattService;
	    	}
	    	
	    	if(singlekeyservice == null) {
	    		Log.d(TAG, "Single Key Service not found!");
	    		mSinglekeyTimer.cancel();
	    		return;
	    	}
	
	    	BluetoothGattCharacteristic singlekeycharacteristic = singlekeyservice.getCharacteristic(UUID.fromString(UUIDConstants.KEY_PRESS_STATE_UUID));
	    	if(singlekeycharacteristic == null) {
	    		Log.d(TAG, "Single Key Press not found!");
	    		return;
	    	}
	
	    	setCharacteristicNotification(singlekeycharacteristic, true);
    	}
    }
    
    // Tx Power
    private void GetTxPower(){
        mTxPowerTimer = new Timer("TxPowerTimer");
        TimerTask task = new TimerTask() {
        @Override
        public void run() {
        		getTxpower();
        	}
        };
        mTxPowerTimer.schedule(task, 5000, 5000);
    }
    
    public void getTxpower() {
    	if (mBluetoothGatt == null) {
    		//Log.e(TAG, "lost connection");
    		return;
    	} 
    	
    	if(mBluetoothGatt != null){
	    	BluetoothGattService txpowerService = null;
	    	
	    	for (BluetoothGattService gattService : mBluetoothGatt.getServices()) {
	    		if(gattService.getUuid().equals(UUID.fromString(UUIDConstants.TX_POWER_UUID)))
	    			txpowerService = gattService;
	    	}
	    	
	    	if(txpowerService == null) {
	    		Log.d(TAG, "TxPower Service not found!");
	    		mTxPowerTimer.cancel();
	    		return;
	    	}
	
	    	BluetoothGattCharacteristic txPowerLevel = txpowerService.getCharacteristic(UUID.fromString(UUIDConstants.TX_POWER_LEVEL_UUID));
	    	if(txPowerLevel == null) {
	    		Log.d(TAG, "TxPower Level not found!");
	    		return;
	    	}
	
	    	readCharacteristic(txPowerLevel);
    	}
    }
    
    // Start Nofitication
    public void StartNofitication(BluetoothGattCharacteristic characteristic){
    	mNotifyCharacteristic = characteristic;
    	
    	if(mNotifyTimer != null)
    		mNotifyTimer.cancel();
    	
    	mNotifyTimer = new Timer("NotifyTimer");
        TimerTask task = new TimerTask() {
        @Override
        public void run() {
        		SetNotification();
        	}
        };
        mNotifyTimer.schedule(task, 1000, 1000);
    }
    
    private void SetNotification(){
    	if (mBluetoothGatt == null) {
    		//Log.e(TAG, "lost connection");
    		return;
    	} 
    	
    	if(mBluetoothGatt != null){
	    	setCharacteristicNotification(mNotifyCharacteristic, true);
    	}
    }

    // Kim Write !!!!!!!!!!!!
    // Write Characteristic
    public void WriteCharacteristic(BluetoothGattCharacteristic characteristic, int value){
    	mNotifyCharacteristic = characteristic;

    	byte[] bytevalue = new byte[1];
    	bytevalue[0] = (byte)value;
    	
    	mNotifyCharacteristic.setValue(bytevalue);
        boolean aa = mBluetoothGatt.writeCharacteristic(mNotifyCharacteristic);
//        Log.i("kim aa", (aa + ""));
    }
}
