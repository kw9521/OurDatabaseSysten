// Table = collection of pages

public class Table {
    private String name;
    private int tableId;
    private Attribute[] attributes;
    private int pageCount;
    private int[] pages; // pageId

    public Table(String name, int tableId, Attribute[] attributes, int[] pages){
        this.name = name;
        this.tableId = tableId;
        this.attributes = attributes;
        this.pageCount = 0;
        this.pages = pages;
    }

    // Implementing still...
    public void addPage(Page page){
        PageBuffer buffer = Main.getBuffer();
        buffer.addPage(page.getPageId(), page);
        this.pageCount++;
    }

    public void dropPage(Page page){

    }

    public void addAttribute(Attribute attribute){

    }

    public void dropAttribute(Attribute attribute){

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
