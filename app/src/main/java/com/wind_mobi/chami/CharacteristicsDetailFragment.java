package com.wind_mobi.chami;

import android.app.Fragment;
import android.bluetooth.BluetoothGattCharacteristic;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

public class CharacteristicsDetailFragment extends Fragment {
	
	private Button but_back = null; 
	
	private TextView txt_ServiceName = null;
	private TextView txt_ServiceUUID = null;
	private TextView txt_CharacteristicName = null;
	private TextView txt_CharacteristicUUID = null;
	private Button but_read = null;
	private Button but_notify = null;
	private Button but_write = null;
	private TextView txt_value = null;
    private TextView txt_value1 = null;
	private EditText edittxt_value = null;
	
	private int mProperity = 0;
	
	@Override
	public void onCreate(Bundle savedInstanceState)	{
		super.onCreate(savedInstanceState);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View v = inflater.inflate(R.layout.characteristic_detail_fragment, container, false);

		Initialize(v);
		
		return v;
	}
	
	@Override
	public void onResume() {
		
		
		super.onResume();
	}

	private void Initialize(View view){
		but_back = (Button) view.findViewById(R.id.but_back);
		txt_ServiceName = (TextView) view.findViewById(R.id.txt_ServiceName);
		txt_ServiceUUID = (TextView) view.findViewById(R.id.txt_ServiceUUID);
		txt_CharacteristicName = (TextView) view.findViewById(R.id.txt_CharacteristicName);
		txt_CharacteristicUUID = (TextView) view.findViewById(R.id.txt_CharacteristicUUID);
		but_read = (Button) view.findViewById(R.id.but_read);
		but_notify = (Button) view.findViewById(R.id.but_notify);
		but_write = (Button) view.findViewById(R.id.but_write);
		txt_value = (TextView) view.findViewById(R.id.txt_value);
        txt_value1 = (TextView) view.findViewById(R.id.txt_value1);
		edittxt_value = (EditText) view.findViewById(R.id.edittxt_value);
		
		SetCharacteristicInfo();
		
		SetListener();
	}
	
	public void SetCharacteristicInfo(){
		txt_ServiceName.setText(DeviceControlActivity.getServiceName());
		txt_ServiceUUID.setText(DeviceControlActivity.getServiceUUID());
		txt_CharacteristicName.setText(DeviceControlActivity.getCharacteristicName());
		txt_CharacteristicUUID.setText(DeviceControlActivity.getCharacteristicUUID());
		
		mProperity = DeviceControlActivity.getSelectBluetoothGattCharacteristicProperties();
		
		switch(mProperity){
			case BluetoothGattCharacteristic.PROPERTY_READ:
				but_read.setEnabled(true);
				but_notify.setEnabled(false);
				but_write.setEnabled(false);
				break;
			case BluetoothGattCharacteristic.PROPERTY_NOTIFY:
				but_read.setEnabled(false);
				but_notify.setEnabled(true);
				but_write.setEnabled(false);
				break;
			case BluetoothGattCharacteristic.PROPERTY_WRITE:
				but_read.setEnabled(false);
				but_notify.setEnabled(false);
				but_write.setEnabled(true);
				break;
		}
	}
	
	private void SetListener(){
		but_back.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				DeviceControlActivity.ShowDeviceInfo();
                Log.i("kim", "but_back");
			}
		});
		
		but_notify.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				DeviceControlActivity.StartNotify();
                Log.i("kim", "but_notify");
			}
		});
		
		but_write.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				if(edittxt_value.getText().length() == 0) {
                    DeviceControlActivity.WriteValue(0);
                    Log.i("kim", "WriteValue(0)");
                }else {
                    DeviceControlActivity.WriteValue(Integer.valueOf(edittxt_value.getText().toString()));
                    Log.i("kim", "edittxt_value.getText()");
                }
			}
		});
	}
	
	public void UpdateNotifyValue(String strValue){
		txt_value.setText("泡茶次数: " + strValue.substring(0, strValue.indexOf("|")));
        txt_value1.setText("泡茶温度: " + strValue.substring(strValue.indexOf("|")+1));
        Log.i("kim", "Notify value: " + strValue);
	}
}
