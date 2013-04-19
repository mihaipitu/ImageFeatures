/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package colorMoments;

import imagefeatures.ImageFeatures;
import imagefeatures.ImageFeaturesAlgorithm;
import java.awt.image.SampleModel;
import javax.media.jai.JAI;
import javax.media.jai.PlanarImage;
import profileEntropyFeatures.PEF;

/**
 *
 * @author Mihai Pîțu
 */
public class CMImageFeaturesAlgorithm implements ImageFeaturesAlgorithm {

    @Override
    public CMImageFeatures getImageFeatures(String imagePath, int[] labels) throws Exception {
        CMImageFeatures imf = CMImageFeatures.getCMImageFeatures(imagePath);
        if (labels != null) {
            for (int label : labels) {
                imf.addAnnotation(label);
            }
        }
        imf.setImageFilename(imagePath);
        return imf;
    }
}
