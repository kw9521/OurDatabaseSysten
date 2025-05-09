import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Record {
    private int size;
    private List<Object> data;
    private List<Byte> nullBitMap; // Tracks attribute null status

    public Record(int size, List<Object> data, List<Byte> nullBitMap) {
        this.data = new ArrayList<>(data); // Defensive copy
        this.nullBitMap = new ArrayList<>(nullBitMap); // Defensive copy
        this.size = size + nullBitMap.size();
    }

    public int addValue(Object value, int index, Attribute attr) {
        boolean isNull = (value == null);
        if (index >= this.data.size()) {
            // Pad with null values if the record is shorter than the schema
            for (int i = this.data.size(); i < index; i++) {
                this.data.add(null);
                this.nullBitMap.add((byte) 1);
            }
            this.data.add(value);
            this.nullBitMap.add((byte) (isNull ? 1 : 0));
        } else {
            this.data.set(index, value);
            this.nullBitMap.set(index, (byte) (isNull ? 1 : 0));
        }
    
        int sizeAdded = Byte.BYTES; // null bitmap size
        if (!isNull) {
            sizeAdded += getAttributeSize(value, attr);
        }
        this.size += sizeAdded;
        return sizeAdded;
    }

    public void setData(List<Object> data) {
        this.data = new ArrayList<>(data);
    }

    public void removeAttribute(int index) {
        if (index < 0 || index >= this.data.size()) {
            throw new IndexOutOfBoundsException("Invalid index: " + index);
        }
    
        // Remove the value and shift remaining values left
        this.data.remove(index);
        this.nullBitMap.remove(index);
    }
    
    
    public int removeValue(int index, Attribute attr) {
        if (index < 0 || index >= this.data.size()) {
            throw new IndexOutOfBoundsException("Invalid index: " + index);
        }

        Object value = this.data.remove(index);
        this.nullBitMap.remove(index);

        int sizeLost = Byte.BYTES; // 1 byte for null bitmap
        if (value != null) {
            sizeLost += getAttributeSize(value, attr);
        }

        this.size -= sizeLost;
        return sizeLost;
    }
    
    // Calculates the size of an attribute dynamically
    private int getAttributeSize(Object value, Attribute attr) {
        return attr.getType().equalsIgnoreCase("varchar") 
            ? ((String) value).length() + Integer.BYTES 
            : attr.getSize();
    }

    public void setBitMapValue(int index, int isNull) {
        if (index >= this.nullBitMap.size()) {
            this.nullBitMap.add((byte) isNull);
            this.size += Byte.BYTES;
        } else {
            this.nullBitMap.set(index, (byte) isNull);
        }
    }
    
    public List<Object> getData() {
        return this.data;
    }

    public int getSize() {
        return this.size;
    }

    public byte getBitMapValue(int index) {
        if (index < 0 || index >= nullBitMap.size()) {
            throw new IndexOutOfBoundsException("Index " + index + " is out of bounds for nullBitMap with size " + nullBitMap.size());
        }
        return nullBitMap.get(index);
    }

    public byte[] toBinary(Attribute[] attributes) {
        ByteBuffer recData = ByteBuffer.allocate(this.size);
        
        // Write null bitmap
        recData.put(getNullBitmapArray());

        int index = 0;
        for (Attribute attr : attributes) {
            if (getBitMapValue(index) == (byte) 1) {
                index++;
                continue;
            }

            Object value = this.data.get(index);
            writeAttributeToBuffer(recData, value, attr);
            index++;
        }
        return recData.array();
    }

    // Helper method to convert null bitmap list to byte array
    private byte[] getNullBitmapArray() {
        byte[] bitMap = new byte[nullBitMap.size()];
        for (int i = 0; i < nullBitMap.size(); i++) {
            bitMap[i] = nullBitMap.get(i);
        }
        return bitMap;
    }

    // Writes attribute value to ByteBuffer based on its type
    private void writeAttributeToBuffer(ByteBuffer buffer, Object value, Attribute attr) {
        switch (attr.getType().toLowerCase()) {
            case "varchar":
                String varcharValue = (String) value;
                buffer.putInt(varcharValue.length());
                buffer.put(varcharValue.getBytes());
                break;
            case "char":
                String charValue = (String) value;
                byte[] charBytes = charValue.getBytes();
                buffer.put(charBytes);
                break;
            case "integer":
                buffer.putInt((int) value);
                break;
            case "double":
                buffer.putDouble((double) value);
                break;
            case "boolean":
                buffer.put((byte) ((boolean) value ? 1 : 0));
                break;
            default:
                throw new IllegalArgumentException("Unsupported attribute type: " + attr.getType());
        }
    }

    public Object getAttributeValue(String attributeName, Attribute[] attributes) {
        String[] parts = attributeName.split("\\.");
        String AttributeName = parts.length > 1 ? parts[1] : parts[0];
    
        for (int i = 0; i < attributes.length; i++) {
            if (attributes[i].getName().equals(AttributeName)) {
                if (i < 0 || i >= data.size()) {
                    System.err.println("Error: Attribute index out of bounds. Attribute: " + AttributeName);
                    return null;
                }
                return data.get(i);
            }
        }
    
        System.err.println("Error: Attribute " + AttributeName + " not found in record.");
        return null;
    }
    
}
