/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package supportVectorMachines;

import imagefeatures.Concepts;
import imagefeatures.ImageFeatures;
import java.io.IOException;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import libsvm.svm;
import libsvm.svm_model;
import libsvm.svm_node;
import libsvm.svm_parameter;
import libsvm.svm_print_interface;
import libsvm.svm_problem;
import supportVectorMachines.Kernels.*;
import utils.Serialization;

/**
 *
 * @author Mihai Pîțu
 */
public class SvmTrain {

    private static svm_print_interface printInterface;
    public static final int NEGATIVE_CLASS = 0;
    public static final int POSITIVE_CLASS = 1;
    //Integer maxIndex;
    private svm_problem svmGramProblem;
    //large C means lower bias, higher variance
    private double C = 20;
    private int[] weightLabels = new int[2];
    private int maxBinFold = 1;
    private int binFold = 1;

    public SvmTrain(svm_print_interface printInterface) {
        SvmTrain.printInterface = printInterface;
        svm.svm_set_print_string_function(printInterface);

        weightLabels[0] = NEGATIVE_CLASS;
        weightLabels[1] = POSITIVE_CLASS;
    }

    public void setBinFold(int fold, int maxFold) throws InvalidParameterException {
        if (fold < 1) {
            throw new InvalidParameterException("Bin fold must be greater than 1.");
        }
        if (maxFold < 1) {
            throw new InvalidParameterException("Max bin fold must be greater than 1.");
        }
        if (fold > maxFold) {
            throw new InvalidParameterException("Max bin fold must be greater than bin fold.");
        }
        this.binFold = fold;
        this.maxBinFold = maxFold;
    }

    public void setGramMatrix(svm_problem svmGramProblem) {
        this.svmGramProblem = svmGramProblem;
    }

    public void setC(double C) {
        this.C = C;
    }

    ///null kernel means using libsvm RBF
    public Map<Integer, LinkedList<svm_model>> trainBinaryGroupedKernels(ArrayList<ImageFeatures> allImagesFeatures, SvmScale svmScale, int nrWorkingThreads) throws InvalidParameterException, Exception {
        final Map<Integer, LinkedList<svm_model>> result = new ConcurrentHashMap<>();
        HashMap<CombinedKernel, HashSet<Integer>> groupedConcepts = Concepts.getKernelGroupedConcepts();

//        //sampling
//        for (Entry<CombinedKernel, HashSet<Integer>> entry : groupedConcepts.entrySet()) {
//
//            HashSet<ImageFeatures> imfSet = new HashSet<>();
//            for (Integer concept : entry.getValue()) {
//                for (ImageFeatures imf : allImagesFeatures) {
//                    if (imf.containsAnnotation(concept)) {
//                        imfSet.add(imf);
//                    }
//                }
//            }
//            ArrayList imfList = new ArrayList<>(imfSet.size());
//            for (ImageFeatures imf : imfSet) {
//                imfList.add(imf);
//            }
//            groupedTrainImages.put(entry.getKey(), imfList);
//        }

        UserTagKernel usk = new UserTagKernel(3, allImagesFeatures);


        for (Entry<CombinedKernel, HashSet<Integer>> entry : groupedConcepts.entrySet()) {
            BlockingQueue<Runnable> bufferTasks = new LinkedBlockingQueue<>();
            ThreadPoolExecutor threadPool = new ThreadPoolExecutor(nrWorkingThreads, nrWorkingThreads, 10, TimeUnit.SECONDS, bufferTasks);

            CombinedKernel k = entry.getKey();

            k.setSurfKernel(new TopSurfAbsoluteKernel());
            k.setRbfKernel(new RbfKernel(1.0f / allImagesFeatures.get(0).getFeatures().length));
            k.setUserTagKernel(usk);
            k.setColorMomentsKernel(new ColorMomentsKernel());

            printInterface.print("Training for " + allImagesFeatures.size() + " images, " + entry.getValue().size() + "concepts.\n" + k.toString());

            HashMap<Integer, String> concepts = new HashMap<>();
            for (Integer concept : groupedConcepts.get(k)) {
                concepts.put(concept, Concepts.getConcept(concept));
            }

            BlockingQueue<TrainTask> tasks = trainBinaryTasks(allImagesFeatures, k, svmScale, concepts, result);

            Collection<Future<?>> futures = new LinkedList<>();

            while (!tasks.isEmpty()) {
                futures.add(threadPool.submit(tasks.poll()));
            }

            for (Future<?> future : futures) {
                future.get();
            }

            threadPool.purge();
            threadPool.shutdown();
        }

        return result;
    }

