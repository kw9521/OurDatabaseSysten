import java.util.ArrayList;
import java.util.List;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;

public class StorageManager {
    private Catalog catalog;
    private PageBuffer buffer;

    public StorageManager(Catalog catalog, PageBuffer buffer){
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

    // Helper method to load pages from buffer or file
    private List<Page> loadPages(int tableNumber) {
        Table table = catalog.getTable(tableNumber);
        List<Page> pages = new ArrayList<>();

        for (int i = 0; i < table.getPageCount(); i++) {
            pages.add(buffer.inBuffer(tableNumber, i) ? 
                    buffer.getPage(tableNumber, i) : 
                    getPage(tableNumber, i));
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
        Page newPage = new Page(page.getPageId() + 1, page.getTableId());
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
        if (page.isOverfull()) splitPage(page);
        else buffer.updatePage(page);

        if (newPage.isOverfull()) splitPage(newPage);
        else buffer.addPage(newPage.getPageId(), newPage);

        return firstRecInNewPage;
    }


    // Function to load from disk

    // Assuming pageID is the number the page is in the list i.e pageID 1 == the page in file at index 1
    public void writePageToDisk(Page page){
        copyBinFile("data.bin");
    }

    // Simply appends page to end of file
    public void appendPageToFile(Page page){
        // Takes in a page object, and writes it to disk
        Table table = this.catalog.getTable(page.getTableId());
        String filePath = table.getName() + ".bin";

        try (RandomAccessFile raf = new RandomAccessFile(filePath, "rw")) {
            raf.seek(raf.length()); // Move to the start of the file and write the full page
            raf.write(page.toBinary());
            System.out.println("Page written to binary file.");

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Creates a temp bin file
    private void copyBinFile(String srcFile){
        String tempPath = "temp" + srcFile; // Temporary file

        try (
            FileChannel sourceChannel = new FileInputStream(srcFile).getChannel();
            FileChannel destChannel = new FileOutputStream(tempPath).getChannel()
        ) {
            destChannel.transferFrom(sourceChannel, 0, sourceChannel.size());
            System.out.println("File copied successfully.");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
