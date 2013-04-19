/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package imagefeatures;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import libsvm.svm_model;
import libsvm.svm_node;
import libsvm.svm_print_interface;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import speededUpRobustFeatures.TopSurfAlgorithm;
import supportVectorMachines.*;
import supportVectorMachines.Kernels.*;
import utils.JOpenCV;
import utils.Serialization;

/**
 *
 * @author Mihai Pîțu
 */
public class ImageFeaturesServer implements Runnable {

    private ServerSocket serverSocket;
    private final int workingThreads;
    private final long keepAliveTime = 10;
    private BlockingQueue<Runnable> tasks = new LinkedBlockingQueue<>();
    private ThreadPoolExecutor threadPool;
    private final ArrayList<ImageFeatures> allFeatures;
    private Map<Integer, LinkedList<svm_model>> models;
    private final Kernel kernel;
    private final SvmScale svmScale;
    private final ImageFeaturesAlgorithm alg;
    private final svm_print_interface print;

    public ImageFeaturesServer(int port, int workingThreads, ArrayList<ImageFeatures> features, Map<Integer, LinkedList<svm_model>> models, Kernel k, final SvmScale svmScale, final svm_print_interface print) {
        try {
            serverSocket = new ServerSocket(port);
        } catch (IOException ex) {
            Logger.getLogger(ImageFeaturesServer.class.getName()).log(Level.SEVERE, null, ex);
        }
        this.workingThreads = workingThreads;
        this.allFeatures = features;
        this.models = models;
        this.kernel = k;
        this.svmScale = svmScale;
        this.print = print;
        this.alg = allFeatures.get(0).getProcessingAlgorithm();

        threadPool = new ThreadPoolExecutor(workingThreads, workingThreads, keepAliveTime, TimeUnit.DAYS, tasks);
        threadPool.submit(new Runnable() {

            @Override
            public void run() {
                try {
                    //SvmGridSearch.doGridSearch(allFeatures, svmScale, print);
                } catch (Exception ex) {
                    Logger.getLogger(ImageFeaturesServer.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        });
        print.print("Server listening at " + port + "...\n");
    }

    @Override
    public void run() {
        try {
            while (true) {
                Socket clientSocket = serverSocket.accept();
                try {
                    Task t = new Task(clientSocket);
                    threadPool.submit(t).get();
                } catch (Exception ex) {
                    if (clientSocket != null) {
                        clientSocket.close();
                    }
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private class Task implements Runnable {

        private final Socket clientSocket;
        private BufferedReader inFromClient = null;
        private DataOutputStream outToClient = null;

        public Task(Socket clientSocket) {
            this.clientSocket = clientSocket;
        }

        private String responsePredict(String imagePath, String tagsRaw) throws Exception {
            if (models == null) {
                models = SvmGridSearch.models;
            }
            HashSet<String> usertags = new HashSet<>();
            if (tagsRaw != null) {
                for (String tag : tagsRaw.split("\t")) {
                    if (tag != null) {
                        usertags.add(UserTagKernel.stem(tag));
                    }
                }
            }
            
            ImageFeatures imFeat = alg.getImageFeatures(imagePath, null);
            imFeat.setUserTags(usertags);
            //SvmPrediction p = SvmPredict.robotPredict(allFeatures, imFeat, models, kernel, svmScale, SvmPredict.PredictType.NoPostPredict, null);
            SvmPrediction p = SvmPredict.predictBinaryPrecomputedKernel(allFeatures, imFeat, models, kernel, svmScale, null);
            //SvmPrediction p = SvmPredict.predictBinaryGroupedKernels(allFeatures, SvmGridSearch.groupedTrainImages, imFeat, models, svmScale, null);

            return p.toXml();
        }

        private synchronized File createTempFile(byte[] bytes) throws IOException {
            File temp;
            int i = 0;
            do {
                i++;
                temp = new File("temp" + i + ".jpg");
            } while (temp.exists());
            try (FileOutputStream fos = new FileOutputStream(temp.getPath())) {
                fos.write(bytes, 0, bytes.length);
            }

            return temp;
        }

        @Override
        public void run() {
            try {
                inFromClient = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                outToClient = new DataOutputStream(clientSocket.getOutputStream());

                String line = inFromClient.readLine();
                String command = line.substring(0, line.indexOf(' '));
                String argument = line.substring(line.indexOf(' ') + 1);
                String response = "Invalid command!\n";

                try {
                    switch (command) {
                        case "predict": //predict path/to/image
                            char type = argument.charAt(0);
                            argument = argument.substring(2);
                            int spaceIndex = argument.indexOf(' ');
                            String tags = argument.substring(0, spaceIndex);
                            argument = argument.substring(spaceIndex + 1);
                            
                            switch (type) {
                                case '1':
                                    byte[] imageBytes = new sun.misc.BASE64Decoder().decodeBuffer(argument);

                                    File temp = createTempFile(imageBytes);
                                    argument = temp.getPath();

                                    response = responsePredict(argument, tags);
                                    if (!temp.delete()) {
                                        temp.deleteOnExit();
                                    }
                                    break;
                                case '0':
                                    response = responsePredict(argument, tags);
                                    break;
                            }
                            break;
                        case "visualize":
                            byte[] imageBytes = new sun.misc.BASE64Decoder().decodeBuffer(argument);

                            File temp = createTempFile(imageBytes);
                            argument = temp.getPath();

                            response = TopSurfAlgorithm.visualizeDescriptorImagebase64(argument);
                            if (!temp.delete()) {
                                temp.deleteOnExit();
                            }
                            break;

                        case "facedetection":
                            imageBytes = new sun.misc.BASE64Decoder().decodeBuffer(argument);
                            temp = createTempFile(imageBytes);
                            argument = temp.getPath();

                            response = JOpenCV.base64FaceDetection(argument);
                            if (!temp.delete()) {
                                temp.deleteOnExit();
                            }
                            break;

                        case "concepts":
                            StringBuilder sb = new StringBuilder();
                            for (Entry<Integer, String> e : Concepts.getIntLabels().entrySet()) {
                                sb.append(e.getKey()).append(" ").append(e.getValue()).append('|');
                            }
                            sb.deleteCharAt(sb.length() - 1);
                            sb.append('\n');

                            response = sb.toString();
                            break;

                        case "post":
                            SvmPrediction svmPrediction = new SvmPrediction();
                            InputSource is = new InputSource();
                            is.setCharacterStream(new StringReader(argument));
                            DocumentBuilder db = DocumentBuilderFactory.newInstance().newDocumentBuilder();
                            Document doc = db.parse(is);
                            doc.normalize();
                            Node node = doc.getElementsByTagName("path").item(0);
                            String imageName = node.getFirstChild().getNodeValue();
                            NodeList predictions = doc.getElementsByTagName("prediction");
                            for (int i = 0; i < predictions.getLength(); i++) {
                                Node prediction = predictions.item(i);
                                String concept = prediction.getAttributes().getNamedItem("concept").getNodeValue();
                                float probability = 0;
                                NodeList childNodes = prediction.getChildNodes();
                                for (int j = 0; j < childNodes.getLength(); j++) {
                                    if (childNodes.item(j).getNodeName() == "probability") {
                                        probability = Float.parseFloat(childNodes.item(j).getFirstChild().getNodeValue());
                                        break;
                                    }
                                }
                                svmPrediction.setEstimate(Concepts.getConcept(concept), probability);
                            }
                            int i = imageName.lastIndexOf('\\');
                            if (i == -1) {
                                imageName.lastIndexOf('/');
                            }

                            ImageFeaturesTest.clef2012SubmisionOneToFile(imageName.substring(i + 1, imageName.lastIndexOf('.')), svmPrediction, true);
                            response = "Post ok.\n";
                            break;
                    }
                } catch (Exception ex) {
                    response = "Unable to execute command: " + ex.toString() + "\n";
                }

                byte[] b = response.getBytes("UTF8");
                outToClient.write(b, 0, b.length);
                //outToClient.writeChars(response);
                clientSocket.close();
                print.print("Responding to client " + clientSocket.toString() + "(command:" + command + ")\n");

            } catch (Exception ex) {
                if (this.clientSocket != null) {
                    try {
                        clientSocket.close();
                    } catch (IOException ex1) {
                        Logger.getLogger(ImageFeaturesServer.class.getName()).log(Level.SEVERE, null, ex1);
                    }
                }
            }
        }
    }

    /*
     * "c:\Program Files\Java\jr e7\bin\java.exe" -Xms64M -Xmx13096M -jar
     * "ImageFeatures.jar" -i "../combinedFea turesTest2012.feat" -c
     * "d:\faculta\licenta\Trainingset2012\train_annotations\con cepts.txt"
     */
    public static void main(String[] args) throws Exception {


        String conceptsFile = "d:\\faculta\\licenta\\Trainingset2012\\train_annotations\\concepts.txt";
        String modelsFile = null;//"submission3Models";//null;
        String featuresFile = "combinedFeatures2012.feat";
        //ROBOOT
        //String featuresFile = "robotFeatures";
        //String conceptsFile = "d:\\Programming\\Netbeans projects\\RobotVision\\RobotVision\\concepts\\trainingConcepts.txt";

        for (int i = 0; i < args.length; i++) {
            if (args[i].charAt(0) != '-') {
                break;
            }
            if (++i >= args.length) {
                exitWithUsage();
            }
            switch (args[i - 1].charAt(1)) {
                case 'c':
                    conceptsFile = args[i];
                    break;
                case 'm':
                    modelsFile = args[i];
                    break;
                case 'i':
                    featuresFile = args[i];
                    break;
            }
        }

        if (conceptsFile == null || featuresFile == null) {
            exitWithUsage();
        }

        int i = conceptsFile.lastIndexOf('\\');
        if (i == -1) {
            i = conceptsFile.lastIndexOf('/');
        }

        String cdir = conceptsFile.substring(0, i + 1);
        conceptsFile = conceptsFile.substring(i + 1);
        Concepts.loadConcepts(cdir, conceptsFile);
        ArrayList<ImageFeatures> features = (ArrayList<ImageFeatures>) Serialization.deserialize(featuresFile);
        ArrayList<ImageFeatures> pfeatures = new ArrayList<>();
        for (int j = 0; j < features.size() - 3000; j++) {
            pfeatures.add(features.get(j));
        }
        features = pfeatures;
                for (ImageFeatures imf : features) {
            CombinedImageFeatures cimf = (CombinedImageFeatures) imf;
            for (svm_node n : cimf.getColormImageFeatures().getFeatures()) {
                if (Float.isNaN(n.value)) {
                    System.out.println(imf);
                }
            }
        }
        SvmScale scale = new SvmScale();
        scale.allImagesFeaturesScale(features);
        Map<Integer, LinkedList<svm_model>> models = null;
        Kernel k = new CombinedKernel(new TopSurfAbsoluteKernel(), new RbfKernel(0.166), new UserTagKernel(16, features), new ColorMomentsKernel());

        if (modelsFile == null) {
            SvmTrain svmTrain = new SvmTrain(ImageFeaturesTest.print);
            svmTrain.setC(20);
            models = svmTrain.trainBinary(features, k, null, 2);
            //models = svmTrain.trainBinaryGroupedKernels(features, SvmGridSearch.groupedTrainImages, null, 2);
        } else {
            System.out.println("Deserializing models...");
            models = (Map<Integer, LinkedList<svm_model>>) Serialization.deserialize(modelsFile);
        }
        System.gc();
        ImageFeaturesServer server = new ImageFeaturesServer(9998, 4, features, models, k, scale, ImageFeaturesTest.print);
        server.run();
    }

    private static void exitWithUsage() {
        System.out.println("Usage:"
                + " -c \"concepts.txt\" --Concepts file (Mandatory)\n"
                + " -m \"models.ser\" -- the serialized models file (if not set, new models will be trained)\n"
                + " -i \"trainingimagesFeatures.ser\" -- traing image features (Mandatory)\n");
        System.exit(1);
    }
}
