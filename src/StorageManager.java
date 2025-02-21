import java.util.ArrayList;
import java.util.List;
import java.util.OptionalInt;
import java.util.stream.IntStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;

public class StorageManager {
    private Catalog catalog;
    private PageBuffer buffer;

    public StorageManager(Catalog catalog, PageBuffer buffer) {
        this.catalog = catalog;
        this.buffer = buffer;
    }

    // Returns a list of tuples (each tuple is a list of objects)
    public List<List<Object>> getRecords(int tableNumber) {
        List<Page> pages = loadPages(tableNumber);
        List<List<Object>> tuples = new ArrayList<>();

        for (Page page : pages) {
            for (Record record : page.getRecords()) {
                tuples.add(record.getData());
            }
        }
        return tuples;
    }

    // Returns all pages for a given table number
    public List<Page> getPages(int tableNumber) {
        return loadPages(tableNumber);
    }

    // Retrieves a page: checks buffer first, then loads from disk if missing
    public Page getPage(int tableNumber, int pageNumber) {
        Page page = buffer.getPage(tableNumber, pageNumber);

        if (page == null) { // Page not in buffer, load from disk
            page = loadPageFromDisk(tableNumber, pageNumber);
        }

        return page;
    }

    private Page loadPageFromDisk(int tableNumber, int pageNumber) {
        String fileName = Main.getDBLocation() + "/tables/" + tableNumber + ".bin";
        Table table = catalog.getTable(tableNumber);
        int[] pageLocations = table.getPageLocations();

        // Find the index of the pageNumber in pageLocations
        OptionalInt indexOpt = IntStream.range(0, table.getPageCount())
                                        .filter(i -> pageLocations[i] == pageNumber)
                                        .findFirst();

        if (indexOpt.isEmpty()) {
            System.err.println("Error: Page " + pageNumber + " not found in table " + tableNumber);
            return null;
        }

        int address = Integer.BYTES + (indexOpt.getAsInt() * Main.getPageSize()); // Compute byte offset

        try (RandomAccessFile fileIn = new RandomAccessFile(fileName, "r")) {
            byte[] data = new byte[Main.getPageSize()];
            fileIn.seek(address);
            fileIn.readFully(data); // Ensures full read without risk of partial reads

            Page page = Page.fromBinary(data, tableNumber, pageNumber, catalog);
            buffer.addPage(pageNumber, page);
            page.setUpdated(false);
            return page;
        } catch (IOException e) {
            System.err.println("Error loading page " + pageNumber + " from file: " + fileName);
            e.printStackTrace();
            return null;
        }
    }

    // Helper method to load pages from buffer or file
    private List<Page> loadPages(int tableNumber) {
        Table table = catalog.getTable(tableNumber);
        List<Page> pages = new ArrayList<>();

        for (int i = 0; i < table.getPageCount(); i++) {
            pages.add(buffer.isPageInBuffer(tableNumber, i) ? buffer.getPage(tableNumber, i) : getPage(tableNumber, i));
        }
        return pages;
    }

    // Splits a page into two when it exceeds capacity
    public Record splitPage(Page page) {
        int totalRecords = page.getRecordCount();
        int midIndex = totalRecords / 2;

        // Extract first and second halves efficiently
        List<Record> firstHalf = new ArrayList<>(page.getRecords().subList(0, midIndex));
        List<Record> secondHalf = new ArrayList<>(page.getRecords().subList(midIndex, totalRecords));

        // First record of the new page (important for maintaining order)
        Record firstRecInNewPage = secondHalf.get(0);

        // Calculate new sizes (accounting for numRecords integer)
        int firstPageSize = 4 + firstHalf.stream().mapToInt(Record::getSize).sum();
        int secondPageSize = 4 + secondHalf.stream().mapToInt(Record::getSize).sum();

        // Create new page
        Page newPage = new Page(page.getPageId() + 1, page.getTableId(), true);
        newPage.setRecords(new ArrayList<>(secondHalf));
        newPage.setRecordCount(secondHalf.size());
        newPage.setSize(secondPageSize);

        // Update original page
        page.setRecords(new ArrayList<>(firstHalf));
        page.setRecordCount(firstHalf.size());
        page.setSize(firstPageSize);

        // Update catalog and buffer
        catalog.getTable(page.getTableId()).addPage(newPage);
        buffer.updatePage(page);

        // Adjust page numbers in the buffer (shift pages forward)
        Table table = catalog.getTable(page.getTableId());
        for (int i = newPage.getPageId(); i < table.getPageCount(); i++) {
            Page bufferPage = buffer.getPage(page.getTableId(), i);
            bufferPage.setPageId(i + 1);
            buffer.updatePage(bufferPage);
        }

        // Handle potential recursive splits if overfull
        if (page.isOverfull())
            splitPage(page);
        else
            buffer.updatePage(page);

        if (newPage.isOverfull())
            splitPage(newPage);
        else
            buffer.addPage(newPage.getPageId(), newPage);

        return firstRecInNewPage;
    }

    // Function to load from disk

    // Assuming pageID is the number the page is in the list i.e pageID 1 == the
    // page in file at index 1
    public void writePageToDisk(Page page) {
        copyBinFile("data.bin");
    }

