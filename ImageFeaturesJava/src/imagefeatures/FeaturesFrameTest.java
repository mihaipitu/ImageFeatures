/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package imagefeatures;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedList;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.imageio.ImageIO;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.UIManager;
import libsvm.svm_model;
import profileEntropyFeatures.PEFAlgorithm;
import profileEntropyFeatures.PEFImageFeatures;
import speededUpRobustFeatures.TopSurfAlgorithm;
import sun.awt.VerticalBagLayout;
import supportVectorMachines.*;
import supportVectorMachines.Kernels.*;
import utils.Serialization;

/**
 *
 * @author Mihai Pîțu
 */
public class FeaturesFrameTest extends JFrame {

    public FeaturesFrameTest(Map<Integer, LinkedList<svm_model>> models, 
            SvmScale svmScale,
            ImageFeaturesAlgorithm alg, 
            ArrayList<ImageFeatures> allImageFeatures, 
            Kernel k) {
        this.initComponents();
        this.svmScale = svmScale;
        this.models = models;
        this.alg = alg;
        this.allImageFeatures = allImageFeatures;
        this.k = k;
    }
    
    private SvmScale svmScale;
    private Map<Integer, LinkedList<svm_model>> models;
    private ImageFeaturesAlgorithm alg;
    private ArrayList<ImageFeatures> allImageFeatures;
    private Kernel k;
    
    private javax.swing.JFileChooser jFileChooser1;
    private JPanel panelTitle;
    private ImagePanel panelImage;
    private JPanel panelAnnotations;
    private javax.swing.JTextArea textAreaAnnotations;
    private JButton buttonChooseImage;
    private JLabel labelTitle;

    private void initComponents() {
        this.setPreferredSize(new Dimension(500, 400));
        this.jFileChooser1 = new javax.swing.JFileChooser();
        
        this.labelTitle = new JLabel("Image Features");
        labelTitle.setFont(new Font("Segoe UI", Font.PLAIN, 15));

        this.panelTitle = new JPanel();
        this.panelImage = new ImagePanel();
        this.panelAnnotations = new JPanel();
        this.panelAnnotations.setLayout(new VerticalBagLayout());

        this.panelTitle.add(this.labelTitle);

        buttonChooseImage = new JButton("Choose image");
        buttonChooseImage.setPreferredSize(new Dimension(50, 30));

        this.panelAnnotations.add(this.buttonChooseImage, BorderLayout.NORTH);

        this.setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);

