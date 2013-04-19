/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package supportVectorMachines;

import evaluation.Evaluation;
import imagefeatures.Concepts;
import imagefeatures.ImageFeatures;
import java.awt.image.SampleModel;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.media.jai.JAI;
import javax.media.jai.PlanarImage;
import libsvm.*;
import profileEntropyFeatures.PEF;
import supportVectorMachines.Kernels.Kernel;
import supportVectorMachines.Kernels.*;
import utils.JOpenCV;

/**
 *
 * @author Mihai Pîțu
 */
public class SvmGridSearch {

    private svm_parameter bestParameter;
    HashSet<Double> CSet;
    HashSet<Double> gammaSet;
    private svm_problem svmProblem;

    public SvmGridSearch(svm_problem svmProblem, svm_parameter oldParameter, HashSet<Double> CSet) {
        this.CSet = CSet;
        this.bestParameter = oldParameter;
        this.svmProblem = svmProblem;

    }

    public SvmGridSearch(svm_problem svmProblem, svm_parameter oldParameter, HashSet<Double> CSet, HashSet<Double> gammaSet) {
        this.CSet = CSet;
        this.gammaSet = gammaSet;
        this.bestParameter = oldParameter;
        this.svmProblem = svmProblem;
    }

    public void search(svm_print_interface printInterface) {

        if (bestParameter.kernel_type == svm_parameter.RBF) {
            //search for C and gamma
            double max = 0;
            double bestC = this.CSet.iterator().next();
            double bestGamma = this.gammaSet.iterator().next();
            for (double C : this.CSet) {
                for (double gamma : this.gammaSet) {
                    bestParameter.C = C;
                    bestParameter.gamma = gamma;
                    printInterface.print("Cross validation for C:" + C + "\nGamma:" + gamma + "\n");
                    double fmeasure = doCrossValidation(svmProblem, printInterface, bestParameter, 3);
                    printInterface.print("Done cross validation - fmeasure: " + fmeasure + " \n");
                    if (fmeasure > max) {
                        max = fmeasure;
                        bestC = C;
                        bestGamma = gamma;
                    }
                }
            }
            bestParameter.C = bestC;
            bestParameter.gamma = bestGamma;
            printInterface.print("Best C:" + bestParameter.C + "\nBest gamma:" + bestParameter.gamma + " (Max Fmeasure: " + max + ")\n");
            return;
        }
        //precomputedKernel

        //search for C
        double max = 0;
        double bestC = this.CSet.iterator().next();
        for (double C : this.CSet) {
            bestParameter.C = C;
            printInterface.print("Cross validation for C:" + C + "\n");
            double fmeasure = doCrossValidation(svmProblem, printInterface, bestParameter, 3);
            printInterface.print("Done cross validation - fmeasure: " + fmeasure + " \n");
            if (fmeasure > max) {
                max = fmeasure;
                bestC = C;
            }
        }
        bestParameter.C = bestC;
        printInterface.print("Best C:" + bestParameter.C + " (Max Fmeasure: " + max + ")\n");
    }

    public static double isGrayscaleLikelihood(PlanarImage pi, double p) {
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

        double r = Math.exp(-0.1 * (double) delta / ((double) Math.pow(width * height, 0.35)));
//        double r = (double) delta / (width * height);
//        r -= 5;
//        r =  (r / Math.sqrt(1 + r * r)) / 2 + 0.5;
        return r;
    }

    public static double doGridSearchIsGrayScale(ArrayList<ImageFeatures> features, svm_print_interface print) {
        double[] ps = new double[]{0.35};

        double maxfmeasure = 0, maxp = 0;
        for (double p : ps) {
            int tp = 0;
            int tn = 0;
            int fp = 0;
            int fn = 0;
            for (int i = 0; i < features.size(); i++) {
                if (i % 500 == 0) {
                    print.print(i + " images processed\n");
                }
                PlanarImage pi = JAI.create("fileload", "d:/faculta/licenta/Trainingset2012/images/" + features.get(i).getImageFilename());
                double probab = isGrayscaleLikelihood(pi, p);
                boolean predicted = probab > 0.5 ? true : false;
                boolean actual = features.get(i).containsAnnotation(SvmPredict.style_graycolor);

                if (actual && predicted) {
                    tp++;
                } else if (!actual && !predicted) {
                    tn++;
                } else if (predicted) {
                    fp++;
                } else {
                    fn++;
                }
            }

            Evaluation e = new Evaluation(tp, tn, fp, fn, 0, 0);
            double f = e.getFmeasureMicro();
            if (f > maxfmeasure) {
                maxfmeasure = f;
                maxp = p;
            }

            System.out.println(e.toString());
            System.out.println("Graycolor fmeasure: f:" + f + "\np:" + p + "\n\n");
        }

        System.out.println("Graycolor fmeasure: f:" + maxfmeasure + "\np:" + maxp + "\n\n");

        return maxfmeasure;
    }

