/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package imagefeatures;

import colorMoments.CMImageFeatures;
import colorMoments.CMImageFeaturesAlgorithm;
import com.memetix.mst.translate.Translate;
import evaluation.Evaluation;
import java.io.*;
import java.nio.channels.FileChannel;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import utils.*;
import javax.swing.UIManager;
import libsvm.svm_model;
import libsvm.svm_print_interface;
import libsvm.svm_problem;
import profileEntropyFeatures.PEFAlgorithm;
import profileEntropyFeatures.PEFImageFeatures;
import speededUpRobustFeatures.TopSurfAlgorithm;
import speededUpRobustFeatures.TopSurfDescriptor;
import speededUpRobustFeatures.TopSurfImageFeatures;
import speededUpRobustFeatures.TopSurfVisualword;
import supportVectorMachines.*;
import supportVectorMachines.Kernels.*;

/**
 *
 * @author Mihai Pîțu
 */
public class ImageFeaturesTest {

    private int imageCount = 0;
    private boolean alive = true;
    public static ExceptionHandler exceptionHandler = new ExceptionHandler() {

        @Override
        public void handle(Exception ex) {
            ex.printStackTrace();
        }
    };

    public ArrayList<ImageFeatures> robot(ImageFeaturesAlgorithm alg, String imagesFolder, String locationsFolder) // boolean turnGreyscale)
    {
        ArrayList<ImageFeatures> imageFeatures = new ArrayList<>();

        DataInputStream in = null;
        try {
            FileInputStream fstream = new FileInputStream(locationsFolder);
            in = new DataInputStream(fstream);

            final BufferedReader br = new BufferedReader(new InputStreamReader(in));

            String strLine;
            while ((strLine = br.readLine()) != null) {

                String concept = strLine.substring(strLine.indexOf(':') + 2);
                String filename = strLine.subSequence(0, strLine.indexOf(':')).toString();

                long start = System.currentTimeMillis();
                imageFeatures.add(alg.getImageFeatures(imagesFolder + filename, new int[]{Concepts.getConcept(concept)}));
                print.print((imageCount++) + ". Computed features for " + filename + " in " + (System.currentTimeMillis() - start) + " millisec\n");

                System.out.println("Processed image " + filename);
            }
        } catch (IOException ex) {
            ex.printStackTrace();
            System.out.println(ex.toString());
        } catch (Exception ex) {
            ex.printStackTrace();
            System.out.println(ex.toString());
        } finally {
            try {

                in.close();
            } catch (IOException ex) {
                ex.printStackTrace();
                System.out.println(ex.toString());
            }
        }

        return imageFeatures;
    }

    public ArrayList<ImageFeatures> getAllImagesFeatures(ImageFeaturesAlgorithm alg, final String annotationsDir, final String imagesDir, int nrWorkingThreads) throws Exception {

        //ThreadPool threadPool = new ThreadPool(nrWorkingThreads, exceptionHandler);

        final ArrayList<ImageFeatures> allfeatures = new ArrayList<>();
        final HashMap<String, ImageFeatures> computedFeatures = new HashMap<>();

        File dir = new File(annotationsDir);

        String[] children = dir.list();
        if (children == null) {
            throw new Exception("No image children");
        } else {
            for (int i = 0; i < children.length; i++) {
                final String filename = children[i];

                if (filename.endsWith(".txt")) {
                    final int label = Concepts.addConcept(filename);
                    print.print("\n" + filename + "\n");

                    try {
                        FileInputStream fstream = new FileInputStream(annotationsDir + filename);
                        DataInputStream in = new DataInputStream(fstream);
                        final BufferedReader br = new BufferedReader(new InputStreamReader(in));

                        String strLine;
                        while ((strLine = br.readLine()) != null) {
                            String imageName = imagesDir + "im" + strLine + ".jpg";

                            print.print(imageCount + ". image concept " + imageName + "\n");

                            if (computedFeatures.containsKey(imageName)) {
                                allfeatures.add(computedFeatures.get(imageName));
                                //computedFeatures.get(imageName).addAnnotation(label);
                            } else {
                                ImageFeatures features = alg.getImageFeatures(imageName, new int[]{label});
                                if (features != null) {
                                    allfeatures.add(features);
                                    computedFeatures.put(imageName, features);
                                }
                            }
                        }

                        in.close();
                    } catch (Exception ex) {
                        exceptionHandler.handle(ex);
                    }
                }
            }
        }
        alive = false;

        return allfeatures;
    }

