// Table = collection of pages
import java.util.ArrayList;
import java.util.List;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

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
        PageBuffer buffer = Main.getBuffer();
        buffer.removePage(page.getTableId(), page.getPageId());
        this.pageCount--;
    }

    public void addAttribute(Attribute attribute){
        this.attributes.add(attribute);
        this.attributesCount++;
    }

    public void dropAttribute(String attrName) {
        boolean removed = attributes.removeIf(attr -> attr.getName().equals(attrName));
        
        if (removed) {
            attributesCount--;
            System.out.println("Attribute " + attrName + " removed from table " + this.name);
        } else {
            System.out.println("Attribute " + attrName + " not found in table " + this.name);
        }
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

    public int getTableID(){
        return this.tableId;
    }

    public void setTableID(int newtableID){
        this.tableId = newtableID;
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

    // Experimenting with ByteBuffer, might not work
    public void writeToBuffer(ByteBuffer buffer) {
        buffer.putInt(this.attributesCount);
        byte[] nameBytes = this.name.getBytes(StandardCharsets.UTF_8);
        buffer.putInt(nameBytes.length);
        buffer.put(nameBytes);
        buffer.putInt(this.tableId);
        buffer.putInt(this.pageCount);
        
        for (Attribute attr : this.attributes) {
            attr.writeToBuffer(buffer);
        }
        
        for (int location : this.pages) {
            buffer.putInt(location);
        }
    }

    public static Table readFromBuffer(ByteBuffer buffer) {
        int attributesCount = buffer.getInt();
        int nameLength = buffer.getInt();
        byte[] nameBytes = new byte[nameLength];
        buffer.get(nameBytes);
        String name = new String(nameBytes, StandardCharsets.UTF_8);
        int tableNumber = buffer.getInt();
        int numPages = buffer.getInt();
        List<Attribute> attributes = new ArrayList<>();
        
        for (int i = 0; i < attributesCount; i++) {
            attributes.add(Attribute.readFromBuffer(buffer));
        }
        
        Table table = new Table(name, tableNumber, attributesCount, attributes);
        table.pageCount = numPages;
        
        for (int i = 0; i < numPages; i++) {
            table.pages.add(buffer.getInt());
        }
        
        return table;
    }

}
