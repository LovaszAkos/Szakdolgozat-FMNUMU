package com.fmnumu.szakdolgozat.ui.home;

import android.annotation.SuppressLint;
import android.content.DialogInterface;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
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
import com.google.android.material.slider.Slider;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.w3c.dom.Text;

import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

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
        fabSub.setOnClickListener(view -> {
            AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
            builder.setTitle("Enter topic to subscribe to");

            final EditText input = new EditText(getContext());

            input.setInputType(InputType.TYPE_CLASS_TEXT);
            input.setGravity(Gravity.CENTER);
            builder.setView(input);

            builder.setPositiveButton("Next", (dialog, which) -> {
                topic = input.getText().toString();
                if (topic.equals("")){
                    Toast toast = Toast.makeText(getContext(), "Topic can't be empty", Toast.LENGTH_SHORT);
                    toast.show();
                }
                else{

                    AlertDialog.Builder builder1 = new AlertDialog.Builder(getContext());
                    final String[] actionType = new String[1];

                    builder1.setTitle("Pick the action type");

                    final Spinner typeSpinner = new Spinner(getContext());

                    List<String> allTypes = ((MainActivity)getActivity()).getAllInteractTypes();

                    typeSpinner.setAdapter(new ArrayAdapter<>(getContext(), R.layout.subscribe_spinner, allTypes));

                    builder1.setView(typeSpinner);

                    typeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                        @Override
                        public void onItemSelected(AdapterView<?> adapterView, View view1, int i, long l) {
                            actionType[0] = typeSpinner.getSelectedItem().toString();
                        }

                        @Override
                        public void onNothingSelected(AdapterView<?> adapterView) {

                        }
                    });

                    builder1.setPositiveButton("Subscribe", (dialog1, which1) -> {

                        List<String> cardData = new ArrayList<>();
                        cardData.add(topic);
                        cardData.add(actionType[0]);
                        cardData.add("null");
                        subscribeMQTT(((MainActivity)getActivity()).getClient(), cardData);
                    });

                    builder1.show();
                }
            });

            builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());

            builder.show();
        });

        return root;
    }

    //TODO: replace R.id's with dynamic type
    @SuppressLint("NonConstantResourceId")
    private void createCard(LinearLayout layout, List<String> savedCardData, int type) {
        ViewGroup mqttCard = (ViewGroup) this.getLayoutInflater().inflate(type, null);
        TextView topicDisplay = (TextView) mqttCard.findViewById(R.id.text_topicDisplay);
        topicDisplay.setText(savedCardData.get(0));
        layout.addView(mqttCard);

        switch (type) {
            case R.layout.mqtt_card_text: //TODO: ADD WITH DATA
                //TODO: add appropriate listener
                /* EXAMPLE LISTENER, ONLY ADD TO APPROPRIATE CARD TYPES

                    TextView text_data = (TextView) mqttCard.findViewById(R.id.text_data);
                    text_data.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            publishMessage(((MainActivity)getActivity()).getClient(), topic, "message");
                        }
                    });
                */
                break;
            case R.layout.mqtt_card_button: //TODO: ADD WITH DATA
                break;
            case R.layout.mqtt_card_switch: //TODO: ADD WITH DATA
                SwitchMaterial switch_data = (SwitchMaterial) mqttCard.findViewById(R.id.switch_data);
                switch_data.setOnClickListener(view -> {
                    String message = switch_data.isChecked() ? "on" : "off";
                    publishMessage(((MainActivity)getActivity()).getClient(), savedCardData.get(0), message);
                });
                break;
            case R.layout.mqtt_card_input:  //TODO: ADD WITH DATA
                //TODO: add appropriate listener
                break;
            case R.layout.mqtt_card_checkbox:   //TODO: ADD WITH DATA
                //TODO: stuff
                break;
            case R.layout.mqtt_card_slider:
                Slider slider_data = (Slider) mqttCard.findViewById(R.id.slider_data);

                if (savedCardData.get(2).equals("null")) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
                    builder.setTitle("Enter the slider's range");
                    ViewGroup sliderRangeView = (ViewGroup) this.getLayoutInflater().inflate(R.layout.subscribe_slider, null);

                    TextInputLayout rangeMaxView = (TextInputLayout) sliderRangeView.findViewById(R.id.slider_top_limit);
                    TextInputLayout rangeMinView = (TextInputLayout) sliderRangeView.findViewById(R.id.slider_bottom_limit);

                    builder.setView(sliderRangeView);
                    builder.setPositiveButton("Set", (dialog, which) -> {
                        TextView sliderDataDisplay = mqttCard.findViewById(R.id.slider_data_display);
                        String rangeMin = rangeMinView.getEditText().getText().toString();
                        String rangeMax = rangeMaxView.getEditText().getText().toString();

                        slider_data.setValueTo(Float.parseFloat(rangeMax));
                        slider_data.setValue(Float.parseFloat(rangeMin));
                        slider_data.setValueFrom(Float.parseFloat(rangeMin));
                        sliderDataDisplay.setText(rangeMin);
                    });

                    builder.show();
                }
                else {
                    List<String> sliderSubData = Arrays.asList(savedCardData.get(2).split("\\."));
                    TextView sliderDataDisplay = mqttCard.findViewById(R.id.slider_data_display);

                    String rangeMin = sliderSubData.get(0);
                    String rangeMax = sliderSubData.get(1);
                    String currentVal = sliderSubData.get(2);

                    slider_data.setValueTo(Float.parseFloat(rangeMax));
                    slider_data.setValue(Float.parseFloat(currentVal));
                    slider_data.setValueFrom(Float.parseFloat(rangeMin));

                    sliderDataDisplay.setText(currentVal);
                }


                final Slider.OnSliderTouchListener touchListener =
                        new Slider.OnSliderTouchListener() {
                            @SuppressLint("RestrictedApi")
                            @Override
                            public void onStartTrackingTouch(@NonNull Slider slider) {

                            }

                            @SuppressLint("RestrictedApi")
                            @Override
                            public void onStopTrackingTouch(Slider slider) {
                                String value = String.valueOf(slider_data.getValue()).replace(".0", "");
                                TextView sliderDataDisplay = mqttCard.findViewById(R.id.slider_data_display);
                                sliderDataDisplay.setText(value);
                                publishMessage(((MainActivity)getActivity()).getClient(), savedCardData.get(0), value);
                            }
                        };

                slider_data.addOnSliderTouchListener(touchListener);
                break;
                //TODO: add more card types?

            default:
                Toast toast = Toast.makeText(getContext(), "Failed to create card + " + savedCardData.get(0), Toast.LENGTH_SHORT);
                toast.show();
                break;
        }
    }

    public void subscribeMQTT(MqttAndroidClient mqttAndroidClient, List<String> cardData){
        LinearLayout layout = (LinearLayout) getView().findViewById(R.id.cardList);
        try {
            if (!mqttAndroidClient.isConnected()) {
                mqttAndroidClient.connect();
            }
            mqttAndroidClient.subscribe(cardData.get(0), 0, getContext(), new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    if (!((MainActivity)getActivity()).getCardDataStoreAll().contains(cardData.get(0)+":"+cardData.get(1)+":"+cardData.get(2))){ //data[0]+":"+type
                        ((MainActivity)getActivity()).addCardData(cardData);
                    }
                    switch (cardData.get(1)) {
                        case "Text":
                            createCard(layout, cardData, R.layout.mqtt_card_text);
                            break;
                        case "Button":
                            createCard(layout, cardData, R.layout.mqtt_card_button);
                            break;
                        case "Switch":
                            createCard(layout, cardData, R.layout.mqtt_card_switch);
                            break;
                        case "Input":
                            createCard(layout, cardData, R.layout.mqtt_card_input);
                            break;
                        case "Checkbox":
                            createCard(layout, cardData, R.layout.mqtt_card_checkbox);
                            break;
                        case "Slider":
                            createCard(layout, cardData, R.layout.mqtt_card_slider);

                        //TODO: add more card types?

                        default:
                            Toast toast = Toast.makeText(getContext(), "Failed to create card", Toast.LENGTH_SHORT);
                            toast.show();
                            break;
                    }

                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                   snackBarMaker(getView(), "Failed to subscribe");
                }
            });
        } catch (Exception e) {
            Toast toast = Toast.makeText(getContext(), "MQTT is not connected!", Toast.LENGTH_SHORT);
            toast.show();
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
                            .make(root.findViewById(R.id.snackRoot), "MQTT Connection lost!", Snackbar.LENGTH_LONG);
                    snackbar.setAction("Reconnect", new snackBarReconnectListener());
                    snackbar.show();
                }

                @Override
                public void messageArrived(String topic, MqttMessage message) throws Exception {
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
            Toast toast = Toast.makeText(getContext(), "MQTT is not connected!", Toast.LENGTH_SHORT);
            toast.show();
        }

    }

    private void cardHandlerOnMessageReceived(String topic, MqttMessage message,
                                              LinearLayout cardList, int i)
            throws UnsupportedEncodingException {

        String topicDisplay = (String) ((TextView) cardList.getChildAt(i).findViewById(R.id.text_topicDisplay)).getText();
        
        ViewGroup cardRoot = (ViewGroup) cardList.getChildAt(i);
        ViewGroup cardViewGroup = (ViewGroup) cardRoot.getChildAt(0);
        ViewGroup cardLayout = (ViewGroup) cardViewGroup.getChildAt(0);
        
        View activeElement = cardLayout.getChildAt(1);

        if (topicDisplay.equals(topic)){

            if (activeElement instanceof SwitchMaterial) {

                SwitchMaterial switchView = cardList.getChildAt(i).findViewById(R.id.switch_data);

                if (decodeMQTT(message).equals("on")) {
                    switchView.setChecked(true);
                } else if (decodeMQTT(message).equals("off")) {
                    switchView.setChecked(false);
                }
            }
            else if(activeElement instanceof TextInputLayout){
                TextInputEditText textInputEditText = cardList.getChildAt(i).findViewById(R.id.input_data);
                textInputEditText.setText(decodeMQTT(message));
            }
            else if(activeElement instanceof Slider){
                Slider sliderData = cardLayout.getChildAt(i).findViewById(R.id.slider_data);
                TextView sliderDataDisplay = cardLayout.getChildAt(i).findViewById(R.id.slider_data_display);
                sliderDataDisplay.setText(decodeMQTT(message));
                sliderData.setValue(Integer.parseInt(decodeMQTT(message)));
            }
            else if (activeElement instanceof MaterialCheckBox){
                //todo: do something useful with a checkbox?
            }

            else if(activeElement instanceof TextView){
                TextView dataDisplay = cardList.getChildAt(i).findViewById(R.id.text_data);
                dataDisplay.setText(decodeMQTT(message));
            }
        }
    }

    public class snackBarReconnectListener implements View.OnClickListener {

        @Override
        public void onClick(View v) {
            MqttAndroidClient client = ((MainActivity)getActivity()).getClient();
            if (!client.isConnected()) {
               ((MainActivity)getActivity()).connectMQTT();
                snackBarMaker(getView(), "MQTT reconnected");
            }

        }
    }

    public void publishMessage(MqttAndroidClient mqttAndroidClient, String topic,  String payload) {
        try {
            if (!mqttAndroidClient.isConnected()) {
                mqttAndroidClient.connect();
            }

            MqttMessage message = new MqttMessage();
            message.setPayload(payload.getBytes());
            message.setQos(0);
            mqttAndroidClient.publish(topic, message,null, new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    Log.d("PUBLISH", "published on: " + topic + " message: " + payload);
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    Toast toast = Toast.makeText(getContext(), "Publish failed! status: MQTT disconnected", Toast.LENGTH_SHORT);
                    toast.show();
                    Log.d("PUBLISH", "onFail");
                }
            });
        } catch (MqttException e) {
            Log.e("mqttException", e.toString());

            Toast toast = Toast.makeText(getContext(), "Fatal MQTT Error", Toast.LENGTH_SHORT);
            toast.show();
        }
        catch (NullPointerException e){
            Log.e("nullPointerException", e.toString());

            Toast toast = Toast.makeText(getContext(), "Publish Failed, MQTT not connected", Toast.LENGTH_SHORT);
            toast.show();
        }
    }

    private String decodeMQTT(MqttMessage msg) {
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
            ((MainActivity)getActivity()).subscribeAllTopics();
        }
    }
    
    @Override
    public void onPause(){
        super.onPause();
        LinearLayout cardList = getView().findViewById(R.id.cardList);
        List<String> cardData = new ArrayList<>();
        for (int i = 0; i < cardList.getChildCount(); i++) {
            ViewGroup cardInstance = (ViewGroup) cardList.getChildAt(i);

            String cardInstanceTopic = "null";
            String cardInstanceType = "null";
            String cardInstanceData = "null";

            int cardType = cardInstance.getId();
            Log.d("PAUSE", "onPause: "+cardType);
            switch (cardType){
                case R.id.text_type_card:
                    cardInstanceTopic = (String) ((TextView) cardInstance.findViewById(R.id.text_topicDisplay)).getText();
                    cardInstanceType = "Text";
                    cardInstanceData = (String) ((TextView) cardInstance.findViewById(R.id.text_data)).getText();
                    break;
                case R.id.button_type_card:
                    cardInstanceTopic = (String) ((TextView) cardInstance.findViewById(R.id.text_topicDisplay)).getText();
                    cardInstanceType = "Button";
                    break;
                case R.id.switch_type_card:
                    cardInstanceTopic = (String) ((TextView) cardInstance.findViewById(R.id.text_topicDisplay)).getText();
                    cardInstanceType = "Switch";
                    cardInstanceData = ((SwitchMaterial) cardInstance.findViewById(R.id.switch_data)).isChecked() ? "on" : "off";
                    break;
                case R.id.input_type_card:
                    cardInstanceTopic = (String) ((TextView) cardInstance.findViewById(R.id.text_topicDisplay)).getText();
                    cardInstanceType = "Input";
                    break;
                case R.id.checkbox_type_card:
                    cardInstanceTopic = (String) ((TextView) cardInstance.findViewById(R.id.text_topicDisplay)).getText();
                    cardInstanceType = "Checkbox";
                    cardInstanceData = ((CheckBox) cardInstance.findViewById(R.id.checkbox_data)).isChecked() ? "on" : "off";
                    break;
                case R.id.slider_type_card:
                    String currentValue = String.valueOf(((Slider) cardInstance.findViewById(R.id.slider_data)).getValue()).replace(".0","");
                    String minValue = String.valueOf(((Slider) cardInstance.findViewById(R.id.slider_data)).getValueFrom()).replace(".0","");
                    String maxValue = String.valueOf(((Slider) cardInstance.findViewById(R.id.slider_data)).getValueTo()).replace(".0","");;

                    cardInstanceTopic = (String) ((TextView) cardInstance.findViewById(R.id.text_topicDisplay)).getText();
                    cardInstanceType = "Slider";
                    cardInstanceData = minValue+"."+maxValue+"."+currentValue;
            }

            cardData.add(cardInstanceTopic);
            cardData.add(cardInstanceType);
            cardData.add(cardInstanceData);

            ((MainActivity)getActivity()).addCardData(cardData);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }


}