    public static double doGridSearchFaceDetection(ArrayList<ImageFeatures> features, svm_print_interface print) {
        double[] ds = new double[]{1.15, 1.2, 1.3};
        int[] is = new int[]{10, 3, 2};
        int[] i0s = new int[]{1};
        double bd = 0;
        int bi = 0, bi0 = 0;

        double maxAccuracy = 0;
        for (double d : ds) {
            JOpenCV.setScaleFactor(d);
            for (int i : is) {
                JOpenCV.setMinNeighbors(i);
                for (int i0 : i0s) {
                    JOpenCV.setFlags(i0);

                    int tp = 0;
                    int tn = 0;
                    int fp = 0;
                    int fn = 0;

                    int correct = 0;
                    int total = 0;
                    int totalPortrait = 0;
                    int correctPortrait = 0;
                    for (int j = 0; j < features.size(); j++) {
                        ImageFeatures imf = features.get(j);
                        int totalFaces = JOpenCV.nrFaceDetection("d:/faculta/licenta/Trainingset2012/images/" + imf.getImageFilename());

                        if (j % 500 == 0) {
                            print.print(j + " images processed\n");
                        }

                        boolean predicted = totalFaces == 1 ? true : false;
                        boolean actual = imf.containsAnnotation(SvmPredict.viewportrait);

                        if (predicted && !actual) {
                            System.out.println(predicted + ": " + imf.getImageFilename());
                        }

                        if (actual && predicted) {
                            tp++;
                        } else if (!actual && !predicted) {
                            tn++;
                        } else if (predicted) {
                            fp++;
                        } else {
                            fn++;
                        }

                        if (imf.containsAnnotation(SvmPredict.noPersons)) {
                            total++;
                            if (totalFaces == 0) {
                                correct++;
                            }
                        } else if (imf.containsAnnotation(SvmPredict.onePerson)) {
                            total++;
                            if (totalFaces == 1) {
                                correct++;
                            }
                        } else if (imf.containsAnnotation(SvmPredict.twoPersons)) {
                            total++;
                            if (totalFaces == 2) {
                                correct++;
                            }
                        } else if (imf.containsAnnotation(SvmPredict.threePersons)) {
                            total++;
                            if (totalFaces == 3) {
                                correct++;
                            }
                        } else if (imf.containsAnnotation(SvmPredict.smallgroup)) {
                            total++;
                            if (4 <= totalFaces && totalFaces <= 9) {
                                correct++;
                            }
                        } else if (imf.containsAnnotation(SvmPredict.biggroup)) {
                            total++;
                            if (totalFaces >= 10) {
                                correct++;
                            }
                        }
                    }

                    double accuracy = ((double) correct / total);

                    print.print("d:" + d + "\ni:" + i + "\ni0:" + i0 + "\nAccuracy:" + accuracy + "(Total:" + total + ")\n");

                    System.out.println("Portrait:");
                    System.out.println(new Evaluation(tp, tn, fp, fn, 0, 0).toString());

                    if (maxAccuracy < accuracy) {
                        maxAccuracy = accuracy;
                        bd = d;
                        bi = i;
                        bi0 = i0;
                    }
                }
            }
        }

        print.print("Done! \nd:" + bd + "\ni:" + bi + "\ni0:" + bi0 + "\nAccuracy:" + maxAccuracy + "\n\n");

        return maxAccuracy;
    }
    public static Map<Integer, LinkedList<svm_model>> models;

