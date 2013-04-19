/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package imagefeatures;

import colorMoments.CMImageFeatures;
import java.security.InvalidParameterException;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import libsvm.svm_node;
import profileEntropyFeatures.PEFImageFeatures;
import speededUpRobustFeatures.TopSurfImageFeatures;

/**
 *
 * @author Mihai Pîțu
 */
public class CombinedImageFeatures extends ImageFeatures {

    private TopSurfImageFeatures tsImageFeatures;
    private PEFImageFeatures pefImageFeatures;
    private CMImageFeatures colormImageFeatures;
    private svm_node[] result;
    

    public CombinedImageFeatures(TopSurfImageFeatures tsImageFeatures, PEFImageFeatures pefImageFeatures, CMImageFeatures colormImageFeatures) {
        super(tsImageFeatures.getAnnotations(), tsImageFeatures.getUserTags());
        int[] tsAnn = tsImageFeatures.getAnnotations();
        int[] pefAnn = pefImageFeatures.getAnnotations();
        if (tsAnn != null && pefAnn != null) {
            for (int label : tsAnn) {
                if (!pefImageFeatures.containsAnnotation(label) || ! colormImageFeatures.containsAnnotation(label)) {
                    throw new InvalidParameterException("The images contain diffrent annotations.");
                }
            }
        }
        this.tsImageFeatures = tsImageFeatures;
        this.pefImageFeatures = pefImageFeatures;
        this.colormImageFeatures = colormImageFeatures;
    }

    public CombinedImageFeatures(TopSurfImageFeatures tsImageFeatures, PEFImageFeatures pefImageFeatures, CMImageFeatures colormImageFeatures, int[] annotations) {
        super(annotations);
        this.tsImageFeatures = tsImageFeatures;
        this.pefImageFeatures = pefImageFeatures;
        this.colormImageFeatures = colormImageFeatures;
    }

    /**
     * @return the tsImageFeatures
     */
    public TopSurfImageFeatures getTsImageFeatures() {
        return tsImageFeatures;
    }

    /**
     * @return the pefImageFeatures
     */
    public PEFImageFeatures getPefImageFeatures() {
        return pefImageFeatures;
    }

    @Override
    public svm_node[] getFeatures() {

        if (result == null) {
            svm_node[] pefNodes = this.pefImageFeatures.getFeatures();
            //svm_node[] tsNodes = this.tsImageFeatures.getFeatures();
            result = new svm_node[pefNodes.length];

            System.arraycopy(pefNodes, 0, result, 0, pefNodes.length);
            //System.arraycopy(tsNodes, 0, result, pefNodes.length, tsNodes.length);
        }
        return result;
    }

    @Override
    public ImageFeatures partialClone(int[] annotations) {
        tsImageFeatures.setAnnotations(annotations);
        pefImageFeatures.setAnnotations(annotations);
        CombinedImageFeatures comb = new CombinedImageFeatures(tsImageFeatures, pefImageFeatures, getColormImageFeatures(), annotations);
        comb.setUserTags(this.getUserTags());
        return comb;
    }

    @Override
    public ImageFeatures partialClone(svm_node[] features, int[] annotations) {
        tsImageFeatures.setAnnotations(annotations);
        pefImageFeatures.setAnnotations(annotations);
        CombinedImageFeatures comb = new CombinedImageFeatures(tsImageFeatures, new PEFImageFeatures(features, annotations), getColormImageFeatures(), annotations);
        comb.setUserTags(this.getUserTags());
        return comb;
    }

    @Override
    public ImageFeaturesAlgorithm getProcessingAlgorithm() {
        return new CombinedImageFeaturesAlgorithm();
    }

    /**
     * @param tsImageFeatures the tsImageFeatures to set
     */
    public void setTsImageFeatures(TopSurfImageFeatures tsImageFeatures) {
        this.tsImageFeatures = tsImageFeatures;
    }

    /**
     * @param pefImageFeatures the pefImageFeatures to set
     */
    public void setPefImageFeatures(PEFImageFeatures pefImageFeatures) {
        this.pefImageFeatures = pefImageFeatures;
    }

    /**
     * @return the colormImageFeatures
     */
    public CMImageFeatures getColormImageFeatures() {
        return colormImageFeatures;
    }

    /**
     * @param colormImageFeatures the colormImageFeatures to set
     */
    public void setColormImageFeatures(CMImageFeatures colormImageFeatures) {
        this.colormImageFeatures = colormImageFeatures;
    }
}
