package com.reactlibrary;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;

import javax.annotation.Nullable;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.modules.core.DeviceEventManagerModule;
import com.reactlibrary.fequency_tools.fft_utils.FFTCooleyTukey;
import com.reactlibrary.fequency_tools.FrequencyDetector;
import com.reactlibrary.fequency_tools.windows.HammingWindow;

import java.util.HashMap;
import java.util.Map;

public class RNAudioProcessingModule extends ReactContextBaseJavaModule {
    private static final int SAMPLE_RATE = 22050;
    private static final int DEFAULT_BUFF_SIZE = 16384;
    private static final int MIN_FREQUENCY = 40;
    private static final int MAX_FREQUENCY = 1300;
    private static final String FREQUENCY_DETECTED_EVENT_NAME = "FrequencyDetected";

    private final ReactApplicationContext reactContext;
		
    private AudioRecord audioRecord;
    private int buffSize = 0;
    private boolean stopFlag = false;

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

    @ReactMethod
    public void init() {
            int minBufSize = AudioRecord.getMinBufferSize( RNAudioProcessingModule.SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_FLOAT);

            this.buffSize = Math.max( RNAudioProcessingModule.DEFAULT_BUFF_SIZE, minBufSize * 4);

            if (minBufSize != AudioRecord.ERROR_BAD_VALUE && minBufSize != AudioRecord.ERROR) {
                audioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC,
                                            RNAudioProcessingModule.SAMPLE_RATE,
                                            AudioFormat.CHANNEL_IN_MONO,
                                            AudioFormat.ENCODING_PCM_FLOAT,
                                            this.buffSize);
            }
    }

    @ReactMethod
    public void stop() {
        stopFlag = true;
        audioRecord.stop();
        audioRecord.release();
    }

    @ReactMethod
    public void run() {
        audioRecord.startRecording();
        final int sampleRate = audioRecord.getSampleRate();
        float[] readBuffer = new float[this.buffSize];
        FrequencyDetector detector = new FrequencyDetector();

        do {
            final int read = audioRecord.read(readBuffer, 0, this.buffSize, AudioRecord.READ_NON_BLOCKING);
            if (read > 0) {
                float frequency = detector.findFrequency(readBuffer,
                                                        sampleRate,
                                                        RNAudioProcessingModule.MIN_FREQUENCY,
                                                        RNAudioProcessingModule.MAX_FREQUENCY,
                                                        new FFTCooleyTukey(),
                                                        new HammingWindow());

                WritableMap params = Arguments.createMap();
                params.putDouble("frequency", frequency);

                this.sendEvent(this.reactContext, RNAudioProcessingModule.FREQUENCY_DETECTED_EVENT_NAME, params);
            }
        } while (!stopFlag);
    }

    private void sendEvent(ReactContext reactContext,
                           String eventName,
                           @Nullable WritableMap params) {
        reactContext
                .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class)
                .emit(eventName, params);
    }
}
