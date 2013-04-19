/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package HarrisLaplaceFeatures;

public class GaussianPyramid {

    // the levels of the pyramid 
    private PyramidLevel[][] levels = null;
    // number of octaves and scales
    private int octaves = 0, scales = 0;

    // public constructor
    public GaussianPyramid(int octaves, int scales) {
        this.octaves = octaves;
        this.scales = scales;
        this.levels = new PyramidLevel[this.octaves][this.scales + 1];
    }

    // ---- private methodes ------------------------------------------------------------------
    // create a downsampled version of the level "src" 
    // L(x,y,s) = L(x/2,y/2,s/4)
    private PyramidLevel newDownSampled(PyramidLevel src) {
        int w = src.width / 2, h = src.height / 2;
        PyramidLevel dest = new PyramidLevel(w, h, src.sigma2 / 4);
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                dest.data[x][y] = src.getData(2 * x + 1, 2 * y + 1);
            }
        }
        return dest;
    }

    // create a downscaled version of the level "src" using the given kernel
    // L(x,y,s) = L(x,y,t) * G(x,y,w) 
    private PyramidLevel newDownScaled(PyramidLevel src, GaussianKernel kernel) {
        PyramidLevel dest = new PyramidLevel(src.width, src.height, src.sigma2 + kernel.sigma2);
        convolve2D(src, dest, kernel);
        return dest;
    }

    // 2D convolution (using kernel separation) 
    private void convolve2D(PyramidLevel src, PyramidLevel dest, GaussianKernel kernel) {
        PyramidLevel buffer = new PyramidLevel(src.width, src.height, 0.0);
        float normalizer = kernel.normalizer * kernel.normalizer;
        float v;
        // horizontal
        for (int y = 0; y < src.height; y++) {
            for (int x = 0; x < src.width; x++) {
                v = 0;
                for (int k = -kernel.radius; k <= kernel.radius; k++) {
                    v += kernel.data[kernel.radius + k] * src.getData(x + k, y);
                }
                buffer.data[x][y] = v;
            }
        }
        // vertical
        for (int y = 0; y < src.height; y++) {
            for (int x = 0; x < src.width; x++) {
                v = 0;
                for (int k = -kernel.radius; k <= kernel.radius; k++) {
                    v += kernel.data[kernel.radius + k] * buffer.getData(x, y + k);
                }
                dest.data[x][y] = v / normalizer;
            }
        }
    }

    // ---- public methodes -------------------------------------------------------------------
    // build the pyramid using the given image
    public void build(int[][] img, int width, int height) {

        // copy initial image into our custom structure
        this.levels[0][0] = new PyramidLevel(width, height, 1.0);
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                this.levels[0][0].data[x][y] = img[x][y];
            }
        }

        // precompute the gaussian kernels used at each scale such that
        GaussianKernel[] kernels = new GaussianKernel[this.scales + 1];
        // the scales increase geometricaly from 1.0 to 4.0 => K = 4^(1/scales)
        double K = Math.pow(4, 1.0 / this.scales);
        for (int s = 1; s < this.scales + 1; s++) {
            // Level[s] = Level[s-1]*G(w) => K^s = K^(s-1) + w
            double w = Math.pow(K, s) - Math.pow(K, s - 1);
            kernels[s] = new GaussianKernel(w);
        }

        // for each octave
        for (int o = 0; o < this.octaves; o++) {
            // first scale : create a downsampled copy of the previous octave
            if (o > 0) {
                this.levels[o][0] = newDownSampled(this.levels[o - 1][this.scales]);
            }
            // subsequent scales : create a convolved copy of the previous scale 
            for (int s = 1; s < this.scales + 1; s++) {
                this.levels[o][s] = newDownScaled(this.levels[o][s - 1], kernels[s]);
            }
        }
    }

    // return value of pixel(x,y) at scale(o,s) using bilinear interpolation
    public float getValue(int x, int y, int o, int s) {
        PyramidLevel p = this.levels[o][s];
        // coords of the pixel (x,y) in the (possibly downsampled) data 
        int xp = x >> o, yp = y >> o;
        // size of the "original" area of pixels (before downsampling)
        int size = 1 << o;
        // position of the pixel (x,y) in the "original" area
        int ypos = y - (yp << o);
        int xpos = x - (xp << o);
        // bilinear interpolation
        float v = 0;
        v += (size - ypos) * ((size - xpos) * p.getData(xp, yp) + (xpos) * p.getData(xp + 1, yp));
        v += (ypos) * ((size - xpos) * p.getData(xp, yp + 1) + (xpos) * p.getData(xp + 1, yp + 1));
        return v / (size * size);
    }

    // ---- getters / setters -----------------------------------------------------------------
    public PyramidLevel getLevel(int o, int s) {
        return this.levels[o][s];
    }

    public double getSigma2(int o, int s) {
        return Math.pow(4, o) * this.levels[o][s].sigma2;
    }

    public int getOctaves() {
        return octaves;
    }

    public int getScales() {
        return scales;
    }
}