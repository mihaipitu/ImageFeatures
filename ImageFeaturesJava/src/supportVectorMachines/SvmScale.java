package supportVectorMachines;

import imagefeatures.ImageFeatures;
import java.io.*;
import java.util.*;
import libsvm.svm_node;

public class SvmScale implements Serializable {

    private String line = null;
    private double lower = -1.0;
    private double upper = 1.0;
    private double y_lower;
    private double y_upper;
    private boolean y_scaling = false;
    private double[] feature_max;
    private double[] feature_min;
    private double y_max = -Double.MAX_VALUE;
    private double y_min = Double.MAX_VALUE;
    private int max_index;
    private long num_nonzeros = 0;
    private long new_num_nonzeros = 0;

    private static void exit_with_help() {
        System.out.print(
                "Usage: svm-scale [options] data_filename\n"
                + "options:\n"
                + "-l lower : x scaling lower limit (default -1)\n"
                + "-u upper : x scaling upper limit (default +1)\n"
                + "-y y_lower y_upper : y scaling limits (default: no y scaling)\n"
                + "-s save_filename : save scaling parameters to save_filename\n"
                + "-r restore_filename : restore scaling parameters from restore_filename\n");
        System.exit(1);
    }

    private BufferedReader rewind(BufferedReader fp, String filename) throws IOException {
        fp.close();
        return new BufferedReader(new FileReader(filename));
    }

    private void output_target(double value) {
        if (y_scaling) {
            if (value == getY_min()) {
                value = getY_lower();
            } else if (value == getY_max()) {
                value = getY_upper();
            } else {
                value = getY_lower() + (getY_upper() - getY_lower())
                        * (value - getY_min()) / (getY_max() - getY_min());
            }
        }

        System.out.print(value + " ");
    }

    private double output(int index, double value) {
        /* skip single-valued attribute */
        if (getFeature_max()[index] == getFeature_min()[index]) {
            return 0;
        }

        if (value == getFeature_min()[index]) {
            value = getLower();
        } else if (value == getFeature_max()[index]) {
            value = getUpper();
        } else {
            value = getLower() + (getUpper() - getLower())
                    * (value - getFeature_min()[index])
                    / (getFeature_max()[index] - getFeature_min()[index]);
        }

        return value;
        /*
        if (value != 0) {
        //System.out.print(index + ":" + value + " ");
        new_num_nonzeros++;
        return value;
        }
         */
    }

    public void imageFeatureScale(imagefeatures.ImageFeatures imageFeatures) {
        final svm_node[] features = imageFeatures.getFeatures();

        for (int i = 0; i < features.length; i++) {
            features[i].value = (float) output(i, features[i].value);
        }
    }

    public void allImagesFeaturesScale(ArrayList<imagefeatures.ImageFeatures> allImageFeatures) {
        //ArrayList<imagefeatures.ImageFeatures> result = new ArrayList<>();
        lower = -1;
        upper = 1;
        svm_node[] firstFeatures = allImageFeatures.get(0).getFeatures();
        feature_min = new double[firstFeatures.length];
        feature_max = new double[firstFeatures.length];

        for (int i = 0; i < getFeature_max().length; i++) {
            feature_max[i] = feature_min[i] = firstFeatures[i].value;
        }

        for (imagefeatures.ImageFeatures imageFeatures : allImageFeatures) {
            svm_node[] features = imageFeatures.getFeatures();
            for (int i = 0; i < features.length; i++) {
                feature_max[i] = (feature_max[i] > features[i].value) ? feature_max[i] : features[i].value;
                feature_min[i] = (feature_min[i] < features[i].value) ? feature_min[i] : features[i].value;
            }
        }

        for (imagefeatures.ImageFeatures imageFeatures : allImageFeatures) {
            
            final svm_node[] features = imageFeatures.getFeatures();
            //final int[] annotations = imageFeatures.getAnnotations();
            //System.out.print(imageFeatures.getAnnotations()[0] + " ");

            for (int i = 0; i < features.length; i++) {
                features[i].value = (float) output(i, features[i].value);
            }

            //result.add(imageFeatures.partialClone(features, annotations));
        }

        //return result;
    }

    private String readline(BufferedReader fp) throws IOException {
        line = fp.readLine();
        return line;
    }

