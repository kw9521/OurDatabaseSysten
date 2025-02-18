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
    private ByteBuffer buffer;
    private List<Record> records;
    private int recordCount;

    public Page(int pageId, int tableId){
        this.pageId = pageId;
        this.tableId = tableId;
        this.records = new ArrayList<>();
        this.recordCount = 0;
    }

    public void addRecord(Record record){
        this.records.add(record);
        this.recordCount++;
        this.size += record.getSize();
    }

    public void deleteRecord(Record record, int index){
        this.records.remove(index);
        this.recordCount--;
        this.size -= record.getSize();
    }

    public List<Record> getRecords(){
        return records;
    }

    public void setRecords(List<Record> records){
        this.records = records;
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

    public boolean isOverfull(){
        return getSize() > Main.getPageSize();
    }

    public byte[] toBinary(){
        ByteBuffer data = ByteBuffer.allocate(Main.getPageSize());
        data.putInt(getRecordCount());

        for (Record record : records){
            //record.toBinary is broken
            byte[] recordBytes = record.toBinary();
            buffer.put(recordBytes);
        }
        return buffer.array();
    }
}


// Possible datatypes:
// - Integer
// – Double
// – Boolean
// – Char(N)
// – Varchar(N) (up to N chars)