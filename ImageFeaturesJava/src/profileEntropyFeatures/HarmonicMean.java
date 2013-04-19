/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package profileEntropyFeatures;

/**
 *
 * @author Mihai Pîțu
 */
public class HarmonicMean implements Mean {

    @Override
    public float compute(float[] array) {
        float s = 0;
        for (int i = 0; i < array.length; i++) {
            s += 1 / array[i];
        }
        
        return array.length / s;
    }

    @Override
    public double compute(double[] array) {
        double s = 0;
        for (int i = 0; i < array.length; i++) {
            s += 1 / array[i];
        }
        
        return array.length / s;
    }
    
}