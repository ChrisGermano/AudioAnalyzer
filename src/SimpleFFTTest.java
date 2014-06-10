/*
 * Generate a simple sinusoidal wave signal from a given FREQ peak
 * Apply FFT and get the frequency peak of the signal
 */
public class SimpleFFTTest {
    private static final int SAMPLE_RATE = 4096*8;     // Note: use a power of two as sample rate
    private static final int NSAMPLES = 4096;
    private static final int FREQ = 8192;             // Note: use a frequency smaller than SAMPLE_RATE / 2

    public static void main(String[] args) {
        float[] data = new float[NSAMPLES];

        //Generate a simple sin signal
        for(int i = 0; i<NSAMPLES; i++)
            data[i] = (float) (30 * Math.sin(2 * Math.PI * FREQ    * ((float) i / SAMPLE_RATE)));

        System.out.println("*\n*\nFrequency peak: " + FFT.calculateFFT(data, NSAMPLES, SAMPLE_RATE, 1));
    }
}
