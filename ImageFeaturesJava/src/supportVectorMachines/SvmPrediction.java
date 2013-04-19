/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package supportVectorMachines;

import imagefeatures.Concepts;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.util.*;
import java.util.AbstractMap.SimpleEntry;
import java.util.Map.Entry;
import sun.misc.Compare;
import sun.misc.Sort;
/**
 *
 * @author Mihai Pîțu
 */
public class SvmPrediction {

    private Map<Integer, SvmEstimate> estimates = new HashMap<>();
    
    public float getEstimate(int label) {
        return this.estimates.get(label).getProbability();
    }
    
    public boolean getActual(int label) {
        return this.estimates.get(label).getActual();
    }
    
    public boolean getBinaryScore(int label) {
        return this.estimates.get(label).getBinaryScore();
    }
    
    public void setEstimate(int label, float estimate) {
        this.estimates.put(label, new SvmEstimate(label, estimate));
    }

    public void setEstimate(int label, float estimate, boolean actual) {
        this.estimates.put(label, new SvmEstimate(label, estimate, actual));
    }
    
    public void setActual(int label, boolean actual) {
        this.estimates.get(label).setActual(actual);
    }
    
    public SvmEstimate getEstimateObj(int label) {
        return this.estimates.get(label);
    }
    
    /**
     * @return the estimates
     */
    public Collection<SvmEstimate> getEstimates() {
        return estimates.values();
    }
    
    public SvmEstimate[] getSortedEstimates() {

        Collection<SvmEstimate> c = this.estimates.values();
        SvmEstimate[] sortedEstimates = new SvmEstimate[c.size()];
        c.toArray(sortedEstimates);
        Arrays.sort(sortedEstimates, sortedEstimates[0]);
        
        return sortedEstimates;
        
//        ArrayList<Entry<String, Double>> result = new ArrayList<>(this.labels.length);
//        Set<Entry<Integer, Double>> entries = getEstimates().entrySet();
//        Object[] arrayEntries = entries.toArray();
//
//        Compare compare = new Compare() {
//
//            @Override
//            public int doCompare(Object o, Object o1) {
//                Entry<Integer, Double> entry1 = (Entry<Integer, Double>) o;
//                Entry<Integer, Double> entry2 = (Entry<Integer, Double>) o1;
//                if (entry1.getValue() == entry2.getValue()) {
//                    return 0;
//                }
//                if (entry1.getValue() > entry2.getValue()) {
//                    return -1;
//                }
//                return 1;
//            }
//        };
//        
//        Sort.quicksort(arrayEntries, compare);
//
//        for (int i = 0; i < arrayEntries.length; i++) {
//            Entry<Integer, Double> entry = (Entry<Integer, Double>) arrayEntries[i];
//            String key = "(" + entry.getKey() + ") ";
//
//            for (Entry<String, Integer> e : stringLabels.entrySet()) {
//                if (e.getValue().equals(entry.getKey())) {
//                    key += e.getKey();
//                    break;
//                }
//            }
//
//            Entry<String, Double> r = new SimpleEntry<>(key, entry.getValue());
//            result.add(r);
//        }
//
//        return result;
    }
    
    public String toCsv(char lineSeparator) {
        StringBuilder sb = new StringBuilder();
        SvmEstimate[] sortedEstimates =  this.getSortedEstimates();
        
        for (int i = 0; i < sortedEstimates.length; i++) {
            sb.append(Concepts.getConcept(sortedEstimates[i].getLabel())).append(',');
            sb.append(sortedEstimates[i].getProbability()).append(',');
            sb.append(sortedEstimates[i].getBinaryScore()).append(lineSeparator);
        }
        
        return sb.toString();
    }
    
    public String toXml() {
        NumberFormat nf = NumberFormat.getInstance();
        nf.setMaximumFractionDigits(6);
        nf.setMinimumFractionDigits(6);
        
        StringBuilder sb = new StringBuilder();
        SvmEstimate[] sortedEstimates =  this.getSortedEstimates();
        sb.append("<?xml version=\"1.0\" encoding=\"utf-8\" ?>");
        sb.append("<predictions conceptsDir=\"").append(Concepts.getConceptsDirectory()).append("\">");
        for (int i = 0; i < sortedEstimates.length; i++) {
            sb.append("<prediction concept=\"").append(Concepts.getConcept(sortedEstimates[i].getLabel()));
            sb.append("\" rank=\"").append(i).append("\">");
            sb.append("<probability>").append(nf.format(sortedEstimates[i].getProbability())).append("</probability>");
            sb.append("<binary>").append(sortedEstimates[i].getBinaryScore()).append("</binary>");
            sb.append("</prediction>");
        }
        
        sb.append("</predictions>");
        return sb.toString();
    }
   
}