        buttonChooseImage.addActionListener(new java.awt.event.ActionListener() {

            @Override
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonChooseImageActionPerformed(evt);
            }
        });

        JScrollPane jScrollPane1 = new JScrollPane();
        textAreaAnnotations = new javax.swing.JTextArea();
        textAreaAnnotations.setColumns(50);
        textAreaAnnotations.setRows(15);
        jScrollPane1.setViewportView(textAreaAnnotations);
        this.panelAnnotations.add(jScrollPane1, BorderLayout.CENTER);

        JPanel panel = new JPanel();
        panel.add(this.panelImage, BorderLayout.CENTER);
        this.getContentPane().setLayout(new VerticalBagLayout());
        this.getContentPane().add(this.panelTitle);
        this.getContentPane().add(panel, BorderLayout.CENTER);
        this.getContentPane().add(this.panelAnnotations);

        this.pack();
    }

    private void buttonChooseImageActionPerformed(java.awt.event.ActionEvent evt) {
        try {
            if (JFileChooser.APPROVE_OPTION == this.jFileChooser1.showOpenDialog(this)) {
                File file = this.jFileChooser1.getSelectedFile();
                //this.jLabel2.setText(file.getPath());
                this.labelTitle.setText(file.getPath());

                BufferedImage img = ImageIO.read(file);

                this.panelImage.setImage(img);

                ImageFeatures features = alg.getImageFeatures(file.getAbsolutePath(), null);

                SvmPrediction p = SvmPredict.predictBinaryPrecomputedKernel(allImageFeatures, features, models, k, svmScale, null);

                StringBuilder output = new StringBuilder();
                SvmEstimate[] entries = p.getSortedEstimates();

                for (int i = 0; i < 15; i++) {
                    output.append(i).append(".").append(Concepts.getConcept(entries[i].getLabel())).append(" => probability :").append(entries[i].getProbability()).append("\r\n");
                }
//                this.jLabel1.setText(output.toString());
                this.textAreaAnnotations.setText(output.toString());
            }
        } catch (Exception ex) {
            Logger.getLogger(FeaturesFrameTest.class.getName()).log(Level.SEVERE, null, ex);
            JOptionPane.showMessageDialog(this, ex.toString());
            ex.printStackTrace();
        }

    }

    
    public static void main(String args[]) throws Exception {
        final SvmScale svmScale;
        final Map<Integer, LinkedList<svm_model>> models;
        final ImageFeaturesAlgorithm alg;
        final ArrayList<ImageFeatures> allImageFeatures;
        final Kernel k;
        
        System.out.println("Loading concepts...");
        Concepts.loadConcepts("d:/faculta/licenta/Trainingset/", "concepts.txt");
        System.out.println("Loading scaling...");
        svmScale = null;//(SvmScale) Serialization.deserialize("imageFeaturesSvmScale.scale");
        System.out.println("Loading models...");
        allImageFeatures = (ArrayList<ImageFeatures>) Serialization.deserialize(ImageFeaturesTest.featuresTrainFile);
        //SvmTrain svmTrain = new SvmTrain(ImageFeaturesTest.print);
        //svmTrain.setC(10);
        
        models = (Map<Integer, LinkedList<svm_model>>) Serialization.deserialize("combinedModel");//svmTrain.trainBinary(allImageFeatures, k, null, 6);
        k = new CombinedKernel(new TopSurfAbsoluteKernel(), new RbfKernel(0.01), new UserTagKernel(3, allImageFeatures), new ColorMomentsKernel());

        alg = allImageFeatures.get(0).getProcessingAlgorithm();
        
        UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());

        java.awt.EventQueue.invokeLater(new Runnable() {

            @Override
            public void run() {
                new FeaturesFrameTest(models, svmScale, alg, allImageFeatures, k).setVisible(true);
            }
        });
    }

    class ImagePanel extends JPanel {

        private BufferedImage image;
        public static final int MAXWIDTH = 500;
        public static final int MAXHEIGHT = 500;

        public ImagePanel() {
        }

        public ImagePanel(BufferedImage displayImage) {
            this.image = displayImage;

        }

        @Override
        public void paint(Graphics g) {
            super.paint(g);
            Graphics2D g2d = (Graphics2D) g;
            if (image != null) {

                int w = image.getWidth();
                int h = image.getHeight();
                if (w > MAXWIDTH) {
                    int r = w / MAXWIDTH;
                    w = MAXWIDTH;
                    h = h / r;
                } else {
                    if (h > MAXHEIGHT) {
                        int r = h / MAXHEIGHT;
                        h = MAXHEIGHT;
                        w = w/ r;
                    }
                }
                
//                Graphics2D graphics2D = (Graphics2D) this.image.getGraphics();
//                AffineTransform xform = AffineTransform.getScaleInstance(w, h);
//                graphics2D.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
//                graphics2D.drawImage(image, xform, null);
                

                g2d.drawImage(this.image, 0, 0, w, h, null);
                this.setPreferredSize(new Dimension(w, h));
            }
        }

        /**
         * @return the image
         */
        public BufferedImage getImage() {
            return image;
        }

        /**
         * @param image the image to set
         */
        public void setImage(BufferedImage image) {
            this.image = image;
            this.repaint();
            int h = FeaturesFrameTest.this.panelTitle.getHeight() + FeaturesFrameTest.this.panelImage.getHeight() + FeaturesFrameTest.this.panelAnnotations.getHeight();
            FeaturesFrameTest.this.setSize(image.getWidth(), h);
            FeaturesFrameTest.this.setPreferredSize(new Dimension(image.getWidth(), h));
            //FeaturesFrameTest.this.pack();
        }
    }
}
