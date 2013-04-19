/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package profileEntropyFeatures;

/**
 *
 * @author Mihai Pîțu
 */
public class Entropy {
    
    private float estimate;
    private float nbias; //bias (or bias function) of an estimator is the difference between this estimator's expected value and the true value of the parameter being estimated
    private float sigma;
    private Histogram histogram;

    public static enum Approach {Biased, Unbiased, MinSquareError};
    
    public Entropy (float estimate, float nbias, float sigma, Histogram histogram) {
        this.estimate = estimate;
        this.nbias = nbias;
        this.sigma = sigma;
        this.histogram = histogram;
    }
    
    public static Entropy compute(Histogram histogram, Approach approach, float base) {
        
        float estimate = 0;
        float sigma = 0;
        float count = 0;
        float lambda = 0;
        
        float[] hResult = histogram.getResult();
        for (int i = 0; i < histogram.getNcells(); i++) {
            float logf = 0;
            if (hResult[i] != 0)
                logf = (float) Math.log(hResult[i]);
            
            count += hResult[i];
            estimate -= hResult[i] * logf;
            sigma += hResult[i] * logf * logf;
        }
        
        //biased estimate
        estimate = estimate / count;
        sigma = (float) Math.sqrt((sigma / count - estimate * estimate) / (count - 1));
        estimate += Math.log(count) + Math.log((histogram.getUpperBound() - histogram.getLowerBound()) / histogram.getNcells());
        
        float nbias = -(histogram.getNcells() - 1) / (2 * count);
        
        
        switch (approach) {
            case Unbiased: //conversion to unbiased estimate
                estimate -= nbias;
                break;
                
            case MinSquareError: //conversion to minimum Mean Square Error estimate
                estimate -= nbias;
                lambda = (estimate * estimate) / (estimate * estimate + sigma * sigma);
                nbias = (1 - lambda) * estimate;
                estimate = lambda * estimate;
                sigma = lambda * sigma;
                break;
        }
        
        //base transform
        estimate = (float) (estimate / Math.log(base));
        nbias = (float) (nbias / Math.log(base));
        sigma = (float) (sigma / Math.log(base));
        
        return new Entropy(estimate, nbias, sigma, histogram);
    }
    
    public static Entropy compute(Histogram h) {
        Approach app = Approach.Unbiased;
        float base = (float) Math.exp(1);
        
        return Entropy.compute(h, app, base);
    }
    
    /**
     * @return the estimate
     */
    public float getEstimate() {
        return estimate;
    }

    /**
     * @return the nbias
     */
    public float getNbias() {
        return nbias;
    }

    /**
     * @return the sigma
     */
    public float getSigma() {
        return sigma;
    }

    /**
     * @return the histogram
     */
    public Histogram getHistogram() {
        return histogram;
    }
}