    ///null kernel means using libsvm RBF
    public Map<Integer, LinkedList<svm_model>> trainBinary(ArrayList<ImageFeatures> allImagesFeatures, Kernel k, SvmScale svmScale, int nrWorkingThreads) throws InvalidParameterException, Exception {
        final Map<Integer, LinkedList<svm_model>> result = new ConcurrentHashMap<>();
        BlockingQueue<TrainTask> tasks = trainBinaryTasks(allImagesFeatures, k, svmScale, Concepts.getIntLabels(), result);

        BlockingQueue<Runnable> bufferTasks = new LinkedBlockingQueue<>();
        ThreadPoolExecutor threadPool = new ThreadPoolExecutor(nrWorkingThreads, nrWorkingThreads, 10, TimeUnit.SECONDS, bufferTasks);
        Collection<Future<?>> futures = new LinkedList<>();

        while (!tasks.isEmpty()) {
            futures.add(threadPool.submit(tasks.poll()));
        }

        for (Future<?> future : futures) {
            future.get();
        }

        return result;
    }

    ///null kernel means using libsvm RBF
    public Map<Integer, LinkedList<svm_model>> trainBinary(ArrayList<ImageFeatures> allImagesFeatures, Kernel k, SvmScale svmScale) throws InvalidParameterException, Exception {
        final Map<Integer, LinkedList<svm_model>> result = new HashMap<>();
        BlockingQueue<TrainTask> tasks = trainBinaryTasks(allImagesFeatures, k, svmScale, Concepts.getIntLabels(), result);

        while (!tasks.isEmpty()) {
            tasks.poll().run();
        }

        return result;
    }

    private BlockingQueue<TrainTask> trainBinaryTasks(ArrayList<ImageFeatures> allImagesFeatures, final Kernel k, SvmScale svmScale, final HashMap<Integer, String> concepts, final Map<Integer, LinkedList<svm_model>> result) throws InvalidParameterException, Exception {

        BlockingQueue<TrainTask> tasks = new LinkedBlockingQueue<>();

        if (svmScale != null) {
            printInterface.print("Scaling problem...\n");
            svmScale.allImagesFeaturesScale(allImagesFeatures);
        }

        printInterface.print("Build binary problem...\n");

        if (Concepts.getStringLabels().isEmpty()) {
            throw new Exception("No concepts were loaded");
        }

        svm_problem svmGProblem = null;
        if (k != null) {

            if (this.svmGramProblem == null) {
                printInterface.print("Computing Gram Matrix\n");
                svmGProblem = k.computeGramMatrixTrain(allImagesFeatures);
            } else {
                svmGProblem = this.svmGramProblem;
            }
        }

        for (final Entry<Integer, String> concept : concepts.entrySet()) {

            LinkedList<Integer> positivesInd = new LinkedList<>();
            LinkedList<Integer> negativesInd = new LinkedList<>();

            for (int i = 0; i < allImagesFeatures.size(); i++) {
                ImageFeatures features = allImagesFeatures.get(i);

                if (features.containsAnnotation(concept.getKey())) {
                    positivesInd.add(i);
                } else {
                    negativesInd.add(i);
                }
            }

            int positives = positivesInd.size();
            int negatives = negativesInd.size();
            if (positives < negatives) {
                this.sampleBinary(allImagesFeatures, svmGProblem, k, concept, positivesInd, negativesInd, this.weightType, POSITIVE_CLASS, NEGATIVE_CLASS, tasks, result, false);
            } else {
                this.sampleBinary(allImagesFeatures, svmGProblem, k, concept, negativesInd, positivesInd, this.weightType, NEGATIVE_CLASS, POSITIVE_CLASS, tasks, result, true);
            }
        }

        return tasks;
    }

    private void sampleMultiClass(final ArrayList<ImageFeatures> allImagesFeatures,
            final svm_problem svmGProblem,
            final Kernel k,
            final Map.Entry<Integer, String> concept,
            final LinkedList<Integer> minInd,
            final LinkedList<Integer> maxInd,
            final WeightType weightsType,
            final int MINCLASS,
            final int MAXCLASS,
            final BlockingQueue<TrainTask> tasks,
            final Map<Integer, LinkedList<svm_model>> result,
            boolean flip) {

        int mins = minInd.size();
        int maxs = maxInd.size();
        //double real = (double) positives / (positives + negatives);
        //double n = ((double) (1.0 - maxBinRatio) * positives) / maxBinRatio;
        int n = maxs / (mins * binFold);
        if (n > maxBinFold) {
            n = maxBinFold;
        }
        if (n == 0) {
            n = 1;
        }

        int r = (maxs / n);
        int i = 0;
        for (int j = 0; j < n - 1; j++) {
            i = sampleOnceBinary(allImagesFeatures, svmGProblem, k, concept, minInd, maxInd, weightsType, MINCLASS, MAXCLASS, tasks, result, r, i, flip);
        }

        r = maxs - i;
        sampleOnceBinary(allImagesFeatures, svmGProblem, k, concept, minInd, maxInd, weightsType, MINCLASS, MAXCLASS, tasks, result, r, i, flip);
    }

