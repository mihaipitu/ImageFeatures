/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package profileEntropyFeatures;

/**
 *
 * @author Mihai Pîțu
 */
public class ArithmeticMean implements Mean {

    @Override
    public float compute(float[] array) {
        float result = 0;
        for (int i = 0; i < array.length; i++) {
            result += array[i];
        }
        return result / array.length;
    }

    @Override
    public double compute(double[] array) {
        double result = 0;
        for (int i = 0; i < array.length; i++) {
            result += array[i];
        }
        return result / array.length;
    }
}