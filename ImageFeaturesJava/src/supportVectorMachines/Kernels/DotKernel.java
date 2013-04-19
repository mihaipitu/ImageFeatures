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
public class DotKernel extends Kernel {

    @Override
    protected double compute(ImageFeatures if1, ImageFeatures if2) {
        svm_node[] x = if1.getFeatures();
        svm_node[] y = if2.getFeatures();
        double sum = 0;
        int xlen = x.length;
        int ylen = y.length;
        int i = 0;
        int j = 0;
        while (i < xlen && j < ylen) {
            if (x[i].index == y[j].index) {
                sum += x[i++].value * y[j++].value;
            } else {
                if (x[i].index > y[j].index) {
                    ++j;
                } else {
                    ++i;
                }
            }
        }
        return sum;
    }
}
