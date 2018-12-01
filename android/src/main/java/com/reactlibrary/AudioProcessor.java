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
        void onFrequencyDetected(double freq);
    }

    private AudioRecord audioRecord;
    private FrequencyDetectionListener frequencyDetectionListener = null;
    private double lastComputedFrequency = 1;
    private int buffSize = 0;
    private boolean stopFlag = false;


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
        short[] buffer = new short[this.buffSize];
        FFTFrequencyDetector detector = new FFTFrequencyDetector();

        do {
            final int read = audioRecord.read(buffer, 0, this.buffSize);
            if (read > 0) {
                double[] doubleBuff = shortToDouble(buffer, read);
                double frequency = detector.findFrequency(
                        doubleBuff,
                        sampleRate,
                        FFTFrequencyDetector.MIN_FREQUENCY,
                        FFTFrequencyDetector.MAX_FREQUENCY,
                        new FFTCooleyTukey(),
                        new HammingWindow()
                );
                if ((Math.abs(frequency - lastComputedFrequency) <= ALLOWED_FREQUENCY_DIFFERENCE) && frequency != 0) {
                    frequencyDetectionListener.onFrequencyDetected(frequency);
                }
                if(frequency != 0) {
                    lastComputedFrequency = frequency;
                }
            }
        } while (!stopFlag);
    }

    private double[] shortToDouble(short[] source, int length) {
        length = (length > source.length) ? source.length : length;

        double[] resultArray = new double[length];

        for(int i = 0; i < length; i++) {
            // The nominal range of ENCODING_PCM_FLOAT audio data is [-1.0, 1.0], but here we use
            // ENCODING_PCM_16BIT, because ENCODING_PCM_FLOAT is supported only in API LOLLIPOP+ and higher
            // so to work with float values, extended to double, we need to divide it
            // by max 16-bit integer value
            resultArray[i] = source[i] / 32768.0F;
        }
        return resultArray;
    }
}
