import java.util.ArrayList;
import java.util.List;

public class Catalog {
    private String dbLocation;
    private int pageSize;
    private int bufferSize;

    private List<Table> tables;
    private int tableCount;

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

    // catalog format: ["PS", "numOfTable", 
    // "Table1ID", "lengthOfTable1Name", "TableName1", "NumOfAttributeinTable1", 
        // "Len of Att1 Name", "Att1 Name", "datatype", "bitmap is a string of 01s", 
        // "Len of Att2 Name", "Att2 Name", "datatype", "bitmap is a string of 01s",
        // ...
        // "Len of AttN Name", "AttN Name", "datatype", "bitmap is a string of 01s",
    // "Table2ID", "lengthOfTable2Name", "TableName2", "NumOfAttributeinTable2"]
        // "Len of Att1 Name", "Att1 Name", "datatype", "bitmap is a string of 01s", 
        // "Len of Att2 Name", "Att2 Name", "datatype", "bitmap is a string of 01s",
        // ...
        // "Len of AttN Name", "AttN Name", "datatype", "bitmap is a string of 01s",
    // ...
        // "Len of Att1 Name", "Att1 Name", "datatype", "bitmap is a string of 01s", 
        // "Len of Att2 Name", "Att2 Name", "datatype", "bitmap is a string of 01s",
        // ...
        // "Len of AttN Name", "AttN Name", "datatype", "bitmap is a string of 01s"]
    public void parseCatalog(ArrayList<String> catalog){
        for (int i=0; i< catalog.size(); i++){  
            if (i == 0) {
                // index 0 = pageSIze
                this.pageSize = Integer.parseInt(catalog.get(i));
            } else if (i == 1) {
                // index 1 = # of tables
                this.tableCount = Integer.parseInt(catalog.get(i));
            } else {
                // rest of the arraylist are tables 
            }

        }


    }


}
