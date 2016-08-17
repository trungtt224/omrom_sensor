package com.hvc.anhgt;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


import com.hvc.anhgt.hvc.BleDeviceSearch;
import com.hvc.anhgt.hvc.HVC;
import com.hvc.anhgt.hvc.HVCBleCallback;
import com.hvc.anhgt.hvc.HVC_BLE;
import com.hvc.anhgt.hvc.HVC_PRM;
import com.hvc.anhgt.hvc.HVC_RES;
import com.hvc.anhgt.R;
import com.hvc.anhgt.hvc.utils.MQTTUtils;

import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import java.util.List;

/**
 * Created by anhgt on 7/5/16.
 */

public class MainActivity extends Activity {
    public static final int EXECUTE_STOP = 0;
    public static final int EXECUTE_START = 1;
    public static final int EXECUTE_END = -1;

    private HVC_BLE hvcBle = null;
    private HVC_PRM hvcPrm = null;
    private HVC_RES hvcRes = null;

    private HVCDeviceThread hvcThread = null;

    private static int isExecute = 0;
    private static int nSelectDeviceNo = -1;
    private static List<BluetoothDevice> deviceList = null;
    private static DeviceDialogFragment newFragment = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        hvcBle = new HVC_BLE();
        hvcPrm = new HVC_PRM();
        hvcRes = new HVC_RES();

