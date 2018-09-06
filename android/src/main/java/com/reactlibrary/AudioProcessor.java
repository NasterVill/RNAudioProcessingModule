package com.reactlibrary;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;

import com.reactlibrary.fequency_tools.fft_utils.FFTCooleyTukey;
import com.reactlibrary.fequency_tools.FrequencyDetector;
import com.reactlibrary.fequency_tools.windows.HammingWindow;

public class AudioProcessor implements Runnable {
    private static final int SAMPLE_RATE = 44100;
    private static final int DEFAULT_BUFF_SIZE = 65536;
    private static final int MIN_FREQUENCY = 50;
    private static final int MAX_FREQUENCY = 400;
    private static final float ALLOWED_FREQUENCY_DIFFERENCE = 1;

    public interface FrequencyDetectionListener {
        void onFrequencyDetected(float freq);
    }

    private AudioRecord audioRecord;
    private FrequencyDetectionListener frequencyDetectionListener = null;
    private int buffSize = 0;
    private boolean stopFlag = false;
    private float previousFrequency = 0;

    public void setFrequencyDetectionListener(FrequencyDetectionListener frequencyDetectionListener) {
        this.frequencyDetectionListener = frequencyDetectionListener;
    }

    public void init() {
        int minBufSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_FLOAT);
        this.buffSize = Math.max(DEFAULT_BUFF_SIZE, minBufSize * 4);
        if (minBufSize != AudioRecord.ERROR_BAD_VALUE && minBufSize != AudioRecord.ERROR) {
            audioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC,
                    SAMPLE_RATE,
                    AudioFormat.CHANNEL_IN_MONO,
                    AudioFormat.ENCODING_PCM_FLOAT,
                    this.buffSize);
        }
    }

    public void stop() {
        stopFlag = true;
        audioRecord.stop();
        audioRecord.release();
    }

    @Override
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
                        MIN_FREQUENCY,
                        MAX_FREQUENCY,
                        new FFTCooleyTukey(),
                        new HammingWindow());
                if(Math.abs(frequency - previousFrequency) < ALLOWED_FREQUENCY_DIFFERENCE) {
                    frequencyDetectionListener.onFrequencyDetected(frequency);
                }
                previousFrequency = frequency;
            }
        } while (!stopFlag);
    }
}