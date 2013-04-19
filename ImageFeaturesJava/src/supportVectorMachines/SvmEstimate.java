/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package supportVectorMachines;

import java.util.Comparator;

/**
 *
 * @author Mihai Pîțu
 */
public class SvmEstimate implements Comparator<SvmEstimate> {

    private int label;
    private float probability;
    private boolean binaryScore;
    private boolean actual = false;

    SvmEstimate(int label, float estimate) {
        this.label = label;
        this.probability = estimate;
        this.binaryScore = SvmPredict.binaryScore(estimate);
    }

    SvmEstimate(int label, float estimate, boolean actual) {
        this.label = label;
        this.binaryScore = SvmPredict.binaryScore(estimate);
        this.probability = estimate;
        this.actual = actual;
    }

    @Override
    public int compare(SvmEstimate o1, SvmEstimate o2) {
        if (o1.getProbability() > o2.getProbability()) {
            return -1;
        }
        if (o1.getProbability() < o2.getProbability()) {
            return 1;
        }
        return 0;
    }
    
    @Override
    public String toString() {
        return "" + label + ": " + probability + "(" + actual + ")";
    }

    /**
     * @return the label
     */
    public int getLabel() {
        return label;
    }

    /**
     * @return the probability
     */
    public float getProbability() {
        return probability;
    }

    /**
     * @return the actual
     */
    public boolean getActual() {
        return actual;
    }

    /**
     * @param label the label to set
     */
    public void setLabel(int label) {
        this.label = label;
    }

    /**
     * @param probability the probability to set
     */
    public void setProbability(float probability) {
        this.probability = probability;
        this.binaryScore = SvmPredict.binaryScore(probability);
    }

    /**
     * @param actual the actual to set
     */
    public void setActual(boolean actual) {
        this.actual = actual;
    }

    /**
     * @return the binaryScore
     */
    public boolean getBinaryScore() {
        return binaryScore;
    }
}
