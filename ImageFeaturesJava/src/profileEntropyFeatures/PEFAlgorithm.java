/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package profileEntropyFeatures;

import imagefeatures.ImageFeatures;
import imagefeatures.ImageFeaturesAlgorithm;
import java.awt.image.SampleModel;
import java.util.ArrayList;
import javax.media.jai.JAI;
import javax.media.jai.PlanarImage;
import javax.media.jai.TiledImage;

/**
 *
 * @author Mihai Pîțu
 */
public class PEFAlgorithm implements ImageFeaturesAlgorithm {

    private Mean mean = new ArithmeticMean();

    @Override
    public PEFImageFeatures getImageFeatures(String imagePath, int[] labels) {
        PlanarImage pi = JAI.create("fileload", imagePath);
        TiledImage ti = new TiledImage(pi, true);
        //int[] pixels = PEF.getPixels(pi);
        PEFImageFeatures f;
        //try {
        f = compute(ti, mean, false, true);
        //} catch (Exception ex) {
        //    f = new PEFImageFeatures();
        //    ex.printStackTrace();
        //}
        if (labels != null) {
            for (int label : labels) {
                f.addAnnotation(label);
            }
        }
        pi.dispose();
        ti.dispose();
        
        f.setImageFilename(imagePath);
        return f;
    }

    public static PEFImageFeatures compute(TiledImage image, Mean mean, boolean normalize, boolean horizontal) {

        PEFImageFeatures pefFeatures = new PEFImageFeatures();

        if (normalize) {
            image = PEF.normalize(image);
        }

        SampleModel sm;
        int width = image.getWidth();
        int height = image.getHeight();
        int bands;

        ArrayList<TiledImage> images = new ArrayList<>();
        images.add(image);

        int tiles = 3;
        if (horizontal) {
            int subHeight = height / tiles;
            for (int i = 0; i < tiles; i++) {
                int offset = i * subHeight;
                images.add(image.getSubImage(0, offset, width, subHeight));
            }
        } else {
            int subWidth = width / tiles;
            for (int i = 0; i < tiles; i++) {
                images.add(image.getSubImage(i * subWidth, 0, subWidth, height));
            }
        }

        for (TiledImage selectedImage : images) {

            sm = selectedImage.getSampleModel();
            width = selectedImage.getWidth();
            height = selectedImage.getHeight();
            bands = sm.getNumBands();

            for (int band = 0; band < bands; band++) {

                PlanarImage bandImage = JAI.create("bandselect", selectedImage, new int[]{band});
                //int nrOfBands = 1;
                int[] pixels = PEF.getPixels(bandImage);
                PEF pef = new PEF();

                boolean[] rgb = new boolean[bands];
                for (int i = 0; i < bands; i++) {
                    rgb[i] = false;
                }
                rgb[band] = true;

                float feat;
                Histogram h;
                Entropy e;
                try {
                    //projection on X axis
                    float[] projectionOnX = pef.projectOnX(pixels, width, height, mean, rgb);
                    h = pef.probabilityDistributionFuction(projectionOnX);
                    e = pef.entropy(h);
                    feat = (float) (e.getEstimate() / Math.log(h.getNcells()));
                    if (Float.isNaN(feat)) {
                        //throw new InvalidPefException("Invalid feature");
                        feat = 0;
                    }
                } catch (InvalidPefException ex) {
                    feat = 0;
                }
                pef.setFeatureX(feat);

                try {
                    //projection on Y axis
                    float[] projectionOnY = pef.projectOnY(pixels, width, height, mean, rgb);
                    h = pef.probabilityDistributionFuction(projectionOnY);
                    e = pef.entropy(h);
                    feat = (float) (e.getEstimate() / Math.log(h.getNcells()));
                    if (Float.isNaN(feat)) {
                        //throw new InvalidPefException("Invalid feature");

                        feat = 0;
                    }
                } catch (InvalidPefException ex) {
                    feat = 0;
                }
                pef.setFeatureY(feat);

                //the whole image
                float[] wholeImage = pef.imageLuminance(pixels, width, height, rgb);

                try {
                    h = pef.probabilityDistributionFuction(wholeImage);
                    e = pef.entropy(h);
                    feat = (float) (e.getEstimate() / Math.log(h.getNcells()));
                    if (Float.isNaN(feat)) {
                        //throw new InvalidPefException("Invalid feature");
                        feat = 0;
                    }
                } catch (InvalidPefException ex) {
                    feat = 0;
                }
                pef.setFeatureB(feat);

                //mean
                float M;
                M = mean.compute(wholeImage);
                if (Float.isNaN(M)) {
                    //throw new InvalidPefException("Invalid feature");
                    M = 0;
                }
                pef.setMean(M);

                //standard deviation
                feat = pef.standardDeviation(wholeImage, M);
                if (feat == Float.NaN) {
                    //throw new InvalidPefException("Invalid feature");
                    feat = 0;
                }
                pef.setStandardDeviation(feat);

                pefFeatures.addFeature(pef);
            }
        }

        return pefFeatures;
    }
}
