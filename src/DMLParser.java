import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.naming.directory.AttributeInUseException;

public class DMLParser {
    // Example creation:
    // JottQL> create table foo(
    // num integer primarykey);
    // SUCCESS

    private static final String INSERT_INTO_STATEMENT = "insert into";
    private static final String DISPLAY_SCHEMA_STATEMENT = "display schema";
    private static final String DISPLAY_INFO_STATEMENT = "display info";
    private static final String SELECT_STATEMENT = "select";

    private static final String INVALID_STATEMENT = "An invalid statement has been parsed.";

    public void parseDMLstatement(String statement, Catalog catalog, StorageManager storageManager ) throws DMLParserException {
        // Lowercase + strip leading whitespace
        String normalizedStatement = statement.stripLeading();
        // Split into command and args
        String[] tokens = normalizedStatement.split("\s+", 2);

        if (tokens.length == 0) {
            throw new DMLParserException(INVALID_STATEMENT + ": " + statement);
        }

        String command = tokens[0].toLowerCase();

        try {
            switch (command) {
                case "insert":
                    if (tokens.length > 1 && tokens[1].startsWith("into")) {
                        insertInto(normalizedStatement);
                    } else {
                        throw new DMLParserException("Invalid insert syntax: " + statement);
                    }
                    break;
                case "display":
                    if (tokens.length > 1) {
                        if (tokens[1].startsWith("schema")) {
                            displaySchema(normalizedStatement, catalog);
                        } else if (tokens[1].startsWith("info")) {
                            displayInfo(normalizedStatement, catalog);
                        } else {
                            throw new DMLParserException("Invalid display syntax: " + statement);
                        }
                    } else {
                        throw new DMLParserException("Incomplete display statement: " + statement);
                    }
                    break;
                case "select":
                    select(normalizedStatement, catalog, storageManager);
                    break;
                default:
                    throw new DMLParserException(INVALID_STATEMENT + ": " + statement);
            }
        } catch (DMLParserException e) {
            // Handle custom DMLParserException
            System.err.println("DMLParserException: " + e.getMessage());
            throw e; // Re-throwing the exception if needed
        } catch (Exception e) {
            // Handle any other unexpected exceptions
            System.err.println("An unexpected error occurred: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void insertInto(String statement) {
        // Implement
        System.out.println("Inserting into table...");
    }

    private static void displaySchema(String normalizedStatement, Catalog catalog) {
        System.out.println("\nDB Location: " + catalog.getDbLocation());
        System.out.println("Page Size: " + catalog.getPageSize());
        System.out.println("Buffer Size: " + catalog.getBufferSize() + "\n");

        List<Table> tables = catalog.getTables();
        if (tables == null || tables.isEmpty()) {
            System.out.println("No tables to display.\n");
            System.out.println("SUCCESS");
            return;
        }

        System.out.println("Tables:\n");
        for (Table table : tables) {
            System.out.println("Table Name: " + table.getName());
            System.out.println("Table Schema:");

            for (Attribute attr : table.getAttributes()) {
                StringBuilder attributeDetails = new StringBuilder();
                attributeDetails.append("    ").append(attr.getName()).append(":").append(attr.getType());

                if (attr.isPrimaryKey()) attributeDetails.append(" primarykey");
                if (attr.isUnique()) attributeDetails.append(" unique");
                if (attr.isNullable()) attributeDetails.append(" notnull");

                System.out.println(attributeDetails);
            }

            System.out.println("Pages: " + table.getPageCount());
            System.out.println("Records: " + table.getRecordCount() + "\n");
        }

        System.out.println("SUCCESS");
    }

    private static void displayInfo(String normalizedStatement, Catalog catalog) {
        String[] parts = normalizedStatement.split(" ");
        if (parts.length < 3) {
            System.out.println("Invalid command format.");
            return;
        }
        
        String tableName = parts[2];
        
        List<Table> tables = catalog.getTables();
        if (tables == null || tables.isEmpty()) {
            System.out.println("Table" + tableName + "not found.");
            return;
        }
    
        for (Table table : tables) {
            if (table.getName().equalsIgnoreCase(tableName)) {
                System.out.println("Table Name: " + table.getName());
                System.out.println("Table Schema:");
    
                for (Attribute attr : table.getAttributes()) {
                    StringBuilder attributeDetails = new StringBuilder();
                    attributeDetails.append("    ").append(attr.getName()).append(":").append(attr.getType());
    
                    if (attr.isPrimaryKey()) attributeDetails.append(" primarykey");
                    if (attr.isUnique()) attributeDetails.append(" unique");
                    if (attr.isNullable()) attributeDetails.append(" notnull");
    
                    System.out.println(attributeDetails);
                }
    
                System.out.println("Pages: " + table.getPageCount());
                System.out.println("Records: " + table.getRecordCount());
                System.out.println("SUCCESS");
                return;
            }
        }
        
        System.out.println("No such table " + tableName);
        System.out.println("ERROR");
    }    

    private void select(String normalizedStatement, Catalog catalog, StorageManager storageManager) {

        // check if input is in format: select * from foo;
        String[] parts = normalizedStatement.split(" ");
        if (parts.length < 4) {
            System.out.println("Invalid command format.");
            return;
        }

        String tableName = parts[3];
        Table selectedTable = catalog.getTableByName(tableName);

        // check if table exists...does not exist
        if (selectedTable == null) {
            System.out.println("No such table " + tableName);
            System.out.println("ERROR");
            return;
        }

        // used to store the max length of each attribute
        // Key = atrr name
        // Value = max length of the attribute's data
        HashMap<String, Integer> maxAttributeLength = new HashMap<String, Integer>();
        List<Attribute> attrOfSelectedTable = selectedTable.getAttributes();


        // go thru each attribute associated with selected table and get length of attribute name
        // initial max length will be max length of the attribute name
        for (Attribute attr : attrOfSelectedTable) {
            maxAttributeLength.put(attr.getName(), attr.getName().length());
        }

        // get ID associated with selcted Table
        int tableNumber = selectedTable.getTableID();

        // go thru each page of the selected table and get all records associated with this page via getRecords() 
            // go thru all the records and call getData() of Record.java and get length of the name of the record
            
        // [ [Tuples], [Tuples], [Tuples], ... ]
        // getRecords gets all records associated with the table, even with page splits
        List<List<Object>> listOfRecordTuples = storageManager.getRecords(tableNumber);
        
        // recordTuple = each individual tuple in the list of tuples
        for (List<Object> recordTuple : listOfRecordTuples) {
            
            // go thru each value in above tuple
             // tuple: [value, value, value, ...]
            for (int j = 0; j < recordTuple.size(); j++) {
                Object value = recordTuple.get(j);
                if (value != null) {
                    String valueString = value.toString();
                    
                    // go thru entire tuple, check if length of that attr's value is greater than the one currently stored in the hashmap
                    if (valueString.length() > maxAttributeLength.get(attrOfSelectedTable.get(j).getName())) {
                        // update the max length of the attribute
                        maxAttributeLength.put(attrOfSelectedTable.get(j).getName(), valueString.length());
                    }
                }
            }
        }

        // everything above is just to determine the max size of each tuple
        // everything below will be for PRINTING to output

    }
}
