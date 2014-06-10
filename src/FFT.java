//A Fast Fourier Transform algorithm used
//with verified/downsampled audio files.

public class FFT {

    /**
     * FFT Algorithm (frequency peak oriented)
     * @param data: array of samples
     * @param nSamples: number of samples
     * @param sampleRate: sample rate of the audio data (must be 2^N!)
     * @param dir: FFT or ReverseFFT (1, -1)
     * @return absolute frequency peak
     */
    public static int calculateFFT(float data[], long nSamples, int sampleRate,
            int dir) {
        int m, n, j, k, max, ffreq;
        double wtemp, wr, wpr, wpi, wi, theta, tempr, tempi;

        // We need a complex array with real + complex part
        // create a complex array of twice the original size
        float complex[] = new float[2 * sampleRate];

        // put the real data array in the new complex array
        // complex part is filled with 0
        // padding with 0 is added at the remaining part
        for (int i = 0; i < sampleRate; i++) {
            complex[2 * i + 1] = 0;
            if (i < nSamples)
                complex[2 * i] = data[i];
            else
                complex[2 * i] = 0;
        }

        // First Part: Bit reversal (binary inversion)
        // the original data array is transformed into a bit-reverse order array
        // in order to improve the cost of the calculations in the second part
        // real part: even indexes
        // complex part odd indexes
        n = sampleRate << 1;
        j = 0;
        for (int i = 0; i < n; i += 2) {
            if (j > i) {
                complex = swapVectorPos(complex, j, i);
                complex = swapVectorPos(complex, j+1, i+1);
            }
            if ((j / 2) < (n / 4)) {
                complex = swapVectorPos(complex, n - (i + 2), n - (j + 2));
                complex = swapVectorPos(complex, n - (i + 2), n - (j + 2));
            }
            m = n/2;
            while (m >= 2 && j >= m) {
                j -= m;
                m = m/2;
            }
            j += m;
        }

        // Second part: Danielson-Lanczos routine
        max = 2;
        while (n > max) {
            k = max << 1;
            theta = dir * (2 * Math.PI / max);
            wtemp = Math.sin((int) 0.5 * theta);
            wpr = -2.0 * Math.pow(wtemp, 2);
            wpi = Math.sin(theta);
            wr = 1.0;
            wi = 0.0;
            for (m = 1; m < max; m += 2) {
                for (int i = m; i <= n; i += k) {
                    j = i + max;
                    tempr = wr * complex[j - 1] - wi * complex[j];
                    tempi = wr * complex[j] + wi * complex[j - 1];
                    complex[j - 1] = complex[i - 1] - (float) tempr;
                    complex[j] = complex[i] - (float) tempi;
                    complex[i - 1] += tempr;
                    complex[i] += tempi;
                }
                wtemp = wr;
                wr = wr * wpr - wi * wpi + wr;
                wi = wi * wpr + wtemp * wpi + wi;
            }
            max = k;
        }

        // the fundamental freq will be the maximum (abs) value in the complex
        //array
        ffreq = 0;
        for (int i = 2; i < sampleRate; i += 2)

            if (Math.abs(complex[i]) + Math.abs(complex[i+1]) >
                (Math.abs(complex[ffreq]) + Math.abs(complex[ffreq])))
                ffreq = i;

        return Math.min(ffreq/2, Math.abs(sampleRate/2 - ffreq/2));

    }

    /*
     * Auxiliary method, swaps two values in a given array
     * (improves algorithm performance)
     */
    private static float[] swapVectorPos(float[] vect, long pos1, long pos2) {
        float temp;
        int x = (int) pos1;
        int y = (int) pos2;

        temp = vect[x];
        vect[x] = vect[y];
        vect[y] = temp;

        return vect;
    }

    //For testing/debugging
    private static void printComplexVect(float[] cv){
        for (int i = 0; i < cv.length; i+=2)
            System.out.println(cv[i] + "\t" + cv[i+1] + "j");
    }

}
