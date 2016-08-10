package com.hvc.anhgt.hvc.utils;

import android.content.Context;
import android.util.Log;
import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

/**
 * Created by anhgt on 7/6/16.
 */
public class MQTTUtils {

    String clientId = MqttClient.generateClientId();
    MqttAndroidClient client ;

    public MQTTUtils(Context context){
        client = new MqttAndroidClient(context, "tcp://10.22.20.140:1883","ffffd");
    }

    public void connect(){
        try {

            IMqttToken token = client.connect();
            token.setActionCallback(new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    // We are connected
                  //  Log.d("TAG", "onSuccess");

                    publish("ss","zz");
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    // Something went wrong e.g. connection timeout or firewall problems
                    Log.d("TAG", "onFailure");

                }
            });
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    public void publish(String topic,String payload){

        byte[] encodedPayload = new byte[0];
        try {
            encodedPayload = payload.getBytes("UTF-8");
            MqttMessage message = new MqttMessage(payload.getBytes());
            message.setQos(0);
            client.publish(topic, message);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void subcribe(String topic){
        int qos = 1;
        try {
            IMqttToken subToken = client.subscribe(topic, qos);
            subToken.setActionCallback(new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    // The message was published
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken,
                                      Throwable exception) {
                    // The subscription could not be performed, maybe the user was not
                    // authorized to subscribe on the specified topic e.g. using wildcards
                }
            });
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }


    public void unsubcribe(String topic){
        try {
            IMqttToken unsubToken = client.unsubscribe(topic);
            unsubToken.setActionCallback(new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    // The subscription could successfully be removed from the client
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken,
                                      Throwable exception) {
                    // some error occurred, this is very unlikely as even if the client
                    // did not had a subscription to the topic the unsubscribe action
                    // will be successfully
                }
            });
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    public void disconnect(){
        try {
            IMqttToken disconToken = client.disconnect();
            disconToken.setActionCallback(new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    // we are now successfully disconnected
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken,
                                      Throwable exception) {
                    // something went wrong, but probably we are disconnected anyway
                }
            });
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }


    public static void sendMessageToBroker(String message) throws MqttException {

        final String topic        = "iot";
        final String content      = message;
        final int qos             = 0;
        String broker             = "tcp://10.22.20.140:1888";
        String clientId           = "12ds3";

        MemoryPersistence persistence = new MemoryPersistence();

        try {
            MqttClient sampleClient = new MqttClient(broker, clientId, persistence);
            MqttConnectOptions connOpts = new MqttConnectOptions();
            connOpts.setUserName("pub/1/face_detection_stream/omron");
            connOpts.setPassword("hdaf33f14f24744a0a0e4337c40bcbc0e4bd57de85bebc14475e007cbc005e680".toCharArray());

            connOpts.setCleanSession(true);
            System.out.println("Connecting to broker: "+broker);
            sampleClient.connect(connOpts);
            System.out.println("Connected");
            System.out.println("Publishing message: " +message);
            MqttMessage msg = new MqttMessage(content.getBytes());
            msg.setQos(qos);
            sampleClient.publish(topic, msg);
            System.out.println("Message published");;
        } catch(MqttException me) {
            System.out.println("reason "+me.getReasonCode());
            System.out.println("msg "+me.getMessage());
            System.out.println("loc "+me.getLocalizedMessage());
            System.out.println("cause "+me.getCause());
            System.out.println("excep "+me);
            me.printStackTrace();
        }
//
//        final MqttAndroidClient client =
//                new MqttAndroidClient(this.getApplicationContext(), broker,
//                        clientId);
//
//        try {
//            IMqttToken token = client.connect();
//            token.setActionCallback(new IMqttActionListener() {
//                @Override
//                public void onSuccess(IMqttToken asyncActionToken) {
//                    // We are connected
//                    MqttMessage msg = new MqttMessage(content.getBytes());
//                    msg.setQos(qos);
//                    try {
//                        client.publish(topic, msg);
//
//                    } catch (MqttException e) {
//                        e.printStackTrace();
//                    }
//                    Log.d("tag", "onSuccess");
//                }
//
//                @Override
//                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
//                    // Something went wrong e.g. connection timeout or firewall problems
//                    Log.d("tag", "onFailure");
//
//                }
//            });
//        } catch (MqttException e) {
//            e.printStackTrace();
//        }
    }



}