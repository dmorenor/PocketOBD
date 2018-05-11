package com.example.danny.pocketobd;

import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.DialogInterface;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.graphics.drawable.GradientDrawable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.TextView;
import android.widget.ImageView;
import android.widget.Toast;

import com.github.pires.obd.commands.SpeedCommand;
import com.github.pires.obd.commands.engine.RPMCommand;
import com.github.pires.obd.commands.engine.ThrottlePositionCommand;
import com.github.pires.obd.commands.fuel.FuelLevelCommand;
import com.github.pires.obd.commands.protocol.EchoOffCommand;
import com.github.pires.obd.commands.protocol.LineFeedOffCommand;
import com.github.pires.obd.commands.protocol.SelectProtocolCommand;
import com.github.pires.obd.commands.protocol.TimeoutCommand;
import com.github.pires.obd.commands.temperature.AirIntakeTemperatureCommand;
import com.github.pires.obd.commands.temperature.EngineCoolantTemperatureCommand;
import com.github.pires.obd.enums.ObdProtocols;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Set;
import java.util.UUID;


public class MainActivity extends AppCompatActivity {
    String deviceAddress;
    BluetoothSocket mmSocket;
    Boolean btConnected = false;
    Boolean skConnected = false;
    Context contex;
    int duration = Toast.LENGTH_SHORT;
    int rounded = 0;
    int rounded2 = 0;
    int rounded3 = 0;
    float convert = 0;
    float convert2 = 0;
    Runnable runnable;
    Handler handler = new Handler();

    private TextView rpmView;
    private TextView mphView;
    private TextView tposView;
    private TextView fuelView;
    private TextView airTempView;
    private TextView coolantTempView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        final Button button = (Button) findViewById(R.id.button);
        rpmView = (TextView) findViewById(R.id.rpm_id);
        mphView = (TextView) findViewById(R.id.mph_id);
        tposView = (TextView) findViewById(R.id.tpos_id);
        fuelView = (TextView) findViewById(R.id.fuel_id);
        airTempView = (TextView) findViewById(R.id.airtemp_id);
        coolantTempView = (TextView) findViewById(R.id.cooltemp_id);

