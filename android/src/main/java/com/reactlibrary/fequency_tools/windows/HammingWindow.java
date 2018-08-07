package com.reactlibrary.fequency_tools.windows;

public class HammingWindow implements Window {
    @Override
    public float[] applyWindow(float[] data) {
        float[] newData = new float[data.length];
        for(int i = 0; i < data.length; i++) {
            newData[i] = data[i] * iterationHamming(i, data.length);
        }
        return newData;
    }

    private float iterationHamming(float n, int size) {
        return (float)(0.54 - 0.46 * Math.cos(2 * Math.PI * n / size));
    }
}
