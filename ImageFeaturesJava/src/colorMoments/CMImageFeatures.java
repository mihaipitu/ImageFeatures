/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package colorMoments;

import imagefeatures.ImageFeatures;
import imagefeatures.ImageFeaturesAlgorithm;
import java.awt.Color;
import java.awt.image.SampleModel;
import javax.media.jai.JAI;
import javax.media.jai.PlanarImage;
import libsvm.svm_node;
import profileEntropyFeatures.PEF;

/**
 *
 * @author Mihai Pîțu
 */
public class CMImageFeatures extends imagefeatures.ImageFeatures {
//http://homepages.inf.ed.ac.uk/rbf/CVonline/LOCAL_COPIES/AV0405/KEEN/av_as2_nkeen.pdf
    
    private float HmeanMoment;
    private float SmeanMoment;
    private float VmeanMoment;
    private float HdeviationMoment;
    private float SdeviationMoment;
    private float VdeviationMoment;
    private float HskewnessMoment;
    private float SskewnessMoment;
    private float VskewnessMoment;
    private svm_node[] result;

    public CMImageFeatures() {
    }

    public CMImageFeatures(int[] annotations) {
        super(annotations);
    }

    public CMImageFeatures(svm_node[] features, int[] annotations) {
        super(annotations);
        this.result = features;
    }
    
    public static float colorMomentsSimilarity(CMImageFeatures cimf1, CMImageFeatures cimf2, float weightHue, float weightSaturation, float weightValue) {
        float sim = 0;

        sim += weightHue * Math.abs(cimf1.HmeanMoment - cimf2.HmeanMoment);
        sim += weightHue * Math.abs(cimf1.HdeviationMoment - cimf2.HdeviationMoment);
        sim += weightHue * Math.abs(cimf1.HskewnessMoment - cimf2.HskewnessMoment);

        sim += weightSaturation * Math.abs(cimf1.SmeanMoment - cimf2.SmeanMoment);
        sim += weightSaturation * Math.abs(cimf1.SdeviationMoment - cimf2.SdeviationMoment);
        sim += weightSaturation * Math.abs(cimf1.SskewnessMoment - cimf2.SskewnessMoment);

        sim += weightValue * Math.abs(cimf1.VmeanMoment - cimf2.VmeanMoment);
        sim += weightValue * Math.abs(cimf1.VdeviationMoment - cimf2.VdeviationMoment);
        sim += weightValue * Math.abs(cimf1.VskewnessMoment - cimf2.VskewnessMoment);

        return sim;
    }

    public static CMImageFeatures getCMImageFeatures(String imagePath) {
        CMImageFeatures imf = new CMImageFeatures();

        PlanarImage pi = JAI.create("fileload", imagePath);
        int[] pixels = PEF.getPixels(pi);
        SampleModel sm = pi.getSampleModel();
        int width = pi.getWidth();
        int height = pi.getHeight();
        int bands = sm.getNumBands();
        float[] resultH = new float[pixels.length / 3];
        float[] resultS = new float[pixels.length / 3];
        float[] resultV = new float[pixels.length / 3];

        imf.HmeanMoment = 0;
        imf.SmeanMoment = 0;
        imf.VmeanMoment = 0;

        imf.HdeviationMoment = 0;
        imf.SdeviationMoment = 0;
        imf.VdeviationMoment = 0;

        imf.HskewnessMoment = 0;
        imf.SskewnessMoment = 0;
        imf.VskewnessMoment = 0;

        int n = 0;
        int offset;
        for (int h = 0; h < height; h++) {
            for (int w = 0; w < width; w++) {
                offset = h * width * bands + w * bands;

                int r = pixels[offset];
                int g = pixels[offset + 1];
                int b = pixels[offset + 2];

                float[] hsv = new float[3];
                Color.RGBtoHSB(r, g, b, hsv);
                
                resultH[n] = hsv[0];
                resultS[n] = hsv[1];
                resultV[n] = hsv[2];

                imf.HmeanMoment += hsv[0];
                imf.SmeanMoment += hsv[1];
                imf.VmeanMoment += hsv[2];
                n++;
            }
        }

        imf.HmeanMoment /= n;
        imf.SmeanMoment /= n;
        imf.VmeanMoment /= n;

        for (int j = 0; j < n; j++) {
            imf.HdeviationMoment += Math.pow(resultH[j] - imf.HmeanMoment, 2);
            imf.HskewnessMoment += Math.pow(resultH[j] - imf.HmeanMoment, 3);
            imf.SdeviationMoment += Math.pow(resultS[j] - imf.SmeanMoment, 2);
            imf.SskewnessMoment += Math.pow(resultS[j] - imf.SmeanMoment, 3);
            imf.VdeviationMoment += Math.pow(resultV[j] - imf.VmeanMoment, 2);
            imf.VskewnessMoment += Math.pow(resultV[j] - imf.VmeanMoment, 3);
        }

        imf.HdeviationMoment = (float) Math.sqrt(imf.HdeviationMoment / n);
        imf.SdeviationMoment = (float) Math.sqrt(imf.SdeviationMoment / n);
        imf.VdeviationMoment = (float) Math.sqrt(imf.VdeviationMoment / n);

        imf.HskewnessMoment = (float) Math.pow(Math.abs(imf.HskewnessMoment / n), 1.0d / 3.0f);
        imf.SskewnessMoment = (float) Math.pow(Math.abs(imf.SskewnessMoment / n), 1.0d / 3.0f);
        imf.VskewnessMoment = (float) Math.pow(Math.abs(imf.VskewnessMoment / n), 1.0d / 3.0f);

        pi.dispose();
       
        return imf;
    }

