import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class parser {
    
    private static void createTable(String inputLine, Catalog catalog) {
        String[] parts = inputLine.trim().split("\\s+", 3);
    
        if (parts.length < 3 || !parts[0].equalsIgnoreCase("create") || !parts[1].equalsIgnoreCase("table")) {
            System.err.println("Syntax error in CREATE TABLE command.");
            return;
        }
    
        String tableDef = parts[2].trim();
        int openParenIdx = tableDef.indexOf('(');
    
        if (openParenIdx == -1 || !tableDef.endsWith(");")) {
            System.err.println("Expected '(' after table name and ');' at the end of the statement.");
            return;
        }
    
        String tableName = tableDef.substring(0, openParenIdx).trim();
        String attributesLine = tableDef.substring(openParenIdx + 1, tableDef.length() - 2).trim();
    
        if (attributesLine.isEmpty()) {
            System.err.println("Error: Table must have at least one attribute.");
            return;
        }
    
        // Parse attributes
        String[] tokens = attributesLine.split(",\\s*");
        ArrayList<Attribute> attributes = new ArrayList<>();
        Attribute primaryKey = null;
    
        for (String token : tokens) {
            Attribute attr = Attribute.parse(token.trim());
    
            if (attr == null) {
                String invalidType = token.trim().split("\\s+")[1];
                System.err.println("Invalid datatype \"" + invalidType + "\"");
                System.err.println("ERROR");
                return;
            }
    
            attributes.add(attr);
            if (attr.getPrimaryKey()) primaryKey = attr;
        }
    
        // Ensure exactly one primary key
        long primaryKeyCount = attributes.stream().filter(Attribute::getPrimaryKey).count();
        if (primaryKeyCount != 1) {
            System.err.println(primaryKeyCount == 0
                ? "Error: No primary key defined."
                : "Error: More than one primary key.");
            return;
        }
    
        // Check for duplicate attribute names
        Set<String> seen = new HashSet<>();
        Set<String> duplicates = attributes.stream()
                                           .map(Attribute::getName)
                                           .filter(name -> !seen.add(name))
                                           .collect(Collectors.toSet());
    
        if (!duplicates.isEmpty()) {
            String duplicateNames = String.join(" ", duplicates.stream().map(n -> "\"" + n + "\"").toList());
            System.err.println("Duplicate attribute name(s): " + duplicateNames);
            System.err.println("ERROR");
            return;
        }
    
        // Check if table already exists
        boolean tableExists = catalog.getTables().stream()
                                     .anyMatch(table -> table.getName().equalsIgnoreCase(tableName));
    
        if (tableExists) {
            System.err.println("Error: Table \"" + tableName + "\" already exists.");
            return;
        }
    
        // Create and register the table
        Table table = new Table(tableName, catalog.getNextTableID(), attributes.size(), attributes.toArray(new Attribute[0]));
        catalog.addTable(table);
    
        // Create a B+ Tree index if indexing is enabled
        if (Main.getIndexing()) {
            ArrayList<BPlusTree> trees = Main.getBPlusTrees();
            BPlusTree index = new BPlusTree(primaryKey, table.getTableID());
            trees.add(index);
            System.out.println("BPlus Tree created successfully.");
        }
    
        System.out.println("Table \"" + tableName + "\" created successfully.");
    }    
    
    private static void alterTable(String inputLine, Catalog catalog, StorageManager storageManager) {
        String[] tokens = inputLine.split("\\s+", 5);

        if (tokens.length < 5 || !tokens[0].equalsIgnoreCase("alter") || !tokens[1].equalsIgnoreCase("table")) {
            System.out.println("Syntax error in ALTER TABLE command.");
            return;
        }

        String tableName = tokens[2];
        Table table = catalog.getTableByName(tableName);

        if (table == null) {
            System.out.println("Table " + tableName + " not found.");
            return;
        }

        String operation = tokens[3].toLowerCase();
        String definition = tokens[4].replace(";", "");

        if (operation.equals("add")) {
            Attribute newAttr = Attribute.parse(definition);
            table.addAttribute(newAttr);
        
            Attribute[] attributes = table.getAttributes();
            int newAttributeIndex = attributes.length - 1;
        
            List<Page> pages = storageManager.getPages(table.getTableID());
            for (Page page : pages) {
                for (Record record : page.getRecords()) {
                    Object defaultValue = newAttr.getDefaultValue();
                    
                    if (definition.contains("default")) {
                        String[] definitionParts = definition.split("\\s+");
                        String defVal = definitionParts[definitionParts.length - 1];
                        attributes[newAttributeIndex].setDefaultValue(defaultValue);

                        record.addValue(defVal, newAttributeIndex, newAttr);
                    } else {
                        // Add null value and adjust size
                        record.addValue(null, newAttributeIndex, newAttr);
                    }
                }
                // Recalculate Page Size test
                page.reCalcPageSize(); 
                // Update the page size to reflect new attribute addition
                page.setSize(page.getSize());
                if (page.isOverfull()) {
                    storageManager.splitPage(page);
                }
            }
        
        }
        
         else if (operation.equals("drop")) {
            Attribute[] attributes = table.getAttributes();
            List<Page> pages = storageManager.getPages(table.getTableID());

            OptionalInt attributeIndexOpt = IntStream.range(0, attributes.length)
                    .filter(i -> attributes[i].getName().equals(definition))
                    .findFirst();

            if (attributeIndexOpt.isEmpty()) {
                System.out.println("Attribute " + definition + " not found in table " + tableName + ".");
                return;
            }

            int attributeIndex = attributeIndexOpt.getAsInt();

            for (Page page : pages) {
                for (Record record : page.getRecords()) {
                    int sizeLost = record.removeValue(attributeIndex, attributes[attributeIndex]);
                    page.setSize(page.getSize() - sizeLost);
                    // record.removeAttribute(attributeIndex);
                }
            }

            table.dropAttribute(definition);
        } else {
            System.out.println("Unsupported ALTER TABLE operation: " + operation);
        }
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
            String currentRow = "row (" + valueSet + "):";
            
            // Split by spaces but keep quoted substrings together
            String[] values = valueSet.split("\\s*(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)\\s+");
            for (int i = 0; i < values.length; i++) {
                values[i] = values[i].replaceAll("^\"|\"$", "").trim();
            }
    
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
                System.out.println("\n"+currentRow + " Too many attributes: expect(" + expected + ") got (" + got + ")");
                System.out.println("ERROR\n");
                return;
            }
    
            ArrayList<Byte> nullBitMap = new ArrayList<>(table.getAttributesCount());
            ArrayList<Object> recordValues = new ArrayList<>();
            Object primaryKeyValue = null; // used for BPlusTree

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
                    String got = "";
                    for(int index = 0; index < values.length; index++){
                        got += getActualType(values[index]);
                        if(index != values.length-1){
                            got += " ";
                        }
                    }
                    System.err.println("\n"+currentRow + " Invalid data types: expected (" + expected + ")" + " got (" + got + ")");
                    System.out.println("ERROR\n");
                    return;
                }
                
                // Validate char and varchar lengths
                if (parsedValue instanceof String) {
                    int length = ((String) parsedValue).length();
                    if ((attribute.getType().equals("char") || attribute.getType().equals("varchar")) && length > attribute.getSize()) {
                        System.err.println("\nrow (" + valueSet + "): " + attribute.getType() + "(" + 
                            attribute.getSize() + ") can only accept " + attribute.getSize() + " " + 
                            attribute.getType() + "s; " + value + " is " + value.length());
                        System.err.println("ERROR\n");
                        return;
                    }
                }
    
                recordValues.add(parsedValue);
                nullBitMap.add((byte) 0);

                if (attribute.getPrimaryKey()) primaryKeyValue = parsedValue;
            }
    
            int recordSize = calculateRecordSize(recordValues, table.getAttributes());
            Record newRecord = new Record(recordSize, recordValues, nullBitMap);
                // choose insert operation based on if indexing is on or not
                if (Main.getIndexing()) {
                    BPlusTree bPlusTree = Main.getBPlusTrees().get(table.getTableID());
                    boolean success = bPlusTree.insert(newRecord, primaryKeyValue, recordSize);
                    if (!success) {
                        System.out.println("Insert failed: duplicate primary key");
                        return;
                    }
                }
                else{
                    if (!storageManager.addRecord(catalog, newRecord, table.getTableID())) {
                        return;
                    }
                }
        }
        System.out.println("SUCCESS\n");
    
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
                case "char": return value; 
                case "varchar": return value;
                default:
                    System.err.println("Unsupported attribute type: " + attribute.getType());
                    return null;
            }
        }
        catch(Exception e){
            // e.printStackTrace();
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
    
    private static void select(String normalizedStatement, Catalog catalog, StorageManager storageManager){
        // select * from foo;
        // select name, gpa from student;
        // select name, dept_name from student, department where student.dept_id = department.dept_id;
        // select * from foo orderby x;
        // select t1.a, t2.b, t2.c, t3.d from t1, t2, t3 where t1.a = t2.b and t2.c = t3.d orderby t1.a;    length = 20
        
        if (normalizedStatement.endsWith(";")) {
            normalizedStatement = normalizedStatement.substring(0, normalizedStatement.length() - 1);
        }
        String[] words = normalizedStatement.replace(",", "").trim().split("\\s+");

        // Find the index positions of "from", "where", and "orderby"
        int fromIndex = -1, whereIndex = -1, orderByIndex = -1;
        for (int i = 0; i < words.length; i++) {
            if (words[i].equalsIgnoreCase("from")) {
                fromIndex = i;
            } else if (words[i].equalsIgnoreCase("where")) {
                whereIndex = i;
            } else if (words[i].equalsIgnoreCase("orderby")) {
                orderByIndex = i;
            }
        }

        // Error if "from" is missing or incomplete: SELECT x; or SELECT x FROM;
        if (fromIndex == -1 || fromIndex == words.length - 1) {
            System.out.println("Syntax error: Missing or incomplete 'from' clause.");
            return;
        }

        // Extract attributes between "select" and "from": SELECT x, y, z FROM ...
        ArrayList<String> allAttr = new ArrayList<>();
        for (int i = 1; i < fromIndex; i++) {
            allAttr.add(words[i]);
        }

        // Extract tables between "from" and either "where" or "orderby" or end of statement
        ArrayList<String> allTables = new ArrayList<>();
        int endOfTables = (whereIndex != -1) ? whereIndex : (orderByIndex != -1 ? orderByIndex : words.length);
        for (int i = fromIndex + 1; i < endOfTables; i++) {
            allTables.add(words[i]);
        }

        // Validate table existence in the catalog
        List<Table> tableObjects = new ArrayList<>();
        for (String tableName : allTables) {
            Table table = catalog.getTableByName(tableName);
            if (table == null) {
                System.out.println("No such table " + tableName);
                System.out.println("ERROR\n");
                return;
            }
            tableObjects.add(table);
        }

        // If select * was used, replace allAttr with all attributes from the tables
        // allAttr will be in format: [foo.x, foo.y, ...]
        if (allAttr.size() == 1 && allAttr.get(0).equals("*")) {
            allAttr.clear(); // Remove "*"
            for (Table table : tableObjects) {
                for (Attribute attr : table.getAttributes()) {
                    allAttr.add(table.getName() + "." + attr.getName());
                }
            }
        } else {
            // Validate that all selected attributes exist in the specified tables
            ArrayList<String> qualifiedAttr = new ArrayList<>();
            for (String attr : allAttr) {
                boolean found = false;
                for (Table table : tableObjects) {
                    for (Attribute attribute : table.getAttributes()) {
                        // Check for fully qualified attributes (tableName.attr)
                        if (attr.equals(table.getName() + "." + attribute.getName())) {
                            qualifiedAttr.add(attr); // Attribute is already qualified
                            found = true;
                            break;
                        }
                        // Check for unqualified attributes (attr), and qualify it
                        if (attr.equals(attribute.getName())) {
                            // If found in multiple tables, ambiguity error
                            if (found) {
                                System.out.println("Ambiguous attribute " + attr + " found in multiple tables.");
                                System.out.println("ERROR\n");
                                return;
                            }
                            qualifiedAttr.add(table.getName() + "." + attribute.getName());
                            found = true;
                            break;
                        }
                    }
                }
                // If attribute not found in any table, print error and return
                if (!found) {
                    System.out.println("There is no such attribute " + attr + ".");
                    System.out.println("ERROR\n");
                    return;
                }
            }

            // Replace allAttr with the fully qualified attribute names
            allAttr.clear();
            allAttr.addAll(qualifiedAttr);

        }

        // Get all records from tables and generate the Cartesian product
        // SELECT x, y, z FROM t1, t2, t3 WHERE ... ORDER BY ...
        // allRecords stores t1, t2, t3 records in a 3D list
        List<List<List<Object>>> allRecords = new ArrayList<>();
        List<String> columnNames = new ArrayList<>();
        for (Table table : tableObjects) {
            List<List<Object>> records = storageManager.getRecords(table.getTableID());
            String tableName = table.getName();
            for (Attribute attr : table.getAttributes()) {
                columnNames.add(tableName + "." + attr.getName());
            }
            allRecords.add(records);
        }
        List<List<Object>> cartesianProduct = cartesianProduct(allRecords);

        // Map each attribute in allAttr to its correct index in the original columnNames
        ArrayList<Integer> attrIndices = new ArrayList<>();
        for (String attr : allAttr) {
            int index = columnNames.indexOf(attr);
            if (index == -1) {
                System.out.println("ERROR: Attribute " + attr + " not found.");
                return;
            }
            attrIndices.add(index);
        }


        // Process WHERE clause if present
        List<List<Object>> validRecords = cartesianProduct;
        if (whereIndex != -1) {
        ArrayList<String> allConditionals = new ArrayList<>();
        int whereEnd = (orderByIndex != -1) ? orderByIndex : words.length;
        for (int i = whereIndex + 1; i < whereEnd; i++) {
            allConditionals.add(words[i]);
        }

        // Resolve unqualified attributes and check for ambiguity
        for (int i = 0; i < allConditionals.size(); i++) {
            String condition = allConditionals.get(i);

            // Skip operators and logical keywords
            if (condition.equals("=") || condition.equals(">") || condition.equals("<") ||
                condition.equals(">=") || condition.equals("<=") || condition.equals("!=") ||
                condition.equalsIgnoreCase("and") || condition.equalsIgnoreCase("or")) {
                continue;
            }

            // Check for ambiguous attributes
            int matchedIndex = -1;
            int matchCount = 0;
            for (int j = 0; j < columnNames.size(); j++) {
                String columnName = columnNames.get(j);
                String[] parts = columnName.split("\\.");
                if (parts.length == 2 && parts[1].equals(condition)) {
                    matchCount++;
                    matchedIndex = j;
                }
            }

            // Ambiguity detected
            if (matchCount > 1) {
                System.out.println("Ambiguous attribute '" + condition + "' found in multiple tables.");
                System.out.println("ERROR\n");
                return;
            }

            // Replace unqualified attribute with fully qualified name
            if (matchCount == 1) {
                allConditionals.set(i, columnNames.get(matchedIndex));
            }
        }

        // Build where tree after resolving ambiguity
        Node tree = buildWhereTree(allConditionals);
        if (tree != null) {
            validRecords = evaluateWhereTree(cartesianProduct, columnNames, tree);
        }
    }


        // Process ORDER BY clause if present
        if (orderByIndex != -1 && orderByIndex + 1 < words.length) {
            String orderByCondition = words[orderByIndex + 1];        
            int attrIndex = getAttributeIndex(orderByCondition, columnNames);
            if (attrIndex == -1) {
                // Error already handled in getAttributeIndex(), no need for further action
                return;
            }
        
            // Sort records by the found attribute index
            validRecords = sortRecords(validRecords, attrIndex);
        }
        

        // Print the final results
        printGiven2List(validRecords, allAttr, attrIndices);
    }

    // order by will always be an element in the select's parsed str
    // allAttr[] = t1.a, t2.b, t2.c, t3.d
    // attrName = t1.a
    private static int getAttributeIndex(String attrName, List<String> allAttr) {
        // Check for fully qualified match (e.g., foo.x or bar.x)
        if (allAttr.contains(attrName)) {
            return allAttr.indexOf(attrName);
        }
    
        // Check for unqualified attribute match (e.g., x matches foo.x or bar.x)
        int matchedIndex = -1;
        int matchCount = 0;
    
        for (int i = 0; i < allAttr.size(); i++) {
            String columnName = allAttr.get(i);
            // Split to check for unqualified match
            String[] parts = columnName.split("\\.");
            if (parts.length == 2 && parts[1].equals(attrName)) {
                matchCount++;
                matchedIndex = i;
            }
        }
    
        // If more than one table has the same attribute, print ambiguous error
        if (matchCount > 1) {
            System.out.println("Ambiguous attribute '" + attrName + "' found in multiple tables.");
            System.out.println("ERROR\n");
            return -1;
        }
    
        // If exactly one match is found, return the matched index
        if (matchCount == 1) {
            return matchedIndex;
        }
    
        // Attribute not found in any table
        return -1;
    }
    

    public static List<List<Object>> sortRecords(List<List<Object>> wtvDylanNamesIt, int attrIndex) {
        return mergeSort(wtvDylanNamesIt, attrIndex);
    }

    private static List<List<Object>> mergeSort(List<List<Object>> wtvDylanNamesIt, int attrIndex) {
        if (wtvDylanNamesIt.size() <= 1) {
            return wtvDylanNamesIt;
        }
        int mid = wtvDylanNamesIt.size() / 2;
        List<List<Object>> left = mergeSort(wtvDylanNamesIt.subList(0, mid), attrIndex);
        List<List<Object>> right = mergeSort(wtvDylanNamesIt.subList(mid, wtvDylanNamesIt.size()), attrIndex);
        return merge(left, right, attrIndex);
    }
    
    private static List<List<Object>> merge(List<List<Object>> left, List<List<Object>> right, int attrIndex) {
        List<List<Object>> sortedList = new ArrayList<>();
        int i = 0, j = 0;
        while (i < left.size() && j < right.size()) {
            Object leftVal = left.get(i).get(attrIndex);
            Object rightVal = right.get(j).get(attrIndex);
            if (compareValues(leftVal, rightVal) <= 0) {
                sortedList.add(left.get(i));
                i++;
            } else {
                sortedList.add(right.get(j));
                j++;
            }
        }
        sortedList.addAll(left.subList(i, left.size()));
        sortedList.addAll(right.subList(j, right.size()));
        return sortedList;
    }

    private static int compareValues(Object a, Object b) {
        if (a == null && b == null) return 0;
        if (a == null) return 1;
        if (b == null) return -1;

        if (a instanceof Integer) {
            return Integer.compare((Integer) a, (Integer) b);
        } else if (a instanceof Double) {
            return Double.compare((Double) a, (Double) b);
        } else if (a instanceof Boolean) {
            return Boolean.compare((Boolean) a, (Boolean) b);
        } else if (a instanceof String) {
            return ((String) a).compareTo((String) b);
        } else if (a instanceof Character) {
            return Character.compare((Character) a, (Character) b);
        } else {
            throw new IllegalArgumentException("Unsupported type: " + a.getClass().getSimpleName());
        }
    }


    private static List<List<Object>> cartesianProduct(List<List<List<Object>>> tables) {
        if (tables.isEmpty()) {
            return new ArrayList<>();
        }
        
        List<List<Object>> result = new ArrayList<>();
        result.add(new ArrayList<>());
        
        for (List<List<Object>> table : tables) {
            List<List<Object>> temp = new ArrayList<>();
            for (List<Object> currentCombo : result) {
                for (List<Object> record : table) {
                    List<Object> newCombo = new ArrayList<>(currentCombo);
                    newCombo.addAll(record); // Appends record elements in order
                    temp.add(newCombo);
                }
            }
            result = temp;
        }
        
        return result;
    }

    private static Node buildWhereTree(ArrayList<String> conditionals){
        ArrayList<String> operators = new ArrayList<>();
        ArrayList<String> operands = new ArrayList<>();
        ArrayList<String> andOrList = new ArrayList<>();
        ArrayList<Node> nodes = new ArrayList<>();
        
        for(String condition : conditionals){
            switch(condition){
                case "=":
                if(operands.size() > 1){
                    System.out.println("ERROR: Invalid Syntax in Where Class");
                    return null;
                }
                operands.add("=");
                break;

                case ">":
                if(operands.size() > 1){
                    System.out.println("ERROR: Invalid Syntax in Where Class");
                    return null;
                }
                operands.add(">");
                break;

                case "<":
                if(operands.size() > 1){
                    System.out.println("ERROR: Invalid Syntax in Where Class");
                    return null;
                }
                operands.add("<");
                break;

                case ">=":
                if(operands.size() > 1){
                    System.out.println("ERROR: Invalid Syntax in Where Class");
                    return null;
                }
                operands.add(">=");
                break;

                case "<=":
                if(operands.size() > 1){
                    System.out.println("ERROR: Invalid Syntax in Where Class");
                    return null;
                }
                operands.add("<=");
                break;

                case "!=":
                if(operands.size() > 1){
                    System.out.println("ERROR: Invalid Syntax in Where Class");
                    return null;
                }
                operands.add("!=");
                break;

                case "and":
                andOrList.add("and");
                break;

                case "or":
                andOrList.add("or");
                break;

                default:
                operators.add(condition);
            }

            if(operators.size() == 2){
                Node tempNode = new Node(operands.get(0));
                Node left = new Node(operators.get(0));
                Node right = new Node(operators.get(1));

                tempNode.setLeftLeaf(left);
                tempNode.setRightLeaf(right);

                nodes.add(tempNode);

                operands.remove(0);
                operators.clear();
            }
        }

        for(int i = 0; i < andOrList.size();){
            if(andOrList.get(i).equals("and")){
                Node left = nodes.get(i);
                Node right = nodes.get(i + 1);
                Node andNode = new Node("and");
                andNode.setLeftLeaf(left);
                andNode.setRightLeaf(right);

                nodes.remove(i + 1);
                nodes.set(i, andNode);
                andOrList.remove(i);
            }
            else{
                i++;
            }
        }

        while(andOrList.size() > 0){
            Node left = nodes.get(0);
            Node right = nodes.get(1);
            Node orNode = new Node("or");
            orNode.setLeftLeaf(left);
            orNode.setRightLeaf(right);

            nodes.remove(1);
            nodes.set(0, orNode);
            andOrList.remove(0);
        }

        return nodes.get(0);

    }

    //Evaluates the where tree taking in a 2-D array of the cartesianed tables requested
    public static List<List<Object>> evaluateWhereTree(List<List<Object>> cartesianedRecords, List<String> columnNames, Node tree){
        List<List<Object>> validRecords = new ArrayList<>();

        for(List<Object> combinedRecords : cartesianedRecords){
            try{
                if(tree.evaluate(combinedRecords, columnNames)){
                    validRecords.add(combinedRecords);
                }
            }
            catch(Exception e){
                System.out.println(e.getMessage());
                break;
            }
        }
        
        return validRecords;
    }

    // Note: This prints the table: listToPrint when given the specific select conditions. Takes on 
    // the select from phase 1 but instead of dealing w specific Attribute types, it is a String

    // listToPrint = [ ["John", "8", "CS"], ["Poppy", "10", "Math"], ... ]
    private static void printGiven2List(List<List<Object>> listToPrint, ArrayList<String> allAttr, ArrayList<Integer> attrIndices) {
        // used to store the max length of each attribute
        // Key = atrr name, Value = max length of the attribute's data
        
        if (listToPrint.size() == 0) {
            return;
        }
        
        HashMap<String, Integer> maxAttributeLength = new HashMap<>();

        // initial max length will be max length of the attribute name
        for (String attr : allAttr) {
            maxAttributeLength.put(attr, attr.length());
        }    

        // recordTuple = each individual tuple in the list of tuples
        // listToPrint = ( (1 2.1), (2 3.7), (3 2.1), (4 0.1), (5 7.8) )
        // allAttr = [foo.x]
        for (List<Object> recordTuple : listToPrint) {

            // go thru each value in above tuple
            // tuple: [value, value, value, ...]
            for (int j = 0; j < allAttr.size(); j++) {
                int attrIndex = attrIndices.get(j);  // Get the correct column index
                Object value = recordTuple.get(attrIndex); // Use the correct index
                if (value != null) {
                    String valueString = value.toString();
                    // Check if length of the value is greater than the currently stored max length
                    if (valueString.length() > maxAttributeLength.get(allAttr.get(j))) {
                        maxAttributeLength.put(allAttr.get(j), valueString.length());
                    }
                }
            }
        }
    

        //      everything above is just to determine the max size of each tuple        //
        //      everything below will be for PRINTING to output                         //

        // num of dashes = (number of attributes total + 1) + (each attribute's max length +1)
        int totalLength = 0;
        for (String attr : allAttr) {
            totalLength += maxAttributeLength.get(attr) + 1;
        }
        totalLength += allAttr.size() + 1; // Add space for '|'

        // first "---------" line
        System.out.println();
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
        
        for (String attr : allAttr) {
            int maxAttrLength = maxAttributeLength.get(attr) + 1;
            int spacesFront = 0, spacesBack = 0;
    
            if (maxAttrLength % 2 == 0) {
                spacesFront = ((maxAttrLength - attr.length()) / 2) + 1;
                spacesBack = maxAttrLength - spacesFront - attr.length();
            } else {
                int spaces = maxAttrLength - attr.length();
                spacesFront = spaces / 2;
                spacesBack = spaces - spacesFront;
            }
    
            System.out.print("|");
            System.out.print(" ".repeat(Math.max(0, spacesFront)));
            System.out.print(attr);
            System.out.print(" ".repeat(Math.max(0, spacesBack)));
        }

        // remianing // line 
        System.out.println("|");
        
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
        for (List<Object> recordTuple : listToPrint) {
            
            // [value, value, value, ...]
            for (int j = 0; j < allAttr.size(); j++) {

                int attrIndex = attrIndices.get(j);  // Get the correct column index
                // value of curr attr
                Object value = recordTuple.get(attrIndex); // Get the correct value

                String currAttr = allAttr.get(j);

                int maxAttrLength = maxAttributeLength.get(currAttr);
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

    private static void delete(String normalizedStatement, Catalog catalog, StorageManager storageManager) {
        normalizedStatement = normalizedStatement.trim();
        if (normalizedStatement.endsWith(";")) {
            normalizedStatement = normalizedStatement.substring(0, normalizedStatement.length() - 1);
        }
    
        int fromIndex = normalizedStatement.toLowerCase().indexOf(" from ");
        int whereIndex = normalizedStatement.toLowerCase().indexOf(" where ");
    
        if (fromIndex == -1) {
            System.err.println("Missing FROM clause.");
            return;
        }
    
        String tableName;
        String whereClause = null;
    
        if (whereIndex == -1) {
            tableName = normalizedStatement.substring(fromIndex + 6).trim();
        } else {
            tableName = normalizedStatement.substring(fromIndex + 6, whereIndex).trim();
            whereClause = normalizedStatement.substring(whereIndex + 7).trim();
        }
    
        Table table = catalog.getTableByName(tableName);
        if (table == null) {
            System.err.println("Table not found: " + tableName);
            return;
        }
    
        List<String> columnNames = new ArrayList<>();
        for (Attribute attr : table.getAttributes()) {
            columnNames.add(tableName + "." + attr.getName());
        }
    
        // Build WHERE tree if applicable
        Node whereTree = null;
        if (whereClause != null) {
            ArrayList<String> tokens = new ArrayList<>(Arrays.asList(whereClause.split("\\s+")));
            whereTree = buildWhereTree(tokens);
            if (whereTree == null) {
                System.err.println("Error parsing WHERE clause.");
                return;
            }
        }
    
        // Use indexing. B+ Tree tracks exact (PageID, IndexInPage) for each key so u can j delete record using the ptr
        if (Main.getIndexing()) {
            Attribute pkAttr = null;
            for (Attribute attr : table.getAttributes()) {
                if (attr.isPrimaryKey()) {
                    pkAttr = attr;
                    break;
                }
            }

            if (pkAttr == null) {
                System.err.println("No primary key found for table: " + tableName);
            } else if (whereTree != null && whereTree.getValue().equals("=")) {
                String pkCond = whereTree.getLeftLeaf().getValue();
                String value = whereTree.getRightLeaf().getValue().replaceAll("^\"|\"$", "");

                // Check if WHERE clause targets the PK 
                if (pkCond.equals(pkAttr.getName()) || pkCond.equals(tableName + "." + pkAttr.getName())) {
                    Object key = parseValueBasedOnType(value, pkAttr);
                    BPlusTree index = Main.getBPlusTrees().get(table.getTableID());

                    if (index != null) {
                        Record deleted = index.delete(key);
                        if (deleted != null) {
                            System.out.println("SUCCESS\n");
                            return; 
                        } else {
                            System.out.println("No matching record found to delete.\n");
                            return; 
                        }
                    }
                }
            }
        }
    
        // Not using indexing...Reg full scan delete
        List<Page> pages = storageManager.getPages(table.getTableID());
        List<List<Object>> allRecordData = new ArrayList<>();
        for (Page page : pages) {
            for (Record record : page.getRecords()) {
                allRecordData.add(record.getData());
            }
        }
    
        List<List<Object>> matchingRecords = whereTree != null
            ? evaluateWhereTree(allRecordData, columnNames, whereTree)
            : allRecordData; // If no WHERE, delete all
    
        for (Page page : pages) {
            List<Record> records = page.getRecords();
            int i = 0;
    
            while (i < records.size()) {
                Record record = records.get(i);
                List<Object> recordData = record.getData();
    
                if (rowMatches(recordData, matchingRecords)) {
                    page.deleteRecord(record, i);
                    if (page.getRecordCount() == 0) {
                        table.dropPage(page.getPageId());
                        break; // exit this page
                    }
                } else {
                    i++;
                }
            }
        }
    
        System.out.println("SUCCESS\n");
    }
    
    

    private static void update(String normalizedStatement, Catalog catalog, StorageManager storageManager){
        normalizedStatement = normalizedStatement.trim();
        if (normalizedStatement.endsWith(";")) {
            normalizedStatement = normalizedStatement.substring(0, normalizedStatement.length() - 1);
        }

        int setIndex = normalizedStatement.toLowerCase().indexOf(" set ");
        int whereIndex = normalizedStatement.toLowerCase().indexOf(" where ");

        if (setIndex == -1) {
            System.err.println("Missing SET clause.");
            return;
        }
        if (whereIndex == -1) {
            System.err.println("Missing WHERE clause.");
            return;
        }

        String tableName = normalizedStatement.substring(7, setIndex).trim(); 
        String setClause = normalizedStatement.substring(setIndex + 5, whereIndex).trim(); 
        String whereClause = normalizedStatement.substring(whereIndex + 7).trim();        

        String[] assignment = setClause.split("=", 2);
        String columnName = assignment[0].trim();
        String valueStr = assignment[1].trim();
        if (valueStr.startsWith("\"") && valueStr.endsWith("\"")) {
            valueStr = valueStr.substring(1, valueStr.length() - 1); // Remove quotes
        }

        //Get current table 
        Table tableToUpdate = catalog.getTableByName(tableName);
        if (tableToUpdate == null) {
            System.err.println("Table not found: " + tableName);
            return;
        }
        
        // Construct a list of column names from the table's attributes.
        List<String> columnNames = new ArrayList<>();
        for (Attribute attr : tableToUpdate.getAttributes()) {
            columnNames.add(tableName + "." + attr.getName());
        }

        //Find the attribute to update
        Attribute[] attributes = tableToUpdate.getAttributes();
        int columnIndex = -1;
        for (int i = 0; i < attributes.length; i++) {
            if (attributes[i].getName().equalsIgnoreCase(columnName)) {
                columnIndex = i;
                break;
            }
        }
        if (columnIndex == -1) {
            System.err.println("Column not found: " + columnName);
            return;
        }  
        Attribute targetAttr = attributes[columnIndex];
        
        //Parse the new value based on attribute type
        Object newValue = parseValueBasedOnType(valueStr, targetAttr);
        if (newValue == null) {
            System.err.println("Invalid value for attribute type: " + targetAttr.getType());
            return;
        }

        //Get all pages.
        List<Page> pages = storageManager.getPages(tableToUpdate.getTableID());

        //Parse where condition into tree
        ArrayList<String> conditionTokens = new ArrayList<>(Arrays.asList(whereClause.split("\\s+")));
        Node conditionTree = buildWhereTree(conditionTokens);
        if (conditionTree == null) {
            System.err.println("Error parsing WHERE clause.");
            return;
        }

        List<List<Object>> allRecordData = new ArrayList<>();
        for (Page page : pages) {
            for (Record record : page.getRecords()) {
                allRecordData.add(record.getData());
            }
        }

        List<List<Object>> whereTable = evaluateWhereTree(allRecordData, columnNames, conditionTree);

        for (Page page : pages) {
            List<Record> records = page.getRecords();
            for (Record record : records) {
                System.out.println(record.getData());
            }
        }

        //Apply Update
        for (Page page : pages) {
            List<Record> records = page.getRecords();
            for (Record record : records) {

                //If current record don't satisfy where condition, skip
                if (!rowMatches(record.getData(), whereTable)) {
                    continue;
                }

                //Verify that new value is unique if attribute is primary key
                if(targetAttr.isPrimaryKey()){
                    if(newValue == null){
                        System.err.println("Primary Key cannot be assign Null Value");
                    }
                    for (Page otherPage : pages) {
                        for (Record otherRecord : otherPage.getRecords()) {
                            if (otherRecord == record) {
                                continue;
                            }
                            Object otherVal = otherRecord.getData().get(columnIndex);
                            if(newValue.equals(otherVal)){
                                System.err.println("Duplicate primarykey");
                                System.out.println("ERROR\n");
                                return;
                            }
                        }
                    }
                } 

                //Verify that new value is unique if attribute is declared unique
                if(targetAttr.isUnique()){
                    for (Page otherPage : pages) {
                        for (Record otherRecord : otherPage.getRecords()) {
                            if (otherRecord == record) {
                                continue;
                            }
                            Object otherVal = otherRecord.getData().get(columnIndex);
                            if(newValue.equals(otherVal)){
                                System.err.println("Value already exist in this unique attribute");
                                System.out.println("ERROR\n");
                                return;
                            }
                        }
                    }
                } 
                
                // Update the record's value for the target column.
                Object oldVal = record.getData().get(columnIndex);
                int sizeDiff = 0;
                if (oldVal != null) {
                    sizeDiff -= getAttributeSize(oldVal, targetAttr);
                }
                record.getData().set(columnIndex, newValue);
                record.setBitMapValue(columnIndex, newValue == null ? 1 : 0);
                if (newValue != null) {
                    sizeDiff += getAttributeSize(newValue, targetAttr);
                }

                // Adjust the page's size to account for the change in the record's size.
                page.setSize(page.getSize() + sizeDiff);
                page.setUpdated(true);

                // If the updated page becomes overfull, call the storage manager to split the page.
                if (page.isOverfull()) {
                    storageManager.splitPage(page);
                }

            }
        }

        for (Page page : pages) {
            List<Record> records = page.getRecords();
            for (Record record : records) {
                System.out.println(record.getData());
            }
        }

        System.out.println("SUCCESS\n");
    }

    /**
    * Helper method to calculate the size (in bytes) of a given attribute value.
    * For fixed-size types, it may use the attribute's declared size.
    * For variable-length types like varchar, it calculates the actual byte length plus overhead.
    */
    private static int getAttributeSize(Object value, Attribute attr) {
        if (value == null) return 0;
        switch (attr.getType().toLowerCase()) {
            case "integer":
                return Integer.BYTES;
            case "double":
                return Double.BYTES;
            case "boolean":
                return 1;
            case "char":
                return attr.getSize(); // Assuming size is set correctly for fixed-length char.
            case "varchar":
                String s = (String) value;
                return s.getBytes().length + Integer.BYTES; // Include 4 bytes for length storage.
            default:
                return 0;
        }
    }

    private static boolean rowMatches(List<Object> recordData, List<List<Object>> filteredData) {
        for (List<Object> match : filteredData) {
            if (recordData.equals(match)) {
                return true;
            }
        }
        return false;
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
                System.out.println("Purging page buffer...");
                Main.writeBuffer();
                try {
                    System.out.println("Saving catalog...\n");
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
                alterTable(statement, catalog, storageManager);
                break;

            case "insert":
                insertInto(statement, catalog, storageManager);
                break;

            case "select":
                select(statement, catalog, storageManager);
                break;

            case "delete":
                delete(statement, catalog, storageManager);
                break;

            case "update":
                update(statement, catalog, storageManager);
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