    public static double doGridSearch(ArrayList<ImageFeatures> features, SvmScale svmScale, svm_print_interface print) throws Exception {
        if (svmScale == null) {
            svmScale = new SvmScale();
            svmScale.allImagesFeaturesScale(features);
        }

        UserTagKernel tagK = new UserTagKernel(16, features);

        double[] Cs = new double[]{20};
        double[] gammas = new double[]{0.0166};
        double[][] contributions = new double[][]{
            //            {0.2, 0.7, 1.0},
            //            {1.0, 0.2, 0.0},
            {1.0, 0.2, 0.9, 0.05}
//            {1.0, 0.3, 0.5, 0.2}
        };

        SvmPredict.PredictType[] predictTypes = new SvmPredict.PredictType[]{SvmPredict.PredictType.BoostProbability};
        SvmTrain.WeightType[] weightTypes = new SvmTrain.WeightType[]{SvmTrain.WeightType.BoostUnballanced1, SvmTrain.WeightType.BoostUnballanced2, SvmTrain.WeightType.NoWeights};


        for (ImageFeatures imFeat : features) {
            imFeat.setImageFilename("d:/faculta/licenta/Trainingset2012/images/" + imFeat.getImageFilename());
        }

        double maxFmeasure = 0;
        double maxC = 0, maxGamma = 0, maxTsC = 0, maxPefC = 0, maxTagC = 0;
        SvmPredict.PredictType maxPredictType = null;
        SvmTrain.WeightType maxWeightType = null;

        for (int t = 0; t < gammas.length; t++) {
            double gamma = gammas[t];
            for (int co = 0; co < contributions.length; co++) {
                double topsurfContr = contributions[co][0];
                double pefContr = contributions[co][1];
                double tagContr = contributions[co][2];
                double cmContr = contributions[co][3];

                CombinedKernel k = new CombinedKernel(new TopSurfAbsoluteKernel(), new RbfKernel(gamma), tagK, new ColorMomentsKernel());
                k.setContributions((float) topsurfContr, (float) pefContr, (float) tagContr, (float) cmContr);

                for (int c = 0; c < Cs.length; c++) {
                    double C = Cs[c];

                    print.print("Grid search for:\n" + k.toString() + "C:" + C);

                    int n = features.size();

                    HashMap<SvmPredict.PredictType, Evaluation> predictions = new HashMap<>();

                    final int nrFoldCrossValidation = 10;

                    for (int i = 0; i < nrFoldCrossValidation; i++) {
                        ArrayList<ImageFeatures> trainFeatures = new ArrayList<>(n - n / nrFoldCrossValidation);
                        ArrayList<ImageFeatures> testFeatures = new ArrayList<>(n / nrFoldCrossValidation);
                        int startTestIndex = i * (n / nrFoldCrossValidation);
                        int stopTestIndex = (i + 1) * (n / nrFoldCrossValidation);

                        for (int j = 0; j < startTestIndex; j++) {
                            trainFeatures.add(features.get(j));
                        }
                        for (int j = startTestIndex; j < stopTestIndex; j++) {
                            testFeatures.add(features.get(j));
                        }
                        for (int j = stopTestIndex; j < n; j++) {
                            trainFeatures.add(features.get(j));
                        }

                        SvmTrain svmTrain = new SvmTrain(print);
                        svmTrain.setC(C);

                        models = null;
                        svmTrain.setDoGridSearch(true);
                        models = svmTrain.trainBinary(trainFeatures, k, null, 2);
                        //models = svmTrain.trainBinaryGroupedKernels(trainFeatures, null, 2);

                        for (SvmPredict.PredictType predictType : predictTypes) {
                            print.print("Predicting type: " + predictType + "\n");
                            SvmPredict.predictType = predictType;
                            for (float boost : new float[]{0.45f, 0.55f, 0.58f, 0.62f, 0.65f, 0.75f, 0.95f, 1.2f, 1.6f}) {
                                SvmPredict.boostThreshold = boost;
                                System.out.println("boost:"+ boost);
                                
                                int tp = 0, tn = 0, fp = 0, fn = 0;
                                for (int j = 0; j < testFeatures.size(); j++) {
                                    if (j % 500 == 0) {
                                        print.print(j + " images predicted. Relative f-measure: " + Evaluation.computeFmeasure(tp, fp, fn) + "\n");
                                    }
                                    ImageFeatures imFeat = testFeatures.get(j);

                                    //SvmPrediction p = SvmPredict.predictBinaryGroupedKernels(trainFeatures, imFeat, models, null, print);
                                    SvmPrediction p = SvmPredict.predictBinaryPrecomputedKernel(trainFeatures, imFeat, models, k, null, print);
                                    //SvmPrediction p = SvmPredict.robotPredict(trainFeatures, imFeat, models, k, null, predictType, print);

                                    Evaluation e = new Evaluation(new SvmPrediction[]{p});
                                    //System.out.println(e.toString());
                                    tp += e.getTruePositivesSum();
                                    tn += e.getTrueNegativesSum();
                                    fn += e.getFalseNegativesSum();
                                    fp += e.getFalsePositivesSum();
                                }

                                Evaluation localEval = new Evaluation(tp, tn, fp, fn, testFeatures.size(), Concepts.getIntLabels().size());
                                if (!predictions.containsKey(predictType)) {
                                    predictions.put(predictType, localEval);
                                }

                                predictions.get(predictType).setFalseNegativesSum(predictions.get(predictType).getFalseNegativesSum() + fn);
                                predictions.get(predictType).setTrueNegativesSum(predictions.get(predictType).getTrueNegativesSum() + tn);
                                predictions.get(predictType).setTruePositivesSum(predictions.get(predictType).getTruePositivesSum() + tp);
                                predictions.get(predictType).setFalsePositivesSum(predictions.get(predictType).getFalsePositivesSum() + fp);

                                print.print("Done " + i + " part fold cross validation for " + predictType + "\n" + localEval.toString());
                            }
                        }
                        break;
                    }

                    double fmeasure = 0;
                    SvmPredict.PredictType bestPredictType = null;

                    for (SvmPredict.PredictType predictType : predictTypes) {
                        print.print("Predicting type: " + predictType + " results:\n");
                        print.print(predictions.get(predictType).toString());
                        if (fmeasure < predictions.get(predictType).getFmeasureMicro()) {
                            fmeasure = predictions.get(predictType).getFmeasureMicro();
                            bestPredictType = predictType;
                        }
                    }

                    print.print("\n\n");

                    print.print("total test: " + n + "\n");
                    //double fmeasure = doCrossValidation(features, false, k, C, 5, 2, SvmPredict.PredictType.PostPredict, print);
                    print.print(k.toString() + "C:" + C + "\nFmeasure: " + fmeasure + "\n\n\n");

                    if (fmeasure > maxFmeasure) {
                        maxC = C;
                        maxGamma = gamma;
                        maxTsC = topsurfContr;
                        maxPefC = pefContr;
                        maxTagC = tagContr;
                        maxPredictType = bestPredictType;
                    }
                }
            }
        }

        print.print("Done grid search. Max Fmeasure:" + maxFmeasure + "\n"
                + " Best parameters found: \n"
                + "C:" + maxC + "\n"
                + "gamma:" + maxGamma + "\n"
                + "TopSurfContr:" + maxTsC + "\n"
                + "PefContr:" + maxPefC + "\n"
                + "UserTagContr:" + maxTagC + "\n"
                + "Max predict type:" + maxPredictType);

        return maxFmeasure;
    }

