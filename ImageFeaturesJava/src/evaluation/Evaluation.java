/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package evaluation;

import java.util.Collection;
import java.util.HashMap;
import supportVectorMachines.SvmEstimate;
import supportVectorMachines.SvmPredict;
import supportVectorMachines.SvmPrediction;

//http://trec.nist.gov/pubs/trec15/appendices/CE.MEASURES06.pdf
//Data Mining - Chapter 5: Credibility: Evaluating What's Been Learned
public class Evaluation {

//    private double accuracy;
//    private double recall;
//    private double precision;
//    private double fmeasure;
    
    private double accuracyMicro;
    private double recallMicro;
    private double precisionMicro;
    private double fmeasureMicro;
    
    private int truePositivesSum;
    private int trueNegativesSum;
    private int falsePositivesSum;
    private int falseNegativesSum;
        
    
    public Evaluation(SvmPrediction[] predictions) {
        this.computeMeasures(predictions);
    }
    
    
    public Evaluation(int truePositives, int trueNegatives, int falsePositives, int falseNegatives, int numInstances, int numLabels) {
        this.truePositivesSum = truePositives;
        this.falsePositivesSum = falsePositives;
        this.falseNegativesSum = falseNegatives;
        this.trueNegativesSum = trueNegatives;
        this.accuracyMicro = computeAccuracy(truePositives, trueNegatives, numInstances, numLabels);
        this.recallMicro = computeRecall(truePositives, falseNegatives);
        this.precisionMicro = computePrecision(truePositives, falsePositives);
        this.fmeasureMicro = computeFmeasure(this.precisionMicro, this.recallMicro);
    }

    private double mean(Collection<Double> values) {
        double result = 0;
        for (double d : values) {
            result += d;
        }

        return result / values.size();
    }

    private void computeMeasures(SvmPrediction[] predictions) {
        int numInstances = predictions.length;
        int numLabels = predictions[0].getEstimates().size();
        
        HashMap<Integer, Double> labelAccuracy = new HashMap<>(numLabels);
        HashMap<Integer, Double> labelRecall = new HashMap<>(numLabels);
        HashMap<Integer, Double> labelPrecision = new HashMap<>(numLabels);
        HashMap<Integer, Double> labelFmeasure = new HashMap<>(numLabels);
    
        HashMap<Integer, Integer> falsePositives = new HashMap<>(numLabels);
        HashMap<Integer, Integer> truePositives = new HashMap<>(numLabels);
        HashMap<Integer, Integer> falseNegatives = new HashMap<>(numLabels);
        HashMap<Integer, Integer> trueNegatives = new HashMap<>(numLabels);

        for (int i = 0; i < numInstances; i++) {
            for (SvmEstimate estimate : predictions[i].getEstimates()) {
                boolean actual = estimate.getActual();
                boolean predicted = estimate.getBinaryScore();
                int label = estimate.getLabel();
                
                if (actual && predicted) {
                    if (!truePositives.containsKey(label)) {
                        truePositives.put(label, 0);
                    }
                    truePositives.put(label, truePositives.get(label) + 1);
                } else if (!actual && !predicted) {
                    if (!trueNegatives.containsKey(label)) {
                        trueNegatives.put(label, 0);
                    }
                    trueNegatives.put(label, trueNegatives.get(label) + 1);
                } else if (!actual && predicted) {
                    if (!falsePositives.containsKey(label)) {
                        falsePositives.put(label, 0);
                    }
                    falsePositives.put(label, falsePositives.get(label) + 1);
                } else { //actual && !predicted
                    if (!falseNegatives.containsKey(label)) {
                        falseNegatives.put(label, 0);
                    }
                    falseNegatives.put(label, falseNegatives.get(label) + 1);
                }
            }
        }
        
        Collection<SvmEstimate> estimates = predictions[0].getEstimates();
        
        for (SvmEstimate estimate : estimates) {
            int label = estimate.getLabel();
            int tp = truePositives.containsKey(label) ? truePositives.get(label) : 0;
            int tn = trueNegatives.containsKey(label) ? trueNegatives.get(label) : 0;
            int fn = falseNegatives.containsKey(label) ? falseNegatives.get(label) : 0;
            int fp = falsePositives.containsKey(label) ? falsePositives.get(label) : 0;
        
            setTruePositivesSum(truePositivesSum + tp);
            setTrueNegativesSum(trueNegativesSum + tn);
            setFalseNegativesSum(falseNegativesSum + fn);
            setFalsePositivesSum(falsePositivesSum + fp);
            
            labelAccuracy.put(label, (double) (tp + tn) / numInstances);

            labelRecall.put(label, computeRecall(tp, fn));

            labelPrecision.put(label, computePrecision(tp, fp));

            labelFmeasure.put(label, computeFmeasure(labelPrecision.get(label), labelRecall.get(label)));
        }

//        this.accuracy = mean(labelAccuracy.values());
//        this.recall = mean(labelRecall.values());
//        this.precision = mean(labelPrecision.values());
//        this.fmeasure = mean(labelFmeasure.values());
        
        this.accuracyMicro  = computeAccuracy(this.truePositivesSum, this.trueNegativesSum, numInstances, numLabels);
        this.recallMicro    = computeRecall(truePositivesSum, falseNegativesSum);
        this.precisionMicro = computePrecision(truePositivesSum, falsePositivesSum);
        this.fmeasureMicro  = computeFmeasure(this.precisionMicro, this.recallMicro);   
    }
    
