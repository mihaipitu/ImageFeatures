/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package supportVectorMachines.Kernels;

import imagefeatures.ImageFeatures;
import speededUpRobustFeatures.TopSurfDescriptor;
import speededUpRobustFeatures.TopSurfImageFeatures;

/**
 *
 * @author Mihai Pîțu
 */
public class TopSurfCosineKernel extends Kernel {

    @Override
    protected double compute(ImageFeatures if1, ImageFeatures if2) {
        double x = TopSurfDescriptor.compareDescriptorsCosine(((TopSurfImageFeatures) if1).getDescriptor(), ((TopSurfImageFeatures) if2).getDescriptor());
//        if (x != 0)
//            System.out.println(x + " -> " + (-x * x + 2 * x));
        return x;//(-x * x + 2 * x);
    }
}