    public static double doCrossValidation(ArrayList<ImageFeatures> features, boolean scale, Kernel k, double C, int nrFoldCrossValidation, int nrWorkingThreads, SvmPredict.PredictType predictType, svm_print_interface print) {
        try {
            print.print("\nCross validation: " + nrFoldCrossValidation + "(fold)\n");

            if (scale) {
                new SvmScale().allImagesFeaturesScale(features);
            }


            int n = features.size();

            int truePositivesSum = 0;
            int trueNegativesSum = 0;
            int falseNegativesSum = 0;
            int falsePositivesSum = 0;

            for (int i = 0; i < nrFoldCrossValidation; i++) {
                ArrayList<ImageFeatures> trainFeatures = new ArrayList<>(n - n / nrFoldCrossValidation);
                ArrayList<ImageFeatures> testFeatures = new ArrayList<>(n / nrFoldCrossValidation);
                int startTestIndex = i * (n / nrFoldCrossValidation);
                int stopTestIndex = (i + 1) * (n / nrFoldCrossValidation);

                for (int j = 0; j < startTestIndex; j++) {
                    trainFeatures.add(features.get(j));
                }
                for (int j = startTestIndex; j < stopTestIndex; j++) {
                    testFeatures.add(features.get(j));
                }
                for (int j = stopTestIndex; j < n; j++) {
                    trainFeatures.add(features.get(j));
                }

                SvmTrain svmTrain = new SvmTrain(print);
                svmTrain.setC(C);
//                if (gramMatrix != null)
//                    svmTrain.setGramMatrix(gramMatrix);

                models = svmTrain.trainBinary(trainFeatures, k, null, nrWorkingThreads);
                SvmPredict.predictType = predictType;
                int tp = 0, tn = 0, fp = 0, fn = 0;

                for (int j = 0; j < testFeatures.size(); j++) {
                    if (j % 500 == 0) {
                        print.print(j + " images predicted.\n");
                    }
                    ImageFeatures imFeat = testFeatures.get(j);

                    SvmPrediction p = SvmPredict.predictBinaryPrecomputedKernel(trainFeatures, imFeat, models, k, null, print);

                    Evaluation e = new Evaluation(new SvmPrediction[]{p});
                    tp += e.getTruePositivesSum();
                    tn += e.getTrueNegativesSum();
                    fn += e.getFalseNegativesSum();
                    fp += e.getFalsePositivesSum();
                }

                print.print("Done " + i + " part fold cross validation.\n" + new Evaluation(tp, tn, fp, fn, testFeatures.size(), testFeatures.get(0).getFeatures().length).toString());

                truePositivesSum += tp;
                trueNegativesSum += tn;
                falseNegativesSum += fn;
                falsePositivesSum += fp;
                break;
            }

            print.print(
                    "True positives sum: " + truePositivesSum + "\t"
                    + "True negatives sum: " + trueNegativesSum + "\n"
                    + "False positives sum: " + falsePositivesSum + "\t"
                    + "False negatives sum: " + falseNegativesSum + "\n\n");

            print.print("total test: " + n + "\n");
            double f = Evaluation.computeFmeasure(truePositivesSum, falsePositivesSum, falseNegativesSum);
            print.print("Done cross validation. C was:" + C + ", fmeasure = " + f + "\n");

            return f;
        } catch (Exception ex) {
            Logger.getLogger(SvmGridSearch.class.getName()).log(Level.SEVERE, null, ex);
        }

        return 0;
    }