    private void sampleBinary(final ArrayList<ImageFeatures> allImagesFeatures,
            final svm_problem svmGProblem,
            final Kernel k,
            final Map.Entry<Integer, String> concept,
            final LinkedList<Integer> minInd,
            final LinkedList<Integer> maxInd,
            final WeightType weightsType,
            final int MINCLASS,
            final int MAXCLASS,
            final BlockingQueue<TrainTask> tasks,
            final Map<Integer, LinkedList<svm_model>> result,
            boolean flip) {

        int mins = minInd.size();
        int maxs = maxInd.size();
        //double real = (double) positives / (positives + negatives);
        //double n = ((double) (1.0 - maxBinRatio) * positives) / maxBinRatio;
        int n = maxs / (mins * binFold);
        if (n > maxBinFold) {
            n = maxBinFold;
        }

        if (n == 0) {
            n = 1;
        }

        int r = (maxs / n);
        int i = 0;
        for (int j = 0; j < n - 1; j++) {
            i = sampleOnceBinary(allImagesFeatures, svmGProblem, k, concept, minInd, maxInd, weightsType, MINCLASS, MAXCLASS, tasks, result, r, i, flip);
        }

        r = maxs - i;
        sampleOnceBinary(allImagesFeatures, svmGProblem, k, concept, minInd, maxInd, weightsType, MINCLASS, MAXCLASS, tasks, result, r, i, flip);
    }

    private int sampleOnceBinary(final ArrayList<ImageFeatures> allImagesFeatures,
            final svm_problem svmGProblem,
            final Kernel k,
            final Map.Entry<Integer, String> concept,
            final LinkedList<Integer> minInd,
            final LinkedList<Integer> maxInd,
            final WeightType weightsType,
            final int MINCLASS,
            final int MAXCLASS,
            final BlockingQueue<TrainTask> tasks,
            final Map<Integer, LinkedList<svm_model>> result,
            final int r,
            int i,
            boolean flip) {

        final int[] minsAnn = new int[]{MINCLASS};
        final int[] maxsAnn = new int[]{MAXCLASS};

        int mins = minInd.size();
        int maxs = maxInd.size();

        if (svmGProblem != null) {
            svm_problem svmProblem = new svm_problem();
            svmProblem.x = new svm_node[r + mins][];
            svmProblem.l = r + mins;
            svmProblem.y = new double[svmProblem.l];

            for (int j = 0; j < r; j++, i++) {
                int ind = maxInd.get(i);
                svmProblem.x[j] = svmGProblem.x[ind];
                svmProblem.y[j] = MAXCLASS;
            }

            for (int j = r; j < r + mins; j++) {
                int ind = minInd.get(j - r);
                svmProblem.x[j] = svmGProblem.x[ind];
                svmProblem.y[j] = MINCLASS;
            }

            if (flip) {
                tasks.add(new TrainTask(svmProblem, k, concept, this.weightLabels, weightsType, new int[]{r, mins}, result));
            } else {
                tasks.add(new TrainTask(svmProblem, k, concept, this.weightLabels, weightsType, new int[]{mins, r}, result));
            }

        } else {

            ArrayList<ImageFeatures> binaryFeatures = new ArrayList<>(r + mins);

            for (int j = 0; j < r; j++, i++) {
                int ind = maxInd.get(i);
                binaryFeatures.add(allImagesFeatures.get(ind).partialClone(maxsAnn));
            }

            for (int j = r; j < r + mins; j++) {
                int ind = minInd.get(i);
                binaryFeatures.add(allImagesFeatures.get(ind).partialClone(minsAnn));
            }

            if (flip) {
                tasks.add(new TrainTask(binaryFeatures, concept, this.weightLabels, weightsType, new int[]{r, mins}, result));
            } else {
                tasks.add(new TrainTask(binaryFeatures, concept, this.weightLabels, weightsType, new int[]{mins, r}, result));
            }
        }

        return i;
    }

    public static enum WeightType {

