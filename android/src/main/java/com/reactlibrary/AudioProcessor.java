package com.reactlibrary;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;

import com.reactlibrary.fequency_tools.fft_utils.FFTCooleyTukey;
import com.reactlibrary.fequency_tools.FFTFrequencyDetector;
import com.reactlibrary.fequency_tools.windows.HammingWindow;

public class AudioProcessor implements Runnable {
    private static final int SAMPLE_RATE = 44100;
    private static final int DEFAULT_BUFF_SIZE = 65536;
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
        int minBufSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);
        if (minBufSize != AudioRecord.ERROR_BAD_VALUE && minBufSize != AudioRecord.ERROR) {
            audioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC, SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, Math.max(DEFAULT_BUFF_SIZE, minBufSize * 4));
            this.buffSize = Math.max(DEFAULT_BUFF_SIZE, minBufSize * 4);
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
        short[] mainBuffer = new short[this.buffSize];
        FFTFrequencyDetector detector = new FFTFrequencyDetector();

        do {
            final int read = audioRecord.read(mainBuffer, 0, this.buffSize);
            if (read > 0) {
                float[] floatBuff = shortToFloat(mainBuffer, read);
                float frequency = detector.findFrequency(
                        floatBuff,
                        sampleRate,
                        FFTFrequencyDetector.MIN_FREQUENCY,
                        FFTFrequencyDetector.MAX_FREQUENCY,
                        new FFTCooleyTukey(),
                        new HammingWindow()
                );
                if(Math.abs(frequency - previousFrequency) < ALLOWED_FREQUENCY_DIFFERENCE) {
                    frequencyDetectionListener.onFrequencyDetected(frequency);
                }
                previousFrequency = frequency;
            }
        } while (!stopFlag);
    }

    private float[] shortToFloat(short[] source, int length) {
        length = (length > source.length) ? source.length : length;

        float[] resultArray = new float[length];

        for(int i = 0; i < length; i++) {
            resultArray[i] = source[i] / 32768.0F;
        }
        return resultArray;
    }
}
