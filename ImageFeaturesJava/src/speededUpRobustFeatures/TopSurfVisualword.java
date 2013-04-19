/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package speededUpRobustFeatures;

import java.io.Serializable;

/**
 *
 * @author Mihai Pîțu
 */
public class TopSurfVisualword implements Serializable, Comparable<TopSurfVisualword> {

    public TopSurfVisualword() {
        identifier = -1;
        tf = 0.0f;
        idf = 0.0f;
        count = 0;
    }
    
    @Override
    public int compareTo(TopSurfVisualword o) {
        //on the same line
        if (this.getY() == o.getY()) {
            if (this.getX() > o.getX()) {
                return 1;
            }
            if (this.getX() < o.getX()) {
                return -1;
            }
            return 0;
        }

        if (this.getY() > o.getY()) {
            return 1;
        }

        return -1;
    }

    public TopSurfVisualword(int id, float tf, float idf, int count, float x, float y, float orientation, float scale) {
        this.identifier = id;
        this.tf = tf;
        this.idf = idf;
        this.count = count;
        this.x = x;
        this.y = y;
        this.orientation = orientation;
        this.scale = scale;
    }
    // visual word identifier
    private int identifier;
    // term frequency is the number of times a term occurs in a document
    private float tf;
    // inverse document frequency is a measure of whether the term is common or rare across all documents
    private float idf;
    // number of visual words detected
    private int count;
    // the locations of the detected visual words
    private float x;
    private float y;
    private float orientation;
    private float scale;

    /**
     * @return the x
     */
    public float getX() {
        return x;
    }

    /**
     * @param x the x to set
     */
    public void setX(float x) {
        this.x = x;
    }

    /**
     * @return the y
     */
    public float getY() {
        return y;
    }

    /**
     * @param y the y to set
     */
    public void setY(float y) {
        this.y = y;
    }

    /**
     * @return the orientation
     */
    public float getOrientation() {
        return orientation;
    }

    /**
     * @param orientation the orientation to set
     */
    public void setOrientation(float orientation) {
        this.orientation = orientation;
    }

    /**
     * @return the scale
     */
    public float getScale() {
        return scale;
    }

    /**
     * @param scale the scale to set
     */
    public void setScale(float scale) {
        this.scale = scale;
    }

    // compare two elements by their identifiers
    // Note: calling this during sorting will result in the elements
    //       being sorted from low to high
    static public boolean VWCompare(TopSurfVisualword vw1, TopSurfVisualword vw2) {
        if (vw1.getIdentifier() < vw2.getIdentifier()) {
            return true;
        }
        return false;
    }
    // compare two elements by their tf scores
    // Note: calling this during sorting will result in the elements
    //       being sorted from high to low

    static boolean TFCompare(TopSurfVisualword vw1, TopSurfVisualword vw2) {
        if (vw1.getTf() > vw2.getTf()) {
            return true;
        }
        return false;
    }
    // compare two elements by their tf-idf scores
    // Note: calling this during sorting will result in the elements
    //       being sorted from high to low

    static boolean TFIDFCompare(TopSurfVisualword vw1, TopSurfVisualword vw2) {
        if (vw1.getTf() * vw1.getIdf() > vw2.getTf() * vw2.getIdf()) {
            return true;
        }
        return false;
    }

    /**
     * @return the identifier
     */
    public int getIdentifier() {
        return identifier;
    }

    /**
     * @param identifier the identifier to set
     */
    public void setIdentifier(int identifier) {
        this.identifier = identifier;
    }

    /**
     * @return the tf
     */
    public float getTf() {
        return tf;
    }

    /**
     * @param tf the tf to set
     */
    public void setTf(float tf) {
        this.tf = tf;
    }

    /**
     * @return the idf
     */
    public float getIdf() {
        return idf;
    }

    /**
     * @param idf the idf to set
     */
    public void setIdf(float idf) {
        this.idf = idf;
    }

    /**
     * @return the count
     */
    public int getCount() {
        return count;
    }

    /**
     * @param count the count to set
     */
    public void setCount(int count) {
        this.count = count;
    }

}
