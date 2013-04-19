/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package supportVectorMachines.Kernels;

import imagefeatures.ImageFeatures;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.util.ArrayList;
import libsvm.svm_node;
import libsvm.svm_problem;

/**
 *
 * @author Mihai Pîțu
 */
public abstract class Kernel implements Serializable {

    protected abstract double compute(ImageFeatures if1, ImageFeatures if2);

    public svm_problem computeGramMatrixTrain(ArrayList<ImageFeatures> allImagesFeatures) {
        svm_problem svmProblem = new svm_problem();

        svmProblem.l = allImagesFeatures.size();
        svmProblem.x = new svm_node[svmProblem.l][svmProblem.l + 1];
        svmProblem.y = new double[svmProblem.l];

        for (int i = 0; i < allImagesFeatures.size(); i++) {
            if (i % 1000 == 0) {
                System.out.println(i + "\n");
                System.gc();
            }
            svmProblem.y[i] = (double) allImagesFeatures.get(i).getAnnotations()[0];

            svmProblem.x[i][0] = new svm_node();
            svmProblem.x[i][0].index = 0;
            svmProblem.x[i][0].value = i + 1;

            svmProblem.x[i][i + 1] = new svm_node();
            svmProblem.x[i][i + 1].index = i + 1;
            svmProblem.x[i][i + 1].value = (float) compute(allImagesFeatures.get(i), allImagesFeatures.get(i));

            for (int j = i + 1; j < svmProblem.l; j++) {
                svmProblem.x[i][j + 1] = new svm_node();
                svmProblem.x[i][j + 1].index = j + 1;
                svmProblem.x[i][j + 1].value = (float) compute(allImagesFeatures.get(i), allImagesFeatures.get(j));
                
                svmProblem.x[j][i + 1] = new svm_node();
                svmProblem.x[j][i + 1].index = i + 1;
                svmProblem.x[j][i + 1].value = svmProblem.x[i][j + 1].value;
            }
        }

        return svmProblem;
    }

    public svm_problem computeGramMatrixTest(ArrayList<ImageFeatures> allImagesFeatures, ImageFeatures testImageFeatures) {

        svm_problem svmProblem = new svm_problem();

        svmProblem.l = 1;
        svmProblem.x = new svm_node[1][allImagesFeatures.size() + 1];
        svmProblem.y = new double[1];
        int[] ann = testImageFeatures.getAnnotations();
        if (ann.length != 0) {
            svmProblem.y[0] = ann[0];
        } else {
            svmProblem.y[0] = 0;
        }

        final svm_node[] gramFeatures = new svm_node[allImagesFeatures.size() + 1];

        gramFeatures[0] = new svm_node();
        gramFeatures[0].index = 0;
        //gramFeatures[0].value = 1;

        for (int j = 0; j < allImagesFeatures.size(); j++) {

            gramFeatures[j + 1] = new svm_node();
            gramFeatures[j + 1].index = j + 1;
            gramFeatures[j + 1].value = (float) compute(testImageFeatures, allImagesFeatures.get(j));//TopSurfDescriptor.compareDescriptorsCosine(descriptor, tsImageFeatures1.getDescriptor());
        }
        svmProblem.x[0] = gramFeatures;

        return svmProblem;
    }

    public svm_problem computeGramMatrixTest(ArrayList<ImageFeatures> allImagesFeatures, ArrayList<ImageFeatures> testImageFeatures) {
        svm_problem svmProblem = new svm_problem();

        svmProblem.l = testImageFeatures.size();
        svmProblem.x = new svm_node[svmProblem.l][allImagesFeatures.size() + 1];
        svmProblem.y = new double[svmProblem.l];

        for (int i = 0; i < testImageFeatures.size(); i++) {
            svm_problem gram = computeGramMatrixTest(allImagesFeatures, testImageFeatures.get(i));
            svmProblem.x[i] = gram.x[0];
            svmProblem.y[i] = gram.y[0];
        }

        return svmProblem;
    }
}
