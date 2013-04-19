/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package imagefeatures;

import java.io.*;
import java.util.*;
import java.util.Map.Entry;
import supportVectorMachines.Kernels.*;

/**
 *
 * @author Mihai Pîțu
 */
public class Concepts {

    private static int n = 0;
    private static HashMap<String, Integer> stringLabels = new HashMap<>();
    private static HashMap<Integer, String> intLabels = new HashMap<>();
    private static HashMap<Integer, float[]> kernelSettings = new HashMap<>();
    private static HashMap<CombinedKernel, HashSet<Integer>> kernelGroupedConcepts;

    public static int addConcept(String concept) {
        getStringLabels().put(concept, n++);
        getIntLabels().put(n, concept);
        return n;
    }

    public static void addConcept(String concept, int index) {
        getStringLabels().put(concept, index);
        getIntLabels().put(index, concept);
    }

    public static int getConcept(String concept) {
        return stringLabels.get(concept);
    }

    public static String getConcept(int index) {
        return intLabels.get(index);
    }

    public static float[] getKernelSettings(int concept) {
        if (kernelSettings.containsKey(concept)) {
            return kernelSettings.get(concept);
        }
        return null;
    }

    private static boolean sameSettings(float[] a1, float[] a2) {
        if (a1 == null || a2 == null) {
            return false;
        }
        if (a1.length != a2.length) {
            return false;
        }
        for (int i = 0; i < a1.length; i++) {
            if (a1[i] != a2[i]) {
                return false;
            }
        }
        
        return true;
    }

    public static HashMap<CombinedKernel, HashSet<Integer>> getKernelGroupedConcepts() {
        if (kernelGroupedConcepts == null) {
            kernelGroupedConcepts = new HashMap<>();

            for (Entry<Integer, float[]> settingsEntry : kernelSettings.entrySet()) {
                boolean added = false;
                for (Entry<CombinedKernel, HashSet<Integer>> retEntry : kernelGroupedConcepts.entrySet()) {
                    if (sameSettings(settingsEntry.getValue(), retEntry.getKey().toArrayContributions())) {
                        retEntry.getValue().add(settingsEntry.getKey());
                        added = true;
                        break;
                    }
                }
                if (!added) {
                    CombinedKernel k = new CombinedKernel();
                    float[] values = settingsEntry.getValue();
                    k.fromArrayContributions(values);
                    kernelGroupedConcepts.put(k, new HashSet<Integer>());
                    kernelGroupedConcepts.get(k).add(settingsEntry.getKey());
                }
            }
        }
        
        return kernelGroupedConcepts;
    }

    /**
     * @return the stringLabels
     */
    public static HashMap<String, Integer> getStringLabels() {
        return stringLabels;
    }

    /**
     * @return the intLabels
     */
    public static HashMap<Integer, String> getIntLabels() {
        return intLabels;
    }
    private static String conceptsDirectory;

    public static void loadConcepts(String conceptsDir, String filename) throws IOException {
        DataInputStream in = null;
        conceptsDirectory = conceptsDir;

        try {
            FileInputStream fstream = new FileInputStream(conceptsDir + filename);
            in = new DataInputStream(fstream);
            final BufferedReader br = new BufferedReader(new InputStreamReader(in));

            String strLine;
            while ((strLine = br.readLine()) != null) {
                try {
                    String[] c = strLine.split("( |\t)+");

                    int index = Integer.parseInt(c[0]);
                    String concept = c[1];
                    Concepts.addConcept(concept, index);

                    if (c.length - 2 > 0) {
                        float[] settings = new float[c.length - 2];
                        for (int i = 2; i < c.length; i++) {
                            settings[i - 2] = Float.parseFloat(c[i]);
                        }
                        kernelSettings.put(index, settings);
                    }
                } catch (NumberFormatException ex) {
                }
            }
        } finally {
            in.close();
        }
    }

    public static HashMap<Integer, Integer> conceptOccurences(ArrayList<ImageFeatures> trainImages) {
        HashMap<Integer, Integer> occurences = new HashMap<>();
        for (ImageFeatures imf : trainImages) {
            for (int concept : imf.getAnnotations()) {
                if (!occurences.containsKey(concept)) {
                    occurences.put(concept, 0);
                }
                occurences.put(concept, occurences.get(concept) + 1);
            }
        }

        return occurences;
    }