    // Simply appends page to end of file
    public void appendPageToFile(Page page) {
        // Takes in a page object, and writes it to disk
        Table table = this.catalog.getTable(page.getTableId());
        String filePath = table.getName() + ".bin";

        try (RandomAccessFile raf = new RandomAccessFile(filePath, "rw")) {
            raf.seek(raf.length()); // Move to the start of the file and write the full page
            raf.write(page.toBinary(table));
            System.out.println("Page written to binary file.");

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Creates a temp bin file
    private void copyBinFile(String srcFile) {
        String tempPath = "temp" + srcFile; // Temporary file

        try (
                FileChannel sourceChannel = new FileInputStream(srcFile).getChannel();
                FileChannel destChannel = new FileOutputStream(tempPath).getChannel()) {
            destChannel.transferFrom(sourceChannel, 0, sourceChannel.size());
            System.out.println("File copied successfully.");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // write a page to file
    public void writePage(Page page) {
        if (!page.isUpdated())
            return;

        try (RandomAccessFile fileOut = new RandomAccessFile(Main.getDBLocation() +
                "/tables/" + page.getTableId() + ".bin", "rw")) { // Open the file

            Table table = Main.getCatalog().getTable(page.getTableId());
            byte[] data = page.toBinary(table);
            int[] pageLocations = table.getPageLocations();
            int index = -1;

            for (int i = 0; i < table.getPageCount(); i++) { // Find the page in the table
                if (pageLocations[i] == page.getPageId()) {
                    index = i;
                    break;
                }
            }

            if (index < 0) { // Page not found
                System.err.println("Can't write page: No pages in table");
                return;
            }

            fileOut.seek(Integer.BYTES + (index * Main.getPageSize())); // Seek to the page location
            fileOut.write(data); // Write the page data
            System.out.println("Page data saved in binary format at " + fileOut);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Might not work... but maybe works now - Gian
    public boolean addRecord(Catalog catalog, Record record, int tableNumber) {
        Table table = catalog.getTable(tableNumber);
        Attribute[] attributes = table.getAttributes();
    
        boolean maintainConstraints = false;
        boolean indexFound = false;
        int pageIndex = -1, recIndex = -1;
    
        // Iterate through pages to find the insertion point
        for (int i = 0; i < table.getPageCount(); i++) {
            if (indexFound && !maintainConstraints) break;
            
            Page page = getPage(table.getTableID(), i);
            List<Record> existingRecords = page.getRecords();
    
            for (int j = 0; j < page.getRecordCount(); j++) {
                if (indexFound && !maintainConstraints) break;
                
                Record existingRecord = existingRecords.get(j);
                if (checkConstraints(attributes, record, existingRecord, i, j, maintainConstraints)) {
                    return false; // Constraint violation
                }                
    
                if (!indexFound && shouldInsertBefore(record, existingRecord, attributes)) {
                    indexFound = true;
                    pageIndex = i;
                    recIndex = j;
                }
            }
        }
    
        insertRecord(table, record, tableNumber, indexFound, pageIndex, recIndex);
        return true;
    }
    
    // Checks constraints like primary key and uniqueness
    private boolean checkConstraints(Attribute[] attributes, Record record, Record existingRecord, int pageIndex, int recIndex, boolean maintainConstraints) {
        for (int i = 0; i < attributes.length; i++) {
            Attribute attr = attributes[i];
    
            if (attr.isUnique() || attr.isnonNull()) maintainConstraints = true;
    
            if (attr.isPrimaryKey() || attr.isUnique()) {
                int comparison = compare(attr, record, existingRecord, i);
                if (comparison == 0) {
                    System.err.println("Error: Duplicate " + (attr.isPrimaryKey() ? "primary key" : "unique attribute"));
                    return true; // Constraint violation
                }
            }
        }
        return false;
    }
    
    // Determines whether record should be inserted before another record
    private boolean shouldInsertBefore(Record record, Record existingRecord, Attribute[] attributes) {
        for (int i = 0; i < attributes.length; i++) {
            if (attributes[i].isPrimaryKey() && compare(attributes[i], record, existingRecord, i) < 0) {
                return true;
            }
        }
        return false;
    }
    
    // Inserts a record into the correct page
    private void insertRecord(Table table, Record record, int tableNumber, boolean indexFound, int pageIndex, int recIndex) {
        System.out.println("ADDING RECORD");
        Page targetPage;
    
        if (table.getPageCount() == 0) {
            targetPage = new Page(0, tableNumber, true);
            targetPage.addRecord(record);
            table.addPage(targetPage);
        } else {
            pageIndex = indexFound ? pageIndex : table.getPageCount() - 1;
            targetPage = getPage(tableNumber, pageIndex);
    
            if (indexFound) {
                targetPage.shiftRecordsAndAdd(record, recIndex);
            } else {
                targetPage.addRecord(record);
            }
        }
    
        if (targetPage.isOverfull()) {
            splitPage(targetPage);
        } else {
            buffer.updatePage(targetPage);
        }
    }
    
    // Compares attribute values
    private int compare(Attribute attr, Record record, Record existingRecord, int index) {
        Object value1 = record.getData().get(index);
        Object value2 = existingRecord.getData().get(index);
    
        switch (attr.getType().toLowerCase()) {
            case "varchar":
            case "char":
                return ((String) value1).compareTo((String) value2);
            case "integer":
                return Integer.compare((int) value1, (int) value2);
            case "double":
                return Double.compare((double) value1, (double) value2);
            case "boolean":
                return Boolean.compare((boolean) value1, (boolean) value2);
            default:
                throw new IllegalArgumentException("Unsupported attribute type: " + attr.getType());
        }
    }
    

}
