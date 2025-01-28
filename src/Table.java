// Table = collection of pages

public class Table {
    private String name;
    private int tableId;
    private Attribute[] attributes;
    private int[] pages; // pageId

    public Table(String name, int tableId, Attribute[] attributes, int[] pages){
        this.name = name;
        this.tableId = tableId;
        this.attributes = attributes;
        this.pages = pages;
    }

    public void addPage(Page page){

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
