/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package imagefeatures;

import java.awt.image.ColorModel;
import java.awt.image.Raster;
import java.awt.image.SampleModel;
import java.awt.image.WritableRaster;
import java.io.Serializable;
import java.util.HashSet;
import java.util.TreeSet;
import javax.media.jai.PlanarImage;
import libsvm.svm_node;
import profileEntropyFeatures.PEF;

/**
 *
 * @author Mihai Pîțu
 */
public abstract class ImageFeatures implements Serializable {

    private HashSet<Integer> sortedAnnotations = new HashSet<>();
    private HashSet<String> userTags;
    private String imageFilename;

    public ImageFeatures() {
    }

    public ImageFeatures(int[] annotations) {
        setAnnotations(annotations);
    }

    public ImageFeatures(int[] annotations, HashSet<String> userTags) {
        setAnnotations(annotations);
        this.userTags = userTags;
    }
    
    public int[] getAnnotations() {
        int[] result = new int[sortedAnnotations.size()];
        Object[] objects = sortedAnnotations.toArray();

        for (int i = 0; i < sortedAnnotations.size(); i++) {
            result[i] = (Integer) objects[i];
        }

        return result;
    }

    public boolean containsAnnotation(int annotation) {
        return this.sortedAnnotations.contains(annotation);
    }

    public void addAnnotation(int annotation) {
        sortedAnnotations.add(annotation);
    }

    public void setAnnotation(int annotation) {
        sortedAnnotations.clear();
        addAnnotation(annotation);
    }

    public final void setAnnotations(int[] annotations) {
        sortedAnnotations.clear();
        if (annotations != null) {
            for (int i = 0; i < annotations.length; i++) {
                sortedAnnotations.add(annotations[i]);
            }
        }
    }
    
    /**
     * @return the userTags
     */
    public HashSet<String> getUserTags() {
        if (userTags == null)
            userTags = new HashSet<>();
        
        return userTags;
    }

    /**
     * @param userTags the userTags to set
     */
    public void setUserTags(HashSet<String> userTags) {
        this.userTags = userTags;
    }
    
    public abstract svm_node[] getFeatures();

    public abstract ImageFeatures partialClone(int[] annotations);

    public abstract ImageFeatures partialClone(svm_node[] features, int[] annotations);

    public abstract ImageFeaturesAlgorithm getProcessingAlgorithm();

    /**
     * @return the imageFilename
     */
    public String getImageFilename() {
        return imageFilename;
    }

    /**
     * @param imageFilename the imageFilename to set
     */
    public void setImageFilename(String imageFilename) {
        this.imageFilename = imageFilename;
    }
}
