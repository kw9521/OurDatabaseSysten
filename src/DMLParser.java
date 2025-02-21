import java.nio.channels.Pipe.SourceChannel;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
                        insertInto(normalizedStatement, catalog, storageManager);
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

    private void insertInto(String statement, Catalog catalog, StorageManager storageManager) {
        
        //Replace multiple consecutive whitespace with one. 
        statement = statement.replaceAll("\s+", " ").strip();

        int valuesIndex = statement.toLowerCase().indexOf(" values ");
        if (valuesIndex == -1) {
            System.err.println("Syntax error: Missing 'VALUES' keyword.");
            return;
        }

        //Validate that table exist in database 
        String tableName = statement.substring(11, valuesIndex).strip();
        Table table = catalog.getTableByName(tableName);
        if (table == null) {
            System.out.println("No such table " + tableName);
            System.out.println("ERROR\n");
            return;
        }

        //Extract Tuples 
        String valuesSection = statement.substring(valuesIndex + 7).strip();
        List<String> extractedTuples = new ArrayList<>();
        int start = -1; 
        int end = -1;   

        for (int i = 0; i < valuesSection.length(); i++) {
            char c = valuesSection.charAt(i);

            if (c == '(') {
                start = i + 1;  
            } else if (c == ')' && start != -1) {
                end = i;  
                extractedTuples.add(valuesSection.substring(start, end).trim());
                start = -1; 
            }
        }

        //Get all the attributes of the table
        List<Attribute> attributes = table.getAttributes();
        int attributeCount = attributes.size();

        // Get all existing records for primary key validation
        List<List<Object>> existingRecords = storageManager.getRecords(table.getTableID());
        Set<Object> primaryKeyValues = new HashSet<>();

        // Identify the primary key attribute index, then store existing primary key values
        int primaryKeyIndex = -1;
        for (int i = 0; i < attributes.size(); i++) {
            if (attributes.get(i).isPrimaryKey()) {
                primaryKeyIndex = i;
                break;
            }
        }
        for (List<Object> record : existingRecords) {
            primaryKeyValues.add(record.get(primaryKeyIndex));
        }

        //Process Each Tuples 
        for(String tuple: extractedTuples){
            //Parese each tuples into lists
            List<String> tokens = new ArrayList<>();
            String currentToken = "";
            boolean inQuotes = false;
            for (int i = 0; i < tuple.length(); i++) {
                char c = tuple.charAt(i);
                if (c == '"') {
                    if (!inQuotes && !currentToken.isEmpty()) {
                        tokens.add(currentToken);
                        currentToken = "";
                    }
                    inQuotes = !inQuotes; 
                    continue; 
                }
                else if (c == ' ' && !inQuotes) {
                    if (!currentToken.isEmpty()) {
                        tokens.add(currentToken);
                        currentToken = "";
                    }
                } 
                else {
                    if (i > 0 && Character.isDigit(tuple.charAt(i - 1)) && c == '"') {
                        tokens.add(currentToken);
                        currentToken = "";
                    }
                    currentToken += c;
                }
            }

            if (!currentToken.isEmpty()) {
                tokens.add(currentToken);
            }

            //Handle when input values does not match table attribute
            if(tokens.size() != attributeCount){
                System.err.println("Error: Mismatch in number of values and attributes.");
                return;
            }

            //Parse each tokens to it corresponding type
            ArrayList<Object> parsedValues = new ArrayList<>();
            for (int i = 0; i < tokens.size(); i++) {
                Attribute attr = attributes.get(i);
                String value = tokens.get(i);
                
                if(value == null){
                    if(!attr.isNullable()){
                        System.err.println("Error: Attribute '" + attr.getName() + "' cannot be NULL.");
                    return;
                    }
                    parsedValues.add(null);
                    continue;
                }

                try {
                    switch (attr.getType()) {
                        case "integer":
                            parsedValues.add(Integer.parseInt(value));
                            break;
                        case "double":
                            parsedValues.add(Double.parseDouble(value));
                            break;
                        case "boolean":
                            parsedValues.add(Boolean.parseBoolean(value));
                            break;
                        case "char":
                            //How to parse to char
                            break;
                        case "varchar":
                            //How to parse to varchar
                            break;
                        default:
                            System.err.println("Error: Unsupported attribute type '" + attr.getType() + "'.");
                            return;
                    }
                } catch (Exception e) {
                    System.err.println("Error: Invalid value for attribute '" + attr.getName() + "'.");
                    return;
                }
            }

            //Handle Primary Key
            Object primaryKeyValue = parsedValues.get(primaryKeyIndex);
            if (primaryKeyValue == null) {
                System.err.println("Error: Primary key '" + attributes.get(primaryKeyIndex).getName() + "' cannot be NULL.");
                return;
            }
            if (primaryKeyValues.contains(primaryKeyValue)) {
                System.err.println("Error: Duplicate primary key value '" + primaryKeyValue + "'.");
                return;
            }

            //Create a record and insert
            Record currentRecord = new Record(0, parsedValues);

            //Call StorageManager to insert currentRecord
            storageManager.addRecord(table.getTableID(), currentRecord);

            // Add primary key value to set
            primaryKeyValues.add(primaryKeyValue);
        }
    }

    private static void displaySchema(String normalizedStatement, Catalog catalog) {
        System.out.println("DB Location: " + catalog.getDbLocation());
        System.out.println("Page Size: " + catalog.getPageSize());
        System.out.println("Buffer Size: " + catalog.getBufferSize() + "\n");

        List<Table> tables = catalog.getTables();
        if (tables == null || tables.isEmpty()) {
            System.out.println("No tables to display");
            System.out.println("SUCCESS\n");
            return;
        }

        System.out.println("Tables:\n");
        for (Table table : tables) {
            System.out.println("Table name: " + table.getName());
            System.out.println("Table schema:");

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
        
        String tableStr = parts[2];
        // get rid of the ";" at the end
        String tableName = tableStr.substring(0, tableStr.length()-1);
        
        List<Table> tables = catalog.getTables();
        if (tables == null || tables.isEmpty()) {
            System.out.println("No such table " + tableName);
            System.out.println("ERROR\n");
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
                System.out.println("SUCCESS\n");
                return;
            }
        }
        
        System.out.println("No such table " + tableName);
        System.out.println("ERROR\n");
    }    

    private void select(String normalizedStatement, Catalog catalog, StorageManager storageManager) {

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
        List<Attribute> attrOfSelectedTable = selectedTable.getAttributes();


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
        totalLength += attrOfSelectedTable.size() + 1;

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
            int maxAttrLength = maxAttributeLength.get(attr.getName());
            int spacesFront = 0;
            int spacesBack = 0;

            if (maxAttrLength % 2 == 0) {
                spacesFront = (maxAttrLength - attr.getName().length()) / 2;
                spacesBack = spacesFront;
            } else {
                int spaces = maxAttrLength - attr.getName().length(); // 5
                spacesFront = spaces / 2 + 1;   // 5/2 = 2 + 1 =3
                spacesBack = spaces - spacesFront;
            }

            System.out.print("|");
            System.out.print(" ".repeat(spacesFront));
            System.out.print(attr.getName());
            System.out.print(" ".repeat(spacesBack));
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

                Attribute currAttr = attrOfSelectedTable.get(j);

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
        System.out.println("SUCCESS");

    }
}
