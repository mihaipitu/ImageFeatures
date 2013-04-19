/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package HarrisLaplaceFeatures;

import java.util.ArrayList;
import java.util.List;

public class HarrisLaplace {

    private GaussianPyramid gp = null;
    // corners list
    public List<InterestCorner> interestcorners = new ArrayList<InterestCorner>();

    // compute Gradient (sobel) 
    private float[] sobel(int x, int y, int o, int s, float[] sobel) {
        PyramidLevel p = this.gp.getLevel(o, s);
        float v00 = p.getData(x - 1, y - 1), v10 = p.getData(x, y - 1), v20 = p.getData(x + 1, y - 1);
        float v01 = p.getData(x - 1, y), v21 = p.getData(x + 1, y);
        float v02 = p.getData(x - 1, y + 1), v12 = p.getData(x, y + 1), v22 = p.getData(x + 1, y + 1);
        float sx = (v20 + 2 * v21 + v22) - (v00 + 2 * v01 + v02);
        float sy = (v02 + 2 * v12 + v22) - (v00 + 2 * v10 + v20);
        if (sobel == null) {
            sobel = new float[2];
        }
        sobel[0] = sx / 4;
        sobel[1] = sy / 4;
        return sobel;
    }

    // compute Laplacian
    private float laplacian(int x, int y, int o, int s) {
        PyramidLevel p = this.gp.getLevel(o, s);
        float v10 = p.getData(x, y - 1);
        float v01 = p.getData(x - 1, y), v11 = p.getData(x, y), v21 = p.getData(x + 1, y);
        float v12 = p.getData(x, y + 1);
        float laplacian = -v11 + 0.25f * (v10 + v01 + v21 + v12);
        return laplacian;
    }

    // compute Scale-Normalized Laplacian
    private float laplacianNormalized(int x, int y, int o, int s) {
        float sigma2 = (float) this.gp.getLevel(o, s).sigma2;
        return sigma2 * laplacian(x, y, o, s);
    }

    // compute Harris measure
    private float harris(int x, int y, int o, int s) {
        float m00 = 0, m01 = 0, m10 = 0, m11 = 0;
        float[] sobel = new float[2];
        // Gaussian kernel of the integration window
        float[][] kernel = {{1, 2, 1}, {2, 4, 2}, {1, 2, 1}};
        float normalizer = 16;
        // for each pixel in the integration window
        for (int dy = -1; dy <= 1; dy++) {
            for (int dx = -1; dx <= 1; dx++) {
                int xk = x + dx;
                int yk = y + dy;
                // gradient value
                sobel(xk, yk, o, s, sobel);
                float gx = sobel[0];
                float gy = sobel[1];
                // gaussian weight
                float w = kernel[1 + dx][1 + dy];
                // second-moment matrix elements
                m00 += gx * gx * w;
                m01 += gx * gy * w;
                m10 = m01;
                m11 += gy * gy * w;
            }
        }
        // harris measure
        float harris = m00 * m11 - m01 * m10 - 0.06f * (m00 + m11) * (m00 + m11);
        harris = harris / (255 * 255 * normalizer);
        return harris;
    }

    // return true if the pixel (x,y,o,s) is a scale extrema 
    private boolean isScaleExtrema(int x, int y, int o, int s) {
        float lap = laplacianNormalized(x, y, o, s);
        if (Math.abs(lap) < 1) {
            return false;
        }

        float lap_prev = laplacianNormalized(x, y, o, s - 1);
        float lap_next = laplacianNormalized(x, y, o, s + 1);
        if (lap_prev < lap && lap > lap_next) {
            return true;
        }
        if (lap_prev > lap && lap < lap_next) {
            return true;
        }
        return false;
    }

    // return true if the pixel (x,y,o,s) is a spatial maxima 
    private boolean isSpatialMaxima(int x, int y, int o, int s) {
        double h = harris(x, y, o, s);
        if (Math.abs(h) < 1) {
            return false;
        }

        if (h <= harris(x - 1, y - 1, o, s) || h <= harris(x, y - 1, o, s) || h <= harris(x + 1, y - 1, o, s)) {
            return false;
        }
        if (h <= harris(x - 1, y, o, s) || h <= harris(x + 1, y, o, s)) {
            return false;
        }
        if (h <= harris(x - 1, y + 1, o, s) || h <= harris(x, y + 1, o, s) || h <= harris(x + 1, y + 1, o, s)) {
            return false;
        }
        return true;
    }

    // main algorithm
    public void detect(int[][] image, int width, int height) {
        int octaves = 5, scales = 9;

        this.gp = new GaussianPyramid(octaves, scales);
        this.gp.build(image, width, height);

        // for each octave+scale
        for (int o = 0; o < gp.getOctaves(); o++) {
            for (int s = 1; s < gp.getScales() - 1; s++) {
                int w = gp.getLevel(o, 0).width;
                int h = gp.getLevel(o, 0).height;

                // explore all pixels of the level
                for (int y = 1; y < h - 1; y++) {
                    for (int x = 1; x < w - 1; x++) {

                        // keep only spatial maxima
                        if (!isSpatialMaxima(x, y, o, s)) {
                            continue;
                        }

                        // keep only scale extrema
                        if (!isScaleExtrema(x, y, o, s)) {
                            continue;
                        }

                        // localization in original image (average)
                        int xc = (int) Math.round((0.5 + x) * (1 << o));
                        int yc = (int) Math.round((0.5 + y) * (1 << o));
                        int sc = (int) Math.round(gp.getSigma2(o, s));

                        System.out.printf("Found points x=%d, y=%d, sigma²=%d\n", xc, yc, sc);
                        interestcorners.add(new InterestCorner(xc, yc, sc));
                    }
                }
            }
        }
    }
}

// corner class
class InterestCorner {

    int x, y;
    float sigma;

    public InterestCorner(int x, int y, float sigma) {
        this.x = x;
        this.y = y;
        this.sigma = sigma;
    }
}