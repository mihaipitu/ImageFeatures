/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package supportVectorMachines;

import com.googlecode.javacv.cpp.opencv_core.CvSeq;
import imagefeatures.Concepts;
import imagefeatures.ImageFeatures;
import java.awt.Rectangle;
import java.io.IOException;
import java.util.*;
import java.util.Map.Entry;
import libsvm.*;
import supportVectorMachines.Kernels.CombinedKernel;

import supportVectorMachines.Kernels.Kernel;

/**
 *
 * @author Mihai Pîțu
 */
public class SvmPredict {

    public static enum PredictType {
        NoPostPredict, PostPredictFacesOnly, PostPredict, BoostProbability
    };
    public static float highConfidenceThreshold = 0.8f;
    public static float lowConfidenceThreshold = 0.3f;
    public static float BinaryThreshold = 0.5f;
    public static float boostThreshold = 0.70f;

    public static boolean binaryScore(float probability) {
        return (probability > BinaryThreshold) ? true : false;
    }

    private static void disjunctConcepts(SvmPrediction p, int highConfidenceConcept, int... lowConfidenceConcepts) {
        float eH = p.getEstimate(highConfidenceConcept);
        if (eH < highConfidenceThreshold) {
            float neweH = (eH + highConfidenceThreshold) / 2.0f;
            p.setEstimate(highConfidenceConcept, neweH, p.getActual(highConfidenceConcept));
        }

        for (int lowConfidenceConcept : lowConfidenceConcepts) {
            float e = p.getEstimate(lowConfidenceConcept);
            if (e > lowConfidenceThreshold) {
                float newE = (e + lowConfidenceThreshold) / 2.0f;
                p.setEstimate(lowConfidenceConcept, newE, p.getActual(lowConfidenceConcept));
            }
        }
    }
    public final static int noPersons = 45;
    public final static int onePerson = 46;
    public final static int twoPersons = 47;
    public final static int threePersons = 48;
    public final static int smallgroup = 49;
    public final static int biggroup = 50;
    public final static int viewportrait = 70;
    public final static int style_graycolor = 68;
    public final static int gender_male = 56;
    public final static int gender_female = 57;