        btConnect();

        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(btConnected == true) {
                    handler.removeCallbacks(runnable);

                    try {
                        mmSocket.close();
                        button.setText("CONNECT");
                        btConnected = false;
                        rpmView.setText("0");
                        mphView.setText("0");
                        tposView.setText("0");
                        fuelView.setText("0");
                        airTempView.setText("0");
                        coolantTempView.setText("0");
                        contex = getApplicationContext();
                        Toast toast = Toast.makeText(contex, "Bluetooth connection terminated", duration);
                        toast.show();
                    }
                    catch (IOException e) {
                        // Error
                        contex = getApplicationContext();
                        Toast toast = Toast.makeText(contex, "Unable to disconnect: " + e.toString(), duration);
                        toast.show();
                    }

                }
                else {
                    btConnect();
                    button.setText("DISCONNECT");
                }
            }
        });
    }

    //Allows user to select bluetooth adapter and creates connection
    public void btConnect() {
        ArrayList deviceStrs = new ArrayList();
        final ArrayList devices = new ArrayList();

        BluetoothAdapter btAdapter = BluetoothAdapter.getDefaultAdapter();
        Set<BluetoothDevice> pairedDevices = btAdapter.getBondedDevices();
        if (pairedDevices.size() > 0)
        {
            for (BluetoothDevice device : pairedDevices)
            {
                deviceStrs.add(device.getName() + "\n" + device.getAddress());
                devices.add(device.getAddress());
            }
        }

        // show list
        final AlertDialog.Builder alertDialog = new AlertDialog.Builder(this);

        ArrayAdapter adapter = new ArrayAdapter(this, android.R.layout.select_dialog_singlechoice,
                deviceStrs.toArray(new String[deviceStrs.size()]));

        alertDialog.setSingleChoiceItems(adapter, -1, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which)
            {
                dialog.dismiss();
                int position = ((AlertDialog) dialog).getListView().getCheckedItemPosition();
                deviceAddress = devices.get(position).toString();

                BluetoothAdapter btAdapter = BluetoothAdapter.getDefaultAdapter();

                BluetoothDevice mmDevice = btAdapter.getRemoteDevice(deviceAddress);

                UUID uuid = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

                try {
                    mmSocket = mmDevice.createRfcommSocketToServiceRecord(uuid);
                    mmSocket.connect();

                    contex = getApplicationContext();
                    Toast toast = Toast.makeText(contex, "Bluetooth connection successful", duration);
                    toast.show();
                    btConnected = true;
                }
                catch (IOException e){
                    // handle errors
                    contex = getApplicationContext();
                    Toast toast = Toast.makeText(contex, "Bluetooth connection error: " + e.toString(), duration);
                    toast.show();
                }

                if(btConnected == true) {
                    socketConnect();

                    if(skConnected == true) {
                        liveData();
                    }
                }
            }
        });

        alertDialog.setTitle("Choose Bluetooth device");
        alertDialog.show();
    }

    //Inplements input/output stream for bluetooth socket
    public void socketConnect() {
        try {
            new EchoOffCommand().run(mmSocket.getInputStream(), mmSocket.getOutputStream());
            new LineFeedOffCommand().run(mmSocket.getInputStream(), mmSocket.getOutputStream());
            new TimeoutCommand(125).run(mmSocket.getInputStream(), mmSocket.getOutputStream());
            new SelectProtocolCommand(ObdProtocols.ISO_15765_4_CAN).run(mmSocket.getInputStream(), mmSocket.getOutputStream());

            contex = getApplicationContext();
            Toast toast = Toast.makeText(contex, "Reading live data", duration);
            toast.show();
            skConnected = true;
        } catch (Exception e) {
            // handle errors
            contex = getApplicationContext();
            Toast toast = Toast.makeText(contex, "API Error: " + e.toString(), duration);
            toast.show();
        }
    }

    //Handles live data read and display
    public void liveData() {

        runnable = new Runnable() {
            @Override
            public void run() {
                try {
                    RPMCommand engineRpmCommand = new RPMCommand();
                    SpeedCommand mphCommand = new SpeedCommand();
                    ThrottlePositionCommand tposCommand = new ThrottlePositionCommand();
                    FuelLevelCommand fuelLevelCommand = new FuelLevelCommand();
                    AirIntakeTemperatureCommand airTempCommand =  new AirIntakeTemperatureCommand();
                    EngineCoolantTemperatureCommand engTempCommand =  new EngineCoolantTemperatureCommand();

                    engineRpmCommand.run(mmSocket.getInputStream(), mmSocket.getOutputStream());
                    mphCommand.run(mmSocket.getInputStream(), mmSocket.getOutputStream());
                    tposCommand.run(mmSocket.getInputStream(), mmSocket.getOutputStream());
                    fuelLevelCommand.run(mmSocket.getInputStream(), mmSocket.getOutputStream());
                    airTempCommand.run(mmSocket.getInputStream(), mmSocket.getOutputStream());
                    engTempCommand.run(mmSocket.getInputStream(), mmSocket.getOutputStream());

                    float mphConvert = mphCommand.getImperialSpeed();
                    rounded = Math.round(mphConvert);

                    float throttlePos = tposCommand.getPercentage();
                    throttlePos = round(throttlePos, 2);

                    float fuelLevel = fuelLevelCommand.getFuelLevel();
                    fuelLevel = round(fuelLevel, 2);

                    String air = airTempCommand.getCalculatedResult();
                    convert = Float.parseFloat(air);
                    convert = (9/5) * convert + 32;
                    rounded2 = Math.round(convert);

                    String coolant = engTempCommand.getCalculatedResult();
                    convert2 = Float.parseFloat(coolant);
                    convert2 = (9/5) * convert2 + 32;
                    rounded3 = Math.round(convert2);

                    rpmView.setText(engineRpmCommand.getCalculatedResult());
                    mphView.setText(Integer.toString(rounded));
                    tposView.setText(Float.toString(throttlePos));
                    fuelView.setText(Float.toString(fuelLevel));
                    airTempView.setText(Integer.toString(rounded2));
                    coolantTempView.setText(Integer.toString(rounded3));
                }
                catch (Exception e){
                    // handle errors
                    contex = getApplicationContext();
                    Toast toast = Toast.makeText(contex, "Live data Error: " + e.toString(), duration);
                    toast.show();
                }
                handler.postDelayed(runnable, 100);
            }
        };
        handler.post(runnable);
    }

    //Rounding function
    public static float round(float number, int decPoint) {
        int num = 10;
        for (int i = 1; i < decPoint; i++)
            num *= 10;
        float tmp = number * num;
        return ( (float) ( (int) ((tmp - (int) tmp) >= 0.5f ? tmp + 1 : tmp) ) ) / num;
    }

    //Ends threads when app is terminated
    @Override
    protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacks(runnable);
    }
}