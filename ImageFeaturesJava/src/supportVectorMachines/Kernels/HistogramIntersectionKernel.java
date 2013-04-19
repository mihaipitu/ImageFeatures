/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package supportVectorMachines.Kernels;

import imagefeatures.ImageFeatures;
import libsvm.svm_node;

/**
 *
 * @author Mihai Pîțu
 */
public class HistogramIntersectionKernel extends Kernel {

    @Override
    protected double compute(ImageFeatures if1, ImageFeatures if2) {
        svm_node[] x = if1.getFeatures();
        svm_node[] l = if2.getFeatures();
        
        double sum = 0;
        int xlen = x.length;
        int ylen = l.length;
        int i = 0;
        int j = 0;
        while (i < xlen && j < ylen) {
            if (x[i].index == l[j].index) {
                sum += Math.min(x[i].value, l[j].value);
                i++;
                j++;
            } else {
                if (x[i].index > l[j].index) {
                    ++j;
                } else {
                    ++i;
                }
            }
        }

        return 1.0 - sum;
    }

}