    public static void postSvmPredict(SvmPrediction prediction, ImageFeatures imageFeatures, PredictType predictType) {

        try {
            int faces = utils.JOpenCV.nrFaceDetection(imageFeatures.getImageFilename()); //detector.getFaces(imageFile, 1.2f,1.1f,.05f, 2,true);

            switch (faces) {
                case 0:
                    disjunctConcepts(prediction, noPersons, onePerson, twoPersons, threePersons, smallgroup, biggroup, viewportrait, gender_female, gender_male);
                    break;
                case 1:
                    disjunctConcepts(prediction, viewportrait);
                    disjunctConcepts(prediction, onePerson, noPersons, twoPersons, threePersons, smallgroup, biggroup);
                    break;
                case 2:
                    disjunctConcepts(prediction, twoPersons, noPersons, onePerson, threePersons, smallgroup, biggroup);
                    break;
                case 3:
                    disjunctConcepts(prediction, threePersons, noPersons, onePerson, twoPersons, smallgroup, biggroup);
                    break;
                case 4:
                case 5:
                case 6:
                case 7:
                case 8:
                case 9:
                    disjunctConcepts(prediction, smallgroup, noPersons, onePerson, twoPersons, threePersons, biggroup);
                    break;
                default:
                    disjunctConcepts(prediction, biggroup, noPersons, onePerson, twoPersons, threePersons, smallgroup);
                    break;
            }



            if (PredictType.PostPredictFacesOnly == predictType) {
                return;
            }

            Collection<SvmEstimate> c = prediction.getEstimates();
            SvmEstimate[] estimates = new SvmEstimate[c.size()];
            c.toArray(estimates);

            for (int i = 0; i < estimates.length; i++) {

                if (estimates[i].getBinaryScore()) {
                    if (implicationConceptsCombinationsOccurrences.containsKey(estimates[i].getLabel())) {
                        for (int concept : implicationConceptsCombinationsOccurrences.get(estimates[i].getLabel())) {
                            if (!prediction.getBinaryScore(concept)) {
                                prediction.getEstimateObj(concept).setProbability(estimates[i].getProbability() - 0.01f);
                            }
                        }
                    }
                }

                for (int j = i + 1; j < estimates.length; j++) {
                    if (estimates[i].getBinaryScore() && estimates[j].getBinaryScore()) {
                        //check for exclusive concepts

                        if (Concepts.exclusiveConcepts(estimates[i].getLabel(), estimates[j].getLabel(), conceptCombinationsOccurrences)) {
                            //two exclusive concepts 
                            if (estimates[i].getProbability() > estimates[j].getProbability()) {
                                estimates[j].setProbability(BinaryThreshold - 0.01f);
                            } else {
                                estimates[i].setProbability(BinaryThreshold - 0.01f);
                            }
                        }
                    }
                }
            }

        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
    private static HashMap<Integer, HashSet<Integer>> conceptCombinationsOccurrences;
    private static HashMap<Integer, HashSet<Integer>> implicationConceptsCombinationsOccurrences;
    public static int exclusiveConceptCombinationsThreshold = 2;
    public static float implicationConceptCombinationsRaport = 0.95f;
    public static PredictType predictType = PredictType.BoostProbability;

    public static SvmPrediction predictBinaryGroupedKernels(ArrayList<ImageFeatures> allImageFeatures, ImageFeatures imageFeatures, Map<Integer, LinkedList<svm_model>> models, SvmScale svmScale, svm_print_interface printInterface) throws IOException, InvalidOperationException {
        if (conceptCombinationsOccurrences == null) {
            conceptCombinationsOccurrences = Concepts.exclusiveConceptCombinationsOccurrencesThreshold(allImageFeatures, exclusiveConceptCombinationsThreshold);
            implicationConceptsCombinationsOccurrences = Concepts.implicationConceptCombinationsOccurrencesThreshold(allImageFeatures, implicationConceptCombinationsRaport);
        }

        SvmPrediction prediction = new SvmPrediction();

        if (svmScale != null) {
            svmScale.imageFeatureScale(imageFeatures);
        }

        HashMap<CombinedKernel, HashSet<Integer>> groupedConcepts = Concepts.getKernelGroupedConcepts();

        for (Entry<CombinedKernel, HashSet<Integer>> entry : groupedConcepts.entrySet()) {

            svm_problem svmProblem = entry.getKey().computeGramMatrixTest(allImageFeatures, imageFeatures);
            
            for (Integer concept : groupedConcepts.get(entry.getKey())) {
                SvmPrediction svmPrediction = predictBinaryProbability(svmProblem.x[0], models.get(concept), printInterface);
                float e = svmPrediction.getEstimate(SvmTrain.POSITIVE_CLASS);
                
                if (predictType == PredictType.BoostProbability) {
                    e = (float) Math.pow((1 - Math.pow((e - 1), 2) * 1), boostThreshold); // from circle's equation
                }

                if (imageFeatures.containsAnnotation(concept)) {
                    prediction.setEstimate(concept, e, true);
                } else {
                    prediction.setEstimate(concept, e, false);
                }
            }
        }

        if (PredictType.NoPostPredict != predictType) {
            postSvmPredict(prediction, imageFeatures, predictType);
        }

        return prediction;
    }

    public static SvmPrediction predictBinaryPrecomputedKernel(ArrayList<ImageFeatures> allImageFeatures, ImageFeatures imageFeatures, Map<Integer, LinkedList<svm_model>> models, Kernel k, SvmScale svmScale, svm_print_interface printInterface) throws IOException, InvalidOperationException {

        if (conceptCombinationsOccurrences == null) {
            conceptCombinationsOccurrences = Concepts.exclusiveConceptCombinationsOccurrencesThreshold(allImageFeatures, exclusiveConceptCombinationsThreshold);
            implicationConceptsCombinationsOccurrences = Concepts.implicationConceptCombinationsOccurrencesThreshold(allImageFeatures, implicationConceptCombinationsRaport);
        }

        SvmPrediction prediction = new SvmPrediction();

        if (svmScale != null) {
            svmScale.imageFeatureScale(imageFeatures);
        }

        svm_problem svmProblem = k.computeGramMatrixTest(allImageFeatures, imageFeatures);

        for (Entry<Integer, LinkedList<svm_model>> entry : models.entrySet()) {

            SvmPrediction svmPrediction = predictBinaryProbability(svmProblem.x[0], entry.getValue(), printInterface);
            //if (svmPrediction.getEstimate(SvmTrain.POSITIVE_CLASS) > 0.5)
            //System.out.println(Concepts.getConcept(entry.getKey()) + ": " + svmPrediction.getEstimate(SvmTrain.POSITIVE_CLASS) + "\n");
            float e = svmPrediction.getEstimate(SvmTrain.POSITIVE_CLASS);

            if (predictType == PredictType.BoostProbability) {
                e = (float) Math.pow((1 - Math.pow((e - 1), 2) * 1), boostThreshold); // from circle's equation  ((1 - (x - 1)^2) * 1)^0.6
            }

            if (imageFeatures.containsAnnotation(entry.getKey())) {
                prediction.setEstimate(entry.getKey(), e, true);
            } else {
                prediction.setEstimate(entry.getKey(), e, false);
            }
        }

        if (PredictType.NoPostPredict != predictType) {
            postSvmPredict(prediction, imageFeatures, predictType);
        }

        return prediction;
    }

    public static SvmPrediction predictBinary(ImageFeatures imageFeatures, Map<Integer, LinkedList<svm_model>> models, SvmScale svmScale, svm_print_interface printInterface) throws IOException, InvalidOperationException {
        SvmPrediction prediction = new SvmPrediction();

        if (svmScale != null) {
            svmScale.imageFeatureScale(imageFeatures);
        }
        svm_node[] x = imageFeatures.getFeatures();

        for (Entry<Integer, LinkedList<svm_model>> entry : models.entrySet()) {
            SvmPrediction svmPrediction = predictBinaryProbability(x, entry.getValue(), printInterface);

            float e = svmPrediction.getEstimate(SvmTrain.POSITIVE_CLASS);
            if (imageFeatures.containsAnnotation(entry.getKey())) {
                prediction.setEstimate(entry.getKey(), e, true);
            } else {
                prediction.setEstimate(entry.getKey(), e, false);
            }

//            labels[entry.getKey()] = entry.getKey();
//            prob_estimates[entry.getKey()] = svmPrediction.getEstimate(SvmTrain.POSITIVE_CLASS);
        }

        return prediction;
    }

    public static SvmPrediction predictProbability(svm_node[] x, svm_model model, svm_print_interface printInterface, int[] label) throws IOException, InvalidOperationException {


        if (svm.svm_check_probability_model(model) == 0) {
            throw new InvalidOperationException("Model does not support probabiliy estimates\n");
        }

        int svm_type = svm.svm_get_svm_type(model);
        int nr_class = svm.svm_get_nr_class(model);

        if (svm_type == svm_parameter.EPSILON_SVR || svm_type == svm_parameter.NU_SVR) {
            printInterface.print("Prob. model for test data: target value = predicted value + z,\nz: Laplace distribution e^(-|z|/sigma)/(2sigma),sigma=" + svm.svm_get_svr_probability(model) + "\n");
        }

        int[] labels = new int[nr_class];
        svm.svm_get_labels(model, labels);

        double[] prob_estimates = new double[nr_class];

        double v;
        if ((svm_type == svm_parameter.C_SVC || svm_type == svm_parameter.NU_SVC)) {
            v = svm.svm_predict_probability(model, x, prob_estimates);
        }

        SvmPrediction prediction = new SvmPrediction();
        for (int i = 0; i < prob_estimates.length; i++) {
            prediction.setEstimate(labels[i], (float) prob_estimates[i]);
        }

        return prediction;
    }

    public static SvmPrediction predictBinaryProbability(svm_node[] x, LinkedList<svm_model> models, svm_print_interface printInterface) throws IOException, InvalidOperationException {

        int svm_type = svm.svm_get_svm_type(models.getFirst());
        int nr_class = svm.svm_get_nr_class(models.getFirst());
        int labels[] = new int[nr_class];
        svm.svm_get_labels(models.getFirst(), labels);

        int positives = 0;
        int negatives = 0;
        double[] meanEstimates = new double[nr_class];
        for (svm_model model : models) {

            double[] prob_estimates = new double[nr_class];

            if ((svm_type == svm_parameter.C_SVC || svm_type == svm_parameter.NU_SVC)) {
                if (SvmTrain.POSITIVE_CLASS == (int) svm.svm_predict_probability(model, x, prob_estimates)) {
                    positives++;
                } else {
                    negatives++;
                }
            }

            for (int i = 0; i < nr_class; i++) {
                meanEstimates[i] += prob_estimates[i];
            }
        }

        SvmPrediction svmPrediction = new SvmPrediction();
        if (positives == models.size() - 1) {
            svmPrediction.setEstimate(SvmTrain.POSITIVE_CLASS, (float) meanEstimates[1] / models.size());
            svmPrediction.setEstimate(SvmTrain.NEGATIVE_CLASS, 1 - (float) meanEstimates[1] / models.size());
        } else {
            svmPrediction.setEstimate(SvmTrain.POSITIVE_CLASS, 1 - (float) meanEstimates[0] / models.size());
            svmPrediction.setEstimate(SvmTrain.NEGATIVE_CLASS, (float) meanEstimates[0] / models.size());
        }
        //for (int i = 0; i < labels.length; i++) {
        //    svmPrediction.setEstimate(labels[i], meanEstimates[i] / models.size());
        //}

        return svmPrediction;
    }

    public static double predictClass(svm_node[] x, svm_model model, svm_print_interface printInterface) throws IOException, InvalidOperationException {

        if (svm.svm_check_probability_model(model) != 0) {
            throw new InvalidOperationException("Model supports probability estimates, but disabled in prediction.\n");
        }

//        int svm_type = svm.svm_get_svm_type(model);
//        int nr_class = svm.svm_get_nr_class(model);

        return svm.svm_predict(model, x);
    }

    public static SvmPrediction robotPredict(ArrayList<ImageFeatures> allImageFeatures, ImageFeatures imageFeatures, Map<Integer, LinkedList<svm_model>> models, Kernel k, SvmScale svmScale, final PredictType predictType, svm_print_interface printInterface) throws IOException, InvalidOperationException {
        SvmPrediction prediction = new SvmPrediction();

        if (svmScale != null) {
            svmScale.imageFeatureScale(imageFeatures);
        }

        svm_problem svmProblem = k.computeGramMatrixTest(allImageFeatures, imageFeatures);

        for (Entry<Integer, LinkedList<svm_model>> entry : models.entrySet()) {

            SvmPrediction svmPrediction = predictBinaryProbability(svmProblem.x[0], entry.getValue(), printInterface);
            //if (svmPrediction.getEstimate(SvmTrain.POSITIVE_CLASS) > 0.5)
            //System.out.println(Concepts.getConcept(entry.getKey()) + ": " + svmPrediction.getEstimate(SvmTrain.POSITIVE_CLASS) + "\n");
            float e = svmPrediction.getEstimate(SvmTrain.POSITIVE_CLASS);

            if (imageFeatures.containsAnnotation(entry.getKey())) {
                prediction.setEstimate(entry.getKey(), e, true);
            } else {
                prediction.setEstimate(entry.getKey(), e, false);
            }
        }

        SvmEstimate[] estimates = prediction.getSortedEstimates();
        if (!estimates[0].getBinaryScore()) {
            estimates[0].setProbability(BinaryThreshold + 0.1f);
        }
        for (int i = 1; i < estimates.length; i++) {
            if (estimates[i].getBinaryScore()) {
                estimates[i].setProbability(BinaryThreshold - 0.1f);
            }
        }

        return prediction;
    }
}
