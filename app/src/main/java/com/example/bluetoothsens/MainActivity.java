package com.example.bluetoothsens;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import android.Manifest;
import android.annotation.SuppressLint;

import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;

import android.content.ContentResolver;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.AssetFileDescriptor;
import android.media.AudioAttributes;

import android.media.MediaPlayer;
import android.net.Uri;

import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;

import android.os.Message;
import android.os.ParcelFileDescriptor;
import android.os.SystemClock;

import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.ListView;

import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;


import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;

import java.util.ArrayList;
import java.util.Set;
import java.util.UUID;




public class MainActivity extends AppCompatActivity {
    Button  diser, btndis;
    Switch on_off;
    ImageView alert;
    ListView lw;
    boolean fail = false;
    BluetoothAdapter myBluetoothadapter = BluetoothAdapter.getDefaultAdapter();
    Intent btEnablingIntent;
    int requestcodeforenable;
    Intent myfileintent;
    String music;
    public static final String PREFS_NAME = "Preference";
    /**/
    final static int MY_PERMISSIONS_REQUEST=1;
    Runnable runnable;
    private boolean ala =true;
    MediaPlayer mysound=null;
    ArrayList list = new ArrayList();
    private final static int MESSAGE_READ = 2;
    private final static int CONNECTING_STATUS = 3;
    private ConnectedThread mConnectedThread;
    String address = null;
    private ArrayAdapter<String> arrayAdapter;
    private ProgressDialog progress;
    BluetoothSocket btSocket = null;
    private boolean isBtConnected = false;
//    static final UUID myUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    private static final UUID BTMODULEUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
             if(ContextCompat.checkSelfPermission(this,
                Manifest.permission.READ_EXTERNAL_STORAGE)
                !=PackageManager.PERMISSION_GRANTED){
            if(ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.READ_EXTERNAL_STORAGE)){
                Toast.makeText(this, "External Storage Permission Needed", Toast.LENGTH_SHORT).show();
            }
            else {
                ActivityCompat.requestPermissions(this,new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                        MY_PERMISSIONS_REQUEST);
                Toast.makeText(this, "External Storage Permission Needed", Toast.LENGTH_SHORT).show();
            }
        }
        Toolbar toolbar=findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        on_off=findViewById(R.id.on_off);
        alert=findViewById(R.id.alert);
        lw = findViewById(R.id.btlist);
        diser = findViewById(R.id.discover);
        btndis = findViewById(R.id.btndis);
         btEnablingIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
        requestcodeforenable = 1;



        SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
        SharedPreferences.Editor editor = settings.edit();
        String musicpath=settings.getString("muscipath","");
        if(musicpath.equals("")){
            mysound=MediaPlayer.create(getApplicationContext(),R.raw.sss);
        }
        if(!musicpath.equals("")){
               try {
                        Uri contenturi = Uri.parse(Environment.getExternalStorageDirectory().getPath() + musicpath);
                        mysound = new MediaPlayer();
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {

                            mysound.setAudioAttributes(new AudioAttributes.Builder().setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).setUsage(AudioAttributes.USAGE_MEDIA).build());
                        }
                        try {
                            mysound.setDataSource(getApplicationContext(), contenturi);
                            mysound.prepare();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
               }
                catch (Exception e){editor.putString("muscipath", "").apply();
               mysound=MediaPlayer.create(getApplicationContext(),R.raw.sss);}
           }
        if(myBluetoothadapter.isEnabled()){on_off.setChecked(true);}
        else{on_off.setChecked(false);}
        on_off.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked){
                    if (!myBluetoothadapter.isEnabled()) {
                        startActivityForResult(btEnablingIntent, requestcodeforenable);
                        btndis.setClickable(true);
                    }
                }
                else {
                    if (myBluetoothadapter.isEnabled()) {
                    myBluetoothadapter.disable();
                    btndis.setClickable(false);
                    msg("Bluetooth is Disable");
                }}
            }
        });
        bluedevicelist();
        btndis.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                 if (myBluetoothadapter.isEnabled()) {
                    if(fail==false)
                    {try {
                        mConnectedThread.cancel();
                    }catch (Exception e){}}
                }else if(!myBluetoothadapter.isEnabled()){msg("Bluetooth is not enable");}
            }
        });
       runnable=new Runnable() {
           @Override
           public void run() {
                   //mysound.prepare();
                   //msg(Environment.getExternalStorageDirectory().getPath()+music);
              try { mysound.start();}catch (Exception e){msg("Custom sound need Storage Permission");}
               //mysound=MediaPlayer.create(getApplicationContext(), Uri.parse(music));
           }
       };
       lw.setOnItemClickListener(myListClickListener);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu,menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id=item.getItemId();

        if(id==R.id.alarmpath){
            myfileintent=new Intent(Intent.ACTION_GET_CONTENT);
            myfileintent.setType("audio/*");
            startActivityForResult(myfileintent,10);
        }
        if(id==R.id.about)
        {
           Intent i=new Intent(MainActivity.this,about.class);
        startActivity(i);
          //  runnable.run();
        }
        if(id==R.id.alarmdefault){
          SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
        SharedPreferences.Editor editor = settings.edit();
        editor.putString("muscipath", "").apply();
        msg("Default Alert Sound");
        mysound=MediaPlayer.create(getApplicationContext(),R.raw.sss);
        }
        else if(id==R.id.alersoundprofile)
        {
            if(ala==true) {
               // Toast.makeText(getApplicationContext(), "Silent", Toast.LENGTH_SHORT).show();
                item.setIcon(R.drawable.alertoff);
                ala=false;
                try {
                    mysound.setVolume(0,0);
                }catch (Exception e){}
            }else{item.setIcon(R.drawable.alerton);
            //Toast.makeText(getApplicationContext(), "", Toast.LENGTH_SHORT).show();
            ala=true;
            try {
                mysound.setVolume(1,1);
            }catch (Exception e){}
            }
        }
        return true;
    }

    private AdapterView.OnItemClickListener myListClickListener = new AdapterView.OnItemClickListener() {
        public void onItemClick(AdapterView av, View v, int arg2, long arg3) {
            // Get the device MAC address, the last 17 chars in the View
//        String info = ((TextView) v).getText().toString();
//        address = info.substring(info.length() - 17);
//        msg(info+""+address);
//         new ConnectBT().execute();
            if (!myBluetoothadapter.isEnabled()) {
                Toast.makeText(getBaseContext(), "Bluetooth not on", Toast.LENGTH_SHORT).show();
                return;
            }
            msg("Connecting to bluetooth...");
            // Get the device MAC address, which is the last 17 chars in the View
            String info = ((TextView) v).getText().toString();
             address = info.substring(info.length() - 17);
            final String name = info.substring(0, info.length() - 17);
            // Spawn a new thread to avoid blocking the GUI one
            new Thread() {
                public void run() {
                    BluetoothDevice device = myBluetoothadapter.getRemoteDevice(address);
                    try {
                        btSocket = createBluetoothSocket(device);
                    } catch (IOException e) {
                        fail = true;
                        Toast.makeText(getBaseContext(), "Socket creation failed", Toast.LENGTH_SHORT).show();
                    }
                    // Establish the Bluetooth socket connection.
                    try {
                        btSocket.connect();
                    } catch (IOException e) {
                        try {
                            fail = true;
                            btSocket.close();
                            mHandler.obtainMessage(CONNECTING_STATUS, -1, -1)
                                    .sendToTarget();
                        } catch (IOException e2) {
                            //insert code to deal with this
                            Toast.makeText(getBaseContext(), "Socket creation failed", Toast.LENGTH_SHORT).show();
                        }
                    }
                    if (fail == false) {
                        mConnectedThread = new ConnectedThread(btSocket);
                        mConnectedThread.start();
                        mHandler.obtainMessage(CONNECTING_STATUS, 1, -1, name).sendToTarget();
                    }
                }
            }.start();
        }
    };




    private BluetoothSocket createBluetoothSocket(BluetoothDevice device) throws IOException {
        return device.createRfcommSocketToServiceRecord(BTMODULEUUID);
        //creates secure outgoing connection with BT device using UUID
    }

    private void bluedevicelist() {
        diser.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Set<BluetoothDevice> bt = myBluetoothadapter.getBondedDevices();
                list.clear();
                 arrayAdapter = new ArrayAdapter(getApplicationContext(), android.R.layout.simple_list_item_1, list);
                 lw.setAdapter(arrayAdapter);

                 if(!myBluetoothadapter.isEnabled()){
                    msg("Enable the Bluetooth");
                 }
                if (bt.size() > 0) {
                    for (BluetoothDevice device : bt) {
                        list.add(device.getName() + "\n" + device.getAddress());
                            arrayAdapter = new ArrayAdapter(getApplicationContext(), android.R.layout.simple_list_item_1, list);
                 lw.setAdapter(arrayAdapter);
                    }
                }
            }
        });

    }
    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
