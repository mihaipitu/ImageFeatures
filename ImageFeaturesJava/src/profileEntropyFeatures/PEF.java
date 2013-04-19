/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package profileEntropyFeatures;

import imagefeatures.ImageFeatures;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.Raster;
import java.awt.image.SampleModel;
import java.awt.image.WritableRaster;
import java.io.Serializable;
import java.util.ArrayList;
import javax.media.jai.JAI;
import javax.media.jai.PlanarImage;
import javax.media.jai.TiledImage;
import libsvm.svm_node;
import supportVectorMachines.InvalidParameterException;

/**
 *
 * @author Mihai Pîțu
 */
public class PEF implements Serializable {

    private float featureX;
    private float featureY;
    private float featureB;
    private float mean;
    private float standardDeviation;

    public static int[] getPixels(PlanarImage pi) {

        SampleModel sm = pi.getSampleModel();
        int width = pi.getWidth();
        int height = pi.getHeight();
        int bands = sm.getNumBands();

        Raster raster = pi.getData();

        int[] pixels = new int[width * height * bands];
        int y = pi.getMinY();
        int x = pi.getMinX();
        pixels = raster.getPixels(x, y, width, height, pixels);

        return pixels;
    }

    public static byte[] getPixelsB(PlanarImage pi) {

        SampleModel sm = pi.getSampleModel();
        int width = pi.getWidth();
        int height = pi.getHeight();
        int bands = sm.getNumBands();

        Raster raster = pi.getData();

        int[] pixels = new int[width * height * bands];
        int y = pi.getMinY();
        int x = pi.getMinX();
        pixels = raster.getPixels(x, y, width, height, pixels);

        byte[] pixelsB = new byte[width * height * bands];

        int offset;
        for (int h = 0; h < height; h++) {
            for (int w = 0; w < width; w++) {
                offset = h * width * bands + w * bands;
                pixelsB[offset] = (byte) pixels[offset];
                pixelsB[offset + 1] = (byte) pixels[offset + 1];
                pixelsB[offset + 2] = (byte) pixels[offset + 2];
            }
        }
        return pixelsB;
    }

    public static float[] hsvHistogram(PlanarImage pi)  {
        int[] pixels = PEF.getPixels(pi);

        SampleModel sm = pi.getSampleModel();
        int width = pi.getWidth();
        int height = pi.getHeight();
        int bands = sm.getNumBands();
        
        final int bins = 32;
        float[] resultH = new float[pixels.length / 3];
        float[] resultS = new float[pixels.length / 3];
        float[] resultV = new float[pixels.length / 3];
        float upperH = Float.MIN_VALUE;
        float upperS = Float.MIN_VALUE;
        float upperV = Float.MIN_VALUE;
        float lowerH = Float.MAX_VALUE;
        float lowerS = Float.MAX_VALUE;
        float lowerV = Float.MAX_VALUE;
        
        int n = 0;
        int offset;
        int delta = 0;
        for (int h = 0; h < height; h++) {
            for (int w = 0; w < width; w++) {
                offset = h * width * bands + w * bands;

                int r = pixels[offset];
                int g = pixels[offset + 1];
                int b = pixels[offset + 2];

                delta += Math.abs(r - g) + Math.abs(g - b) + Math.abs(b - r);
                
                int min;    //Min. value of RGB
                int max;    //Max. value of RGB
                int delMax; //Delta RGB value

                if (r > g) {
                    min = g;
                    max = r;
                } else {
                    min = r;
                    max = g;
                }
                if (b > max) {
                    max = b;
                }
                if (b < min) {
                    min = b;
                }

                delMax = max - min;

                float H = 0;
                float S;
                float V = max;

                if (delMax == 0) {
                    H = 0;
                    S = 0;
                } else {
                    S = delMax / 255f;
                    if (r == max) {
                        H = ((g - b) / (float) delMax);
                    } else if (g == max) {
                        H = (2 + (b - r) / (float) delMax);
                    } else if (b == max) {
                        H = (4 + (r - g) / (float) delMax);
                    }
                }
                
                if (H > upperH) {
                    upperH = H;
                }
                if (S > upperS) {
                    upperS = S;
                }
                if (V > upperV) {
                    upperV = V;
                }
                if (H < lowerH) {
                    lowerH = H;
                }
                if (S < lowerS) {
                    lowerS = S;
                }
                if (V < lowerV) {
                    lowerV = V;
                }
                
                resultH[n] = H;
                resultS[n] = (S);
                resultV[n] = (V);
                n++;
            }
        }
        
        float[] hisH;
        float[] hisS;
        float[] hisV;
        try {
            hisH = Histogram.compute(resultH, lowerH, upperH, bins).getResult();
            hisS = Histogram.compute(resultS, lowerS, upperS, bins).getResult();
            hisV = Histogram.compute(resultV, lowerV, upperV, bins).getResult();
        } catch(InvalidPefException e) {
            hisH = new float[bins];
            hisS = new float[bins];
            hisV = new float[bins];
            System.out.println(e.toString());
        }
        
        float[] result = new float[3 * bins + 1];

        result[0] = delta;
        System.arraycopy(hisH, 0, result, 1, bins);
        System.arraycopy(hisS, 0, result, bins + 1, bins);
        System.arraycopy(hisV, 0, result, 2 * bins + 1, bins);
        
        return result;
    }

