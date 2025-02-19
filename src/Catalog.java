import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
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

    public int getNextTableID(){
        return this.tables.size();
    }

    public void addTable(Table table){
        this.tables.add(table);
        this.tableCount++;
        System.out.println("Added table: " + table.getName());
    }

    /**
     * Look at droppedTable's tableID. Move every other table's tableID to be 1 less if their curr 
     * tableID is bigger than droppedTable's ID. 
     *      Eg: if DroppedTable is 3 then Tables4+ will be moved down
     * Also gets rid of the empty space after table has been dropped
     * @param droppedTab
     */
    public void dropTable(Table droppedTab) {
        // Use below method maybe?
        int id = droppedTab.getTableID();
        
        // remove the table from the list
        // by using .remove(): the elements to the right of the removed element are 
        // automatically shifted one position to the left (towards index 0) to fill the gap
        tables.remove(droppedTab);
    
        // update the table IDs for tables that had a higher ID than the dropped table
        for (Table tab : tables) {
            if (tab.getTableID() > id) {
                tab.setTableID(tab.getTableID() - 1);
            }
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

    // catalog format: ["PS",
    // "numOfTable",
    // "Table1ID", "lengthOfTable1Name", "TableName1Char", "TableName1Char2", ...,
    // "TableName1CharN"
    // "NumOfAttributeinTable1",
    // "Len of Att1 Name", "Att1 Name", "datatype", "bitmap is a string of 01s",
    // "Len of Att2 Name", "Att2 Name", "datatype", "bitmap is a string of 01s",
    // ...
    // "Len of AttN Name", "AttN Name", "datatype", "bitmap is a string of 01s",
    // "Table2ID", "lengthOfTable2Name", "TableName2", "TableName2Char2", ...,
    // "TableName12harN"
    // "NumOfAttributeinTable2",
    // "Len of Att1 Name", "Att1 Name", "datatype", "bitmap is a string of 01s",
    // "Len of Att2 Name", "Att2 Name", "datatype", "bitmap is a string of 01s",
    // ...
    // "Len of AttN Name", "AttN Name", "datatype", "bitmap is a string of 01s",
    // ...
    // "NumOfAttributeinTableN",
    // "Len of Att1 Name", "Att1 Name", "datatype", "bitmap is a string of 01s",
    // "Len of Att2 Name", "Att2 Name", "datatype", "bitmap is a string of 01s",
    // ...
    // "Len of AttN Name", "AttN Name", "datatype", "bitmap is a string of 01s"]
    public void parseCatalog(ArrayList<String> catalog) {

        this.pageSize = Integer.parseInt(catalog.get(0));
        this.tableCount = Integer.parseInt(catalog.get(1));

        // use as pointer to go thru arraylist
        int currIndex = 2;
        // readTable(catalog, currIndex);

    }

    /**
     * Given initial catalog arraylist of strings, parse info into diff tables
     * After parsing, put all table info to create instance of table. 
     * Then add each instance of table into List<Table> tables
     * @param catalog
     * @param currIndex
     */

    // Note: For each table in catalog, call Table.write/readToBuffer

    public void writeCatalogToBuffer(ByteBuffer buffer) throws IOException {
        buffer.putInt(this.tableCount);
        for (Table table : this.tables) {
            table.writeToBuffer(buffer);
        }
    }

    public void readCatalogFromFile(String pathname, ByteBuffer buffer) throws IOException {
        byte[] fileBytes = Files.readAllBytes(Paths.get(pathname));
        buffer.put(fileBytes);
        buffer.flip(); // Prepare for reading
        
        this.tableCount = buffer.getInt();
        this.tables.clear();
        for (int i = 0; i < this.tableCount; i++) {
            Table table = Table.readFromBuffer(buffer);
            this.tables.add(table);
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

    public int calcByteSize(){
        int totalSize = 0;
        totalSize += 4; //Allocate size to store table count
        for(Table table : this.tables){
            totalSize += table.calcByteSize();
        }
        return totalSize;
    }

}
