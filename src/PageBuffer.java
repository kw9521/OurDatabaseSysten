import java.util.LinkedHashMap;
import java.util.Map;

public class PageBuffer {
    private int bufferSize;  // Total size of the buffer in pages
    record PageKey(int tableID, int PageID) {} // or use Map.Entry???
    private final LinkedHashMap<PageKey, Page> pages;
    private final HashSet<PageKey> modifiedPages = new HashSet<>();

    private StorageManager StorageManager;

    public PageBuffer(int bufferSize, StorageManager inputSM){
        this.bufferSize = bufferSize;
        this.pages = new LinkedHashMap<>();
        this.StorageManager = inputSM;
    }

    public void addPage(int pageId, Page page) {
        PageKey key = new PageKey(page.getTableId(), pageId);
        
        // If buffer is full, then store the LRU Page
        if (pages.size() >= bufferSize) {
            evictPage(StorageManager);
        }

        pages.put(key, page);
    }

    private void evictPage(StorageManager storageManager){
        Map.Entry<PageKey, Page> oldestEntry = pages.entrySet().iterator().next();
        PageKey evictedKey = oldestEntry.getKey();
        Page evictedPage = oldestEntry.getValue();

        // check if page was updated
        if (modifiedPages.contains(evictedKey)){
            storageManager.writePageToDisk(evictedPage);
            modifiedPages.remove(evictedKey);
        }

        // Remove page from buffer
        pages.remove(evictedKey);
    }

    // Call when a page has information saved to it so that the buffer knows when to write information to disk
    public void markPage(int tableID, int pageId){
        PageKey key = new PageKey(tableID, pageId);
        modifiedPages.add(key)
    }

    public void removePage(int tableId, int pageId) {
        PageKey key = new PageKey(tableId, pageId);

        if (modifiedPages.contains(key)){
            storageManager.writePageToDisk(pages.get(key));
            modifiedPages.remove(key);
        }

        pages.remove(key);
    }
    
    public Page getPage(int tableId, int pageId) {
        PageKey key = new PageKey(tableId, pageId);

        if (pages.contains(key)){
            Page page = pages.remove(key);
            pages.put(key, page);
            return page;
        }

        return null; // if no page found in buffer return null
    }
    
    public boolean inBuffer(int tableId, int pageId) {
        PageKey key = new PageKey(tableId, pageId);
        return pages.containsKey(key);
    }
}