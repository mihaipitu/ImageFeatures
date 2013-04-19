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
public class RbfKernel extends Kernel {
    
    //small gamma means lower bias, higher variance
    private double gamma;
    
    public RbfKernel(double gamma) {
        this.gamma = gamma;
    }
    
    public double getGamma() {
        return this.gamma;
    }

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
                double d = x[i++].value - y[j++].value;
                sum += d * d;
            } else if (x[i].index > y[j].index) {
                sum += y[j].value * y[j].value;
                ++j;
            } else {
                sum += x[i].value * x[i].value;
                ++i;
            }
        }

        while (i < xlen) {
            sum += x[i].value * x[i].value;
            ++i;
        }

        while (j < ylen) {
            sum += y[j].value * y[j].value;
            ++j;
        }

        return Math.exp(-this.gamma * sum);
    }
}
