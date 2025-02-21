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
    
        List<Attribute> attributes = Arrays.stream(attributesLine.split(",\\s*"))
                                           .map(Attribute::parse)
                                           .collect(Collectors.toList());
    
        // Ensure exactly one primary key exists
        long primaryKeyCount = attributes.stream().filter(Attribute::isPrimaryKey).count();
        if (primaryKeyCount != 1) {
            System.err.println("Error: Tables must have exactly one primary key.");
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
    
        System.out.println("Table '" + tableName + "' created successfully.");
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
            System.out.println("Table not found: " + tableName);
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
            String[] values = valueSet.split(" ");
    
            if (values.length != table.getAttributesCount()) {
                System.out.println("Mismatch between number of columns and values provided.");
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
                    System.err.println("Error parsing value: " + value + " for attribute: " + attribute.getName());
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
    
        System.out.println("Record(s) inserted successfully into table: " + tableName);
    }    
    
    private static Object parseValueBasedOnType(String value, Attribute attribute) {
        value = value.replaceAll("^\"|\"$", "");
    
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
        System.out.printf("\nDB Location: %s%nPage Size: %d%nBuffer Size: %d%n%n", 
                          catalog.getDbLocation(), catalog.getPageSize(), catalog.getBufferSize());
    
        List<Table> tables = catalog.getTables();
        if (tables.isEmpty()) {
            System.out.println("No tables to display.");
            return;
        }
    
        System.out.println("Tables:\n");
        for (Table table : tables) {
            System.out.printf("Table Name: %s%nSchema:%n", table.getName());
    
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
    
        System.out.println("SUCCESS");
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
                //select(statement, catalog, storageManager);
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
                    System.out.println("No such table " + tableName + "\nERROR");
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