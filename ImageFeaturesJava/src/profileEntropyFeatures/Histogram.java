/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package profileEntropyFeatures;

/**
 *
 * @author Mihai Pîțu
 */
public class Histogram {

    private float upperBound; //upper bound of the histogram
    private float lowerBound; //lower bound of the histogram
    private int ncells; //number of cells or bins of the histogram
    private float[] result; //result of the histogram

    public Histogram(float[] result, float upperBound, float lowerBound, int ncells) {
        this.upperBound = upperBound;
        this.lowerBound = lowerBound;
        this.ncells = ncells;
        this.result = result;
    }

    public static Histogram compute(float[] x, float lower, float upper, int ncell) throws InvalidPefException {
        if (ncell < 1) {
            throw new InvalidPefException("Invalid number of cells");
        }
        if (upper <= lower) {
            throw new InvalidPefException("Invalid bounds");
        }
        float[] result = new float[(int) ncell];

        //float[] y = new float[x.length];
        for (int i = 0; i < x.length; i++) {
            int y = Math.round((float) ((x[i] - lower) / (upper - lower) * ncell + 1 / 2));

            if (y >= 0 && y < ncell) {
                result[y]++;
            }
        }

        return new Histogram(result, upper, lower, ncell);
    }

    public static Histogram compute(float[] x, int ncell) throws InvalidPefException {
        float max = x[0];
        float min = x[0];

        for (int i = 0; i < x.length; i++) {
            if (x[i] > max) {
                max = x[i];
            }
            if (x[i] < min) {
                min = x[i];
            }
        }

        float delta = (max - min) / (x.length - 1);
        float lower = min - (delta / 2);
        float upper = max + (delta / 2);

        return Histogram.compute(x, lower, upper, ncell);
    }

    public static Histogram compute(float[] x) throws InvalidPefException {
        int ncell = (int) Math.ceil(Math.sqrt(x.length));

        return Histogram.compute(x, ncell);
    }

    /**
     * @return the upperBound
     */
    public float getUpperBound() {
        return upperBound;
    }

    /**
     * @return the lowerBound
     */
    public float getLowerBound() {
        return lowerBound;
    }

    /**
     * @return the ncells
     */
    public int getNcells() {
        return ncells;
    }

    /**
     * @return the result
     */
    public float[] getResult() {
        return result;
    }
}
