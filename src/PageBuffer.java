import java.util.LinkedHashMap;
import java.util.Map;

public class PageBuffer {
    private int bufferSize;
    record PageKey(int tableID, int PageID) {} // or use Map.Entry???
    private final LinkedHashMap<PageKey, Page> pages;

    public PageBuffer(int bufferSize){
        this.bufferSize = bufferSize;
        this.pages = new LinkedHashMap<>();
    }

    public void addPage(int pageId, Page page) {
        PageKey key = new PageKey(page.getTableId(), pageId);
        pages.put(key, page);
    }

    public void removePage(int tableId, int pageId) {
        PageKey key = new PageKey(tableId, pageId);
        pages.remove(key);
    }
    
    public Page getPage(int tableId, int pageId) {
        PageKey key = new PageKey(tableId, pageId);
        return pages.get(key);
    }
    
    public boolean inBuffer(int tableId, int pageId) {
        PageKey key = new PageKey(tableId, pageId);
        return pages.containsKey(key);
    }
}