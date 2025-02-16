import java.util.ArrayList;
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

    // Implement...
    public ArrayList<ArrayList<Object>> getRecords(int tableNumber) {
        ArrayList<ArrayList<Object>> tuples = new ArrayList<>();

        // Iterate through all pages in the table

            // Retrieve the page from the buffer or load from disk if not in the buffer

        // If the page is valid, retrieve its records and add their data to tuples

        return tuples;
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
