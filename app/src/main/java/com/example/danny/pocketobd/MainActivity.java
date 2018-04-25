package com.example.danny.pocketobd;

import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.DialogInterface;
import android.content.Context;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import com.github.pires.obd.commands.protocol.EchoOffCommand;
import com.github.pires.obd.commands.protocol.LineFeedOffCommand;
import com.github.pires.obd.commands.protocol.SelectProtocolCommand;
import com.github.pires.obd.commands.protocol.TimeoutCommand;
import com.github.pires.obd.enums.ObdProtocols;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Set;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {
    String deviceAddress;
    BluetoothSocket mmSocket;
    int duration = Toast.LENGTH_SHORT;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

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

                    Context contex = getApplicationContext();

                    Toast toast = Toast.makeText(contex, "Connection successful", duration);
                    toast.show();
                }
                catch (IOException e){
                    // handle errors
                    Context contex = getApplicationContext();

                    Toast toast = Toast.makeText(contex, "Connection error: " + e.toString(), duration);
                    toast.show();
                }

                try {
                    new EchoOffCommand().run(mmSocket.getInputStream(), mmSocket.getOutputStream());
                    new LineFeedOffCommand().run(mmSocket.getInputStream(), mmSocket.getOutputStream());
                    new TimeoutCommand(125).run(mmSocket.getInputStream(), mmSocket.getOutputStream());
                    new SelectProtocolCommand(ObdProtocols.AUTO).run(mmSocket.getInputStream(), mmSocket.getOutputStream());
                } catch (Exception e) {
                    // handle errors
                    Context contex = getApplicationContext();

                    Toast toast = Toast.makeText(contex, "W" + e.toString(), duration);
                    toast.show();
                }
            }
        });

        alertDialog.setTitle("Choose Bluetooth device");
        alertDialog.show();
    }
}