/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package supportVectorMachines.Kernels;

import imagefeatures.ImageFeatures;
import java.util.ArrayList;
import libsvm.svm_node;
import libsvm.svm_problem;
import speededUpRobustFeatures.TopSurfDescriptor;
import speededUpRobustFeatures.TopSurfImageFeatures;

/**
 *
 * @author Mihai Pîțu
 */
public class TopSurfAbsoluteKernel extends Kernel {
    
    private double gamma = 0.094;
    
    @Override
    protected double compute(ImageFeatures if1, ImageFeatures if2) {
        //double x = Math.exp(-gamma * TopSurfDescriptor.compareDescriptorsAbsolute(((TopSurfImageFeatures) if1).getDescriptor(), ((TopSurfImageFeatures) if2).getDescriptor()));
        double x = TopSurfDescriptor.compareDescriptorsAbsolute(((TopSurfImageFeatures) if1).getDescriptor(), ((TopSurfImageFeatures) if2).getDescriptor());
        if (x < 1)
            return 1;
        return 1.0f / x;
        //return x;
    }

//    @Override
//    public svm_problem computeGramMatrixTrain(ArrayList<ImageFeatures> allImagesFeatures) {
//        svm_problem svmProblem = new svm_problem();
//
//        svmProblem.l = allImagesFeatures.size();
//        svmProblem.x = new svm_node[svmProblem.l][svmProblem.l + 1];
//        svmProblem.y = new double[svmProblem.l];
//
//        for (int i = 0; i < allImagesFeatures.size(); i++) {
//            TopSurfImageFeatures tsImageFeatures1 = (TopSurfImageFeatures) allImagesFeatures.get(i);
//
//            svmProblem.y[i] = (double) tsImageFeatures1.getAnnotations()[0];
//
//            //Gram Matrix
//            //svm_node[] computedXs = new svm_node[svmProblem.l + 1];
//            svmProblem.x[i][0] = new svm_node();
//            svmProblem.x[i][0].index = 0;
//            svmProblem.x[i][0].value = i + 1;
//            for (int j = i; j < svmProblem.l; j++) {
//                TopSurfImageFeatures tsImageFeatures2 = (TopSurfImageFeatures) allImagesFeatures.get(j);
//                svmProblem.x[i][j + 1] = new svm_node();
//                svmProblem.x[i][j + 1].index = j + 1;
//                svmProblem.x[i][j + 1].value = TopSurfDescriptor.compareDescriptorsCosine(tsImageFeatures1.getDescriptor(), tsImageFeatures2.getDescriptor());
//
//                svmProblem.x[j][i + 1] = new svm_node();
//                svmProblem.x[j][i + 1].index = i + 1;
//                svmProblem.x[j][i + 1].value = svmProblem.x[i][j + 1].value;
//            }
//        }
//
//        return svmProblem;
//    }
//    
//    @Override
//    public svm_problem computeGramMatrixTest(ArrayList<ImageFeatures> allImagesFeatures, ImageFeatures testImageFeatures) {
//        
//        TopSurfImageFeatures tsFeatures = (TopSurfImageFeatures) testImageFeatures;
//        
//        svm_problem svmProblem = new svm_problem();
//
//        svmProblem.l = 1;
//        svmProblem.x = new svm_node[1][allImagesFeatures.size() + 1];
//        svmProblem.y = new double[1];
//        svmProblem.y[0] = tsFeatures.getAnnotations()[0];
//        
//        final TopSurfDescriptor descriptor =  tsFeatures.getDescriptor();
//        final svm_node[] gramFeatures = new svm_node[allImagesFeatures.size() + 1];
//        
//        gramFeatures[0] = new svm_node();
//        gramFeatures[0].index = 0;
//        gramFeatures[0].value = 1;
//        
//        for (int j = 0; j < allImagesFeatures.size(); j++) {
//            TopSurfImageFeatures tsImageFeatures1 = (TopSurfImageFeatures) allImagesFeatures.get(j);
//            
//            gramFeatures[j + 1] = new svm_node();
//            gramFeatures[j + 1].index = j + 1;
//            gramFeatures[j + 1].value = TopSurfDescriptor.compareDescriptorsCosine(descriptor, tsImageFeatures1.getDescriptor());
//        }
//        svmProblem.x[0] = gramFeatures;
//        
//        return svmProblem;
//    }
//    
//    @Override
//    public svm_problem computeGramMatrixTest(ArrayList<ImageFeatures> allImagesFeatures, ArrayList<ImageFeatures> testImageFeatures) {
//        svm_problem svmProblem = new svm_problem();
//
//        svmProblem.l = testImageFeatures.size();
//        svmProblem.x = new svm_node[svmProblem.l][allImagesFeatures.size() + 1];
//        svmProblem.y = new double[svmProblem.l];
//        
//        for (int i = 0; i < testImageFeatures.size(); i++) {
//            svmProblem.x[i] = computeGramMatrixTest(allImagesFeatures, testImageFeatures.get(i)).x[0];
//        }
//        
//        return svmProblem;
//    }
}