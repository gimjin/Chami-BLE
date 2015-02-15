package com.wind_mobi.chami;

public class Constants {
	// Action
    public final static String ACTION_GATT_CONNECTED = "com.turnonmedia.bluetoothlegatt.ACTION_GATT_CONNECTED";
    public final static String ACTION_GATT_DISCONNECTED = "com.turnonmedia.bluetoothlegatt.ACTION_GATT_DISCONNECTED"; 
    public final static String ACTION_GATT_SERVICES_DISCOVERED = "com.turnonmedia.bluetoothlegatt.ACTION_GATT_SERVICES_DISCOVERED";
    public final static String ACTION_DATA_AVAILABLE = "com.turnonmedia.bluetoothlegatt.ACTION_DATA_AVAILABLE";
    public final static String EXTRA_DATA = "com.turnonmedia.bluetoothlegatt.EXTRA_DATA";
    public final static String ACTION_RSSI_UPDATE = "com.turnonmedia.bluetoothlegatt.ACTION_RSSI_UPDATE";
    public final static String RSSI_DATA = "com.turnonmedia.bluetoothlegatt.RSSI_DATA";
	
	// BLE String
	// Device Info
	public static final String Device_Information_Service = "Device Information Service";
	public static final String Manufacturer_Name_String = "Manufacturer Name String";
	
	// Heart Rate Service
	public static final String Heart_Rate_Service = "Heart Rate Service";
	public static final String Heart_Rate_Measurement = "Heart Rate Measurement";
	
	// Battery Service
	public static final String Battery_Servive = "Battery Servive";
	public static final String Battery_Level = "Battery Level";
	
	// Immediate Alert Service
	public static final String Immediate_Alert_Service = "Immediate Alert Service";
	public static final String Immediate_Characteristic = "Immediate Characteristic";
	
	// Tx Power Service
	public static final String Tx_Power = "Tx Power";
	public static final String Tx_Power_Level = "Tx Power Level";
	
	// Simple Key Service
	public static final String Simple_Key_Service = "Simple Key Service";
	public static final String Key_Press_State = "Key Press State";
	
    // BLE Value String
	public final static String BATTERY_LEVEL_VALUE = "BATTERYLEVELVALUE";
	public final static String IMMEDIATE_ALERT_CHARACTERISTIC_VALUE = "IMMEDIATEALERTCHARACTERISTICVALUE";
	public final static String TXPOWER_LEVEL_VALUE = "TXPOWERLEVELVALUE";
	public final static String RSSI_VALUE = "RSSIVALUE";
	public final static String SINGLEKEYPRESS_VALUE = "SINGLEKEYPRESS_VALUE";
	public final static String SELFDEFINENOTIFY_VALUE = "SELFDEFINENOTIFYVALUE";
	
	// Fragment Tag
	public static final String CharacteristicsDetailFragment = "CharacteristicsDetailFragment";
}
