/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package imagefeatures;

/**
 *
 * @author Mihai Pîțu
 */
public interface ImageFeaturesAlgorithm {
    
    public ImageFeatures getImageFeatures(String imagePath, int[] labels) throws Exception;
}
