import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class parser {
    
    private static void createTable(String statement, Catalog catalog) {
        String[] tokens = statement.trim().split("\\s+", 3);
        
        if (tokens.length < 3 || !tokens[0].equalsIgnoreCase("create") || !tokens[1].equalsIgnoreCase("table")) {
            System.out.println("Syntax error in CREATE TABLE command.");
            return;
        }
    
        String tableDef = tokens[2].trim();
        int tableNameEnding = tableDef.indexOf('(');
    
        if (tableNameEnding == -1 || !tableDef.endsWith(");")) {
            System.out.println("Error: Expected '(' after table name and ');' at the end of the command.");
            return;
        }
    
        String tableName = tableDef.substring(0, tableNameEnding).trim();
        String attributesLine = tableDef.substring(tableNameEnding + 1, tableDef.length() - 2).trim();
        if(attributesLine.trim().equals("")){
            System.err.println("Table with no attributes");
            System.err.println("ERROR");
            return;
        }
    
        List<Attribute> attributes = Arrays.stream(attributesLine.split(",\\s*"))
                                           .map(Attribute::parse)
                                           .collect(Collectors.toList());
        
        if(attributes.contains(null)){
            System.err.println("Invalid datatype! Expected integer, double, boolean, char(N), varchar(N)");
            System.err.println("ERROR");
            return;
        }
    
        // Ensure exactly one primary key exists
        long primaryKeyCount = attributes.stream().filter(Attribute::isPrimaryKey).count();
        if (primaryKeyCount != 1) {
            System.err.println("No primary key defined\nERROR\n");
            return;
        }

        Set<String> seen = new HashSet<>();
        Set<String> duplicates = attributes.stream()
                                .map(Attribute::getName)
                                .filter(name -> !seen.add(name))
                                .collect(Collectors.toSet());
        if(duplicates.size() > 0){
            String values = "";
            for(String value : duplicates){
                values += "\"" + value + "\" ";
            }
            values = values.trim();
            System.err.println("Duplicate attribute name(s) " + values);
            System.err.println("ERROR");
            return;
        }
    
        // Check if table already exists
        if (catalog.getTables().stream().anyMatch(table -> table.getName().equals(tableName))) {
            System.err.println("Error: Table '" + tableName + "' already exists.");
            return;
        }
    
        // Create and add the new table
        Table table = new Table(tableName, catalog.getNextTableID(), attributes.size(), attributes.toArray(new Attribute[0]));
        catalog.addTable(table);
    
        System.out.println("SUCCESS\n");
    }    
    
    private static void dropTable(String statement, Catalog catalog) {
        String[] tokens = statement.split("\\s+");
        if (tokens.length != 3 || !tokens[0].equalsIgnoreCase("drop") || !tokens[1].equalsIgnoreCase("table")) {
            System.out.println("Syntax error in DROP TABLE command.");
            return;
        }
        String tableName = tokens[2].replace(";", "");
        catalog.dropTable(tableName);
        System.out.println("Table " + tableName + " dropped successfully.");
    }
    
    private static void insertInto(String statement, Catalog catalog, StorageManager storageManager) {
        String[] tokens = statement.trim().split("\\s+", 4);
    
        if (tokens.length < 4 || !tokens[0].equalsIgnoreCase("insert") || !tokens[1].equalsIgnoreCase("into")) {
            System.out.println("Syntax error in INSERT INTO command.");
            return;
        }
    
        String tableName = tokens[2];
        Table table = catalog.getTableByName(tableName);
        if (table == null) {
            System.out.println("No such table " + tableName);
            System.out.println("ERROR\n");
            return;
        }
    
        // Extract values part
        if (!tokens[3].contains("(") || !tokens[3].endsWith(";")) {
            System.out.println("Syntax error: Expected '(' and ';' in INSERT statement.");
            return;
        }
        String valuesPart = tokens[3].substring(tokens[3].indexOf("("), tokens[3].length() - 1); // Remove trailing semicolon
    
        String[] individualValueSets = valuesPart.split("\\),\\s*\\(");
        for (String valueSet : individualValueSets) {
            valueSet = valueSet.trim().replaceAll("^\\(|\\)$", ""); // Remove outer parentheses
            String currentRow = "Row (" + valueSet + ") :";
            String[] values = valueSet.split(" ");
    
            if (values.length != table.getAttributesCount()) {
                String expected = "";
                for(Attribute attr : table.getAttributes()){
                    expected += attr.getTypeFancy() + " ";
                }
                expected = expected.substring(0, expected.length() - 1); //Remove empty space for formatting
                String got = "";
                for(int index = 0; index < values.length; index++){
                    got += getActualType(values[index]);
                    if(index != values.length-1){
                        got += " ";
                    }
                }
                System.out.println(currentRow + " Too many attributes: expect(" + expected + ") got (" + got + ")");
                return;
            }
    
            ArrayList<Byte> nullBitMap = new ArrayList<>(table.getAttributesCount());
            ArrayList<Object> recordValues = new ArrayList<>();
    
            for (int i = 0; i < values.length; i++) {
                String value = values[i].trim();
                Attribute attribute = table.getAttributes()[i];
    
                // Handle NULL values
                if (value.equalsIgnoreCase("null")) {
                    recordValues.add(null);
                    nullBitMap.add((byte) 1);
                    continue;
                }
    
                // Parse value based on attribute type
                Object parsedValue = parseValueBasedOnType(value, attribute);
                if (parsedValue == null) {
                    String expected = "";
                    for(Attribute attr : table.getAttributes()){
                        expected += attr.getTypeFancy() + " ";
                    }
                    expected = expected.substring(0, expected.length() - 1); //Remove empty space for formatting
                    String acutalType = getActualType(value);
                    System.err.println(currentRow + "Invalid data types: expected (" + expected + ")" + " got (" + acutalType + ")");
                    return;
                }
                
                // Validate char and varchar lengths
                if (parsedValue instanceof String) {
                    int length = ((String) parsedValue).length();
                    if ((attribute.getType().equals("char") || attribute.getType().equals("varchar")) && length > attribute.getSize()) {
                        System.err.println("Error: Expected max length of " + attribute.getSize() + " for attribute " + attribute.getName());
                        return;
                    }
                }
    
                recordValues.add(parsedValue);
                nullBitMap.add((byte) 0);
            }
    
            int recordSize = calculateRecordSize(recordValues, table.getAttributes());
            Record newRecord = new Record(recordSize, recordValues, nullBitMap);
    
            if (!storageManager.addRecord(catalog, newRecord, table.getTableID())) {
                return;
            }
        }
    
        // System.out.println("Record(s) inserted successfully into table: " + tableName);
    }    
    
    private static String getActualType(String value) {
        if (value.matches("-?\\d+")) return "integer";
        if (value.matches("-?\\d+(\\.\\d+)?")) return "double";
        if (value.equalsIgnoreCase("true") || value.equalsIgnoreCase("false")) return "boolean";
        if (value.length() == 1) return "char";
        return "varchar";
    } 

    private static Object parseValueBasedOnType(String value, Attribute attribute) {
        value = value.replaceAll("^\"|\"$", "");
    
        try{
            switch (attribute.getType().toLowerCase()) {
                case "integer": return Integer.parseInt(value);
                case "double": return Double.parseDouble(value);
                case "boolean": return Boolean.parseBoolean(value);
                case "char":
                case "varchar": return value;
                default:
                    System.err.println("Unsupported attribute type: " + attribute.getType());
                    return null;
            }
        }
        catch(Exception e){
            e.printStackTrace();
            return null;
        }
    }
    
    private static int calculateRecordSize(ArrayList<Object> values, Attribute[] attributes) {
        int size = 0;
        for (int i = 0; i < values.size(); i++) {
            Object value = values.get(i);
            Attribute attr = attributes[i];
    
            if (value == null) continue;
            switch (attr.getType().toLowerCase()) {
                case "integer":
                    size += Integer.BYTES;
                    break;
                case "double":
                    size += Double.BYTES;
                    break;
                case "boolean":
                    size += 1;
                    break;
                case "char":
                    size += attr.getSize();
                    break;
                case "varchar":
                    String stringValue = (String) value;
                    size += stringValue.getBytes().length;
                    // Include 4 bytes to store the length of varchar if needed
                    size += Integer.BYTES;
                    break;
                default:
                    System.out.println("Unsupported attribute type: " + attr.getType());
                    break;
            }
        }
        return size;
    }

    private static void displaySchema(Catalog catalog) {
        System.out.printf("DB Location: %s%nPage Size: %d%nBuffer Size: %d%n%n", 
                          catalog.getDbLocation(), catalog.getPageSize(), catalog.getBufferSize());
    
        List<Table> tables = catalog.getTables();
        if (tables.isEmpty()) {
            System.out.println("No tables to display.");
            System.out.println("SUCCESS\n");
            return;
        }
        
        // cannot figure out why display schema is also displaying number of records one by one
        System.out.println("Tables:\n");
        for (Table table : tables) {
            System.out.printf("Table name: %s%nTable schema:%n", table.getName());
            System.out.println("SUCCESS\n");
    
            for (Attribute attr : table.getAttributes()) {
                System.out.printf("    %s: %s%s%s%s%n",
                    attr.getName(), attr.getType(),
                    attr.isPrimaryKey() ? " primarykey" : "",
                    attr.isUnique() ? " unique" : "",
                    attr.isnonNull() ? " notnull" : ""
                );
            }
    
            System.out.printf("Pages: %d%nRecords: %s%n%n", table.getPageCount(), table.getRecordCount());
        }
    
        System.out.println("SUCCESS\n");
    }    

    private static void select(String normalizedStatement, Catalog catalog, StorageManager storageManager) {

        // check if input is in format: select * from foo;
        String[] parts = normalizedStatement.split(" ");
        if (parts.length < 4) {
            System.out.println("Invalid command format.");
            return;
        }
        

        String tableStr = parts[3];
        // get rid of the ";"
        String tableName = tableStr.substring(0, tableStr.length()-1);
        Table selectedTable = catalog.getTableByName(tableName);

        // check if table exists...does not exist
        if (selectedTable == null) {
            System.out.println("No such table " + tableName);
            System.out.println("ERROR\n");
            return;
        }

        // used to store the max length of each attribute
        // Key = atrr name
        // Value = max length of the attribute's data
        HashMap<String, Integer> maxAttributeLength = new HashMap<String, Integer>();
        Attribute[] attrOfSelectedTable = selectedTable.getAttributes();


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
                    if (valueString.length() > maxAttributeLength.get(attrOfSelectedTable[j].getName())) {
                        // update the max length of the attribute
                        maxAttributeLength.put(attrOfSelectedTable[j].getName(), valueString.length());
                        // replace? or add new one
                    }
                }
            }
        }

        // everything above is just to determine the max size of each tuple
        // everything below will be for PRINTING to output

        // num of dashes = (number of attributes total + 1) + (each attribute's max length +1)
        int totalLength = 0;
        for (Attribute attr : attrOfSelectedTable) {
            totalLength += maxAttributeLength.get(attr.getName()) + 1;
        }
        totalLength += attrOfSelectedTable.length + 1;

        // first "-------" line
        System.out.println("-".repeat(totalLength));



        // print the attribute names
        // print "|" at in the beginning
        // if attribute's max length is a even number then:
            // numofSpacesFrontAndBack = ((max length of each attribute+1) - (length of the attribute name)) / 2
            // print:
            //  " "* numofSpacesFrontAndBack
            //  attribute name
            // " "* numofSpacesFrontAndBack
        // else
            // numofSpaces = ((max length of each attribute+1) - (length of the attribute name)) 
            // numOfSpacesFront = numOfSpaces / 2 ROUNDED UP
            // numOfSpacesBack = numOfSpaces / 2 ROUNDED DOWN
            // print:
            //  " "* numofSpacesFront
            //  attribute name
            // " "* numofSpacesBack
        // print "|" at the end
        
        for (Attribute attr : attrOfSelectedTable) {
            int maxAttrLength = maxAttributeLength.getOrDefault(attr.getName(), 0);
            maxAttrLength = Math.max(maxAttrLength, attr.getName().length()); // Ensure it's at least as long as the attribute name
        
            int spacesFront = 0;
            int spacesBack = 0;
        
            if (maxAttrLength % 2 == 0) {
                spacesFront = (maxAttrLength - attr.getName().length()) / 2;
                spacesBack = spacesFront;
            } else {
                int spaces = maxAttrLength - attr.getName().length(); 
                spacesFront = spaces / 2 + 1;  
                spacesBack = spaces - spacesFront;
            }
        
            System.out.print("|");
            System.out.print(" ".repeat(Math.max(0, spacesFront))); // Ensure non-negative repeat value
            System.out.print(attr.getName());
            System.out.print(" ".repeat(Math.max(0, spacesBack))); // Ensure non-negative repeat value
        }
        // print remaining "|"
        System.err.println("|");
        
        // print 2nd "-------" line
        System.out.println("-".repeat(totalLength));


        // print each attribute's values
        // print the actual data now
        // print "|" at in the beginning
        // for each attribute:
            // numofSpaces = attribute's MAX length + 1
            // numOfSpacesFront = numOfSpaces - CURRENT attribute's value length 
            // print:
            //  " "* numofSpacesFront
            //  attribute's value
            // "|"
        // print "|" at the end
        // [ [Tuples], [Tuples], [Tuples], ... ]
        for (List<Object> recordTuple : listOfRecordTuples) {

            // [value, value, value, ...]
            for (int j = 0; j < recordTuple.size(); j++) {

                // value of curr attr
                Object value = recordTuple.get(j);

                Attribute currAttr = attrOfSelectedTable[j];

                int maxAttrLength = maxAttributeLength.get(currAttr.getName());
                int spaces = maxAttrLength + 1;
                int spacesFront = 0;

                if (value != null) {
                    String valueString = value.toString();
                    spacesFront = spaces - valueString.length();
                    System.out.print("|");
                    System.out.print(" ".repeat(spacesFront));
                    System.out.print(valueString);
                } else {
                    System.out.print("|");
                    System.out.print(" ".repeat(spaces));
                }
            }
            System.out.println("|");
        }

        // print ending "SUCCESS" message
        System.out.println("\nSUCCESS\n");

    }


    public static void parse(String statement, Catalog catalog, PageBuffer buffer, String dbDirectory, int pageSize, StorageManager storageManager) {
        String[] tokens = statement.trim().split("\\s+");
        if (tokens.length == 0) {
            System.out.println("No input detected.");
            return;
        }

        String command = tokens[0].toLowerCase();
        
        switch (command) {
            case "quit":
            case "<quit>":
                System.out.println("\nSafely shutting down the database...");
                Main.writeBuffer();
                try {
                    Main.writeCatalogToFile(dbDirectory + "/catalog.bin");
                } catch (IOException e) {
                    System.err.println("Error while saving catalog: " + e.getMessage());
                }
                System.out.println("Exiting the database...\n");
                System.exit(0);
                break;

            case "create":
                createTable(statement, catalog);
                break;

            case "drop":
                dropTable(statement, catalog);
                break;

            case "alter":
                //alterTable(statement, catalog, storageManager);
                break;

            case "insert":
                insertInto(statement, catalog, storageManager);
                break;

            case "select":
                select(statement, catalog, storageManager);
                break;

            case "delete":
                //delete(statement, catalog, storageManager);
                break;

            case "update":
                //update(statement, catalog, storageManager);
                break;

            case "display":
            if (tokens.length > 2 && tokens[1].equalsIgnoreCase("info")) {
                String tableName = tokens[2].replaceAll(";", "");
                if (!catalog.tableExists(tableName)) {
                    System.out.println("No such table " + tableName + "\nERROR\n");
                } else {
                    boolean found = catalog.displayInfo(tableName);
                    if (found) {
                        System.out.println("SUCCESS");
                    } else {
                        System.out.println("ERROR");
                    }
                }
            } else {
                displaySchema(catalog);
            }
            break;
        
            default:
                System.out.println("Unknown command: " + command);
        }
    }
}