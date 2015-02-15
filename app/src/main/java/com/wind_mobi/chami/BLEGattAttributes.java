package com.wind_mobi.chami;

import java.util.HashMap;

/**
* This class includes a small subset of standard GATT attributes for demonstration purposes.
*/
public class BLEGattAttributes {
   private static HashMap<String, String> attributes = new HashMap();
   
   static {
	   // Device Info Service
       attributes.put(UUIDConstants.DEVICE_INFO_SERVICE_UUID, Constants.Device_Information_Service);
       attributes.put(UUIDConstants.MANUFACTURER_MAME_STRING_UUID, Constants.Manufacturer_Name_String);
       
       // Heart Services.
       attributes.put(UUIDConstants.HEART_SERVICE_UUID, Constants.Heart_Rate_Service);
       attributes.put(UUIDConstants.HEART_RATE_MEASUREMENT_UUID, Constants.Heart_Rate_Measurement);
       
       // Battery Service
       attributes.put(UUIDConstants.BATTERY_SERVICE_UUID, Constants.Battery_Servive);
       attributes.put(UUIDConstants.BATTERY_LEVEL_UUID, Constants.Battery_Level);
       
       // Immediate Alert Service
       attributes.put(UUIDConstants.IMMEDIATE_ALERT_SERVICE_UUID, Constants.Immediate_Alert_Service);
       attributes.put(UUIDConstants.IMMEDIATE_ALERT_CHARACTERISTIC_UUID, Constants.Immediate_Characteristic);
       
       // Tx Power Service
       attributes.put(UUIDConstants.TX_POWER_UUID, Constants.Tx_Power);
       attributes.put(UUIDConstants.TX_POWER_LEVEL_UUID, Constants.Tx_Power_Level);
       
       // Simple Key Service
       attributes.put(UUIDConstants.SIMPLE_KEY_SERVICE_UUID, Constants.Simple_Key_Service);
       attributes.put(UUIDConstants.KEY_PRESS_STATE_UUID, Constants.Key_Press_State);
   }

   public static String lookup(String uuid, String defaultName) {
       String name = attributes.get(uuid);
       return name == null ? defaultName : name;
   }
}
