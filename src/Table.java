// Table = collection of pages
import java.util.ArrayList;
import java.util.List;

public class Table {
    private String name;
    private int tableId;
    private List<Attribute> attributes;
    private int attributesCount; 
    private int pageCount;
    private List<Integer> pages; // pageId

    public Table(String name, int tableID, int attributesCount, List<Attribute> attributes){
        this.name = name;
        this.tableId = tableID;
        this.attributesCount = attributesCount;
        this.attributes = attributes;

        this.pageCount = 0;
        this.pages = new ArrayList<>();
    }

    // Implementing still...
    public void addPage(Page page){
        PageBuffer buffer = Main.getBuffer();
        buffer.addPage(page.getPageId(), page);
        this.pageCount++;
    }

    public void dropPage(Page page){
        // TODO
        this.pageCount--;
    }

    public void addAttribute(Attribute attribute){

    }

    public void dropAttribute(Attribute attribute){

    }

    public String getName(){
        return this.name;
    }

    public List<Attribute> getAttributes(){
        return this.attributes;
    }

    public int getPageCount(){
        return this.pageCount;
    }
    
    public int getRecordCount() {
        if (pages == null || pages.isEmpty()) {
            return 0; // No pages, so no records
        }
        // TODO
        return 0;
    }
    

    public void displayTable(){
        System.out.println("Table name: Gian (test)");
        System.out.println("Table schema: ");
        // for loop to call attributes.displayAttributes()
    }

    // function that storage manager calls to convert info to bytes
    // can move this around to diff file later
    public void convertToByte(){

    }

}
