/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package HarrisLaplaceFeatures;

//Pyramid Level structure
public class PyramidLevel {
    // size of the data

    public int width, height;
    // the values
    public float[][] data;
    // the scale value (relative to original image)
    public double sigma2;
    // constructor allocating a new array

    public PyramidLevel(int width, int height, double sigma2) {
        this.width = width;
        this.height = height;
        this.sigma2 = sigma2;
        this.data = new float[width][height];
    }
    // return the data at position(x,y)

    public float getData(int x, int y) {
        if (x < 0) {
            x = 0;
        } else if (x >= this.width) {
            x = this.width - 1;
        }
        if (y < 0) {
            y = 0;
        } else if (y >= this.height) {
            y = this.height - 1;
        }
        return this.data[x][y];
    }
}