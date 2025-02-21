// Individual entry, tuple, in a table.

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

public class Record {
    private int size; //Size in bytes of the record excluding additional information to be stored such as nullbitmap
    private ArrayList<Object> data;
    private ArrayList<Attribute> attributes;
    private ArrayList<Byte> nullBitMap; // Track attribute values

    public Record(ArrayList<Object> data, ArrayList<Attribute> attributes){
        this.data = data;
        this.attributes = attributes;

        int size = 0;
        for(int i = 0; i < attributes.size(); i++){
            if(attributes.get(i).getType() == "varchar"){
                size += String.valueOf(data.get(i)).length();
            }
            else{
                size += attributes.get(i).getSize();
            }
        }

        this.size = size;
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

    public byte getBitMapValue(int index) {
        if (index < 0 || index >= nullBitMap.size()) {
            throw new IndexOutOfBoundsException("Index " + index + " is out of bounds for nullBitMap with size " + nullBitMap.size());
        }
        return nullBitMap.get(index);
    }

    public byte[] toBinary(List<Attribute> attributes) {
        ByteBuffer recData = ByteBuffer.allocate(this.size);
        
        //Need to store the size of the null array to know how many bytes to read from file
        recData.putInt(nullBitMap.size());

        // Convert nullBitMap list to byte array
        byte[] bitMap = new byte[nullBitMap.size()];
        for (int i = 0; i < nullBitMap.size(); i++) {
            bitMap[i] = nullBitMap.get(i);
        }
        //Store null byte array
        recData.put(bitMap);

        int tupleIndex = 0;
        for (Attribute attr : attributes) {
            if (getBitMapValue(tupleIndex) == 1) {
                tupleIndex++;
                continue;
            }

            Object value = data.get(tupleIndex);
            tupleIndex++;
            
            switch (attr.getType().toLowerCase()) {
                case "varchar" -> {
                    byte[] varcharBytes = ((String) value).getBytes();
                    recData.putInt(varcharBytes.length);
                    recData.put(varcharBytes);
                }
                case "char" -> {
                    String paddedCharValue = String.format("%-" + attr.getSize() + "s", value);
                    recData.put(paddedCharValue.getBytes());
                }
                case "integer" -> recData.putInt((int) value);
                case "double" -> recData.putDouble((double) value);
                case "boolean" -> recData.put((byte) ((boolean) value ? 1 : 0));
            }
        }
        return recData.array();
    }
    
}
