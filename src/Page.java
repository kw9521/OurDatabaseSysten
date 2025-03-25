// Tables are made up of pages.
// Pages have a fixed size.
// Pages contain multiple data rows
// Tables can span multiple pages
// Pages can have different number of records based on size of each record
// Varchars cause records to have different sizes

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

public class Page {
    private int size;
    private int pageId;
    private int tableId;
    private List<Record> records;
    private int recordCount;
    private boolean updated;
    private int nextPageID;

    public Page(int pageId, int tableId, boolean updated) {
        this.pageId = pageId;
        this.tableId = tableId;
        this.records = new ArrayList<>();
        this.recordCount = 0;
        this.updated = updated;
        this.nextPageID = 0; //IMPLEMENT
    }

    public void addRecord(Record record) {
        this.records.add(record);
        this.recordCount++;
        this.size += record.getSize();
        this.updated = true;
    }

    public void deleteRecord(Record record, int index) {
        this.records.remove(index);
        this.recordCount--;
        this.size -= record.getSize();
        this.updated = true;
    }

    public void shiftRecordsAndAdd(Record rec, int startingIndex) {
        this.records.add(rec);
        for (int i = this.getRecordCount(); i > startingIndex; i--) {
            this.records.set(i, records.get(i - 1));
        }
        this.records.set(startingIndex, rec);
        this.recordCount++;
        this.size += rec.getSize();
        this.updated = true;
    }

    public List<Record> getRecords() {
        return records;
    }

    public void setRecords(List<Record> records) {
        this.records = records;
        this.updated = true;
    }

    public int getRecordCount() {
        return recordCount;
    }

    public void setRecordCount(int recordCount) {
        this.recordCount = recordCount;
    }

    public int getPageId() {
        return pageId;
    }

    public void setPageId(int pageId) {
        this.pageId = pageId;
    }

    public int getTableId() {
        return tableId;
    }

    public int getSize() {
        return this.size;
    }

    public void setSize(int size) {
        this.size = size;
    }

    public boolean isUpdated() {
        return updated;
    }

    public void setUpdated(boolean updated) {
        this.updated = updated;
    }

    public boolean isOverfull() {
        return getSize() > Main.getPageSize();
    }

    public byte[] toBinary(Table table) {
        ByteBuffer buffer = ByteBuffer.allocate(Main.getPageSize() + 8);
        buffer.putInt(getRecordCount());

        for (Record record : records) {
            byte[] recordBytes = record.toBinary(table.getAttributes());
            buffer.put(recordBytes);
        }
        buffer.putInt(this.nextPageID);
        return buffer.array();
    }

    // Converts binary data into a Page with records
    public static Page fromBinary(byte[] data, int tableNumber, int pageNumber, Catalog catalog) {
        ByteBuffer buffer = ByteBuffer.wrap(data);
        Table table = catalog.getTable(tableNumber);
        Attribute[] attributes = table.getAttributes();
        int numRecords = buffer.getInt(); // First 4 bytes for the number of records

        Page page = new Page(pageNumber, tableNumber, false);

        // Populate page with records from binary data
        for (int i = 0; i < numRecords; i++) {
            int recordSize = 0;
            List<Object> attrValues = new ArrayList<>(table.getAttributesCount());
            List<Byte> nullBitMap = readNullBitmap(buffer, table.getAttributesCount());

            for (int j = 0; j < table.getAttributesCount(); j++) {
                if (nullBitMap.get(j) == (byte) 1) {
                    attrValues.add(null);
                    continue;
                }

                Attribute attr = attributes[j];
                Object parsedValue = parseAttributeValue(buffer, attr);
                recordSize += getAttributeSize(parsedValue, attr);
                attrValues.add(parsedValue);
            }

            Record record = new Record(recordSize, attrValues, nullBitMap);
            page.addRecord(record);
            page.size += recordSize;
        }

        return page;
    }

    // Reads the null bitmap from the ByteBuffer
    private static List<Byte> readNullBitmap(ByteBuffer buffer, int attributeCount) {
        byte[] bitMap = new byte[attributeCount];
        buffer.get(bitMap); // Read null bitmap directly
        List<Byte> nullBitMap = new ArrayList<>();
        for (byte b : bitMap) {
            nullBitMap.add(b);
        }
        return nullBitMap;
    }

    // Parses an attribute value from the ByteBuffer based on its type
    private static Object parseAttributeValue(ByteBuffer buffer, Attribute attr) {
        switch (attr.getType().toLowerCase()) {
            case "varchar":
                int strLength = buffer.getInt();
                byte[] strBytes = new byte[strLength];
                buffer.get(strBytes);
                return new String(strBytes);
            case "char":
                byte[] charBytes = new byte[attr.getSize()];
                buffer.get(charBytes);
                return new String(charBytes);
            case "integer":
                return buffer.getInt();
            case "double":
                return buffer.getDouble();
            case "boolean":
                return buffer.get() == 1;
            default:
                throw new IllegalArgumentException("Unsupported attribute type: " + attr.getType());
        }
    }

    // Determines the size of an attribute value in bytes
    private static int getAttributeSize(Object value, Attribute attr) {
        if (value == null)
            return 0;

        switch (attr.getType().toLowerCase()) {
            case "varchar":
                return ((String) value).length() + Integer.BYTES;
            case "char":
                return attr.getSize();
            case "integer":
                return Integer.BYTES;
            case "double":
                return Double.BYTES;
            case "boolean":
                return Byte.BYTES;
            default:
                return 0;
        }
    }

}

// Possible datatypes:
// - Integer
// – Double
// – Boolean
// – Char(N)
// – Varchar(N) (up to N chars)