import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

public class PageBuffer {
    private final int capacity;
    private final LinkedHashMap<PageKey, Page> pages;
    private final StorageManager storageManager = Main.getStorageManager();

    public PageBuffer(int capacity) {
        this.capacity = capacity;
        this.pages = new LinkedHashMap<>() { // LRU cache -- called automatically when something is added
            @Override
            protected boolean removeEldestEntry(Map.Entry<PageKey, Page> eldest) {
                if (size() > PageBuffer.this.capacity) {
                    writePageToHardware(eldest.getValue());
                    return true;
                }
                return false;
            }
        };
    }

    public void addPage(int pageNumber, Page page) {
        pages.put(new PageKey(page.getTableId(), pageNumber), page);
    }

    public Page getPage(int tableID, int pageNumber) {
        return pages.get(new PageKey(tableID, pageNumber));
    }

    public boolean isPageInBuffer(int tableID, int pageNumber) {
        return pages.containsKey(new PageKey(tableID, pageNumber));
    }

    public void writePageToHardware(Page page) {
        storageManager.writePage(page); // calls storage manager to write page to hardware
    }

    public void writeBufferToHardware() {
        //Call the storage manager to write all pages in the buffer to hardware

        Catalog catalog = Main.getCatalog();
        byte[] tableUpdatedArray = new byte[catalog.getTableCount()]; // 0 means not updated, 1 means updated
        Arrays.fill(tableUpdatedArray, (byte) 0); // Initialize the array to 0

        for (Map.Entry<PageKey, Page> entry : pages.entrySet()) {
             Page page = entry.getValue();
             int tableNum = page.getTableId();
            
             if (tableUpdatedArray[tableNum] == 0) {
                Table table = catalog.getTable(tableNum);
                try (RandomAccessFile fileOut = new RandomAccessFile(Main.getDBLocation() + 
                         "/tables/" + tableNum + ".bin", "rw")) { // Open the file
                    fileOut.write(table.getPageCount());
                    tableUpdatedArray[tableNum] = 1; // Mark the table as updated
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            
            writePageToHardware(page);
        }
    }

    public void updatePage(Page targetPage) {
        pages.put(new PageKey(targetPage.getTableId(), targetPage.getPageId()), targetPage);
    }

    public void updateAndMovePage(Page targetPage, int oldPageNumber) {
        addPage(targetPage.getPageId(), targetPage);
        pages.remove(new PageKey(targetPage.getTableId(), oldPageNumber));
    }
}

record PageKey(int tableID, int pageID) {}
