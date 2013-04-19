/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package supportVectorMachines.Kernels;

import imagefeatures.CombinedImageFeatures;
import imagefeatures.ImageFeatures;
import java.security.InvalidParameterException;

/**
 *
 * @author Mihai Pîțu
 */
public class CombinedKernel extends Kernel {
    private TopSurfAbsoluteKernel surfKernel;
    private RbfKernel pefRbfKernel;
    private UserTagKernel userTagKernel;
    private ColorMomentsKernel cmKernel;
    private float topSurfContribution = 1.0f;
    private float pefContribution = 0.0f;
    private float userTagContribution = 0.0f;
    private float colorMomentsContribution = 0.00f;
    
    public CombinedKernel() {
    }
    
    public CombinedKernel(TopSurfAbsoluteKernel surfKernel, RbfKernel pefRbfKernel, UserTagKernel userTagKernel, ColorMomentsKernel cmKernel) {
        this.surfKernel = surfKernel;
        this.pefRbfKernel = pefRbfKernel;
        this.userTagKernel = userTagKernel;
        this.cmKernel = cmKernel;
    }
    
    public void setContributions(float topSurfContribution, float pefContribution, float userTagContribution, float cmContribution) throws InvalidParameterException {
//        if (topSurfContribution + pefContribution != 1.0d)
//            throw new InvalidParameterException("The parameters must sum up to 1.");
        this.topSurfContribution = topSurfContribution;
        this.pefContribution = pefContribution;
        this.userTagContribution = userTagContribution;
        this.colorMomentsContribution = cmContribution;
    }
    
    @Override
    protected double compute(ImageFeatures if1, ImageFeatures if2) {
        CombinedImageFeatures cif1 = (CombinedImageFeatures) if1;
        CombinedImageFeatures cif2 = (CombinedImageFeatures) if2;
        
//        double surfK = surfKernel.compute(cif1.getTsImageFeatures(), cif2.getTsImageFeatures());
//        double pefK = rbfKernel.compute(cif1.getPefImageFeatures(), cif2.getPefImageFeatures());
//        double userK = userTagKernel.compute(if1, if2);
        
        double r = (topSurfContribution * surfKernel.compute(cif1.getTsImageFeatures(), cif2.getTsImageFeatures()) +
                    pefContribution * pefRbfKernel.compute(cif1.getPefImageFeatures(), cif2.getPefImageFeatures()) +
                    userTagContribution * userTagKernel.compute(if1, if2) +
                    colorMomentsContribution * cmKernel.compute(cif1.getColormImageFeatures(), cif2.getColormImageFeatures()));
        
        return r > 1 ? 1 : r;
    }
    
    public float[] toArrayContributions() {
        return new float[] {topSurfContribution, pefContribution, userTagContribution, colorMomentsContribution};
    }
    
    public void fromArrayContributions(float[] values) {
        if (values.length >= 1)
            this.topSurfContribution = values[0];
        if (values.length >= 2)
            this.pefContribution = values[1];
        if (values.length >= 3)
            this.userTagContribution = values[2];
        if (values.length >= 4)
            this.colorMomentsContribution = values[3];
    }
    
    @Override
    public String toString() {
        return "Combined kernel parameters:\n" +
                "TopSurf contribution: " + this.topSurfContribution + "\n" + 
                "Pef contribution: " + this.pefContribution + "\n" + 
                "Usertags contribution: " + this.userTagContribution + "\n" +
                "Color mom contribution: " + this.colorMomentsContribution + "\n" +
                "Rbf gamma: " + this.getRbfKernel().getGamma() + "\n"
               ;
    }

    /**
     * @return the surfKernel
     */
    public TopSurfAbsoluteKernel getSurfKernel() {
        return surfKernel;
    }

    /**
     * @param surfKernel the surfKernel to set
     */
    public void setSurfKernel(TopSurfAbsoluteKernel surfKernel) {
        this.surfKernel = surfKernel;
    }

    /**
     * @return the rbfKernel
     */
    public RbfKernel getRbfKernel() {
        return pefRbfKernel;
    }

    /**
     * @param rbfKernel the rbfKernel to set
     */
    public void setRbfKernel(RbfKernel rbfKernel) {
        this.pefRbfKernel = rbfKernel;
    }

    /**
     * @return the userTagKernel
     */
    public UserTagKernel getUserTagKernel() {
        return userTagKernel;
    }

    /**
     * @param userTagKernel the userTagKernel to set
     */
    public void setUserTagKernel(UserTagKernel userTagKernel) {
        this.userTagKernel = userTagKernel;
    }
    
    
    /**
     * @return the cmKernel
     */
    public ColorMomentsKernel getColorMomentsKernel() {
        return cmKernel;
    }

    /**
     * @param cmKernel the userTagKernel to set
     */
    public void setColorMomentsKernel(ColorMomentsKernel cmKernel) {
        this.cmKernel = cmKernel;
    }
}
