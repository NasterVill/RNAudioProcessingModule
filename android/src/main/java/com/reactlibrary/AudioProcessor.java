package com.reactlibrary;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;

import com.reactlibrary.fequency_tools.fft_utils.FFTCooleyTukey;
import com.reactlibrary.fequency_tools.FrequencyDetector;
import com.reactlibrary.fequency_tools.windows.HammingWindow;

public class AudioProcessor implements Runnable {
    private static final int SAMPLE_RATE = 22050;
    private static final int DEFAULT_BUFF_SIZE = 16384;
    private static final int MIN_FREQUENCY = 50;
    private static final int MAX_FREQUENCY = 1300;

    public interface FrequencyDetectionListener {
        void onFrequencyDetected(float freq);
    }

    private AudioRecord audioRecord;
    private FrequencyDetectionListener frequencyDetectionListener = null;
    private int buffSize = 0;
    private boolean stopFlag = false;


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
                frequencyDetectionListener.onFrequencyDetected(frequency);
            }
        } while (!stopFlag);
    }
}