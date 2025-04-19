// Table = collection of pages
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class Table {
    private String name;
    private int tableId;
    private Attribute[] attributes;
    private int attributesCount; 
    private int pageCount;
    private int[] pageLocations; // pageId
    private int numNodes; // Number of nodes in tree
    private ArrayList<Integer> freeSpaces; // Open spaces in index file for nodes, due to deleted nodes

    public Table(String name, int tableID, int attributesCount, Attribute[] attributes){
        this.name = name;
        this.tableId = tableID;
        this.attributesCount = attributesCount;
        this.attributes = attributes;

        this.pageCount = 0;
        this.pageLocations = new int[0];

        this.numNodes = 0;
        this.freeSpaces = new ArrayList<>();
    }

    public void addPage(Page page){
        PageBuffer buffer = Main.getBuffer();
        buffer.addPage(page.getPageId(), page);
        this.pageCount++;
        updatePageLocations(page.getPageId());
    }

    private void updatePageLocations(int newPageId) {
        if (pageLocations == null) {
            pageLocations = new int[] { newPageId };
            return;
        }
    
        int[] updatedLocations = Arrays.copyOf(pageLocations, pageCount);
        
        for (int i = 0; i < pageCount - 1; i++) {
            if (updatedLocations[i] >= newPageId) updatedLocations[i]++;
        }
    
        updatedLocations[pageCount - 1] = newPageId;
        pageLocations = updatedLocations;
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

    public void addAttribute(Attribute newAttr) {
        this.attributes = Arrays.copyOf(this.attributes, this.attributes.length + 1);
        this.attributes[this.attributes.length - 1] = newAttr;
        this.attributesCount++;
        System.out.println("Attribute " + newAttr.getName() + " added to table " + this.name);
    }

    public void dropAttribute(String attrName) {
        this.attributes = Arrays.stream(this.attributes)
                                .filter(attr -> !attr.getName().equals(attrName))
                                .toArray(Attribute[]::new);
        this.attributesCount--;
        System.out.println("Attribute " + attrName + " removed from table " + this.name);
    }

    public String getName(){
        return this.name;
    }

    public Attribute[] getAttributes(){
        return this.attributes;
    }
    public int getAttributesCount(){
        return attributesCount;
    }

    public boolean hasPrimaryKey(Table table) { 
        for(int i = 0; i < table.getAttributesCount(); i++) {
            Attribute attr=table.attributes[i];
            if(attr.getPrimaryKey()) {
                return true;
            }
        }
        return false;
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

    public void addTreeNode() {
        this.numNodes++;
    }

    public void deleteTreeNode() {
        this.numNodes--;
    }

    public int getNumNodes() {
        return this.numNodes;
    }

    public void setNumNodes(int numNodes) {
        this.numNodes = numNodes;
    }

    private void setFreeSpaces(ArrayList<Integer> freeSpacesList) {
        this.freeSpaces = freeSpacesList;
    }
    
    public ArrayList<Integer> getFreeSpaces() {
        return freeSpaces;
    }

    public void displayTable(){
        System.out.printf("Table Name: %s%nTable schema:%n", getName());
    
        for (Attribute attr : this.getAttributes()) {
            System.out.printf("    %s: %s%s%s%s%n",
                attr.getName(), attr.getType(),
                attr.isPrimaryKey() ? " primarykey" : "",
                attr.isUnique() ? " unique" : "",
                attr.isnonNull() ? " notnull" : ""
            );
        }

        System.out.printf("Pages: %d%nRecords: %s%n%n", getPageCount(), getRecordCount());
    }

    public void writeToStream(DataOutputStream dos) throws IOException {
        dos.writeUTF(this.name);
        dos.writeInt(this.tableId);
        dos.writeInt(this.attributesCount);
        dos.writeInt(this.pageCount);
        for (Attribute attr : this.attributes) {
            attr.writeToStream(dos);
        }
        for (int location : this.pageLocations) {
            dos.writeInt(location);
        }

        for (int pageLocation : this.pageLocations) {
        Page page = getPageByNumber(pageLocation);
        if (page != null) {
            for (Record record : page.getRecords()) {
                List<Object> data = record.getData();
                Attribute[] attributes = getAttributes();

                // Pad data with nulls for missing attributes
                while (data.size() < attributes.length) {
                    data.add(null);
                }

                record.setData(data);
            }
        }
    }



    }

    public static Table readFromStream(DataInputStream dis) throws IOException {
        String name = dis.readUTF();
        int tableNumber = dis.readInt();
        int attributesCount = dis.readInt();
        int numPages = dis.readInt();
        Attribute[] attributes = new Attribute[attributesCount];
        for (int i = 0; i < attributesCount; i++) {
            attributes[i] = Attribute.readFromStream(dis);
        }
        Table table = new Table(name, tableNumber, attributesCount, attributes);
        table.pageCount = numPages;
        table.pageLocations = new int[numPages];
        for (int i = 0; i < numPages; i++) {
            table.pageLocations[i] = dis.readInt();
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