//   super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == requestcodeforenable) {
            if (resultCode == RESULT_OK) {
                Toast.makeText(getApplicationContext(), "Bluetooth is Enable", Toast.LENGTH_SHORT).show();
            } else if (resultCode == RESULT_CANCELED) {
                on_off.setChecked(false);
                Toast.makeText(getApplicationContext(), "Bluetooth Enabling Cancelled", Toast.LENGTH_SHORT).show();
            }
        }
        if(requestCode==10){
            if(resultCode==RESULT_OK){
                music =data.getData().getPath();
                int i=music.indexOf(":" );
                music="/"+music.substring(i+1);
                //msg(music);
                 SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
                 SharedPreferences.Editor editor = settings.edit();
                 editor.putString("muscipath", music).apply();
                 //mysound=MediaPlayer.create(getApplicationContext(),Uri.parse(Environment.getExternalStorageDirectory().getPath()+music));
                 //String str=Environment.getExternalStorageDirectory().getPath()+music;
                 //msg(str);

                        Uri contenturi = Uri.parse(Environment.getExternalStorageDirectory().getPath()+music );
                        mysound = new MediaPlayer();
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {

                            mysound.setAudioAttributes(new AudioAttributes.Builder().setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).setUsage(AudioAttributes.USAGE_MEDIA).build());
                        }
                        try{
                           mysound.setDataSource(getApplicationContext(),contenturi);
                            mysound.prepare();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }


            }

        }
    }

    private void msg(String s) {
        Toast.makeText(getApplicationContext(), s, Toast.LENGTH_LONG).show();
    }

    private class ConnectedThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;

        public ConnectedThread(BluetoothSocket socket) {
            mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            // Get the input and output streams, using temp objects because
            // member streams are final
            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) {
            }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }

        public void run() {
            byte[] buffer = new byte[1024];  // buffer store for the stream
            int bytes; // bytes returned from read()
            // Keep listening to the InputStream until an exception occurs
            while (true) {
                try {
                    // Read from the InputStream
                    bytes = mmInStream.available();
                    if (bytes != 0) {
                        SystemClock.sleep(100); //pause and wait for rest of data. Adjust this depending on your sending speed.
                        bytes = mmInStream.available(); // how many bytes are ready to be read?
                        bytes = mmInStream.read(buffer, 0, bytes); // record how many bytes we actually read
                        mHandler.obtainMessage(MESSAGE_READ, bytes, -1, buffer)
                                .sendToTarget(); // Send the obtained bytes to the UI activity
                    }
                } catch (IOException  e) {
                    e.printStackTrace();
                    break;
                }
            }
        }

      public void write(String input) {
            byte[] bytes = input.getBytes();           //converts entered String into bytes
            try {
                mmOutStream.write(bytes);
            } catch (IOException e) { }
        }

        /* Call this from the main activity to shutdown the connection */
        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) { }
        }
    }
    @SuppressLint("HandlerLeak")
    private final Handler mHandler = new Handler() {
        public void handleMessage(Message bms) {
            if (bms.what == MESSAGE_READ) {
                String readMessage = null;

                try {
                    readMessage = new String((byte[]) bms.obj, "UTF-8");

                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
                //mConnectedThread.getPriority();
               // msg(  ""+mConnectedThread.getPriority()+"");

                list.add(0, readMessage);
                lw.setAdapter(arrayAdapter);
                if(readMessage.contains("Alert"))
                {alert.setVisibility(View.VISIBLE);
                runnable.run();}
                else{alert.setVisibility(View.INVISIBLE);
                }

            }
            if (bms.what == CONNECTING_STATUS) {
                if (bms.arg1 == 1)
                    msg("Connected to Device: " + (String) (bms.obj));
                else
                    msg("Connection Failed");
            }
        }
    };

    @Override
    protected void onDestroy() {
        myBluetoothadapter.disable();
        on_off.setChecked(false);
        super.onDestroy();
    }
}