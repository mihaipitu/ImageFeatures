/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package speededUpRobustFeatures;

import com.sun.media.jai.codec.ImageCodec;
import com.sun.media.jai.codec.ImageEncoder;
import com.sun.media.jai.codec.JPEGEncodeParam;
import imagefeatures.ImageFeatures;
import imagefeatures.ImageFeaturesAlgorithm;
import java.awt.image.ColorModel;
import java.awt.image.Raster;
import java.awt.image.SampleModel;
import java.awt.image.WritableRaster;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import javax.media.jai.JAI;
import javax.media.jai.PlanarImage;
import javax.media.jai.TiledImage;
import profileEntropyFeatures.PEF;

/**
 *
 * @author Mihai Pîțu
 */
public class TopSurfAlgorithm implements ImageFeaturesAlgorithm {

    native public static void load(String dictonary, int imagedim, int top);

    native public static byte[] computeSurf(byte[] pixels, int width, int height);

    native public static TopSurfDescriptor extractSurfDescriptor(byte[] pixels, int width, int height, TopSurfVisualword initVw, TopSurfDescriptor initDescriptor);

    native public static TopSurfDescriptor extractSurfDescriptorFile(String imagePath, TopSurfVisualword initVw, TopSurfDescriptor initDescriptor);

    native private static byte[] visualizeDescriptor(byte[] imageBytes, int dimx, int dimy, String imagePath);

    public static String visualizeDescriptorImagebase64(String imagePath) throws IOException {
        PlanarImage pi = JAI.create("fileload", imagePath);
        
        int[] intPixels = PEF.getPixels(pi);
        byte[] bytePixels = new byte[intPixels.length];
        for (int i = 0; i < intPixels.length; i++) {
            bytePixels[i] = (byte) intPixels[i];
        }

        SampleModel sm = pi.getSampleModel();
        ColorModel cm = pi.getColorModel();
        int width = pi.getWidth();
        int height = pi.getHeight();

        bytePixels = visualizeDescriptor(bytePixels, width, height, imagePath);

        Raster raster = pi.getData();
        WritableRaster wRaster = raster.createCompatibleWritableRaster();
        for (int i = 0; i < intPixels.length; i++) {
            intPixels[i] = bytePixels[i];
        }
        wRaster.setPixels(0, 0, width, height, intPixels);
        
        pi.dispose();

        TiledImage ti = new TiledImage(0, 0, width, height, 0, 0, sm, cm);
        ti.setData(wRaster);

        JPEGEncodeParam encodeParam = new JPEGEncodeParam();

    //    JAI.create("filestore",ti,"d:\\oil2.jpg","JPEG");

        ByteArrayOutputStream out = new ByteArrayOutputStream();

        String base64 = null;
        try {
            ImageEncoder encoder = ImageCodec.createImageEncoder("JPEG", out, encodeParam);
            encoder.encode(ti);
            out.flush();
            base64 = new sun.misc.BASE64Encoder().encodeBuffer(out.toByteArray());
        } finally {
            if (out != null) {
                out.close();
            }
        }
        ti.dispose();
        
        return base64;
    }

    //native public static float compareDescriptors(TopSurfDescriptor td1, TopSurfDescriptor td2, TopSurfVisualword initVw);
    native public static void unload();

    static {
        if (System.getProperty("os.arch").toLowerCase().equals("amd64")) {
            System.loadLibrary("TopSurfWrappx64");
        } else {
            System.loadLibrary("TopSurfWrappx86");
        }

        TopSurfAlgorithm.load("TopSurfDictionary", 512, 100);
    }
    private TopSurfDescriptor initDescriptor = new TopSurfDescriptor();
    private TopSurfVisualword initVw = new TopSurfVisualword();

    @Override
    public TopSurfImageFeatures getImageFeatures(String imagePath, int[] labels) throws Exception {
        TopSurfImageFeatures imageFeatures = new TopSurfImageFeatures();
        imageFeatures.addFeatures(extractSurfDescriptorFile(imagePath, initVw, initDescriptor));
        if (labels != null) {
            for (int label : labels) {
                imageFeatures.addAnnotation(label);
            }
        }

        imageFeatures.setImageFilename(imagePath);
        return imageFeatures;
    }
}