    public HashSet<String> getUserTags(String userTagsFilename) {
        HashSet<String> result = new HashSet<>();
        try {
            FileInputStream fstream = new FileInputStream(userTagsFilename);

            DataInputStream in = new DataInputStream(fstream);
            final BufferedReader br = new BufferedReader(new InputStreamReader(in));

            String tag;
            while ((tag = br.readLine()) != null) {
                result.add(UserTagKernel.stem(tag));
            }

        } catch (Exception ex) {
            //exceptionHandler.handle(ex);
            System.out.println("No user tag file associated.");
        }

        return result;
    }

    public ArrayList<ImageFeatures> getAllImagesFeaturesClef2011(ImageFeaturesAlgorithm alg, final String annotationsFile, final String imagesDir, final String userTagsDir) throws Exception {
        final ArrayList<ImageFeatures> allfeatures = new ArrayList<>();

        try {
            FileInputStream fstream = new FileInputStream(annotationsFile);

            DataInputStream in = new DataInputStream(fstream);
            final BufferedReader br = new BufferedReader(new InputStreamReader(in));

            String strLine;
            while ((strLine = br.readLine()) != null) {
                try {
                    String[] tokens = strLine.split(";");
                    String imageName = tokens[0];

                    ArrayList<Integer> conceptsForImg = new ArrayList<>();
                    for (int i = 1; i < tokens.length; i++) {
                        if (Integer.parseInt(tokens[i]) == 1) {
                            conceptsForImg.add(i - 1);
                        }
                    }

                    int[] labels = new int[conceptsForImg.size()];
                    String[] stringConcepts = new String[labels.length];
                    for (int i = 0; i < conceptsForImg.size(); i++) {
                        labels[i] = conceptsForImg.get(i);
                        stringConcepts[i] = Concepts.getConcept(labels[i]);
                    }

                    long start = System.currentTimeMillis();
                    ImageFeatures features = alg.getImageFeatures(imagesDir + imageName, labels);
                    features.setUserTags(getUserTags(userTagsDir + imageName.substring(0, imageName.lastIndexOf(".")) + ".txt"));

                    print.print((imageCount++) + ". Computed features for " + imageName + " in " + (System.currentTimeMillis() - start) + " millisec\n");

                    if (features != null) {
                        allfeatures.add(features);
                    }
                } catch (Exception ex) {
                    exceptionHandler.handle(ex);
                }
            }

        } catch (Exception ex) {
            exceptionHandler.handle(ex);
        }

        return allfeatures;
    }

    public static void copyFile(File sourceFile, File destFile) throws IOException {
        if (!destFile.exists()) {
            destFile.createNewFile();
        }

        FileChannel source = null;
        FileChannel destination = null;
        try {
            source = new FileInputStream(sourceFile).getChannel();
            destination = new FileOutputStream(destFile).getChannel();

            // previous code: destination.transferFrom(source, 0, source.size());
            // to avoid infinite loops, should be:
            long count = 0;
            long size = source.size();
            while ((count += destination.transferFrom(source, count, size - count)) < size);
        } finally {
            if (source != null) {
                source.close();
            }
            if (destination != null) {
                destination.close();
            }
        }
    }

    private class ImfAlg implements Runnable {

        private final String imageName;
        private final ImageFeaturesAlgorithm alg;
        public ImageFeatures features;

        public ImfAlg(String imagename, ImageFeaturesAlgorithm alg) {
            this.imageName = imagename;
            this.alg = alg;
        }

