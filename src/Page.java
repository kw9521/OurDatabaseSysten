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

    public Page(int pageId, int tableId, boolean updated){
        this.pageId = pageId;
        this.tableId = tableId;
        this.records = new ArrayList<>();
        this.recordCount = 0;
        this.updated = updated;
        this.nextPageID = 0; //IMPLEMENT
    }

    public void addRecord(Record record){
        this.records.add(record);
        this.recordCount++;
        this.size += record.getSize();
        this.updated = true;
    }

    public void deleteRecord(Record record, int index){
        this.records.remove(index);
        this.recordCount--;
        this.size -= record.getSize();
        this.updated = true;
    }

    public List<Record> getRecords(){
        return records;
    }

    public void setRecords(List<Record> records){
        this.records = records;
        this.updated = true;
    }

    public int getRecordCount(){
        return recordCount;
    }

    public void setRecordCount(int recordCount){
        this.recordCount = recordCount;
    }

    public int getPageId(){
        return pageId;
    }

    public void setPageId(int pageId){
        this.pageId = pageId;
    }

    public int getTableId(){
        return tableId;
    }

    public int getSize(){
        return this.size;
    }

    public void setSize(int size){
        this.size = size;
    }

    public boolean isUpdated() {
        return updated;
    }

    public boolean isOverfull(){
        return getSize() > Main.getPageSize();
    }

    public byte[] toBinary(Table table) {
        ByteBuffer buffer = ByteBuffer.allocate(Main.getPageSize());
        buffer.putInt(getRecordCount());

        for (Record record : records) {
            byte[] recordBytes = record.toBinary(table.getAttributes());
            buffer.put(recordBytes);
        }
        buffer.putInt(this.nextPageID);
        return buffer.array();
    }
}


// Possible datatypes:
// - Integer
// – Double
// – Boolean
// – Char(N)
// – Varchar(N) (up to N chars)