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

    public Catalog(String dbLocation, int pageSize, int bufferSize){
        this.dbLocation = dbLocation;
        this.pageSize = pageSize;
        this.bufferSize = bufferSize;
    }

    public String getDbLocation(){
        return this.dbLocation;
    }

    public int getPageSize(){
        return this.pageSize;
    }

    public int getBufferSize(){
        return this.bufferSize;
    }

    public List<Table> getTables(){
        return this.tables;
    }

    public int getNextTableNumber(){
        return 0;
    }

    public void addTable(){
        // TODO
    }


    public void populateDict(){
        dataTypes.put(0, "String");

        // initially assume varchar is a char for now
        // use the bitmap to differentiate between char and varchar
        // 0 for char, 1 for varchar
        dataTypes.put(1, "char");
        dataTypes.put(2, "int");
        dataTypes.put(3, "double");
        dataTypes.put(4, "float");
        dataTypes.put(5, "boolean");

    }

    // catalog format: ["PS", 
    // "numOfTable", 
        // "Table1ID", "lengthOfTable1Name", "TableName1Char", "TableName1Char2", ..., "TableName1CharN"
            // "NumOfAttributeinTable1", 
                // "Len of Att1 Name", "Att1 Name", "datatype", "bitmap is a string of 01s", 
                // "Len of Att2 Name", "Att2 Name", "datatype", "bitmap is a string of 01s",
                // ...
                // "Len of AttN Name", "AttN Name", "datatype", "bitmap is a string of 01s",
        // "Table2ID", "lengthOfTable2Name", "TableName2", "TableName2Char2", ..., "TableName12harN"
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
    public void parseCatalog(ArrayList<String> catalog){

        this.pageSize = Integer.parseInt(catalog.get(0));
        this.tableCount = Integer.parseInt(catalog.get(1));

        // use as pointer to go thru arraylist
        int currIndex = 2;

        readTable(catalog, currIndex);

    }

    // table constructor: Table(String name, int tableId, Attribute[] attributes, int[] pages)
    public void readTable(ArrayList<String> catalog, int currIndex){

        // go through tables
        for (int i = 0; i < tableCount; i++){

            String tableName = "";
            int tableID;
            List<Attribute> attributes = new ArrayList<>();

            int[] pages;


            tableID = Integer.parseInt(catalog.get(currIndex));
            currIndex++;

            int lengthOfTableName = Integer.parseInt(catalog.get(currIndex));
            currIndex++;

            
            // go thru length of table name and keep adding to form a tableName
            for (int tabNameIndex = 0; tabNameIndex < lengthOfTableName; tabNameIndex++){
                String tabNameChar = catalog.get(currIndex);
                currIndex++;
                tableName += tabNameChar;
            }

            int numOfAttributes = Integer.parseInt(catalog.get(currIndex));
            currIndex++;

            ArrayList<Attribute> x = readAttribute(catalog, currIndex, numOfAttributes);


        }
    }

    // "NumOfAttributeinTable1", 
                // "Len of Att1 Name", "Att1 Name", "datatype", "bitmap is a string of 0s/1s", 
    public ArrayList<Attribute> readAttribute(ArrayList<String> catalog, int currIndex, int numOfAttributes){

        String attrName = "";
        String dataTypeString;
        ArrayList<Attribute> allAttributes = new ArrayList<>();;

        // go thru num of  attributes
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

}