    public static HashMap<Integer, HashMap<Integer, Integer>> conceptCombinationOccurrences(ArrayList<ImageFeatures> trainImages) {

        HashMap<Integer, HashMap<Integer, Integer>> combinations = new HashMap<>();

        for (int i = 0; i < trainImages.size(); i++) {
            int[] ann = trainImages.get(i).getAnnotations();
            for (int k = 0; k < ann.length; k++) {
                for (int l = k + 1; l < ann.length; l++) {
                    int concept1 = ann[k];
                    int concept2 = ann[l];
                    if (concept1 > concept2) {
                        concept1 = ann[l];
                        concept2 = ann[k];
                    }
                    if (!combinations.containsKey(concept1)) {
                        combinations.put(concept1, new HashMap<Integer, Integer>());
                    }
                    if (!combinations.get(concept1).containsKey(concept2)) {
                        combinations.get(concept1).put(concept2, 0);
                    }
                    int occurrences = combinations.get(concept1).get(concept2);
                    combinations.get(concept1).put(concept2, occurrences + 1);
                }
            }
        }

        for (Entry<Integer, HashMap<Integer, Integer>> entry : combinations.entrySet()) {
            for (int concept : intLabels.keySet()) {
                //if (concept > entry.getKey()) {
                if (!entry.getValue().containsKey(concept)) {
                    combinations.get(entry.getKey()).put(concept, 0);
                }
                //}
            }
        }


        return combinations;
    }

    public static HashMap<Integer, HashSet<Integer>> implicationConceptCombinationsOccurrencesThreshold(ArrayList<ImageFeatures> trainImages, double raport) {
        HashMap<Integer, HashSet<Integer>> result = new HashMap<>();
        HashMap<Integer, HashMap<Integer, Integer>> occurrences = conceptCombinationOccurrences(trainImages);
        HashMap<Integer, Integer> conceptOccurences = conceptOccurences(trainImages);

        int i = 0;
        for (Entry<Integer, HashMap<Integer, Integer>> entry : occurrences.entrySet()) {
            for (int concept : intLabels.keySet()) {
                if (entry.getValue().get(concept) > (raport * conceptOccurences.get(entry.getKey()))) {
                    if (!result.containsKey(entry.getKey())) {
                        result.put(entry.getKey(), new HashSet<Integer>());
                    }
                    result.get(entry.getKey()).add(concept);
                    //System.out.println("Implies" + (i++) + ": " + intLabels.get(entry.getKey()) + " -> " + intLabels.get(concept));
                } else if (entry.getValue().get(concept) > (raport * conceptOccurences.get(concept))) {
                    if (!result.containsKey(concept)) {
                        result.put(concept, new HashSet<Integer>());
                    }
                    result.get(concept).add(entry.getKey());
                    //System.out.println("Implies" + (i++) + ": " + intLabels.get(concept) + " -> " + intLabels.get(entry.getKey()));
                }
            }
        }

        return result;
    }

    public static boolean implicationConcepts(int ifConcept1, int thenConcept2, HashMap<Integer, HashSet<Integer>> implicationConceptCombinations) {
        if (implicationConceptCombinations.containsKey(ifConcept1)) {
            if (implicationConceptCombinations.get(ifConcept1).contains(thenConcept2)) {
                return true;
            }
        }
        return false;
    }

    public static HashMap<Integer, HashSet<Integer>> exclusiveConceptCombinationsOccurrencesThreshold(ArrayList<ImageFeatures> trainImages, int threshold) {
        HashMap<Integer, HashSet<Integer>> result = new HashMap<>();
        HashMap<Integer, HashMap<Integer, Integer>> occurrences = conceptCombinationOccurrences(trainImages);

        for (Entry<Integer, HashMap<Integer, Integer>> entry : occurrences.entrySet()) {
            for (int concept : intLabels.keySet()) {
                if (concept > entry.getKey()) {
                    if (entry.getValue().get(concept) < threshold) {
                        if (!result.containsKey(entry.getKey())) {
                            result.put(entry.getKey(), new HashSet<Integer>());
                        }
                        result.get(entry.getKey()).add(concept);
                        //System.out.println("Exclusive" + (i++) + ": " + intLabels.get(entry.getKey()) + " - " + intLabels.get(concept));
                    }
                }
            }
        }

        return result;
    }

    public static boolean exclusiveConcepts(int concept1, int concept2, HashMap<Integer, HashSet<Integer>> combinations) {
        if (concept1 < concept2) {
            if (combinations.containsKey(concept1)) {
                if (combinations.get(concept1).contains(concept2)) {
                    return true;
                }
            }
        } else {
            if (combinations.containsKey(concept2)) {
                if (combinations.get(concept2).contains(concept1)) {
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * @return the conceptsDirectory
     */
    public static String getConceptsDirectory() {
        return conceptsDirectory;
    }
}