        BoostUnballanced1, BoostUnballanced2, NoWeights
    };
    public WeightType weightType = WeightType.BoostUnballanced2;

    private double[] setBinaryWeights(WeightType weightsType, final int finalPoz, final int finalNeg) {

        double[] weights = new double[2];
        switch (weightsType) {
            case BoostUnballanced1:
                if (finalPoz < finalNeg) {
                    weights[1] = 1;
                    weights[0] = Math.pow((double) finalNeg / finalPoz, 2);
                } else {
                    weights[0] = 1;
                    weights[1] = Math.pow((double) finalPoz / finalNeg, 2);
                }
                break;
            case BoostUnballanced2:
                if (finalPoz < finalNeg) {
                    weights[1] = (double) finalPoz / (finalPoz + finalNeg);
                    weights[0] = 1 - weights[1];
                } else {
                    weights[0] = (double) finalNeg / (finalPoz + finalNeg);
                    weights[1] = 1 - weights[0];
                }
                break;
            case NoWeights:
                weights[1] = 1;
                weights[0] = 1;
                break;
        }

        return weights;
    }

    public svm_model train(Entry<Integer, String> concept, ArrayList<ImageFeatures> allImagesFeatures, int[] weightLabels, WeightType weightType, int[] nrSamples) throws InvalidParameterException {

        svm_problem svmProblem = new svm_problem();
        svm_parameter svmParameter = setParameters(svmProblem, allImagesFeatures, null, this.C, weightLabels, setBinaryWeights(weightType, nrSamples[0], nrSamples[1]));

        return svm.svm_train(svmProblem, svmParameter);
    }
    private boolean doGridSearch = false;

    public void setDoGridSearch(boolean doGridSearch) {
        this.doGridSearch = doGridSearch;
    }

    public svm_model train(Entry<Integer, String> concept, svm_problem svmProblem, Kernel k, int[] weightLabels, WeightType weightType, int[] nrSamples) throws InvalidParameterException {

        printInterface.print("Setting parameters...\n");

        svm_parameter bestParameter = null;

        if (doGridSearch) {
            printInterface.print("Grid search for single model...\n");
            double maxF = -1;
            WeightType bestWeightType = weightType;
            //new double[]{0.5, 1, 10, 20, 30, 45, 70, 100, 150, 300
            for (WeightType searchWeightType : new WeightType[]{WeightType.BoostUnballanced1}) {
                for (double c : new double[]{20, 1, 0.2, 0.5, 0.8, 10, 40, 55, 70, 100, 250, 500, 1500}) {
                    svm_parameter svmParameter = setParameters(svmProblem, null, k, c, weightLabels, setBinaryWeights(searchWeightType, nrSamples[0], nrSamples[1]));
                    double f = SvmGridSearch.doCrossValidation(svmProblem, printInterface, svmParameter, 6);
                    printInterface.print("Done cross validation for C: " + c + ", weight type: " + searchWeightType + " (fmeasure:" + f + ")...\n");
                    if (f > maxF) {
                        bestParameter = svmParameter;
                        maxF = f;
                        bestWeightType = searchWeightType;
                    }
                }
            }
            printInterface.print("Done grid search...\n Best C:" + bestParameter.C + ", weight type:" + bestWeightType + "(f:" + maxF + ") for concept:" + concept.getValue() + "\n");
        } else {
            bestParameter = setParameters(svmProblem, null, k, this.C, weightLabels, setBinaryWeights(weightType, nrSamples[0], nrSamples[1]));
        }

        return svm.svm_train(svmProblem, bestParameter);
    }