    @Override
    public svm_node[] getFeatures() {
        if (this.result == null) {
            this.result = new svm_node[9];
            this.result[0] = new svm_node(0, HmeanMoment);
            this.result[1] = new svm_node(1, SmeanMoment);
            this.result[2] = new svm_node(2, VmeanMoment);
            
            this.result[3] = new svm_node(3, HskewnessMoment);
            this.result[4] = new svm_node(4, SskewnessMoment);
            this.result[5] = new svm_node(5, VskewnessMoment);
            
            this.result[6] = new svm_node(6, HdeviationMoment);
            this.result[7] = new svm_node(7, SdeviationMoment);
            this.result[8] = new svm_node(8, VdeviationMoment);
        }
        
        return result;
    }

    @Override
    public ImageFeatures partialClone(int[] annotations) {
        CMImageFeatures ts = new CMImageFeatures(annotations);
       
        ts.HmeanMoment = this.HmeanMoment;
        ts.SmeanMoment = this.SmeanMoment;
        ts.VmeanMoment = this.VmeanMoment;
        
        ts.HdeviationMoment = this.HdeviationMoment;
        ts.SdeviationMoment = this.SdeviationMoment;
        ts.VdeviationMoment = this.VdeviationMoment;
        
        ts.HskewnessMoment = this.HskewnessMoment;
        ts.SskewnessMoment = this.SskewnessMoment;
        ts.VskewnessMoment = this.VskewnessMoment;
        
        ts.setUserTags(this.getUserTags());
        return ts;
    }

    @Override
    public ImageFeatures partialClone(svm_node[] features, int[] annotations) {
                CMImageFeatures ts = new CMImageFeatures(features, annotations);
       
        ts.HmeanMoment = this.HmeanMoment;
        ts.SmeanMoment = this.SmeanMoment;
        ts.VmeanMoment = this.VmeanMoment;
        
        ts.HdeviationMoment = this.HdeviationMoment;
        ts.SdeviationMoment = this.SdeviationMoment;
        ts.VdeviationMoment = this.VdeviationMoment;
        
        ts.HskewnessMoment = this.HskewnessMoment;
        ts.SskewnessMoment = this.SskewnessMoment;
        ts.VskewnessMoment = this.VskewnessMoment;
        
        ts.setUserTags(this.getUserTags());
        return ts;
    }

    @Override
    public ImageFeaturesAlgorithm getProcessingAlgorithm() {
        return new CMImageFeaturesAlgorithm();
    }

    /**
     * @return the HmeanMoment
     */
    public float getHmeanMoment() {
        return HmeanMoment;
    }

    /**
     * @param HmeanMoment the HmeanMoment to set
     */
    public void setHmeanMoment(float HmeanMoment) {
        this.HmeanMoment = HmeanMoment;
    }

    /**
     * @return the SmeanMoment
     */
    public float getSmeanMoment() {
        return SmeanMoment;
    }

    /**
     * @param SmeanMoment the SmeanMoment to set
     */
    public void setSmeanMoment(float SmeanMoment) {
        this.SmeanMoment = SmeanMoment;
    }

    /**
     * @return the VmeanMoment
     */
    public float getVmeanMoment() {
        return VmeanMoment;
    }

    /**
     * @param VmeanMoment the VmeanMoment to set
     */
    public void setVmeanMoment(float VmeanMoment) {
        this.VmeanMoment = VmeanMoment;
    }

    /**
     * @return the HdeviationMoment
     */
    public float getHdeviationMoment() {
        return HdeviationMoment;
    }

    /**
     * @param HdeviationMoment the HdeviationMoment to set
     */
    public void setHdeviationMoment(float HdeviationMoment) {
        this.HdeviationMoment = HdeviationMoment;
    }

    /**
     * @return the SdeviationMoment
     */
    public float getSdeviationMoment() {
        return SdeviationMoment;
    }

    /**
     * @param SdeviationMoment the SdeviationMoment to set
     */
    public void setSdeviationMoment(float SdeviationMoment) {
        this.SdeviationMoment = SdeviationMoment;
    }

    /**
     * @return the VdeviationMoment
     */
    public float getVdeviationMoment() {
        return VdeviationMoment;
    }

    /**
     * @param VdeviationMoment the VdeviationMoment to set
     */
    public void setVdeviationMoment(float VdeviationMoment) {
        this.VdeviationMoment = VdeviationMoment;
    }

    /**
     * @return the HskewnessMoment
     */
    public float getHskewnessMoment() {
        return HskewnessMoment;
    }

    /**
     * @param HskewnessMoment the HskewnessMoment to set
     */
    public void setHskewnessMoment(float HskewnessMoment) {
        this.HskewnessMoment = HskewnessMoment;
    }

    /**
     * @return the SskewnessMoment
     */
    public float getSskewnessMoment() {
        return SskewnessMoment;
    }

    /**
     * @param SskewnessMoment the SskewnessMoment to set
     */
    public void setSskewnessMoment(float SskewnessMoment) {
        this.SskewnessMoment = SskewnessMoment;
    }

    /**
     * @return the VskewnessMoment
     */
    public float getVskewnessMoment() {
        return VskewnessMoment;
    }

    /**
     * @param VskewnessMoment the VskewnessMoment to set
     */
    public void setVskewnessMoment(float VskewnessMoment) {
        this.VskewnessMoment = VskewnessMoment;
    }
}
