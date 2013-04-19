/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package speededUpRobustFeatures;

import imagefeatures.ImageFeatures;
import imagefeatures.ImageFeaturesAlgorithm;
import java.io.Serializable;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import libsvm.svm_node;
import profileEntropyFeatures.PEFAlgorithm;
import sun.misc.Compare;
import sun.misc.Sort;

/**
 *
 * @author Mihai Pîțu
 */
public class TopSurfImageFeatures extends ImageFeatures implements Serializable {

    private TopSurfDescriptor descriptor;
    private svm_node[] result;
    
    public TopSurfImageFeatures() {
    }
    
    public TopSurfImageFeatures(int[] annotations) {
        super(annotations);
    }
    
    public TopSurfImageFeatures(svm_node[] features, int[] annotations) {
        super(annotations);
        this.result = features;
    }
    
    @Override
    public svm_node[] getFeatures() {
    
        if (this.result == null) {

            TopSurfVisualword[] visualwords = this.descriptor.getVisualwords();
            this.result = new svm_node[visualwords.length];
            
            for (int i = 0; i < result.length; i++) {
              result[i] = new svm_node();
              result[i].index = (short) i;
              result[i].value = visualwords[i].getIdentifier();
            }
        }
        
        return result;
//        Compare compare = new Compare() {
//
//            @Override
//            public int doCompare(Object o, Object o1) {
//                TopSurfVisualword t1 = (TopSurfVisualword) o;
//                TopSurfVisualword t2 = (TopSurfVisualword) o1;
//                //on the same line
//                if (t1.getY() == t2.getY()) {
//                    if (t1.getX() > t2.getX()) {
//                        return 1;
//                    }
//                    if (t1.getX() < t2.getX()) {
//                        return -1;
//                    }
//                    return 0;
//                }
//
//                if (t1.getY() > t2.getY()) {
//                    return 1;
//                }
//
//                return -1;
//            }
//        };
//        TopSurfVisualword[] visualwords = this.descriptor.getVisualwords();
//        Sort.quicksort(visualwords, compare);
//
//        float[] dResult = new float[visualwords.length];
//        for (int i = 0; i < dResult.length; i++) {
//            dResult[i] = visualwords[i].getIdentifier();
//        }
//
//        return dResult;
    }
    

    public void addFeatures(TopSurfDescriptor descriptor) {
        this.descriptor = descriptor;
    }
    
    public TopSurfDescriptor getDescriptor() {
        return descriptor;
    }

    @Override
    public TopSurfImageFeatures partialClone(int[] annotations) {
        TopSurfImageFeatures ts = new TopSurfImageFeatures(annotations);
        ts.descriptor = this.descriptor;
        ts.setUserTags(this.getUserTags());
        return ts;
    }

    @Override
    public ImageFeatures partialClone(svm_node[] features, int[] annotations) {
        TopSurfImageFeatures ts = new TopSurfImageFeatures(features, annotations);
        ts.descriptor = this.descriptor;
        ts.setUserTags(this.getUserTags());
        return ts;
    }
    
    
    @Override
    public ImageFeaturesAlgorithm getProcessingAlgorithm() {
        return new TopSurfAlgorithm();
    }
}
