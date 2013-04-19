/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package utils;

import com.googlecode.javacv.cpp.avcodec;
import com.googlecode.javacv.cpp.opencv_core;
import com.googlecode.javacv.cpp.opencv_objdetect;

import static com.googlecode.javacv.cpp.opencv_core.*;
import static com.googlecode.javacv.cpp.opencv_imgproc.*;
import static com.googlecode.javacv.cpp.opencv_highgui.*;
import com.googlecode.javacv.cpp.opencv_objdetect.CvHaarClassifierCascade;
import static com.googlecode.javacv.cpp.opencv_core.*;
import static com.googlecode.javacv.cpp.opencv_imgproc.*;
import static com.googlecode.javacv.cpp.opencv_calib3d.*;
import static com.googlecode.javacv.cpp.opencv_objdetect.*;
import java.io.*;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 *
 * @author Mihai Pîțu
 */
public class JOpenCV {
    //http://opencv.willowgarage.com/wiki/FaceDetection

    static {
        storage = opencv_core.CvMemStorage.create();
        // We instantiate a classifier cascade to be used for detection, using the cascade definition.
        cascade = new opencv_objdetect.CvHaarClassifierCascade(cvLoad("libs//haarcascade_frontalface_default.xml"));

    }
    //scaleFactor (Double)
    //The factor by which the search window is scaled between the subsequent scans, for example, 1.1 means increasing window by 10%
    private static double scaleFactor = 1.15;
    //minNeighbors (Int32)
    //Minimum number (minus 1) of neighbor rectangles that makes up an object. 
    //All the groups of a smaller number of rectangles than min_neighbors-1 are rejected. 
    //If min_neighbors is 0, the function does not any grouping at all and returns all the detected candidate rectangles, 
    //which may be useful if the user wants to apply a customized grouping procedure
    private static int minNeighbors = 7;
    //flags (Int32)
    //Mode of operation. Currently the only flag that may be specified is CV_HAAR_DO_CANNY_PRUNING. 
    //If it is set, the function uses Canny edge detector to reject some image regions that contain too few or too much edges and 
    //thus can not contain the searched object. 
    //The particular threshold values are tuned for face detection and in this case the pruning speeds up the processing
    private static int flags = CV_HAAR_DO_CANNY_PRUNING;
    private static opencv_objdetect.CvHaarClassifierCascade cascade;
    private static opencv_core.CvMemStorage storage;
//
//    private static opencv_core.CvSeq cvSeqFaceDetection(opencv_core.IplImage grayImage) {
//
//
//        // We need a grayscale image in order to do the recognition, so we
//        // create a new image of the same size as the original one.
//
//
//        // We convert the original image to grayscale.
//        
//
//        // We detect the faces.
//        
//       
//        return faces;
//    }


    public static int nrFaceDetection(String imageName) {
        opencv_core.IplImage originalImage = cvLoadImage(imageName, 1);
        
        opencv_core.IplImage grayImage = opencv_core.IplImage.create(originalImage.width(), originalImage.height(), IPL_DEPTH_8U, 1);
        cvCvtColor(originalImage, grayImage, CV_BGR2GRAY);
        opencv_core.CvSeq faces = cvHaarDetectObjects(grayImage, cascade, storage, getScaleFactor(), getMinNeighbors(), getFlags());
        int total = faces.total();

        cvReleaseImage(originalImage);
        grayImage.release();
        storage.free_space();

        return total;
    }

    public synchronized static String base64FaceDetection(String imageName) throws IOException {
        
        opencv_core.IplImage originalImage = cvLoadImage(imageName, 1);
        opencv_core.IplImage grayImage = opencv_core.IplImage.create(originalImage.width(), originalImage.height(), IPL_DEPTH_8U, 1);
        cvCvtColor(originalImage, grayImage, CV_BGR2GRAY);
        opencv_core.CvSeq faces = cvHaarDetectObjects(grayImage, cascade, storage, getScaleFactor(), getMinNeighbors(), getFlags());

        for (int i = 0; i < faces.total(); i++) {
            CvRect r = new CvRect(cvGetSeqElem(faces, i));
            cvRectangle(originalImage, cvPoint(r.x(), r.y()),
                    cvPoint(r.x() + r.width(), r.y() + r.height()), CvScalar.BLUE, 3, CV_AA, 0);
        }
        int i = 0;
        File temp;
        do {
            i++;
            temp = new File("temp" + i + imageName.substring(imageName.lastIndexOf('.')));
        } while (temp.exists());

        cvSaveImage(temp.getPath(), originalImage);
        cvReleaseImage(originalImage);
        
        
        String base64 = new sun.misc.BASE64Encoder().encodeBuffer(Files.readAllBytes(temp.toPath()));
        temp.delete();

        grayImage.release();
        storage.free_space();
        
        return base64;
    }

//    public static void drawRectangles(opencv_core.CvSeq faces, String filename) {
//        
//    }
    /**
     * @return the scaleFactor
     */
    public static double getScaleFactor() {
        return scaleFactor;
    }

    /**
     * @param aScaleFactor the scaleFactor to set
     */
    public static void setScaleFactor(double aScaleFactor) {
        scaleFactor = aScaleFactor;
    }

    /**
     * @return the minNeighbors
     */
    public static int getMinNeighbors() {
        return minNeighbors;
    }

    /**
     * @param aMinNeighbors the minNeighbors to set
     */
    public static void setMinNeighbors(int aMinNeighbors) {
        minNeighbors = aMinNeighbors;
    }

    /**
     * @return the flags
     */
    public static int getFlags() {
        return flags;
    }

    /**
     * @param aFlags the flags to set
     */
    public static void setFlags(int aFlags) {
        flags = aFlags;
    }
}