        hvcBle.setCallBack(hvcCallback);
        hvcThread = new HVCDeviceThread();
        hvcThread.start();

    }



    @Override
    public void onDestroy() {
        isExecute = EXECUTE_END;
        while ( isExecute == EXECUTE_END );
        if ( hvcBle != null ) {
            try {
                hvcBle.finalize();
            } catch (Throwable e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        hvcBle = null;
        super.onDestroy();
    }

    private class HVCDeviceThread extends Thread {
        @Override
        public void run()
        {
            isExecute = EXECUTE_STOP;
            while (isExecute != EXECUTE_END) {
                BluetoothDevice device = SelectHVCDevice("OMRON_HVC.*|omron_hvc.*");
                if ( (device == null) || (isExecute != EXECUTE_START) ) {
                    continue;
                }

                hvcBle.connect(getApplicationContext(), device);
                wait(15);

                hvcPrm.cameraAngle = HVC_PRM.HVC_CAMERA_ANGLE.HVC_CAMERA_ANGLE_0;
                hvcPrm.face.MinSize = 100;
                hvcPrm.face.MaxSize = 400;
                hvcBle.setParam(hvcPrm);
                wait(15);

                while ( isExecute == EXECUTE_START ) {
                    int nUseFunc = HVC.HVC_ACTIV_BODY_DETECTION |
                            HVC.HVC_ACTIV_HAND_DETECTION |
                            HVC.HVC_ACTIV_FACE_DETECTION |
                            HVC.HVC_ACTIV_FACE_DIRECTION |
                            HVC.HVC_ACTIV_AGE_ESTIMATION |
                            HVC.HVC_ACTIV_GENDER_ESTIMATION |
                            HVC.HVC_ACTIV_GAZE_ESTIMATION |
                            HVC.HVC_ACTIV_BLINK_ESTIMATION |
                            HVC.HVC_ACTIV_EXPRESSION_ESTIMATION;
                    hvcBle.execute(nUseFunc, hvcRes);
                    wait(30);
                }
                hvcBle.disconnect();
            }
            isExecute = EXECUTE_STOP;
        }

        public void wait(int nWaitCount)
        {
            do {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
                if ( !hvcBle.IsBusy() ) {
                    return;
                }
                nWaitCount--;
            } while ( nWaitCount > 0 );
        }
    }

    private BluetoothDevice SelectHVCDevice(String regStr) {
        if ( nSelectDeviceNo < 0 ) {
            if ( newFragment != null ) {
                BleDeviceSearch bleSearch = new BleDeviceSearch(getApplicationContext());
                // Show toast
                showToast("You can select a device");
                while ( newFragment != null ) {
                    deviceList = bleSearch.getDevices();
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                }
                bleSearch.stopDeviceSearch(getApplicationContext());
            }

            if ( nSelectDeviceNo > -1 ) {
                // Generate pattern to determine
                Pattern p = Pattern.compile(regStr);
                Matcher m = p.matcher(deviceList.get(nSelectDeviceNo).getName());
                if ( m.find() ) {
                    // Find HVC device
                    return deviceList.get(nSelectDeviceNo);
                }
                nSelectDeviceNo = -1;
            }
            return null;
        }
        return deviceList.get(nSelectDeviceNo);
    }

    private final HVCBleCallback hvcCallback = new HVCBleCallback() {
        @Override
        public void onConnected() {
            // Show toast
            showToast("Selected device has connected");
        }

        @Override
        public void onDisconnected() {
            // Show toast
            showToast("Selected device has disconnected");
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Button bt = (Button) findViewById(R.id.button2);
                    bt.setText(R.string.buttonS);
                }
            });
            isExecute = EXECUTE_STOP;
        }

        @Override
        public void onPostSetParam(int nRet, byte outStatus) {
            // Show toast
            String str = "Set parameters : " + String.format("ret = %d / status = 0x%02x", nRet, outStatus);
            showToast(str);
        }

        @Override
        public void onPostGetParam(int nRet, byte outStatus) {
            // Show toast
            String str = "Get parameters : " + String.format("ret = %d / status = 0x%02x", nRet, outStatus);
            showToast(str);
        }

        @Override
        public void onPostExecute(int nRet, byte outStatus) {
            if ( nRet != HVC.HVC_NORMAL || outStatus != 0 ) {
                    String str = "execute : " + String.format("ret = %d / status = 0x%02x", nRet, outStatus);
                    showToast(str.toString());
            } else {

                if (hvcRes.face.size() != 0 ) {
                    StringBuilder str = new StringBuilder();
                    for (HVC_RES.FaceResult faceResult : hvcRes.face) {
//                        if ( (hvcRes.executedFunc & HVC.HVC_ACTIV_FACE_DETECTION) != 0 ) {
//                            int size = faceResult.size;
//                            int posX = faceResult.posX;
//                            int posY = faceResult.posY;
//                            int conf = faceResult.confidence;
//                            str.append(String.format(",\"face_detection\" : {\"size\" : %d, \"x\" : %d, \"y\" : %d, \"conf\" : %d}", size, posX, posY, conf));
//                        }
//                        if ( (hvcRes.executedFunc & HVC.HVC_ACTIV_FACE_DIRECTION) != 0 ) {
//                            str.append(String.format(",\"face_direction\" : {\"yaw\" :  %d, \"pitch\" :  %d, \"roll\" :  %d, \"conf\" :  %d}",
//                                    faceResult.dir.yaw, faceResult.dir.pitch, faceResult.dir.roll, faceResult.dir.confidence));
//                        }
//                        if ( (hvcRes.executedFunc & HVC.HVC_ACTIV_AGE_ESTIMATION) != 0 ) {
//                            str.append(String.format(",\"age_stimation\" : {\"age\" :  %d, \"conf\" :  %d}",
//                                    faceResult.age.age, faceResult.age.confidence));
//                        }
//                        if ( (hvcRes.executedFunc & HVC.HVC_ACTIV_GENDER_ESTIMATION) != 0 ) {
//                            str.append(String.format(",\"gender_estimation\" : {\"gender\" :  %s, \"confidence\" :  %d}",
//                                    faceResult.gen.gender == HVC.HVC_GEN_MALE ? "\"male\"" : "\"female\"", faceResult.gen.confidence));
//                        }
//                        if ( (hvcRes.executedFunc & HVC.HVC_ACTIV_GAZE_ESTIMATION) != 0 ) {
//                            str.append(String.format(",\"gaze_estimation\" : {\"LR\" :  %d, \"UD\" :  %d}",
//                                    faceResult.gaze.gazeLR, faceResult.gaze.gazeUD));
//                        }
//                        if ( (hvcRes.executedFunc & HVC.HVC_ACTIV_BLINK_ESTIMATION) != 0 ) {
//                            str.append(String.format(",\"blink_estimation\" : {\"ratioL\" :  %d, \"ratioR\" :  %d}",
//                                    faceResult.blink.ratioL, faceResult.blink.ratioR));
//                        }
                        if ((hvcRes.executedFunc & HVC.HVC_ACTIV_EXPRESSION_ESTIMATION) != 0) {
                            if ( (hvcRes.executedFunc & HVC.HVC_ACTIV_AGE_ESTIMATION) != 0 ) {
                                if ( (hvcRes.executedFunc & HVC.HVC_ACTIV_GENDER_ESTIMATION) != 0 ) {
                                    str.append(String.format("{" +
                                                    "\"device_type\" : \"facial_sensing\", " +
                                                    "\"face_expression\" : %s, " +
                                                    "\"age\" :  %d, " +
                                                    "\"gender\" :  %s, " +
                                                    "\"gender_confidence\": %d, " +
                                                    "\"age_confidence\": %d," +
                                                    " \"expression_score\" : %d}",
                                            faceResult.exp.expression == HVC.HVC_EX_NEUTRAL ? "\"neutral\"" :
                                                    faceResult.exp.expression == HVC.HVC_EX_NEUTRAL ? "\"neutral\"" :
                                                            faceResult.exp.expression == HVC.HVC_EX_HAPPINESS ? "\"happiness\"" :
                                                                    faceResult.exp.expression == HVC.HVC_EX_SURPRISE ? "\"surprise\"" :
                                                                            faceResult.exp.expression == HVC.HVC_EX_ANGER ? "\"anger\"" :
                                                                                    faceResult.exp.expression == HVC.HVC_EX_SADNESS ? "\"sadness\"" : "",
                                            faceResult.age.age,
                                            faceResult.gen.gender == HVC.HVC_GEN_MALE ? "\"male\"" : "\"female\"",
                                            faceResult.gen.confidence,
                                            faceResult.age.confidence,
                                            faceResult.exp.score));
                                }
                            }
                        }
                    }

                    final String viewText = str.toString();
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            TextView tvVer = (TextView) findViewById(R.id.textView1);
                            tvVer.setText(viewText);

                            try {
                                MQTTUtils.sendMessageToBroker(viewText);
                            } catch (MqttException e) {
                                e.printStackTrace();
                            }
                        }
                    });
                }




            }
        }
    };

    public void onClick1(View view) {
        switch (view.getId()){
            case R.id.button1:
                if ( isExecute == EXECUTE_START ) {
                    // Show toast
                    Toast.makeText(this, "You are executing now", Toast.LENGTH_SHORT).show();
                    break;
                }

                nSelectDeviceNo = -1;
                newFragment = new DeviceDialogFragment();
                newFragment.setCancelable(false);
                newFragment.show(getFragmentManager(), "Bluetooth Devices");
                break;
        }
    }

    public void onClick2(View view) {
        switch (view.getId()){
            case R.id.button2:
                if ( nSelectDeviceNo == -1 ) {
                    // Show toast
                    Toast.makeText(this, "You must select device", Toast.LENGTH_SHORT).show();
                    break;
                }
                if ( isExecute == EXECUTE_STOP ) {
                    Button bt = (Button) findViewById(R.id.button2);
                    bt.setText(R.string.buttonE);
                    isExecute = EXECUTE_START;
                } else
                if ( isExecute == EXECUTE_START ) {
                    Button bt = (Button) findViewById(R.id.button2);
                    bt.setText(R.string.buttonS);
                    isExecute = EXECUTE_STOP;
                }
                break;
        }
    }

    public void showToast(final String str) {
        // Show toast
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(getApplicationContext(), str, Toast.LENGTH_SHORT).show();
            }
        });


    }

    public class DeviceDialogFragment extends DialogFragment {
        String[] deviceNameList = null;
        ArrayAdapter<String> ListAdpString = null;

        @SuppressLint("InflateParams")
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            // Use the Builder class for convenient dialog construction
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

            LayoutInflater inflater = (LayoutInflater)getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            View content = inflater.inflate(R.layout.devices, null);
            builder.setView(content);

            ListView listView = (ListView)content.findViewById(R.id.devices);
            // Set adapter
            ListAdpString = new ArrayAdapter<String>(getActivity(), android.R.layout.simple_list_item_single_choice);
            listView.setAdapter(ListAdpString);

            // Set the click event in the list view
            listView.setOnItemClickListener(new AdapterView.OnItemClickListener(){
                /**
                 * It is called when you click on an item
                 */
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    nSelectDeviceNo = position;
                    newFragment = null;
                    dismiss();
                }
            });

            DeviceDialogThread dlgThread = new DeviceDialogThread();
            dlgThread.start();

            builder.setMessage(getString(R.string.button1))
                    .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            newFragment = null;
                        }
                    });
            // Create the AlertDialog object and return it
            return builder.create();
        }

        private class DeviceDialogThread extends Thread {
            @Override
            public void run()
            {
                do {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if ( ListAdpString != null ) {
                                ListAdpString.clear();
                                if ( deviceList == null ) {
                                    deviceNameList = new String[] { "null" };
                                } else {
                                    synchronized (deviceList) {
                                        deviceNameList = new String[deviceList.size()];

                                        int nIndex = 0;
                                        for (BluetoothDevice device : deviceList) {
                                            if (device.getName() == null ) {
                                                deviceNameList[nIndex] = "no name";
                                            } else {
                                                deviceNameList[nIndex] = device.getName();
                                            }
                                            nIndex++;
                                        }
                                    }
                                }
                                ListAdpString.addAll(deviceNameList);
                                ListAdpString.notifyDataSetChanged();
                            }
                        }
                    });
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                } while(true);
            }
        }
    }

    @Override
    public void onBackPressed() {
        new AlertDialog.Builder(this)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setTitle(R.string.popup_title)
                .setMessage(R.string.popup_message)
                .setPositiveButton(R.string.popup_yes, new DialogInterface.OnClickListener()
                {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        try {
                            finish();
                        } catch (Throwable e) {
                            // TODO Auto-generated catch block
                            e.printStackTrace();
                        }
                    }
                })
                .setNegativeButton(R.string.popup_no, null)
                .show();
    }
}