// Individual entry, tuple, in a table.

import java.nio.ByteBuffer;
import java.util.ArrayList;

public class Record {
    private int size;
    private ArrayList<Object> data;

    public Record(int size, ArrayList<Object> data){
        this.size = size;
        this.data = data;
    }

    public void addValue(Object value){
        this.data.add(value);
        // Handle size of varChar
        this.size += 1;
    }

    public void removeValue(int index){
        this.data.remove(index);
        // Handle size of varChar
        this.size -= 1;
    }

    public ArrayList<Object> getData(){
        return this.data;
    }

    public int getSize(){
        return this.size;
    }

    public byte[] toBinary(){
        ByteBuffer data = ByteBuffer.allocate(this.size);
        // get sizes of each datatype, add to "data"
        // ?
        return data.array();
    }
}
