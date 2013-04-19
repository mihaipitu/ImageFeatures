/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package HarrisLaplaceFeatures;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Ellipse2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import javax.imageio.ImageIO;

public class HarrisLaplaceTest {

    public static void main(String args[]) {


        BufferedImage img = null;
        HarrisLaplace hl = null;
        String filepath = "D:\\";
        String filename = "oil.jpg";
        int i, j;


        try {
            img = ImageIO.read(new File(filepath + filename));
        } catch (IOException e) {
            e.printStackTrace();
        }

        int width = img.getWidth(); 	// largeur de l'image
        int height = img.getHeight(); 	// hauteur de l'image
        int[][] input = new int[width][height];		// tableau 2D [x][y] contenant l'image en niveau de gris (0-255)


        for (i = 0; i < width - 1; i++) {
            for (j = 0; j < height - 1; j++) {
                input[i][j] = rgb2gray(img.getRGB(i, j));
            }
        }

        //////////////////////

        hl = new HarrisLaplace();
        System.out.println("huh");
        hl.detect(input, width, height);


        //////////////////////

        Graphics2D g2d = img.createGraphics();
        g2d.setColor(Color.RED);

        for (InterestCorner interestcorner : hl.interestcorners) {
            if (interestcorner.sigma > 100) {
                g2d.draw(new Ellipse2D.Float(interestcorner.x - 5, interestcorner.y - 5, Math.round(2 * Math.sqrt(interestcorner.sigma + 1)), Math.round(2 * Math.sqrt(interestcorner.sigma + 1))));
            }
        }
        System.out.println(hl.interestcorners.size());
        g2d.dispose();

        //////////////////////

        FileOutputStream fos;
        try {
            fos = new FileOutputStream(filepath + "out_" + filename);
            ImageIO.write(img, "jpg", fos);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }


    }

    static int rgb2gray(int srgb) {
        int r = (srgb >> 16) & 0xFF;
        int g = (srgb >> 8) & 0xFF;
        int b = srgb & 0xFF;
        return (int) (0.299 * r + 0.587 * g + 0.114 * b);
    }
}