    public void run(String[] argv) throws IOException {
        int i, index;
        BufferedReader fp = null, fp_restore = null;
        String save_filename = null;
        String restore_filename = null;
        String data_filename = null;


        for (i = 0; i < argv.length; i++) {
            if (argv[i].charAt(0) != '-') {
                break;
            }
            ++i;
            switch (argv[i - 1].charAt(1)) {
                case 'l':
                    lower = Double.parseDouble(argv[i]);
                    break;
                case 'u':
                    upper = Double.parseDouble(argv[i]);
                    break;
                case 'y':
                    y_lower = Double.parseDouble(argv[i]);
                    ++i;
                    y_upper = Double.parseDouble(argv[i]);
                    y_scaling = true;
                    break;
                case 's':
                    save_filename = argv[i];
                    break;
                case 'r':
                    restore_filename = argv[i];
                    break;
                default:
                    System.err.println("unknown option");
                    exit_with_help();
            }
        }

        if (!(upper > lower) || (y_scaling && !(y_upper > y_lower))) {
            System.err.println("inconsistent lower/upper specification");
            System.exit(1);
        }
        if (restore_filename != null && save_filename != null) {
            System.err.println("cannot use -r and -s simultaneously");
            System.exit(1);
        }

        if (argv.length != i + 1) {
            exit_with_help();
        }

        data_filename = argv[i];
        try {
            fp = new BufferedReader(new FileReader(data_filename));
        } catch (Exception e) {
            System.err.println("can't open file " + data_filename);
            System.exit(1);
        }

        /* assumption: min index of attributes is 1 */
        /* pass 1: find out max index of attributes */
        max_index = 0;

        if (restore_filename != null) {
            int idx, c;

            try {
                fp_restore = new BufferedReader(new FileReader(restore_filename));
            } catch (Exception e) {
                System.err.println("can't open file " + restore_filename);
                System.exit(1);
            }
            if ((c = fp_restore.read()) == 'y') {
                fp_restore.readLine();
                fp_restore.readLine();
                fp_restore.readLine();
            }
            fp_restore.readLine();
            fp_restore.readLine();

            String restore_line = null;
            while ((restore_line = fp_restore.readLine()) != null) {
                StringTokenizer st2 = new StringTokenizer(restore_line);
                idx = Integer.parseInt(st2.nextToken());
                max_index = Math.max(getMax_index(), idx);
            }
            fp_restore = rewind(fp_restore, restore_filename);
        }

        while (readline(fp) != null) {
            StringTokenizer st = new StringTokenizer(line, " \t\n\r\f:");
            st.nextToken();
            while (st.hasMoreTokens()) {
                index = Integer.parseInt(st.nextToken());
                max_index = Math.max(getMax_index(), index);
                st.nextToken();
                num_nonzeros++;
            }
        }

        try {
            feature_max = new double[(getMax_index() + 1)];
            feature_min = new double[(getMax_index() + 1)];
        } catch (OutOfMemoryError e) {
            System.err.println("can't allocate enough memory");
            System.exit(1);
        }

        for (i = 0; i <= getMax_index(); i++) {
            feature_max[i] = -Double.MAX_VALUE;
            feature_min[i] = Double.MAX_VALUE;
        }

        fp = rewind(fp, data_filename);

        /* pass 2: find out min/max value */
        while (readline(fp) != null) {
            int next_index = 1;
            double target;
            double value;

            StringTokenizer st = new StringTokenizer(line, " \t\n\r\f:");
            target = Double.parseDouble(st.nextToken());
            y_max = Math.max(getY_max(), target);
            y_min = Math.min(getY_min(), target);

            while (st.hasMoreTokens()) {
                index = Integer.parseInt(st.nextToken());
                value = Double.parseDouble(st.nextToken());

                for (i = next_index; i < index; i++) {
                    feature_max[i] = Math.max(getFeature_max()[i], 0);
                    feature_min[i] = Math.min(getFeature_min()[i], 0);
                }

                feature_max[index] = Math.max(getFeature_max()[index], value);
                feature_min[index] = Math.min(getFeature_min()[index], value);
                next_index = index + 1;
            }

            for (i = next_index; i <= getMax_index(); i++) {
                feature_max[i] = Math.max(getFeature_max()[i], 0);
                feature_min[i] = Math.min(getFeature_min()[i], 0);
            }
        }

        fp = rewind(fp, data_filename);

        /* pass 2.5: save/restore feature_min/feature_max */
        if (restore_filename != null) {
            // fp_restore rewinded in finding max_index 
            int idx, c;
            double fmin, fmax;

            fp_restore.mark(2);				// for reset
            if ((c = fp_restore.read()) == 'y') {
                fp_restore.readLine();		// pass the '\n' after 'y'
                StringTokenizer st = new StringTokenizer(fp_restore.readLine());
                y_lower = Double.parseDouble(st.nextToken());
                y_upper = Double.parseDouble(st.nextToken());
                st = new StringTokenizer(fp_restore.readLine());
                y_min = Double.parseDouble(st.nextToken());
                y_max = Double.parseDouble(st.nextToken());
                y_scaling = true;
            } else {
                fp_restore.reset();
            }

            if (fp_restore.read() == 'x') {
                fp_restore.readLine();		// pass the '\n' after 'x'
                StringTokenizer st = new StringTokenizer(fp_restore.readLine());
                lower = Double.parseDouble(st.nextToken());
                upper = Double.parseDouble(st.nextToken());
                String restore_line = null;
                while ((restore_line = fp_restore.readLine()) != null) {
                    StringTokenizer st2 = new StringTokenizer(restore_line);
                    idx = Integer.parseInt(st2.nextToken());
                    fmin = Double.parseDouble(st2.nextToken());
                    fmax = Double.parseDouble(st2.nextToken());
                    if (idx <= getMax_index()) {
                        feature_min[idx] = fmin;
                        feature_max[idx] = fmax;
                    }
                }
            }
            fp_restore.close();
        }

        if (save_filename != null) {
            Formatter formatter = new Formatter(new StringBuilder());
            BufferedWriter fp_save = null;

            try {
                fp_save = new BufferedWriter(new FileWriter(save_filename));
            } catch (IOException e) {
                System.err.println("can't open file " + save_filename);
                System.exit(1);
            }

            if (y_scaling) {
                formatter.format("y\n");
                formatter.format("%.16g %.16g\n", getY_lower(), getY_upper());
                formatter.format("%.16g %.16g\n", getY_min(), getY_max());
            }
            formatter.format("x\n");
            formatter.format("%.16g %.16g\n", getLower(), getUpper());
            for (i = 1; i <= getMax_index(); i++) {
                if (getFeature_min()[i] != getFeature_max()[i]) {
                    formatter.format("%d %.16g %.16g\n", i, getFeature_min()[i], getFeature_max()[i]);
                }
            }
            fp_save.write(formatter.toString());
            fp_save.close();
        }

        /* pass 3: scale */
        while (readline(fp) != null) {
            int next_index = 1;
            double target;
            double value;

            StringTokenizer st = new StringTokenizer(line, " \t\n\r\f:");
            target = Double.parseDouble(st.nextToken());
            output_target(target);
            while (st.hasMoreElements()) {
                index = Integer.parseInt(st.nextToken());
                value = Double.parseDouble(st.nextToken());
                for (i = next_index; i < index; i++) {
                    output(i, 0);
                }
                output(index, value);
                next_index = index + 1;
            }

            for (i = next_index; i <= getMax_index(); i++) {
                output(i, 0);
            }
            System.out.print("\n");
        }
        if (new_num_nonzeros > num_nonzeros) {
            System.err.print(
                    "WARNING: original #nonzeros " + num_nonzeros + "\n"
                    + "         new      #nonzeros " + new_num_nonzeros + "\n"
                    + "Use -l 0 if many original feature values are zeros\n");
        }

        fp.close();
    }

    /**
     * @return the lower
     */
    public double getLower() {
        return lower;
    }

    /**
     * @return the upper
     */
    public double getUpper() {
        return upper;
    }

    /**
     * @return the y_lower
     */
    public double getY_lower() {
        return y_lower;
    }

    /**
     * @return the y_upper
     */
    public double getY_upper() {
        return y_upper;
    }

    /**
     * @return the feature_max
     */
    public double[] getFeature_max() {
        return feature_max;
    }

    /**
     * @return the feature_min
     */
    public double[] getFeature_min() {
        return feature_min;
    }

    /**
     * @return the y_max
     */
    public double getY_max() {
        return y_max;
    }

    /**
     * @return the y_min
     */
    public double getY_min() {
        return y_min;
    }

    /**
     * @return the max_index
     */
    public int getMax_index() {
        return max_index;
    }
}
