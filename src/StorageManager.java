import java.util.ArrayList;

public class StorageManager {
    private Catalog catalog;
    private PageBuffer buffer;

    public StorageManager(Catalog catalog, PageBuffer buffer){
        this.catalog = catalog;
        this.buffer = buffer;
    }

    // Implement...
    public ArrayList<ArrayList<Object>> getRecords(int tableNumber) {
        ArrayList<ArrayList<Object>> tuples = new ArrayList<>();

        // Iterate through all pages in the table

            // Retrieve the page from the buffer or load from disk if not in the buffer

        // If the page is valid, retrieve its records and add their data to tuples

        return tuples;
    }

    // Function to load from disk

    public void writePageToDisk(Page page){
        // Takes in a page object, and writes it to disk
    }
}