    public static double isGrayscaleLikelihood(PlanarImage pi) {
        int[] pixels = PEF.getPixels(pi);

        SampleModel sm = pi.getSampleModel();
        int width = pi.getWidth();
        int height = pi.getHeight();
        int bands = sm.getNumBands();

        int delta = 0;
        int offset;
        for (int h = 0; h < height; h++) {
            for (int w = 0; w < width; w++) {
                offset = h * width * bands + w * bands;

                int R = pixels[offset];
                int G = pixels[offset + 1];
                int B = pixels[offset + 2];

                delta += Math.abs(R - G) + Math.abs(G - B) + Math.abs(B - R);
            }
        }

        return Math.exp(-(double) delta / Math.pow(width * height, 1.2));
    }
    
     public static TiledImage normalize(TiledImage pi) {

        SampleModel sm = pi.getSampleModel();
        ColorModel cm = pi.getColorModel();
        int width = pi.getWidth();
        int height = pi.getHeight();
        int bands = sm.getNumBands();

        Raster raster = pi.getData();
        WritableRaster wRaster = raster.createCompatibleWritableRaster();

        int[] pixels = new int[width * height * bands];
        raster.getPixels(0, 0, width, height, pixels);

        int offset;
        for (int h = 0; h < height; h++) {
            for (int w = 0; w < width; w++) {
                offset = h * width * bands + w * bands;

                int R = pixels[offset];
                int G = pixels[offset + 1];
                int B = pixels[offset + 2];

                int l = (R + G + B) + 1;
                float r = (float) R / l;
                float g = (float) G / l;
                float b = (float) B / l;

                R = (int) (r * 255);
                G = (int) (g * 255);
                B = (int) (b * 255);
                pixels[offset] = R;
                pixels[offset + 1] = G;
                pixels[offset + 2] = B;
            }
        }

        wRaster.setPixels(0, 0, width, height, pixels);

        TiledImage ti = new TiledImage(0, 0, width, height, 0, 0, sm, cm);
        ti.setData(wRaster);

        return ti;
    }

    public float standardDeviation(float[] x, float mean) {
        float s = 0;

        for (int i = 0; i < x.length; i++) {
            float temp = x[i] - mean;
            s += temp * temp;
        }

        return (float) Math.sqrt(s / (x.length - 1));
    }

    public float[] projectOnX(int[] pixels, int width, int height, Mean mean, boolean[] rgb) {
        //luminance = 0.299*R + 0.587*G + 0.114*B 

        int bands = ((rgb[0]) ? 1 : 0) + ((rgb[1]) ? 1 : 0) + ((rgb[2]) ? 1 : 0);
        float[] result = new float[width];

        int offset;
        for (int w = 0; w < width; w++) {

            float[] columnLuminance = new float[height];

            for (int h = 0; h < height; h++) {
                offset = h * bands + w * height * bands;

                int R = (rgb[0] && (offset < pixels.length)) ? pixels[offset] : 0;
                int G = (rgb[1] && (offset + 1 < pixels.length)) ? pixels[offset + 1] : 0;
                int B = (rgb[2] && (offset + 2 < pixels.length)) ? pixels[offset + 2] : 0;

                columnLuminance[h] = (float) (0.299 * R + 0.587 * G + 0.114 * B);
            }

            result[w] = mean.compute(columnLuminance);
        }

        return result;
    }