    public static double doCrossValidation(svm_problem svmProblem, svm_print_interface printInterface, svm_parameter svmParameter, int nrFoldCrossValidation) {
        int i;
        int totalCorrect = 0;
        double totalError = 0;
        double sumv = 0, sumy = 0, sumvv = 0, sumyy = 0, sumvy = 0;
        double[] target = new double[svmProblem.l];

        svm.svm_cross_validation(svmProblem, svmParameter, nrFoldCrossValidation, target);

        if (svmParameter.svm_type == svm_parameter.EPSILON_SVR || svmParameter.svm_type == svm_parameter.NU_SVR) {
            for (i = 0; i < svmProblem.l; i++) {
                double y = svmProblem.y[i];
                double v = target[i];
                totalError += (v - y) * (v - y);
                sumv += v;
                sumy += y;
                sumvv += v * v;
                sumyy += y * y;
                sumvy += v * y;
            }

            printInterface.print("Cross Validation Mean squared error = " + totalError / svmProblem.l + "\n");
            printInterface.print("Cross Validation Squared correlation coefficient = "
                    + ((svmProblem.l * sumvy - sumv * sumy) * (svmProblem.l * sumvy - sumv * sumy))
                    / ((svmProblem.l * sumvv - sumv * sumv) * (svmProblem.l * sumyy - sumy * sumy)) + "\n");
        } else {
            int tp = 0;
            int fp = 0;
            int tn = 0;
            int fn = 0;

            for (i = 0; i < svmProblem.l; i++) {
                boolean actual = (svmProblem.y[i] == SvmTrain.POSITIVE_CLASS) ? true : false;
                boolean predicted = (target[i] == SvmTrain.POSITIVE_CLASS) ? true : false;

                if (actual && predicted) {
                    tp++;
                } else if (!actual && !predicted) {
                    tn++;
                } else if (predicted) {
                    fp++;
                } else {
                    fn++;
                }

                if (target[i] == svmProblem.y[i]) {
                    ++totalCorrect;
                }
            }
            double accuracy = (double) (tp + tn) / (svmProblem.l);
            double recallMicro = Evaluation.computeRecall(tp, fn);
            double precisionMicro = Evaluation.computePrecision(tp, fp);
            double fmeasureMicro = Evaluation.computeFmeasure(precisionMicro, recallMicro);

            return fmeasureMicro;
        }

        return totalCorrect;
    }
}