    public static double computeAccuracy(double truePositives, double trueNegatives, int numInstances, int numLabels) {
        return (truePositives + trueNegatives) / (numInstances * numLabels);
    }
    
    public static double computeRecall(double truePositives, double falseNegatives) {
        return (truePositives + falseNegatives == 0) ? 0 : (double) truePositives / (truePositives + falseNegatives);   
    }
    
    public static double computePrecision(double truePositives, double falsePositives) {
        return (truePositives + falsePositives == 0) ? 0 : (double) truePositives / (truePositives + falsePositives);   
    }
    
    public static double computeFmeasure(double precision, double recall) {
        return (precision + recall == 0) ? 0.0 : 2.0 * precision * recall / (precision + recall);
    }
    
    public static double computeFmeasure(double truePositives, double falsePositives, double falseNegatives) {
        double precision = computePrecision(truePositives, falsePositives);
        double recall = computeRecall(truePositives, falseNegatives);
        return (precision + recall == 0) ? 0.0 : 2.0 * precision * recall / (precision + recall);
    }

    @Override
    public String toString() {
//        return "Accuracy: " + this.accuracy + "\n" +
//               "Recall: " + this.recall + "\n" +
//               "Precision: " + this.precision + "\n" +
//               "F-measure: " + this.fmeasure + "\n\n" + 
               return 
               "True positives: " + this.getTruePositivesSum() + "\t" +
               "True negatives: " + this.getTrueNegativesSum() + "\n" +
               "False positives: " + this.getFalsePositivesSum() + "\t" +
               "False negatives: " + this.getFalseNegativesSum() + "\n\n" + 
               
               "Micro Accuracy: " + this.accuracyMicro + "\n" +
               "Micro Recall: " + this.recallMicro + "\n" +
               "Micro Precision: " + this.precisionMicro + "\n" +
               "Micro F-measure: " + this.fmeasureMicro + "\n\n";
    }
    
//    /**
//     * @return the accuracy
//     */
//    public double getAccuracy() {
//        return accuracy;
//    }
//
//    /**
//     * @return the recall
//     */
//    public double getRecall() {
//        return recall;
//    }
//
//    /**
//     * @return the precision
//     */
//    public double getPrecision() {
//        return precision;
//    }
//
//    /**
//     * @return the fmeasure
//     */
//    public double getFmeasure() {
//        return fmeasure;
//    }

    /**
     * @return the recallMicro
     */
    public double getRecallMicro() {
        return recallMicro;
    }

    /**
     * @return the precisionMicro
     */
    public double getPrecisionMicro() {
        return precisionMicro;
    }

    /**
     * @return the fmeasureMicro
     */
    public double getFmeasureMicro() {
        return fmeasureMicro;
    }

    /**
     * @return the truePositivesSum
     */
    public int getTruePositivesSum() {
        return truePositivesSum;
    }

    /**
     * @return the trueNegativesSum
     */
    public int getTrueNegativesSum() {
        return trueNegativesSum;
    }

    /**
     * @return the falsePositivesSum
     */
    public int getFalsePositivesSum() {
        return falsePositivesSum;
    }

    /**
     * @return the falseNegativesSum
     */
    public int getFalseNegativesSum() {
        return falseNegativesSum;
    }

    /**
     * @param truePositivesSum the truePositivesSum to set
     */
    public void setTruePositivesSum(int truePositivesSum) {
        this.truePositivesSum = truePositivesSum;
    }

    /**
     * @param trueNegativesSum the trueNegativesSum to set
     */
    public void setTrueNegativesSum(int trueNegativesSum) {
        this.trueNegativesSum = trueNegativesSum;
    }

    /**
     * @param falsePositivesSum the falsePositivesSum to set
     */
    public void setFalsePositivesSum(int falsePositivesSum) {
        this.falsePositivesSum = falsePositivesSum;
    }

    /**
     * @param falseNegativesSum the falseNegativesSum to set
     */
    public void setFalseNegativesSum(int falseNegativesSum) {
        this.falseNegativesSum = falseNegativesSum;
    }
}
