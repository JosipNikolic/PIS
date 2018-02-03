package com.example.josip.rpiandroidbt;


import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import android.net.Uri;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.UUID;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

public class CommsActivity extends AppCompatActivity {

    public static String msg;
    public BluetoothAdapter BTAdapter = BluetoothAdapter.getDefaultAdapter();
    /*private static final String TAG = "CommsActivity";*/
    BluetoothSocket mmSocket;
    BluetoothDevice mmDevice;

    final static String fileName = "data.txt";
    /*final static String path = Environment.getExternalStorageDirectory().getAbsolutePath()+"/"; PIS4*/
    final static String path = "/storage/emulated/0/"; /*PIS5*/
    final static String TAG = CommsActivity.class.getName();

    public class ConnectThread extends Thread {
        private ConnectThread(BluetoothDevice device) throws IOException {
            BluetoothSocket tmp = null;
            mmDevice = device;
            try {
                UUID uuid = UUID.fromString("eb12b754-00e8-11e8-ba89-0ed5f89f718b");
                tmp = mmDevice.createRfcommSocketToServiceRecord(uuid);
            } catch (IOException e) {
                Log.e(TAG, "Socket's create() method failed", e);
            }
            mmSocket = tmp;
            BTAdapter.cancelDiscovery();
            try {
                mmSocket.connect();
            } catch (IOException connectException) {
                Log.v(TAG, "Connection exception!");
                try {
                    mmSocket.close();
                } catch (IOException closeException) {

                }
            }
            send();
        }

        public void send() throws IOException {
            /*int id=findViewById(R.id.tempButton).getId();
            String msg = String.valueOf(id);*/
            OutputStream mmOutputStream = mmSocket.getOutputStream();
            mmOutputStream.write(CommsActivity.msg.getBytes());
            receive();
        }

        public void receive() throws IOException {
            InputStream mmInputStream = mmSocket.getInputStream();
            byte[] buffer = new byte[256];
            int bytes;

            try {
                    bytes = mmInputStream.read(buffer);
                    String readMessage = new String(buffer, 0, bytes);
                    Log.d(TAG, "Received: " + readMessage);
                    TextView result = (TextView) findViewById(R.id.result);

                    DateFormat df = new SimpleDateFormat("EEE, d MMM yyyy, HH:mm:ss z");
                    String date = df.format(Calendar.getInstance().getTime());


                    if (msg == "temp"){
                        result.setText("Vrijednost:\n" + readMessage + " °C");
                        readMessage = readMessage + " °C";
                        readMessage = readMessage + " - "+ String.valueOf(date);}
                    else if (msg == "hum"){
                        result.setText("Vrijednost:\n" + readMessage + " %R.H.");
                        readMessage = readMessage + " %R.H.";
                        readMessage = readMessage + " - "+ String.valueOf(date);}
                    else{
                        result.setText("Vrijednost:\n NaN");}

                    mmSocket.close();


                    saveToFile(readMessage);

                } catch (IOException e) {
                    Log.e(TAG, "Problems occurred!");
                }
            }
        }
    public static boolean saveToFile( String data){
        try {
            new File(path  ).mkdir();
            File file = new File(path+ fileName);
            if (!file.exists()) {
                file.createNewFile();
            }
            FileOutputStream fileOutputStream = new FileOutputStream(file,true);
            fileOutputStream.write((data + System.getProperty("line.separator")).getBytes());

            return true;
        }  catch(FileNotFoundException ex) {
            Log.d(TAG, ex.getMessage());
        }  catch(IOException ex) {
            Log.d(TAG, ex.getMessage());
        }
        return  false;


    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_comms);

        BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        final Intent intent = getIntent();
        final String address = intent.getStringExtra(MainActivity.EXTRA_ADDRESS);
        ImageButton tempButton = (ImageButton) findViewById(R.id.tempButton);
        ImageButton humButton = (ImageButton) findViewById(R.id.humButton);
        ImageButton googleButton = (ImageButton) findViewById(R.id.googleButton);
        ImageButton ebayButton = (ImageButton) findViewById(R.id.ebayButton);
        ImageButton powerButton = (ImageButton) findViewById(R.id.powerButton);

        tempButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                final BluetoothDevice device = BTAdapter.getRemoteDevice(address);
                try {
                        CommsActivity.msg="temp";
                        new ConnectThread(device).start();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });

        humButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                final BluetoothDevice device = BTAdapter.getRemoteDevice(address);
                try {
                    CommsActivity.msg="hum";
                    new ConnectThread(device).start();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });

        googleButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                try {
                    String url = "https://www.google.com/search?q=GY-213V-HTU21D";
                    Intent i = new Intent(Intent.ACTION_VIEW);
                    i.setData(Uri.parse(url));
                    startActivity(i);
                    finish();
                }catch (Exception e){
                    Toast.makeText(getApplicationContext(), "Can't open the link", Toast.LENGTH_SHORT).show();
                }
            }
        });

        ebayButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                try {
                    String url = "https://www.ebay.com/sch/i.html?_nkw=GY-213V-HTU21D";
                    Intent i = new Intent(Intent.ACTION_VIEW);
                    i.setData(Uri.parse(url));
                    startActivity(i);
                    finish();
                }catch (Exception e){
                    Toast.makeText(getApplicationContext(), "Can't open the link", Toast.LENGTH_SHORT).show();
                }
            }
        });

        powerButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                final BluetoothDevice device = BTAdapter.getRemoteDevice(address);
                try {
                    CommsActivity.msg="poweroff";
                    new ConnectThread(device).start();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });

        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableBluetooth = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBluetooth, 0);
        }
    }
    @Override
    protected void onStop() {
        super.onStop();
        try {
            mmSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}