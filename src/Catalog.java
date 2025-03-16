import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Dictionary;
import java.util.Hashtable;

public class Catalog {
    private String dbLocation;
    private int pageSize;
    private int bufferSize;

    private List<Table> tables;
    private int tableCount;
    private Dictionary<Integer, String> dataTypes = new Hashtable<>();

    public Catalog(String dbLocation, int pageSize, int bufferSize) {
        this.dbLocation = dbLocation;
        this.pageSize = pageSize;
        this.bufferSize = bufferSize;
        this.tables = new ArrayList<>();
        this.tableCount = 0;
    }

    public String getDbLocation() {
        return this.dbLocation;
    }

    public int getPageSize() {
        return this.pageSize;
    }

    public int getBufferSize() {
        return this.bufferSize;
    }

    public List<Table> getTables(){
        return this.tables;
    }

    public int getTableCount() {
        return this.tableCount;
    }

    public int getNextTableID(){
        return this.tables.size();
    }

    public void addTable(Table table){
        this.tables.add(table);
        this.tableCount++;
    }

    /**
     * Look at droppedTable's tableID. Move every other table's tableID to be 1 less if their curr 
     * tableID is bigger than droppedTable's ID. 
     *      Eg: if DroppedTable is 3 then Tables4+ will be moved down
     * Also gets rid of the empty space after table has been dropped
     * @param droppedTab
     */
    public void dropTable(String tableName) {
        Optional<Table> tableToRemove = tables.stream()
            .filter(table -> table.getName().equals(tableName))
            .findFirst();

        if (tableToRemove.isPresent()) {
            int tableID = tableToRemove.get().getTableID(); // Get table ID before removing it
            tables.remove(tableToRemove.get());
            tableCount--;
            Main.getBuffer().purgeTablePages(tableID); // Remove pages from the buffer
            System.out.println("Table dropped: " + tableName);
        } else {
            System.err.println("Table not found: " + tableName);
        }
    }

    
    public Table getTableByName(String tableName) {
        return this.tables.stream()
            .filter(table -> table.getName().equals(tableName)).findFirst().orElse(null);
    }

    public void populateDict(){
        dataTypes.put(0, "String");
        dataTypes.put(1, "char");
        dataTypes.put(2, "int");
        dataTypes.put(3, "double");
        dataTypes.put(4, "float");
        dataTypes.put(5, "boolean");

    }

    /**
     * Given initial catalog arraylist of strings, parse info into diff tables
     * After parsing, put all table info to create instance of table. 
     * Then add each instance of table into List<Table> tables
     * @param catalog
     * @param currIndex
     */

    // Note: For each table in catalog, call Table.write/readToBuffer

    public void writeCatalog(String pathname) throws IOException {
        try (DataOutputStream dos = new DataOutputStream(new FileOutputStream(pathname))) {
            dos.writeInt(this.tableCount);
            for (Table table : this.tables) {
                table.writeToStream(dos); 
            }
        }
    }

    public void readCatalog(String pathname) throws IOException {
        try (DataInputStream dis = new DataInputStream(new FileInputStream(pathname))) {
            this.tableCount = dis.readInt();
            this.tables.clear();
            for (int i = 0; i < this.tableCount; i++) {
                Table table = Table.readFromStream(dis); 
                this.tables.add(table);
            }
        }
    }


    /**
     * Read next 3 catalog's boxes, grab name and type of the attribute, use Attribue.java's parse() to create an
     * instance of Attribute and add it to an allAttribute list. 
     * @param catalog arraylist of strings being read
     * @param currIndex where we are in catalog index-wise
     * @param numOfAttributes  # of attributes to read
     * @return allAttributes: an arraylist of all the attributes associated with this table
     */

    // Note: Attribute.readFromBuffer

    public ArrayList<Attribute> readAttribute(ArrayList<String> catalog, int currIndex, int numOfAttributes){

        String attrName = "";
        String dataTypeString;
        ArrayList<Attribute> allAttributes = new ArrayList<>();;

        // go thru num of attributes
        for(int currAttr = 0; currAttr < numOfAttributes; currAttr++){
            int lenOfAttName = Integer.parseInt(catalog.get(currIndex));
            currIndex++;

            // go thru length of attribute1's name and keep adding to form a attribute name
            for (int attrNameIndex = 0; attrNameIndex < lenOfAttName; attrNameIndex++){
                String tabNameChar = catalog.get(currIndex);
                currIndex++;
                attrName += tabNameChar;
            }
            
            // use a dict to differentiate if dataType is a char/string/int/etc
            int intDataType = Integer.parseInt(catalog.get(currIndex));
            currIndex++;
            dataTypeString = dataTypes.get(intDataType);     
            
            String attributeStr = attrName + " " + dataTypeString;
            allAttributes.add(Attribute.parse(attributeStr));
        }

        return allAttributes;
    }

    // Get the table from the tableID
    public Table getTable(int tableID){
        for(Table table : this.tables){
            if(table.getTableID() == tableID){
                return table;
            }
        }
        return null; //Return Null if table doesn't exist
    }

    public boolean tableExists(String name) {
        return this.tables.stream().anyMatch(table -> table.getName().equals(name));
    }

    public boolean displayInfo(String name) {
        Table foundTable = this.tables.stream()
                                 .filter(table -> table.getName().equals(name))
                                 .findFirst()
                                 .orElse(null);
        if (foundTable != null) {
            foundTable.displayTable();
            return true;
        }
        return false;
    }

    public int calcByteSize(){
        int totalSize = 0;
        totalSize += 4; //Allocate size to store table count
        for(Table table : this.tables){
            totalSize += table.calcByteSize();
        }
        return totalSize;
    }

}
