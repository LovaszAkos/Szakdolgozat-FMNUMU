package com.fmnumu.szakdolgozat.ui.home;

import android.content.DialogInterface;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.fmnumu.szakdolgozat.MainActivity;
import com.fmnumu.szakdolgozat.R;
import com.fmnumu.szakdolgozat.databinding.FragmentHomeBinding;
import com.google.android.material.checkbox.MaterialCheckBox;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.switchmaterial.SwitchMaterial;

import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

public class HomeFragment extends Fragment {

    private HomeViewModel homeViewModel;
    private FragmentHomeBinding binding;
    private String topic = "";


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        homeViewModel =
                new ViewModelProvider(this).get(HomeViewModel.class);

        binding = FragmentHomeBinding.inflate(inflater, container, false);
        View root = binding.getRoot();
        LinearLayout layout = (LinearLayout) root.findViewById(R.id.cardList);

        MqttAndroidClient mqttAndroidClient = ((MainActivity)getActivity()).getClient();

        if (mqttAndroidClient != null && mqttAndroidClient.isConnected()){
            mqttNotifier(root, mqttAndroidClient);
        }

        FloatingActionButton fabSub = root.findViewById(R.id.fabSubscribe);
        fabSub.setOnClickListener(new View.OnClickListener(){
            public void onClick(View view){
                AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
                builder.setTitle("Enter topic to subscribe to");

                final EditText input = new EditText(getContext());

                input.setInputType(InputType.TYPE_CLASS_TEXT);
                input.setWidth(10);
                builder.setView(input);

                builder.setPositiveButton("Subscribe", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        topic = input.getText().toString();
                        if (topic.equals("")){
                            Toast toast = Toast.makeText(getContext(), "Topic can't be empty", Toast.LENGTH_SHORT);
                            toast.show();
                        }
                        else{
                            subscribeMQTT(((MainActivity)getActivity()).getClient(), topic);
                        }
                    }
                });
                builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                });

                builder.show();
            }
        });

        return root;
    }

    private void createTile(LinearLayout layout, String topic, int type) {
        View mqttCard = this.getLayoutInflater().inflate(type, null);
        TextView topicDisplay = (TextView) mqttCard.findViewById(R.id.text_topicDisplay);
        topicDisplay.setText(topic);
        layout.addView(mqttCard);
    }

    private void subscribeMQTT(MqttAndroidClient mqttAndroidClient, String topic){
        LinearLayout layout = (LinearLayout) getView().findViewById(R.id.cardList);
        try {
            if (!mqttAndroidClient.isConnected()) {
                mqttAndroidClient.connect();
            }
            mqttAndroidClient.subscribe(topic, 0, getContext(), new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    snackBarMaker(getView(), "Subscribed to " + topic);
                    if (!((MainActivity)getActivity()).getAllTopics().contains(topic)){
                        ((MainActivity)getActivity()).addTopic(topic);
                    }
                    createTile(layout, topic, R.layout.mqtt_card_text);

                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                   snackBarMaker(getView(), "Failed to subscribe");
                }
            });
        } catch (Exception e) {
            Log.d("tag","Error :" + e);
        }
    }


    private void mqttNotifier(View root, MqttAndroidClient mqttAndroidClient){

        try {
            if (!mqttAndroidClient.isConnected()) {
                mqttAndroidClient.connect();
            }
            mqttAndroidClient.setCallback(new MqttCallback() {
                @Override
                public void connectionLost(Throwable cause) {
                    Snackbar snackbar = Snackbar
                            .make(root.findViewById(R.id.snackRoot), "mqtt connection lost", Snackbar.LENGTH_SHORT);
                    snackbar.setAction("Reconnect", new snackBarReconnectListener());
                    snackbar.show();
                }

                @Override
                public void messageArrived(String topic, MqttMessage message) throws Exception {

                    snackBarMaker(root, "received " + decodeMQTT(message) + " on topic: " + topic);

                    LinearLayout cardList = root.findViewById(R.id.cardList);
                    for (int i = 0; i < cardList.getChildCount(); i++) {
                        cardHandlerOnMessageReceived(topic, message, cardList, i);
                    }
                }

                @Override
                public void deliveryComplete(IMqttDeliveryToken token) {

                }
            });

        }
        catch (MqttException e){
           snackBarMaker(root, "mqtt connection lost");
        }

    }

    private void cardHandlerOnMessageReceived(String topic, MqttMessage message, LinearLayout cardList, int i) throws UnsupportedEncodingException {

        String topicDisplay = (String) ((TextView) cardList.getChildAt(i).findViewById(R.id.text_topicDisplay)).getText();
        
        ViewGroup cardRoot = (ViewGroup) cardList.getChildAt(i);
        ViewGroup cardViewGroup = (ViewGroup) cardRoot.getChildAt(0);
        ViewGroup cardLayout = (ViewGroup) cardViewGroup.getChildAt(0);
        
        View activeElement = cardLayout.getChildAt(1);

        if (topicDisplay.equals(topic)){

            if (activeElement instanceof SwitchMaterial){

                SwitchMaterial switchView = cardList.getChildAt(i).findViewById(R.id.switch_data);

                if (decodeMQTT(message).equals("true")){
                    switchView.setChecked(true);
                }

                else if (decodeMQTT(message).equals("false")){
                    switchView.setChecked(false);
                }
            }

            else if (activeElement instanceof MaterialCheckBox){
                //todo: do something useful with a checkbox?
            }

            else if(activeElement instanceof TextView){
                TextView dataDisplay = cardList.getChildAt(i).findViewById(R.id.text_dataDisplay);
                dataDisplay.setText(decodeMQTT(message));
            }
        }
    }

    public class snackBarReconnectListener implements View.OnClickListener {

        @Override
        public void onClick(View v) {
            MqttAndroidClient client = ((MainActivity)getActivity()).getClient();
            if (!client.isConnected()) {
               ((MainActivity)getActivity()).connectMQTT(getView());
                snackBarMaker(getView(), "mqtt connection lost");
            }

        }
    }

    private String decodeMQTT(MqttMessage msg) throws UnsupportedEncodingException {
        return new String(msg.getPayload(), StandardCharsets.UTF_8);
    }

    private void snackBarMaker(View view, String message){
        Snackbar snackbar = Snackbar
                .make(view.findViewById(R.id.snackRoot), message, Snackbar.LENGTH_SHORT);
        snackbar.show();
    }

    @Override
    public void onResume() {
        super.onResume();
        LinearLayout cardList = getView().findViewById(R.id.cardList);

        if (cardList.getChildCount() == 0) {
            ArrayList<String> allTopics = (ArrayList<String>) ((MainActivity) getActivity()).getAllTopics();

            for (int i = 0; i < allTopics.size(); i++) {
                subscribeMQTT(((MainActivity) getActivity()).getClient(), allTopics.get(i));
            }
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }



}