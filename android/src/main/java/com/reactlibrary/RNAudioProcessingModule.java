package com.reactlibrary;

import javax.annotation.Nullable;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.bridge.ReactMethod;

import com.facebook.react.modules.core.DeviceEventManagerModule;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class RNAudioProcessingModule extends ReactContextBaseJavaModule {
    private static final String FREQUENCY_DETECTED_EVENT_NAME = "FrequencyDetected";
    private static final String TAG = "RNAudioProcessingModule";

    private final ReactApplicationContext reactContext;

    private AudioProcessor audioProcessor;
    private boolean isProcessing = false;
    private ExecutorService executor = Executors.newSingleThreadExecutor();

    public RNAudioProcessingModule(ReactApplicationContext reactContext) {
        super(reactContext);
        this.reactContext = reactContext;
    }

    @Override
    public Map<String, Object> getConstants() {
        final Map<String, Object> constants = new HashMap<>();
        constants.put("FREQUENCY_DETECTED_EVENT_NAME", RNAudioProcessingModule.FREQUENCY_DETECTED_EVENT_NAME);
        return constants;
    }

    @Override
    public String getName() {
        return "RNAudioProcessingModule";
    }


    private static void sendEvent(ReactContext reactContext,
                           String eventName,
                           @Nullable WritableMap params) {
        reactContext
                .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class)
                .emit(eventName, params);
    }

    @ReactMethod
    private void start() {
        if (isProcessing)
            return;

        audioProcessor = new AudioProcessor();
        audioProcessor.init();
        audioProcessor.setFrequencyDetectionListener(new AudioProcessor.FrequencyDetectionListener() {
            @Override
            public void onFrequencyDetected(final double frequency) {
                WritableMap params = Arguments.createMap();
                params.putDouble("frequency", frequency);

                sendEvent(reactContext, RNAudioProcessingModule.FREQUENCY_DETECTED_EVENT_NAME, params);
            }
        });

        isProcessing = true;
        executor.execute(audioProcessor);
    }

    @ReactMethod
    private void stop() {
        if (isProcessing) {
            audioProcessor.stop();
            isProcessing = false;
        }
    }
}