/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package imagefeatures;

import colorMoments.CMImageFeaturesAlgorithm;
import java.util.concurrent.*;
import profileEntropyFeatures.PEFAlgorithm;
import speededUpRobustFeatures.TopSurfAlgorithm;

/**
 *
 * @author Mihai Pîțu
 */
public class CombinedImageFeaturesAlgorithm implements ImageFeaturesAlgorithm {

    private final PEFAlgorithm pefAlg = new PEFAlgorithm();
    private final TopSurfAlgorithm surfAlg = new TopSurfAlgorithm();
    private final CMImageFeaturesAlgorithm cmAlg = new CMImageFeaturesAlgorithm();
    
    //private ThreadPoolExecutor threadPool = new ThreadPoolExecutor(2, 3, 10, TimeUnit.MINUTES, new LinkedBlockingQueue<Runnable>());

    @Override
    public ImageFeatures getImageFeatures(String imagePath, int[] labels) throws Exception {

        CombinedImageFeatures cimf = new CombinedImageFeatures(surfAlg.getImageFeatures(imagePath, labels),
                                                               pefAlg.getImageFeatures(imagePath, labels),
                                                               cmAlg.getImageFeatures(imagePath, labels));
        cimf.setImageFilename(imagePath);
        return cimf;
    }
}
