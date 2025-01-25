// Individual entry, tuple, in a table.

import java.util.ArrayList;

public class Record {
    private int size;
    private ArrayList<Object> data;

    public Record(int size, ArrayList<Object> data){
        this.size = size;
        this.data = data;
    }

    public void setData(ArrayList<Object> data){
        this.data = data;
    }

    public ArrayList<Object> getData(){
        return this.data;
    }

    public int getSize(){
        return this.size;
    }
}
