package com.reactlibrary.fequency_tools;

public class YINFrequencyDetector {
    public static final float DEFAULT_THRESHOLD = 0.15F;

    private float[] buffer;		        /** Buffer that stores the results of the intermediate processing steps of the algorithm */
    private float probability = -1;		/** Probability that the pitch found is correct as a decimal (i.e 0.85 is 85%) */
    private float threshold = -1;		/** Allowed uncertainty in the result as a decimal (i.e 0.15 is 15%) */
    private int sampleRate = 0;

    public float getProbability() {
        return this.probability;
    }

    public float getThreshold() {
        return this.threshold;
    }

    public int getSampleRate() {
        return this.sampleRate;
    }

    public void setSampleRate(int sampleRate) {
        this.sampleRate = sampleRate;
    }

    public void setThreshold(float threshold) {
        this.threshold = threshold;
    }

    public float findFrequency(float[] soundData)
            throws IllegalArgumentException {
        if (threshold <= 0 || threshold >= 1) {
            throw new IllegalArgumentException("The value of threshold is invalid!");
        }

        if (probability <= 0 || probability >= 1) {
            throw new IllegalArgumentException("The value of probability is invalid!");
        }

        this.buffer = new float[soundData.length / 2];

        int tauEstimate = -1;
        float pitchInHertz = -1;

        // Step 1: Calculates the squared difference of the signal with a shifted version of itself.
        this.computeDifference(soundData);

        // Step 2: Calculate the cumulative mean on the normalised difference calculated in step 1
        this.cumulativeMeanNormalizedDifference();

        // Step 3: Search through the normalised cumulative mean array and find values that are over the threshold
        tauEstimate = this.absoluteThreshold();

        // Step 4: Interpolate the shift value (tau) to improve the pitch estimate.
        if(tauEstimate != -1){
            pitchInHertz = this.sampleRate / this.parabolicInterpolation(tauEstimate);
        }

        return pitchInHertz;
    }

    private void computeDifference(float[] soundData){
        int i;
        int tau;
        float delta;

        // Calculate the difference for difference shift values (tau) for the half of the samples
        for(tau = 0 ; tau < this.buffer.length; tau++){

            // Take the difference of the signal with a shifted version of itself, then square it.
            // (This is the Yin algorithm's tweak on autocorellation)
            for(i = 0; i < this.buffer.length; i++){
                delta = soundData[i] - soundData[i + tau];
                this.buffer[tau] += delta * delta;
            }
        }
    }

    private void cumulativeMeanNormalizedDifference(){
        int tau;
        float runningSum = 0;
        buffer[0] = 1;

        // Sum all the values in the autocorellation buffer and nomalise the result, replacing
        // the value in the autocorellation buffer with a cumulative mean of the normalised difference
        for (tau = 1; tau < buffer.length; tau++) {
            runningSum += buffer[tau];
            buffer[tau] *= tau / runningSum;
        }
    }

    private int absoluteThreshold(){
        int tau;

        // Search through the array of cumulative mean values, and look for ones that are over the threshold
        // The first two positions in yinBuffer are always so start at the third (index 2)
        for (tau = 2; tau < buffer.length ; tau++) {
            if (buffer[tau] < threshold) {
                while (tau + 1 < buffer.length && buffer[tau + 1] < buffer[tau]) {
                    tau++;
                }
                // found tau, exit loop and return
                // store the probability
                // From the YIN paper: The yin->threshold determines the list of
                // candidates admitted to the set, and can be interpreted as the
                // proportion of aperiodic power tolerated
                // within a periodic signal.
                //
                // Since we want the periodicity and and not aperiodicity:
                // periodicity = 1 - aperiodicity
                probability = 1 - buffer[tau];
                break;
            }
        }

        // if no pitch found, tau => -1
        if (tau == buffer.length|| buffer[tau] >= threshold) {
            tau = -1;
            probability = 0;
        }

        return tau;
    }

    private float parabolicInterpolation(int tauEstimate) {
        float betterTau;
        int x0;
        int x2;

        // Calculate the first polynomial coeffcient based on the current estimate of tau
        if (tauEstimate < 1) {
            x0 = tauEstimate;
        }
        else {
            x0 = tauEstimate - 1;
        }

        // Calculate the second polynomial coeffcient based on the current estimate of tau
        if (tauEstimate + 1 < buffer.length) {
            x2 = tauEstimate + 1;
        }
        else {
            x2 = tauEstimate;
        }

        // Algorithm to parabolically interpolate the shift value tau to find a better estimate
        if (x0 == tauEstimate) {
            if (buffer[tauEstimate] <= buffer[x2]) {
                betterTau = tauEstimate;
            }
            else {
                betterTau = x2;
            }
        }
        else if (x2 == tauEstimate) {
            if (buffer[tauEstimate] <= buffer[x0]) {
                betterTau = tauEstimate;
            }
            else {
                betterTau = x0;
            }
        }
        else {
            float s0, s1, s2;
            s0 = buffer[x0];
            s1 = buffer[tauEstimate];
            s2 = buffer[x2];

            betterTau = tauEstimate + (s2 - s0) / (2 * (2 * s1 - s2 - s0));
        }

        return betterTau;
    }
}
