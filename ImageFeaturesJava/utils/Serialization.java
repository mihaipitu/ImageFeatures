/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package utils;

import imagefeatures.ImageFeatures;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.List;
import libsvm.svm_node;
import libsvm.svm_problem;
import supportVectorMachines.SvmScale;

/**
 *
 * @author Mihai Pîțu
 */
public class Serialization {

    public static void serialize(Object obj, String filename) throws IOException {
        FileOutputStream outFileStream = null;
        ObjectOutputStream outStream = null;
        try {

            outFileStream = new FileOutputStream(filename);
            outStream = new ObjectOutputStream(outFileStream);

            outStream.writeObject(obj);
            outStream.flush();

        } finally {
            try {
                outFileStream.close();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }

    public static Object deserialize(String filename) throws IOException, ClassNotFoundException {
        Object obj = null;
        FileInputStream inFileStream = null;
        ObjectInputStream inStream = null;

        try {
            inFileStream = new FileInputStream(filename);
            inStream = new ObjectInputStream(inFileStream);
            obj = inStream.readObject();
        } finally {
            try {
                inFileStream.close();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }

        return obj;
    }

    public static void saveFeaturesSvmFormat(List<ImageFeatures> list, String filename, boolean scale, String scaleSettingsFilename) throws IOException {
        if (scale) {
            SvmScale svmScale = new SvmScale();
            svmScale.allImagesFeaturesScale((ArrayList<ImageFeatures>) list);
            serialize(svmScale, scaleSettingsFilename);
        }

        BufferedWriter out = new BufferedWriter(new FileWriter(filename));

        try {
            for (ImageFeatures imageFeatures : list) {
                int[] annotations = imageFeatures.getAnnotations();
                int i;
                for (i = 0; i < annotations.length - 1; i++) {
                    out.write(annotations[i] + ", ");
                }
                out.write(annotations[i] + " ");
                svm_node[] features = imageFeatures.getFeatures();
                int j;
                for (j = 0; j < features.length - 1; j++) {
                    out.write(features[j].index + ":" + features[j].value + " ");
                }
                out.write(features[j].index + ":" + features[j].value + "\n");
            }
        } finally {
            if (out != null) {
                out.close();
            }
        }
    }
    
    public static void saveGramMatrix(svm_problem svmProblem, String filename) throws IOException {
        BufferedWriter out = new BufferedWriter(new FileWriter(filename));

        try {
            for (int i = 0; i < svmProblem.l; i++) {
                out.write(svmProblem.y[i] + " ");
                for (int j = 0; j < svmProblem.x[i].length; j++) {
                    out.write(svmProblem.x[i][j].index + ":" + svmProblem.x[i][j].value + " ");
                }
                out.write("\n");
            }
        } finally {
            if (out != null) {
                out.flush();
                out.close();
            }
        }
    }
}
