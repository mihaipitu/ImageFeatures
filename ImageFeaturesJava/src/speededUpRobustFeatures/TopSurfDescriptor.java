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
public class TopSurfDescriptor implements Serializable {

    public TopSurfDescriptor() {
        count = 0;
        visualwords = null;
        length = 0.0f;
    }

    public TopSurfDescriptor(TopSurfVisualword[] vw, float length) {
        this.count = vw.length;
        this.visualwords = vw;
        this.length = length;
    }

    public static float compareDescriptorsAbsolute(TopSurfDescriptor td1, TopSurfDescriptor td2) {
        float distance = 0.0f;
        final TopSurfVisualword[] vw1 = td1.getVisualwords();
        final TopSurfVisualword[] vw2 = td2.getVisualwords();
        int count1 = 0, count2 = 0;
        // calculate distance by using the absolute differences between the tf-idf scores
        while (count1 < td1.count) {
            if (count2 == td2.count) {
                distance += vw1[count1].getTf() * vw1[count1].getIdf();
                count1++;
            } else {
                // compare visual word identifier
                if (vw1[count1].getIdentifier() < vw2[count2].getIdentifier()) {
                    distance += vw1[count1].getTf() * vw1[count1].getIdf();
                    count1++;
                } else if (vw1[count1].getIdentifier() > vw2[count2].getIdentifier()) {
                    do {
                        distance += vw2[count2].getTf() * vw2[count2].getIdf();
                        count2++;
                        if (count2 == td2.count) {
                            break;
                        }
                    } while (vw1[count1].getIdentifier() > vw2[count2].getIdentifier());
                } else {
                    distance += Math.abs((vw1[count1].getTf() * vw1[count1].getIdf()) - (vw2[count2].getTf() * vw2[count2].getIdf()));
                    count1++;
                    count2++;
                }
            }
        }
        while (count2 != td2.count) {
            distance += vw2[count2].getTf() * vw2[count2].getIdf();
            count2++;
        }
        
        return distance;
    }

    public static float compareDescriptorsCosine(TopSurfDescriptor td1, TopSurfDescriptor td2) {
        if (td1.count == 0 || td2.count == 0) {
            return 0.0f;
        }
        // calculate distance by using the cosine normalized score of the descriptor differences
        float distance = 0.0f;
        //int count1 = 0, count2 = 0;
        int td1Index = 0;
        int td2Index = 0;
        while (td1Index < td1.count) {
            if (td2Index == td2.count) {
                break;
            }
            // compare visual word index
            if (td1.visualwords[td1Index].getIdentifier() < td2.visualwords[td2Index].getIdentifier()) {
                td1Index++;
            } else if (td1.visualwords[td1Index].getIdentifier() > td2.visualwords[td2Index].getIdentifier()) {
                do {
                    td2Index++;
                    if (td2Index == td2.count) {
                        break;
                    }
                } while (td1.visualwords[td1Index].getIdentifier() < td2.visualwords[td2Index].getIdentifier());
            } else {
                distance += td1.visualwords[td1Index].getTf() * td1.visualwords[td1Index].getIdf()
                        * td2.visualwords[td2Index].getTf() * td2.visualwords[td2Index].getIdf();
                td1Index++;
                td2Index++;
            }
        }

        distance /= (td1.length * td2.length); //length are floats, norm of the descriptor
        return distance;
    }
    
    
    // total number of visual words detected in the image
    private int count;
    // the visual words
    private TopSurfVisualword[] visualwords;
    // vector length of the descriptor, useful for fast cosine distance calculation
    private float length;

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

    /**
     * @return the length
     */
    public float getLength() {
        return length;
    }

    /**
     * @param length the length to set
     */
    public void setLength(float length) {
        this.length = length;
    }

    /**
     * @return the visualwords
     */
    public TopSurfVisualword[] getVisualwords() {
        return visualwords;
    }

    /**
     * @param visualwords the visualwords to set
     */
    public void setVisualwords(TopSurfVisualword[] visualwords) {
        this.visualwords = visualwords;
    }
}
