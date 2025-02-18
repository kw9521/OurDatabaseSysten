// Individual entry, tuple, in a table.

import java.nio.ByteBuffer;
import java.util.ArrayList;

public class Record {
    private int size;
    private ArrayList<Object> data;
    private ArrayList<Byte> nullBitMap; // Track attribute values

    public Record(int size, ArrayList<Object> data){
        this.size = size;
        this.data = data;
    }

    public int addValue(Object value, int indexInRecord, Attribute attr) {
        boolean isNull = (value == null);
        this.setBitMapValue(indexInRecord, isNull ? 1 : 0);
    
        int sizeAdded = 1;
        if (!isNull) {
            sizeAdded += getAttributeSize(value, attr);
        }
    
        this.data.add(value);
        this.size += sizeAdded;
        return sizeAdded;
    }
    
    public int removeValue(int indexInRecord, Attribute attr) {
        if (indexInRecord < 0 || indexInRecord >= this.data.size()) {
            throw new IndexOutOfBoundsException("Invalid index: " + indexInRecord);
        }
    
        Object value = this.data.remove(indexInRecord);
        this.nullBitMap.remove(indexInRecord);
    
        int sizeLost = 1;
        if (value != null) {
            sizeLost += getAttributeSize(value, attr);
        }
    
        this.size -= sizeLost;
        return sizeLost;
    }
    
    // Helper method to calculate attribute size dynamically
    private int getAttributeSize(Object value, Attribute attr) {
        return attr.getType().equals("varchar") ? ((String) value).length() + Integer.BYTES : attr.getSize();
    }

    public void setBitMapValue(int index, int isNull) {
        if (index >= this.nullBitMap.size()) {
            this.nullBitMap.add((byte)isNull);
            this.size++;
        }
        else this.nullBitMap.set(index, (byte)isNull);
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