    public float[] projectOnY(int[] pixels, int width, int height, Mean mean, boolean[] rgb) {
        //luminance = 0.299*R + 0.587*G + 0.114*B 

        int bands = ((rgb[0]) ? 1 : 0) + ((rgb[1]) ? 1 : 0) + ((rgb[2]) ? 1 : 0);
        float[] result = new float[height];

        int offset;
        for (int h = 0; h < height; h++) {

            float[] rowLuminance = new float[width];

            for (int w = 0; w < width; w++) {
                offset = h * width * bands + w * bands;

                int R = (rgb[0] && (offset < pixels.length)) ? pixels[offset] : 0;
                int G = (rgb[1] && (offset + 1 < pixels.length)) ? pixels[offset + 1] : 0;
                int B = (rgb[2] && (offset + 2 < pixels.length)) ? pixels[offset + 2] : 0;

                rowLuminance[w] = (float) (0.299 * R + 0.587 * G + 0.114 * B);
            }

            result[h] = mean.compute(rowLuminance);
        }

        return result;
    }

    public float[] imageLuminance(int[] pixels, int width, int height, boolean[] rgb) {
        float[] result = new float[width * height];
        int i = 0;
        int offset;
        int bands = ((rgb[0]) ? 1 : 0) + ((rgb[1]) ? 1 : 0) + ((rgb[2]) ? 1 : 0);

        for (int h = 0; h < height; h++) {
            for (int w = 0; w < width; w++) {
                offset = h * width * bands + w * bands;

                int R = (rgb[0] && (offset < pixels.length)) ? pixels[offset] : 0;
                int G = (rgb[1] && (offset + 1 < pixels.length)) ? pixels[offset + 1] : 0;
                int B = (rgb[2] && (offset + 2 < pixels.length)) ? pixels[offset + 2] : 0;

                result[i++] = (float) (0.299 * R + 0.587 * G + 0.114 * B);
            }
        }

        return result;
    }

    public static BufferedImage createGrapghic(float[] array, float h) {

        float max = 0;
        for (int i = 0; i < array.length; i++) {
            if (array[i] * h > max) {
                max = array[i] * h;
            }
        }

        BufferedImage img = new BufferedImage(array.length, (int) (max + 1), BufferedImage.TYPE_INT_RGB);
        Graphics g = img.getGraphics();
        g.setColor(Color.CYAN);
        int lastval = (int) (array[0] * h);
        for (int i = 1; i < array.length; i++) {
            int y = (int) (array[i] * h);
            g.drawOval(i, y, 1, 1);
            g.drawLine(i, y, i - 1, lastval);
            lastval = y;
        }

        return img;
    }

    /*
     * http://doc.utwente.nl/70412/1/Moddemeijer89on.pdf Pdf function estimated
     * using a histogram
     */
    public Histogram probabilityDistributionFuction(float[] t) throws InvalidPefException {
        return Histogram.compute(t);
    }

    /*
     * http://doc.utwente.nl/70412/1/Moddemeijer89on.pdf
     */
    public Entropy entropy(Histogram h) {
        return Entropy.compute(h);
    }

    /**
     * @return the featureX
     */
    public float getFeatureX() {
        return featureX;
    }

    /**
     * @param featureX the featureX to set
     */
    public void setFeatureX(float featureX) {
        this.featureX = featureX;
    }

    /**
     * @return the featureY
     */
    public float getFeatureY() {
        return featureY;
    }

    /**
     * @param featureY the featureY to set
     */
    public void setFeatureY(float featureY) {
        this.featureY = featureY;
    }

    /**
     * @return the featureB
     */
    public float getFeatureB() {
        return featureB;
    }

    /**
     * @param featureB the featureB to set
     */
    public void setFeatureB(float featureB) {
        this.featureB = featureB;
    }

    /**
     * @return the mean
     */
    public float getMean() {
        return mean;
    }

    /**
     * @param mean the mean to set
     */
    public void setMean(float mean) {
        this.mean = mean;
    }

    /**
     * @return the standardDeviation
     */
    public float getStandardDeviation() {
        return standardDeviation;
    }

    /**
     * @param standardDeviation the standardDeviation to set
     */
    public void setStandardDeviation(float standardDeviation) {
        this.standardDeviation = standardDeviation;
    }
}
