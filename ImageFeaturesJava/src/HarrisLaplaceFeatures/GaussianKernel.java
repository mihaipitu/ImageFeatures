/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package HarrisLaplaceFeatures;

public class GaussianKernel {
    // the kernel values (integer)

    public final float[] data;
    // the normalizer (sum of elements)
    public final float normalizer;
    // the radius of the kernel
    public final int radius;
    // the sigma2 value
    public final double sigma2;

    // public constructor
    public GaussianKernel(double sigma2) {
        this.sigma2 = sigma2;
        // radius = 1*sigma, 2*sigma and 3*sigma 
        // represent respectivly 68%, 95% and 99% of the distribution 
        radius = (int) Math.round(2 * Math.sqrt(sigma2));
        // compute gaussian values
        data = new float[1 + 2 * radius];
        for (int r = -radius; r <= radius; r++) {
            data[r + radius] = (float) Math.exp(-(r * r) / (2.0 * sigma2));
        }
        // compute the normalizer
        float sum = 0;
        for (int i = 0; i < data.length; i++) {
            sum += data[i];
        }
        normalizer = sum;
    }
}