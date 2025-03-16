import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.OptionalInt;
import java.util.stream.IntStream;

public class PageBuffer {
    private final int capacity;
    private final LinkedHashMap<PageKey, Page> pages;

    public PageBuffer(int capacity) {
        this.capacity = capacity;
        this.pages = new LinkedHashMap<>() { // LRU cache -- called automatically when something is added
            @Override
            protected boolean removeEldestEntry(Map.Entry<PageKey, Page> eldest) {
                if (size() > PageBuffer.this.capacity) {
                    writePage(eldest.getValue());
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

    public void writePage(Page page) {
        if (!page.isUpdated()) return; // Skip if page is not updated

        String fileName = Main.getDBLocation() + "tables/" + page.getTableId() + ".bin";
        Table table = Main.getCatalog().getTable(page.getTableId());
        byte[] data = page.toBinary(table); 

        // Find the page index in file
        int[] pageLocations = table.getPageLocations();
        OptionalInt indexOpt = IntStream.range(0, table.getPageCount())
                                        .filter(i -> pageLocations[i] == page.getPageId())
                                        .findFirst();

        if (indexOpt.isEmpty()) {
            System.err.println("Error: Cannot write page " + page.getPageId() + " - No pages found in table.");
            return;
        }

        int address = Integer.BYTES + (indexOpt.getAsInt() * Main.getPageSize()); // Compute file offset

        try (RandomAccessFile fileOut = new RandomAccessFile(fileName, "rw")) {
            fileOut.seek(address);
            fileOut.write(data);
            // System.out.println("Page " + page.getPageId() + " written successfully to " + fileName);
        } catch (IOException e) {
            System.err.println("Error writing page " + page.getPageId() + " to file: " + fileName);
            // e.printStackTrace();
        }
    }

    public void writeBuffer() {
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
                    // e.printStackTrace();
                }
            }
            
            writePage(page);
        }
    }

    public void updatePage(Page targetPage) {
        pages.put(new PageKey(targetPage.getTableId(), targetPage.getPageId()), targetPage);
    }

    public void updateAndMovePage(Page targetPage, int oldPageNumber) {
        addPage(targetPage.getPageId(), targetPage);
        pages.remove(new PageKey(targetPage.getTableId(), oldPageNumber));
    }

    public void purgeTablePages(int tableID) {
        pages.entrySet().removeIf(entry -> entry.getKey().tableID() == tableID);
    }
    
}

record PageKey(int tableID, int pageID) {}
