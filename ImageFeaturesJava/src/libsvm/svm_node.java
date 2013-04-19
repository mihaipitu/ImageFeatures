package libsvm;

public class svm_node implements java.io.Serializable {

    public int index;
    public float value;

    public svm_node() {
    }
    
    public svm_node(int index, float value) {
        this.index = index;
        this.value = value;
    }

        
    @Override
    public String toString() {
        return this.index + ":" + this.value;
    }
}