    private svm_parameter setParameters(svm_problem svmProblem, ArrayList<ImageFeatures> allImagesFeatures, Kernel k, double c, int[] weightLabels, double[] weights) throws InvalidParameterException {
        svm_parameter svmParameter = new svm_parameter();
        svm.svm_set_print_string_function(printInterface);

        int maxIndex = 0;

        // default values
        svmParameter.svm_type = svm_parameter.C_SVC;
        svmParameter.degree = 3;
        svmParameter.coef0 = 0;
        svmParameter.nu = 0.5; //0.5 was default
        svmParameter.cache_size = 1000;
        svmParameter.C = c;
        svmParameter.eps = 1e-3;
        svmParameter.p = 0.1;
        svmParameter.shrinking = 0;
        svmParameter.probability = 1;
        svmParameter.nr_weight = weightLabels.length;
        svmParameter.weight_label = weightLabels;
        svmParameter.weight = weights;

        if (svmParameter.gamma == 0 && maxIndex > 0) {
            svmParameter.gamma = 1.0 / maxIndex;
        }

        if (k != null) {
            svmParameter.kernel_type = svm_parameter.PRECOMPUTED;
        } else {
            svmParameter.kernel_type = svm_parameter.RBF;

            ArrayList<svm_node[]> xs = new ArrayList<>();
            ArrayList<Integer> ys = new ArrayList<>();

            for (ImageFeatures imageFeatures : allImagesFeatures) {

                int[] annotations = imageFeatures.getAnnotations();

                for (int j = 0; j < annotations.length; j++) {

                    svm_node[] x = imageFeatures.getFeatures();

                    maxIndex = Math.max(maxIndex, x[x.length - 1].index);

                    xs.add(x);
                    ys.add(annotations[j]);
                }
            }

            svmProblem.l = allImagesFeatures.size();
            svmProblem.x = new svm_node[svmProblem.l][];
            svmProblem.y = new double[svmProblem.l];

            for (int i = 0; i < svmProblem.l; i++) {
                svmProblem.x[i] = xs.get(i);
                svmProblem.y[i] = ys.get(i);
            }

            //printInterface.print("Grid search...\n");
            //SvmGridSearch grid = new SvmGridSearch(svmProblem, svmParameter, 1, 200, 10, 0.01, 1, 0.1);
            //grid.search(printInterface);
        }

        String errorMsg = svm.svm_check_parameter(svmProblem, svmParameter);

        if (errorMsg != null) {
            throw new InvalidParameterException(errorMsg);
        }

        return svmParameter;
    }

    public static void saveSvmModel(svm_model svmModel, String modelFilename) throws IOException {
        svm.svm_save_model(modelFilename, svmModel);
    }

    public static svm_model loadSvmModel(String modelFilename) throws IOException {
        return svm.svm_load_model(modelFilename);
    }

    class TrainTask implements Runnable {

        private Entry<Integer, String> concept;
        private final WeightType weightType;
        private final int[] nrSamples;
        private final int[] weightLabels;
        private Kernel k;
        private final Map<Integer, LinkedList<svm_model>> result;
        private final svm_problem svmProblem;
        private final ArrayList<ImageFeatures> binaryFeatures;

        public TrainTask(svm_problem svmProblem,
                Kernel k,
                Map.Entry<Integer, String> concept,
                int[] weightLabels,
                WeightType weightType,
                int[] nrSamples,
                Map<Integer, LinkedList<svm_model>> result) {

            this.svmProblem = svmProblem;
            this.concept = concept;
            this.weightLabels = weightLabels;
            this.k = k;
            this.result = result;
            this.weightType = weightType;
            this.nrSamples = nrSamples;
            this.binaryFeatures = null;
        }

        public TrainTask(ArrayList<ImageFeatures> binaryFeatures,
                Map.Entry<Integer, String> concept,
                int[] weightLabels,
                WeightType weightType,
                int[] nrSamples,
                Map<Integer, LinkedList<svm_model>> result) {

            this.svmProblem = null;
            this.k = null;
            this.binaryFeatures = binaryFeatures;
            this.concept = concept;
            this.weightLabels = weightLabels;
            this.weightType = weightType;
            this.nrSamples = nrSamples;
            this.result = result;
        }

        @Override
        public void run() {
            try {

                if (!result.containsKey(concept.getKey())) {
                    result.put(concept.getKey(), new LinkedList<svm_model>());
                }
                LinkedList<svm_model> modelParts = result.get(concept.getKey());
                if (printInterface != null) {
                    printInterface.print("\n" + "Training svm for label " + concept.getKey() + " = " + concept.getValue() + " (Part: " + modelParts.size() + ")...\n");
                    for (int i = 0; i < weightLabels.length; i++) {
                        printInterface.print("\n" + "Class " + weightLabels[i] + ", number of samples: " + nrSamples[i] + "\n");
                    }
                }

                if (k != null) {
                    modelParts.add(SvmTrain.this.train(concept, svmProblem, k, this.weightLabels, this.weightType, this.nrSamples));
                    //result.put(concept.getKey(), SvmTrain.this.train(svmProblem, k, weightLabels, weights));
                } else {
                    modelParts.add(SvmTrain.this.train(concept, binaryFeatures, this.weightLabels, this.weightType, this.nrSamples));
                    //result.put(concept.getKey(), SvmTrain.this.train(binaryFeatures, weightLabels, weights));
                }

            } catch (InvalidParameterException ex) {
                Logger.getLogger(SvmTrain.class.getName()).log(Level.SEVERE, null, ex);
                System.exit(1);
            }
        }
    }
}
