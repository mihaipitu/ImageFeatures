/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package profileEntropyFeatures;

import imagefeatures.ImageFeatures;
import imagefeatures.ImageFeaturesAlgorithm;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import libsvm.svm_node;

/**
 *
 * @author Mihai Pîțu
 */
public final class PEFImageFeatures extends ImageFeatures implements Serializable {
    
    private ArrayList<PEF> pefFeatures = new ArrayList<>();
    private svm_node[] result;
    private float[] hsvHistogram;
    
    public PEFImageFeatures() {
    }
    
    public PEFImageFeatures(ArrayList<PEF> pefFeatures, float[] hsvHistogram) {
        this.pefFeatures = pefFeatures;
        this.hsvHistogram = hsvHistogram;
    }
    
    public PEFImageFeatures(int[] annotations) {
        super(annotations);
    }
    
    public PEFImageFeatures(svm_node[] features, int[] annotations) {
        super(annotations);
        this.result = features;
    }
    
    public PEFImageFeatures(svm_node[] features, int[] annotations, HashSet<String> userTags) {
        super(annotations, userTags);
        this.result = features;
    }
    
    @Override
    public svm_node[] getFeatures() {
        
        if (result == null) {
            result = new svm_node[pefFeatures.size() * 5/* + this.hsvHistogram.length*/];
            
            int i = 0;
            int j = 0;
            while (i < pefFeatures.size()) {
                result[j] = new svm_node();
                result[j].index = j;
                result[j].value = pefFeatures.get(i).getFeatureX();
                j++;
                
                result[j] = new svm_node();
                result[j].index = j;
                result[j].value = pefFeatures.get(i).getFeatureY();
                j++;
                
                result[j] = new svm_node();
                result[j].index = j;
                result[j].value = pefFeatures.get(i).getFeatureB();
                j++;
                
                result[j] = new svm_node();
                result[j].index = j;
                result[j].value = pefFeatures.get(i).getMean();
                j++;
                
                result[j] = new svm_node();
                result[j].index = j;
                result[j].value = pefFeatures.get(i).getStandardDeviation();
                j++;
                
                i++;
            }
            
            i = 0;
//            while (j < result.length) {
//                result[j] = new svm_node(j++, this.hsvHistogram[i++]);
//            }
        }
        
        return result;
    }

    public void addFeature(PEF pef) {
        pefFeatures.add(pef);
    }
    
    public void setHsvHistogram(float[] hsvHistogram) {
        this.hsvHistogram = hsvHistogram;
    }

    @Override
    public PEFImageFeatures partialClone(int[] annotations) {
        return new PEFImageFeatures(this.getFeatures(), annotations, this.getUserTags());
    }
    
    @Override
    public ImageFeatures partialClone(svm_node[] features, int[] annotations) {
        return new PEFImageFeatures(features, annotations, this.getUserTags());
    }

    @Override
    public ImageFeaturesAlgorithm getProcessingAlgorithm() {
        return new PEFAlgorithm();
    }
}