        @Override
        public void run() {
            try {
                this.features = alg.getImageFeatures(imageName, null);
            } catch (Exception ex) {
                Logger.getLogger(ImageFeaturesTest.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    private class ImfUsertags implements Runnable {

        private final String tagsFile;
        public HashSet<String> tags;

        public ImfUsertags(String tagsFile) {
            this.tagsFile = tagsFile;
        }

        @Override
        public void run() {
            try {
                tags = getUserTags(tagsFile);
            } catch (Exception ex) {
                Logger.getLogger(ImageFeaturesTest.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    public ArrayList<ImageFeatures> getAllImagesFeaturesClef2012(final ImageFeaturesAlgorithm alg, final String imagesDir, final String userTagsDir) throws Exception {
        ThreadPoolExecutor threadPool = new ThreadPoolExecutor(2, 3, 10, TimeUnit.MINUTES, new LinkedBlockingQueue<Runnable>());

        final ArrayList<ImageFeatures> allfeatures = new ArrayList<>(10000);
        File folder = new File(imagesDir);
        File[] listOfFiles = folder.listFiles();

        for (int i = 0; i < listOfFiles.length; i++) {
            long start = System.currentTimeMillis();
            final String imageName = listOfFiles[i].getName();

            Collection<Future<?>> futures = new LinkedList<>();
            ImfAlg imf = new ImfAlg(imagesDir + imageName, alg);
            ImfUsertags tag = new ImfUsertags(userTagsDir + imageName.substring(0, imageName.lastIndexOf(".")) + ".txt");
            futures.add(threadPool.submit(imf));
            futures.add(threadPool.submit(tag));

            for (Future<?> f : futures) {
                f.get();
            }

            ImageFeatures features = imf.features;
            features.setImageFilename(imageName);
            features.setUserTags(tag.tags);

            print.print((imageCount++) + ". Computed features for " + imageName + " in " + (System.currentTimeMillis() - start) + " millisec, tags:" + tagsString(tag.tags) + "\n");

            if (features != null) {
                allfeatures.add(features);
            }
        }

        return allfeatures;
    }

    public static String tagsString(HashSet<String> tags) {
        StringBuilder sb = new StringBuilder();
        for (String tag : tags) {
            sb.append(tag).append(' ');
        }
        return sb.toString();
    }

    public ArrayList<ImageFeatures> getAllImagesFeaturesClef2012(ImageFeaturesAlgorithm alg, final String annotationsDir, final String imagesDir, final String userTagsDir) throws Exception {
        final ArrayList<ImageFeatures> allfeatures = new ArrayList<>(10000);

        try {
            File folder = new File(annotationsDir);
            File[] listOfFiles = folder.listFiles();

            for (int it = 0; it < listOfFiles.length; it++) {
                String imageName = listOfFiles[it].getName();

                ArrayList<Integer> conceptsForImg = new ArrayList<>();
                try (FileInputStream fstream = new FileInputStream(annotationsDir + imageName);
                        DataInputStream in = new DataInputStream(fstream);
                        BufferedReader br = new BufferedReader(new InputStreamReader(in))) {
                    String line;
                    while ((line = br.readLine()) != null) {
                        String[] tokens = line.split(" ");
                        conceptsForImg.add(Integer.parseInt(tokens[0]));
                    }
                }

                imageName = imageName.substring(0, imageName.lastIndexOf(".")) + ".jpg";

                try {
                    int[] labels = new int[conceptsForImg.size()];
                    //String[] stringConcepts = new String[labels.length];
                    for (int i = 0; i < conceptsForImg.size(); i++) {
                        labels[i] = conceptsForImg.get(i);
                        //stringConcepts[i] = Concepts.getConcept(labels[i]);
                    }

                    long start = System.currentTimeMillis();

                    ImageFeatures features = alg.getImageFeatures(imagesDir + imageName, labels);
                    features.setImageFilename(imageName);
                    HashSet<String> tags = getUserTags(userTagsDir + imageName.substring(0, imageName.lastIndexOf(".")) + ".txt");
                    features.setUserTags(tags);

                    print.print((imageCount++) + ". Computed features for " + imageName + " in " + (System.currentTimeMillis() - start) + " millisec\n");

                    if (features != null) {
                        allfeatures.add(features);
                    }
                } catch (Exception ex) {
                    exceptionHandler.handle(ex);
                }
            }

        } catch (IOException | NumberFormatException ex) {
            exceptionHandler.handle(ex);
        }

        return allfeatures;
    }
    public static svm_print_interface print = new svm_print_interface() {

        @Override
        public void print(String string) {
            System.out.print(string);
        }
    };

    public static String clef2012SubmisionOne(String imageId, SvmPrediction svmPrediction) {
        StringBuilder sb = new StringBuilder();
        sb.append(imageId);

        for (int i = 0; i < 94; i++) {
            sb.append(' ').append(svmPrediction.getEstimate(i)).append(' ').append(svmPrediction.getBinaryScore(i) ? 1 : 0);
        }
        sb.append('\n');

        return sb.toString();
    }
    public static String submisionFile = "submision5.txt";

    public static void clef2012SubmisionOneToFile(String imageId, SvmPrediction svmPrediction) throws IOException {

        BufferedWriter out = null;
        try {
            String submission = clef2012SubmisionOne(imageId, svmPrediction);

            out = new BufferedWriter(new FileWriter(submisionFile, true));
            out.write(submission);
            out.close();
        } finally {
            out.close();
        }
    }

    public static void clef2012SubmisionOneToFile(String imageId, SvmPrediction svmPrediction, boolean rewrite) throws IOException {

        StringBuilder sb = new StringBuilder();
        FileInputStream fstream = null;
        BufferedWriter out = null;
        try {
            String submission = clef2012SubmisionOne(imageId, svmPrediction);
            if (new File(submisionFile).exists()) {
                fstream = new FileInputStream(submisionFile);
                DataInputStream in = new DataInputStream(fstream);
                BufferedReader br = new BufferedReader(new InputStreamReader(in));
                String line;
                while ((line = br.readLine()) != null) {
                    String imageName = line.substring(0, line.indexOf(' '));
                    if (imageName.compareTo(imageId) == 0) {
                        if (rewrite) {
                            continue;
                        } else {
                            submission = line;
                        }
                    } else {
                        sb.append(line).append('\n');
                    }
                }
                in.close();
                br.close();
            }

            fstream.close();

            FileWriter fw = new FileWriter(submisionFile);
            out = new BufferedWriter(fw);
            out.write(sb.toString());
            out.write(submission);
            out.close();
        } finally {
            if (out != null) {
                out.close();
            }

        }
    }
    public static String featuresFile = "combinedFeatures2012.feat";
    public static String featuresTrainFile = "combinedFeaturesTrain2012.feat";
    //public static String _______featuresTestFile = "combinedFeaturesTest2012.feat";
    public static String testFeaturesFile = "testCombinedFeatures2012.feat";

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) throws Exception {
        //Test images

        int i = 0;
        Concepts.loadConcepts("d:\\faculta\\licenta\\Trainingset2012\\train_annotations\\", "concepts.txt");
        
//        ImageFeaturesTest imageFeaturesTest = new ImageFeaturesTest();

//        Serialization.serialize(testFeatures, testFeaturesFile);
//        System.exit(0);
        //Concepts.loadConcepts("d:\\Programming\\Netbeans projects\\RobotVision\\RobotVision\\concepts\\", "trainingConcepts.txt");
        //ArrayList<ImageFeatures> robotFeatures = (ArrayList<ImageFeatures>) Serialization.deserialize("robotFeatures");

        //final ArrayList<ImageFeatures> testFeatures = null;// (ArrayList<ImageFeatures>) Serialization.deserialize(featuresTestFile);
        final ArrayList<ImageFeatures> features = (ArrayList<ImageFeatures>) Serialization.deserialize(featuresFile);
//        for (i = 0; i < features.size(); i++) {
//            System.out.println(i);
//            CombinedImageFeatures c = (CombinedImageFeatures) features.get(i);
//            CMImageFeatures cm = c.getColormImageFeatures();
//            String img = cm.getImageFilename();
//            HashSet<String> u = cm.getUserTags();
//            int[] an = cm.getAnnotations();
//            
//            cm = CMImageFeatures.getCMImageFeatures("d:\\faculta\\licenta\\Testset2012\\images\\" + img);
//            cm.setAnnotations(an);
//            cm.setImageFilename(c.getImageFilename());
//            cm.setUserTags(u);
//            
//            c.setColormImageFeatures(cm);
//        }
//        
//        Serialization.serialize(features, testFeaturesFile);
//        System.exit(0);


        for (ImageFeatures imFeat : features) {
            imFeat.setImageFilename("d:/faculta/licenta/Trainingset2012/images/" + imFeat.getImageFilename());
        }
        //SvmGridSearch.doGridSearchIsGrayScale(features, print);
        //SvmGridSearch.doGridSearchFaceDetection(testFeatures, print);
        //SvmGridSearch.doGridSearch(features, null, print);
        //System.exit(1);

        SvmScale scale = new SvmScale();
        scale.allImagesFeaturesScale(features);


        CombinedKernel k = new CombinedKernel(new TopSurfAbsoluteKernel(), new RbfKernel(0.0166), new UserTagKernel(16, features), new ColorMomentsKernel());
        SvmTrain svmTrain = new SvmTrain(print);
        
        //svmTrain.setDoGridSearch(true);
        Map<Integer, LinkedList<svm_model>> models = svmTrain.trainBinary(features, k, null, 2);

        final ArrayList<ImageFeatures> testFeatures = (ArrayList<ImageFeatures>) Serialization.deserialize(testFeaturesFile); //imageFeaturesTest.getAllImagesFeaturesClef2012(new CombinedImageFeaturesAlgorithm(), "d:\\faculta\\licenta\\Testset2012\\images\\", "d:\\faculta\\licenta\\Testset2012\\test_metadata\\tags\\");
        for (ImageFeatures imFeat : testFeatures) {
            imFeat.setImageFilename("d:/faculta/licenta/Testset2012/images/" + imFeat.getImageFilename());
        }

        for (i = 0; i < testFeatures.size(); i++) {
            if (i % 500 == 0) {
                System.out.println(i + " images predicted");
            }
            ImageFeatures imFeat = testFeatures.get(i);
            SvmPrediction p = SvmPredict.predictBinaryPrecomputedKernel(features, imFeat, models, k, scale, print);
            clef2012SubmisionOneToFile(imFeat.getImageFilename(), p);
        }
        System.out.println("Serializing models...");
        //Serialization.serialize(models, "submission4Models");

        System.exit(0);
        //final ArrayList<ImageFeatures> features = imageFeaturesTest.getAllImagesFeaturesClef2012(new CombinedImageFeaturesAlgorithm(), "d:/faculta/licenta/Trainingset2012/train_annotations/annotations/", "d:/faculta/licenta/Trainingset2012/images/", "d:/faculta/licenta/Trainingset2012/train_metadata/tags/");

//        ArrayList<ImageFeatures> peffeatures = (ArrayList<ImageFeatures>) Serialization.deserialize(featuresTrainFile);
//        ArrayList<ImageFeatures> surffeatures = (ArrayList<ImageFeatures>) Serialization.deserialize("surfFeaturesTrain2012.feat");
//        ArrayList<ImageFeatures> features = new ArrayList<>();
//        for (i = 0; i < surffeatures.size(); i++) {
//            features.add(new CombinedImageFeatures((TopSurfImageFeatures) surffeatures.get(i), (PEFImageFeatures) peffeatures.get(i)));
//        }
//        Serialization.serialize(features, "combinedFeaturesTrain2012.feat");
//        System.exit(0);

//        ArrayList<ImageFeatures> robotfeatures = imageFeaturesTest.robot(new CombinedImageFeaturesAlgorithm(), "d:\\Programming\\Netbeans projects\\RobotVision\\RobotVision\\trainsets\\training1\\std_cam\\", "d:\\Programming\\Netbeans projects\\RobotVision\\RobotVision\\trainsets\\training1\\locations\\rgb.txt");
//        Serialization.serialize(robotfeatures, "robotFeatures");
        //ArrayList<ImageFeatures> features = (ArrayList<ImageFeatures>) Serialization.deserialize("robotFeatures");

//        int test = (int) (features.size() * 0.1);
//        ArrayList<ImageFeatures> featuresTrain = new ArrayList<>();
//        ArrayList<ImageFeatures> testFeatures = new ArrayList<>();
//
//        for (; i < features.size() - test; i++) {
//            featuresTrain.add(features.get(i));
//        }
//        for (; i < features.size(); i++) {
//            testFeatures.add(features.get(i));
//        }
//
//        Serialization.serialize(testFeatures, featuresTestFile);
//        Serialization.serialize(featuresTrain, featuresTrainFile);
//        System.exit(0);
//        String pefScale = "pefImageFeaturesSvmScale.scale";
        //Serialization.serialize(scale, pefScale);
//        final SvmScale scale = new SvmScale();
//        scale.allImagesFeaturesScale(featuresTrain);
        //String binaryModelFile = "binarySVMModelsCombined";
        //SvmScale scale = (SvmScale) Serialization.deserialize(pefScale);

        //       Kernel k = new CombinedKernel(new TopSurfAbsoluteKernel(), new RbfKernel(1.00 / featuresTrain.get(0).getFeatures().length), new UserTagKernel(3, features));
        //Kernel k = new TopSurfCosineKernel();
        //Kernel k = new ChiSquareKernel();


//        svm_problem gramMatrix = k.computeGramMatrixTrain(features);
//
//        double[] Cs = new double[]{1, /*
//         * 2, 4, 6, 8, 10
//         */};
//
//        for (int a = 0; a < Cs.length; a++) {
//            long start = System.currentTimeMillis();
//            double C = Cs[a];
//            SvmTrain svmTrain = new SvmTrain(print);
//            svmTrain.setC(1);
//            svmTrain.setGramMatrix(gramMatrix);
//
//            //print.print(new Date().toString() + " - Training model for C = " + C + "\n");
//            final Map<Integer, LinkedList<svm_model>> models = svmTrain.trainBinary(featuresTrain, k, null, 2);
//
//            int truePositivesSum = 0;
//            int trueNegativesSum = 0;
//            int falseNegativesSum = 0;
//            int falsePositivesSum = 0;
//
//            for (ImageFeatures imFeat : testFeatures) {
//                //SvmPrediction p = SvmPredict.predictBinaryPrecomputedKernel(features, imFeat, models, null, scale, print);
//                SvmPrediction p = SvmPredict.predictBinaryPrecomputedKernel(featuresTrain, imFeat, models, k, scale, print);
//
//                Evaluation e = new Evaluation(new SvmPrediction[]{p});
//                truePositivesSum += e.getTruePositivesSum();
//                trueNegativesSum += e.getTrueNegativesSum();
//                falseNegativesSum += e.getFalseNegativesSum();
//                falsePositivesSum += e.getFalsePositivesSum();
//
//                print.print(e.toString());
//                print.print("\n");
//            }
//
//            //print.print(new Date().toString() + " \nC: " + C + " --- Fmeasure mean: " + (fmeasureMean / testFeatures.size()) + "\n");
//            print.print(
//                    "True positives sum: " + truePositivesSum + "\t"
//                    + "True negatives sum: " + trueNegativesSum + "\n"
//                    + "False positives sum: " + falsePositivesSum + "\t"
//                    + "False negatives sum: " + falseNegativesSum + "\n\n");
//
//            System.out.println("total test: " + testFeatures.size());
//            System.out.println("Done. C was:" + 5 + ", fmeasure = " + Evaluation.computeFmeasure(truePositivesSum, falsePositivesSum, falseNegativesSum));
//            gramMatrix = null;
//            svmTrain = null;
//            final ImageFeaturesAlgorithm alg = features.get(0).getProcessingAlgorithm();
//            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
//
//            java.awt.EventQueue.invokeLater(new Runnable() {
//
//                @Override
//                public void run() {
//                    new FeaturesFrameTest(models, scale, alg, features, k).setVisible(true);
//                }
//            });
//        }


    }
}

/*
 * Flickr tags
        HashMap<String, Integer> stringLabels = new HashMap<String, Integer>();
        stringLabels.put("animals", 0);
        stringLabels.put("baby", 1);
        stringLabels.put("bird", 2);
        stringLabels.put("car", 3);
        stringLabels.put("clouds", 4);
        stringLabels.put("dog", 5);
        stringLabels.put("female", 6);
        stringLabels.put("flower", 7);
        stringLabels.put("food", 8);
        stringLabels.put("indoor", 9);
        stringLabels.put("lake", 10);
        stringLabels.put("male", 11);
        stringLabels.put("night", 12);
        stringLabels.put("people", 13);
        stringLabels.put("plant_life", 14);
        stringLabels.put("portrait", 15);
        stringLabels.put("river", 16);
        stringLabels.put("sea", 17);
        stringLabels.put("sky", 18);
        stringLabels.put("structures", 19);
        stringLabels.put("sunset", 20);
        stringLabels.put("transport", 21);
        stringLabels.put("tree", 22);
        stringLabels.put("water", 23);          
*/