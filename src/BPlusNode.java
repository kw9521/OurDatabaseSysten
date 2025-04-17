import java.util.ArrayList;
import java.util.List;
  
  // Abstract Node class
  public abstract class BPlusNode {
    protected List<Integer> keys; // Primary keys

    public BPlusNode() {
        this.keys = new ArrayList<>();
    }

    abstract BPlusNode insert(int key, RecordPointer pointer);
    abstract RecordPointer search(int key);
}