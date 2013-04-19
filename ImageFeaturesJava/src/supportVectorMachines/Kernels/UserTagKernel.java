/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package supportVectorMachines.Kernels;

import com.memetix.mst.detect.Detect;
import com.memetix.mst.language.Language;
import com.memetix.mst.translate.Translate;
import imagefeatures.ImageFeatures;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;
import utils.Stemmer;

/**
 *
 * @author Mihai Pîțu
 */
public class UserTagKernel extends Kernel {

    static {
        Translate.setClientId("6c6a416b-2e75-4871-8163-bbd0d9e77a25");
        Translate.setClientSecret("Kue5tdP2glySsUE1NwYSH7lDffVdbqk2ezRG2XvJ2Pw=");
    }
    private HashSet<String> tagVocabulary = new HashSet<>();

    public UserTagKernel(ArrayList<ImageFeatures> allImageFeatures) {
        this(16, allImageFeatures);
    }

    public UserTagKernel(int minTagOccurrences, ArrayList<ImageFeatures> allImageFeatures) {

        HashMap<String, Integer> occurrences = new HashMap<>();

        for (ImageFeatures imageFeatures : allImageFeatures) {
            for (String usertag : imageFeatures.getUserTags()) {
                //usertag = stem(usertag);

                if (!occurrences.containsKey(usertag)) {
                    occurrences.put(usertag, 0);
                }
                occurrences.put(usertag, occurrences.get(usertag) + 1);
            }
        }

        for (Entry<String, Integer> entry : occurrences.entrySet()) {
            if (entry.getValue() >= minTagOccurrences && entry.getKey().length() >= 2) {
                tagVocabulary.add(entry.getKey());
            }
        }
    }

    public static String stem(String original) {
        original = original.trim().toLowerCase();
//        try {
//            Language lang = Detect.execute(original);
//            if (lang != Language.ENGLISH && lang != null) {
//                original = Translate.execute(original, lang, Language.ENGLISH).trim().toLowerCase();
//            }
//        } catch (Exception ex) {
//            Logger.getLogger(UserTagKernel.class.getName()).log(Level.SEVERE, null, ex);
//        }

        if (original.matches("[a-z]+")) {
            Stemmer s = new Stemmer();
            s.add(original.toCharArray(), original.length());
            s.stem();
            return s.toString();
        }

        return original;
    }

    @Override
    protected double compute(ImageFeatures if1, ImageFeatures if2) {
        HashSet<String> if1Tags = if1.getUserTags();
        HashSet<String> if2Tags = if2.getUserTags();

        if (if1Tags.size() > if2Tags.size()) {
            HashSet<String> _ = if1Tags;
            if1Tags = if2Tags;
            if2Tags = _;
        }

        int num = 0;

        int e = 0;
        for (String tag1 : if1Tags) {
            //tag1 = stem(tag1);
            if (this.tagVocabulary.contains(tag1)) {
                num++;
                if (if2Tags.contains(tag1)) {
                    e++;
                }
            }
        }

        if (num == 0) {
            return 0.0;
        }

        return (double) e / num;
    }
}
