/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package supportVectorMachines.Kernels;

import colorMoments.CMImageFeatures;
import imagefeatures.ImageFeatures;

/**
 *
 * @author Mihai Pîțu
 */
public class ColorMomentsKernel extends Kernel {
    private float weightHue = 1.5f;
    private float weightSaturation = 1.0f;
    private float weightValue = 1.0f;
    private float gamma = 0.30f;
    
    public ColorMomentsKernel() {
    }
    
    public ColorMomentsKernel(float weightHue, float weightSaturation, float weightValue) {
        this.weightHue = weightHue;
        this.weightSaturation = weightSaturation;
        this.weightValue = weightValue;
    }
    
    @Override
    protected double compute(ImageFeatures if1, ImageFeatures if2) {
       // return 1.0d - CMImageFeatures.colorMomentsSimilarity((CMImageFeatures) if1, (CMImageFeatures) if2, weightMean, weightDeviation, weightSkewness);
        return (float) Math.exp(-this.gamma * CMImageFeatures.colorMomentsSimilarity((CMImageFeatures) if1, (CMImageFeatures) if2, weightHue, weightSaturation, weightValue));
    }

}
