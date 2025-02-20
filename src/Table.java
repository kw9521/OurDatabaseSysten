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
    private int[] pageLocations; // pageId

    public Table(String name, int tableID, int attributesCount, List<Attribute> attributes){
        this.name = name;
        this.tableId = tableID;
        this.attributesCount = attributesCount;
        this.attributes = attributes;

        this.pageCount = 0;
        this.pageLocations = new int[0];
    }

    // Implementing still...
    public void addPage(Page page){
        PageBuffer buffer = Main.getBuffer();
        buffer.addPage(page.getPageId(), page);
        this.pageCount++;
    }

    public void dropPage(int pageNum) {
        int indexToRemove = -1;
        for (int i = 0; i < pageCount; i++) {
            if (pageLocations[i] == pageNum) {
                indexToRemove = i;
                break;
            }
        }

        if (indexToRemove != -1) {
            int[] newArray = new int[pageCount - 1];
            for (int i = 0, j = 0; i < pageCount; i++) {
                if (i != indexToRemove) {
                    newArray[j] = pageLocations[i] > pageNum ? pageLocations[i] - 1 : pageLocations[i];
                    if (pageLocations[i] > pageNum) {
                        Page page = Main.getStorageManager().getPage(tableId, pageLocations[i]);
                        page.setPageId(page.getPageId() - 1);
                        Main.getBuffer().updatePage(page);
                    }
                    j++;
                }
            }
            pageLocations = newArray;
            pageCount--;
        } else {
            System.out.println("Page not found: " + pageNum);
        }
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
    
    public int[] getPageLocations(){
        return this.pageLocations;
    }

    public void displayTable(){
        System.out.println("Table name: Gian (test)");
        System.out.println("Table schema: ");
        // for loop to call attributes.displayAttributes()
    }

    // Experimenting with ByteBuffer, might not work
    public void writeToBuffer(ByteBuffer buffer) {
        buffer.putInt(this.tableId);
        byte[] nameBytes = this.name.getBytes(StandardCharsets.UTF_8);
        buffer.putInt(nameBytes.length);
        buffer.put(nameBytes);
        buffer.putInt(this.attributesCount);
        // buffer.putInt(this.pageCount); // Not sure if this is currently needed may add back later
        
        for (Attribute attr : this.attributes) {
            attr.writeToBuffer(buffer);
        }
        
        // Not sure if this is needed may add back later
        // for (int location : this.pages) {
        //     buffer.putInt(location);
        // }
    }

    public static Table readFromBuffer(ByteBuffer buffer) {
        int attributesCount = buffer.getInt();
        int nameLength = buffer.getInt();
        byte[] nameBytes = new byte[nameLength];
        buffer.get(nameBytes);
        String name = new String(nameBytes, StandardCharsets.UTF_8);
        int tableNumber = buffer.getInt();
        int pageCount = buffer.getInt();
        List<Attribute> attributes = new ArrayList<>();
        
        for (int i = 0; i < attributesCount; i++) {
            attributes.add(Attribute.readFromBuffer(buffer));
        }
        
        Table table = new Table(name, tableNumber, attributesCount, attributes);
        table.pageCount = pageCount;
        
        for (int i = 0; i < pageCount; i++) {
            table.pageLocations[i] = (buffer.getInt());
        }
        
        return table;
    }

    public int calcByteSize(){
        int totalSize = 0;
        totalSize += 4; //For the ID int
        byte[] nameBytes = this.name.getBytes(StandardCharsets.UTF_8);
        totalSize += nameBytes.length;
        totalSize += 4; //For the int length of the string
        totalSize += 4; //Size to allocate for number of attribute stored

        for(Attribute attr : this.attributes){
            totalSize += attr.calcByteSize();
        }

        return totalSize;
    }

    public Page getPageByNumber(int pageId) {
        StorageManager storageManager = Main.getStorageManager();
        Page page = storageManager.getPage(this.tableId, pageId);
        return page;
    }

    public String getRecordCount() {
        int totalRecords = 0;
        for (int pageLocation : this.pageLocations) {
            Page page = getPageByNumber(pageLocation);
            if (page != null) {
                totalRecords += page.getRecordCount(); 
            }
        }
        return String.valueOf(totalRecords);
    }

}
