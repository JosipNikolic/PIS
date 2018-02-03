package com.example.josip.rpiandroidbt;

import android.Manifest;
import android.bluetooth.BluetoothSocket;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;

import java.util.Set;
import java.util.ArrayList;
import java.util.UUID;

import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ArrayAdapter;
import android.content.Intent;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;

public class MainActivity extends AppCompatActivity {
    ImageButton enablebt, disablebt;
    Button scanbt;
    BluetoothSocket btSocket = null;
    private BluetoothAdapter BTAdapter;
    private Set<BluetoothDevice>pairedDevices;
    ListView lv;
    public final static String EXTRA_ADDRESS = null;
    static final UUID myUUID = UUID.fromString("5355ece6-00e9-11e8-ba89-0ed5f89f718b");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        enablebt=(ImageButton)findViewById(R.id.button_enablebt);
        disablebt=(ImageButton)findViewById(R.id.button_disablebt);
        scanbt=(Button)findViewById(R.id.button_scanbt);

        BTAdapter = BluetoothAdapter.getDefaultAdapter();
        lv = (ListView)findViewById(R.id.listView);
        if (BTAdapter.isEnabled()){
            scanbt.setVisibility(View.VISIBLE);
        }

    }

    public void userSupport(View view) {
        Intent sendMail = new Intent(this, MailActivity.class);
        startActivity(sendMail);
    }

    public void on(View v){
        if (!BTAdapter.isEnabled()) {
            Intent turnOn = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(turnOn, 0);
            Toast.makeText(getApplicationContext(), "Bluetooth ON",Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(getApplicationContext(), "Bluetooth Already ON", Toast.LENGTH_SHORT).show();
        }
        scanbt.setVisibility(View.VISIBLE);
        lv.setVisibility(View.VISIBLE);
}

    public void off(View v){
        BTAdapter.disable();
        Toast.makeText(getApplicationContext(), "Bluetooth OFF" ,Toast.LENGTH_SHORT).show();
        scanbt.setVisibility(View.INVISIBLE);
        lv.setVisibility(View.INVISIBLE);
    }

    public void deviceList(View v){
        ArrayList deviceList = new ArrayList();
        pairedDevices = BTAdapter.getBondedDevices();

        if (pairedDevices.size() < 1) {
            Toast.makeText(getApplicationContext(), "No paired devices found", Toast.LENGTH_SHORT).show();
        } else {
            for (BluetoothDevice bt : pairedDevices) deviceList.add(bt.getName() + " " + bt.getAddress());
            Toast.makeText(getApplicationContext(), "Showing paired devices", Toast.LENGTH_SHORT).show();
            final ArrayAdapter adapter = new ArrayAdapter(this, android.R.layout.simple_list_item_1, deviceList);
            lv.setAdapter(adapter);
            lv.setOnItemClickListener(myListClickListener);
        }
    }
    private AdapterView.OnItemClickListener myListClickListener = new AdapterView.OnItemClickListener() {

        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            String info = ((TextView) view).getText().toString();
            String address = info.substring(info.length() - 17);
            Toast.makeText(getApplicationContext(), info, Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(MainActivity.this, CommsActivity.class);
            intent.putExtra(EXTRA_ADDRESS, address);
            startActivity(intent);
        }
    };

}