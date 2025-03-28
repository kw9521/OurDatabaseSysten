import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

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
            System.err.println("\nTable with no attributes");
            System.err.println("ERROR\n");
            return;
        }
    
        List<Attribute> attributes = Arrays.stream(attributesLine.split(",\\s*"))
                                           .map(Attribute::parse)
                                           .collect(Collectors.toList());
        
        if(attributes.contains(null)){
            String invalidType = attributesLine.split(",\\s*")[attributes.indexOf(null)].split(" ")[1];
            System.err.println("Invalid datatype \"" + invalidType + "\"");
            System.err.println("ERROR\n");
            return;
        }
    
        // Ensure exactly one primary key exists
        long primaryKeyCount = attributes.stream().filter(Attribute::isPrimaryKey).count();
        if (primaryKeyCount == 0) {
            System.err.println("No primary key defined\nERROR\n");
            return;
        } else if (primaryKeyCount > 1) {
            System.err.println("More than one primarykey");
            System.out.println("ERROR\n");
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
            System.err.println("Duplicate attribute name " + values);
            System.err.println("ERROR\n");
            return;
        }
    
        // Check if table already exists
        if (catalog.getTables().stream().anyMatch(table -> table.getName().equals(tableName))) {
            System.err.println("\nTable of name " + tableName + " already exists");
            System.err.println("ERROR\n");
            return;
        }
    
        // Create and add the new table
        Table table = new Table(tableName, catalog.getNextTableID(), attributes.size(), attributes.toArray(new Attribute[0]));
        catalog.addTable(table);
    
        System.out.println("\nSUCCESS\n");
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
            }
    
            int recordSize = calculateRecordSize(recordValues, table.getAttributes());
            Record newRecord = new Record(recordSize, recordValues, nullBitMap);
    
            if (!storageManager.addRecord(catalog, newRecord, table.getTableID())) {
                return;
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

        // Error if "from" is missing: SELECT x; or SELECT x FROM; 
        if (fromIndex == -1 || fromIndex == words.length - 1) {
            System.out.println("Syntax error: Missing or incomplete 'from' clause.");
            return;
        }

        // Extract attributes between "select" and "from": SELECT x, y, z FROM ... 
        // allAttr = [x, y, z]
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

        // Process WHERE clause if present
        List<List<Object>> validRecords = cartesianProduct;
        if (whereIndex != -1) {
            ArrayList<String> allConditionals = new ArrayList<>();
            int whereEnd = (orderByIndex != -1) ? orderByIndex : words.length;
            for (int i = whereIndex + 1; i < whereEnd; i++) {
                allConditionals.add(words[i]);
            }
            Node tree = buildWhereTree(allConditionals);
            if (tree != null) {
                validRecords = evaluateWhereTree(cartesianProduct, columnNames, tree);
            }
        }

        // Process ORDER BY clause if present
        if (orderByIndex != -1 && orderByIndex + 1 < words.length) {
            String orderByCondition = words[orderByIndex + 1];
            int attrIndex = getAttributeIndex(orderByCondition, columnNames);
            if (attrIndex != -1) {
                validRecords = sortRecords(validRecords, attrIndex);
            } else {
                System.out.println("Invalid ORDER BY condition.");
                return;
            }
        }

        // Print the final results
        printGiven2List(validRecords, allAttr);




        ////////////////////////// incorrect previous select ////////////////////

        // loop thru nromaledStatement
            // determine if there is a "where" clause or "orderby" clause

        // get rid of all spaces and commas, put rest of the words in an array
        // String[] words = normalizedStatement.replace(",", "").trim().split("\\s+");
        
        // // determine num of attrs and put all attr in a list
        // // allAtrr: ["t1.a", "t2.b", "t2.c", "t3.d"]
        // int numOfSelects = 0; // 4
        // ArrayList<String> allAttr = new ArrayList<>();
        // for (int i = 0; i<words.length-1; i++) {
        //     if (words[i].equals("from")) {
        //         break;
        //     } if (!(words[i].equals("select"))) {
        //         numOfSelects++;
        //         allAttr.add(words[i]);
        //     }
        // }

        // // determine how many tables we are working with
        // // allTables: ["t1", "t2", "t3"]
        // // starts at "from"
        // int numOfTables = 0; //3
        // ArrayList<String> allTables = new ArrayList<>();
        // for (int i = 0; i<words.length-1; i++) {
        //     if (words[i].equals("where")) {
        //         System.out.println("WHERE CALL");
        //         ArrayList<String> conditionals = new ArrayList<>(Arrays.asList("foo > 123 or baz < \"foo\" and bar = 2.1".split(" ")));
        //         Node tree = buildWhereTree(conditionals);
        //         break;
        //     } if (words[i].equals("from")) {
        //         numOfTables++;
        //         //allTables.add(words[i]);
        //     }
        // }
            
        //     //
        //     // Implementation of From starts here
        //     //
        // //Find and validate all table names in catalog
        // List<Table> tableObjects = new ArrayList<>();
        // for (String tableName : allTables) {
        //     Table table = catalog.getTableByName(tableName);
        //     if (table == null) {
        //         System.out.println("No such table " + tableName);
        //         System.out.println("ERROR\n");
        //         return;
        //     }
        //     tableObjects.add(table);
        // }
        
        // //Get all records from tables
        // List<List<List<Object>>> allRecords = new ArrayList<>();
        // List<String> columnNames = new ArrayList<>();
        // for (Table table : tableObjects) {
        //     List<List<Object>> records = storageManager.getRecords(table.getTableID());
        //     String tableName = table.getName();
        //     for(Attribute attr : table.getAttributes()){
        //         columnNames.add(tableName + "." + attr.getName());
        //     }
        //     allRecords.add(records);
        // }

        // // Get the cartesian products of all records
        // List<List<Object>> cartesianProduct = cartesianProduct(allRecords);

        // //From here you have columnNames and all records which can be passed to evaluate where in this class
        
        // //
        // // From implementation ends here
        // //

        // // allConditionals: ["t1.a", "="", "t2.b", "and", "t2.c", "="", "t3.d"]
        // int numOfWheres = 0; // 7
        // ArrayList<String> allConditionals = new ArrayList<>();
        // // starts at "where"
        // for (int i = numOfSelects+numOfTables+2; i<words.length-1; i++) {
        //     if (words[i].equals("orderby")) {
        //         break;
        //     } if (!(words[i].equals("where"))) {
        //         numOfWheres++;
        //         allConditionals.add(words[i]);
        //     }
        // }
        // // where: will return a 2d array of values that are valid
        // // List<List<Object>> wtvDylanNamesIt= [ [record1], [record2], ... ]

        // //        START OF GROUP BY       //

        // // orderBy: ["t1.a"]
        // String orderByCondition = words[words.length-2];     // last word before ";", index 17
        
        // // 1. get index of orderby condition
        // int attrIndex = getAttributeIndex(orderByCondition, allAttr);
        // if (attrIndex == -1) {
        //     System.out.println("Invalid OrderBy Conditon");
        //     return;
        // }

        // // sort the records based on the order by condition

        // // 2. List<List<Object>> finalOrderBySorted = sortRecords(wtvDylanNamesIt, attrIndex);
        // // 3. print finalOrderBySorted
    }

    // order by will always be an element in the select's parsed str
    // allAttr[] = t1.a, t2.b, t2.c, t3.d
    // attrName = t1.a
    private static int getAttributeIndex(String attrName, List<String> allAttr) {
        for (int i = 0; i < allAttr.size(); i++) {
            if (attrName.equals(allAttr.get(i))) {
                return i;
            }
        }
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

            }
        }
        
        return validRecords;
    }

    // Note: This prints the table: listToPrint when given the specific select conditions. Takes on 
    // the select from phase 1 but instead of dealing w specific Attribute types, it is a String

    // listToPrint = [ ["John", "8", "CS"], ["Poppy", "10", "Math"], ... ]
    private static void printGiven2List(List<List<Object>> listToPrint, ArrayList<String> allAttr){
        // used to store the max length of each attribute
        // Key = atrr name, Value = max length of the attribute's data
        HashMap<String, Integer> maxAttributeLength = new HashMap<String, Integer>();

        // initial max length will be max length of the attribute name
        for (String attr : allAttr) {
            maxAttributeLength.put(attr, attr.length());
        }

        // recordTuple = each individual tuple in the list of tuples
        // listToPrint = [[tuple], [tuple], [],]
        for (List<Object> recordTuple : listToPrint) {
            
            // go thru each value in above tuple
             // tuple: [value, value, value, ...]
            for (int j = 0; j < recordTuple.size(); j++) {
                Object value = recordTuple.get(j);
                if (value != null) {
                    String valueString = value.toString();
                    
                    // go thru entire tuple, check if length of that attr's value is greater than the one currently stored in the hashmap
                    if (valueString.length() > maxAttributeLength.get(allAttr.get(j))) {
                        // update the max length of the attribute
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
        totalLength += allAttr.size() + 1;

        // first "-------" line
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
            int maxAttrLength = maxAttributeLength.get(attr) + 1; // 14.5 = length4 +1 =5

            int spacesFront = 0;
            int spacesBack = 0;

            if (maxAttrLength % 2 == 0) {
                spacesFront = ((maxAttrLength - attr.length()) / 2 ) + 1;
                spacesBack = maxAttrLength - spacesFront - attr.length();
            } else {
                int spaces = maxAttrLength - attr.length(); 
                spacesFront = spaces / 2;
                spacesBack = spaces - spacesFront;
            }
        
            System.out.print("|");
            System.out.print(" ".repeat(Math.max(0, spacesFront))); // Ensure non-negative repeat value
            System.out.print(attr);
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
        for (List<Object> recordTuple : listToPrint) {

            // [value, value, value, ...]
            for (int j = 0; j < recordTuple.size(); j++) {

                // value of curr attr
                Object value = recordTuple.get(j);

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

    private static void select1(String normalizedStatement, Catalog catalog, StorageManager storageManager) {

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
        
        for (Attribute attr : attrOfSelectedTable) {
            int maxAttrLength = maxAttributeLength.get(attr.getName()) + 1; // 14.5 = length4 +1 =5

            int spacesFront = 0;
            int spacesBack = 0;

            if (maxAttrLength % 2 == 0) {
                spacesFront = ((maxAttrLength - attr.getName().length()) / 2 ) + 1;
                spacesBack = maxAttrLength - spacesFront - attr.getName().length();
            } else {
                int spaces = maxAttrLength - attr.getName().length(); 
                spacesFront = spaces / 2;
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

        //DEBUGGING PURPOSE: TO SEE WHAT INSIDE THE TABLE BEFORE THE UPDATE
        System.out.println("Debugging Purpose: Print Table before update");
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

        //DEBUGGING PURPOSE: TO SEE WHAT After THE TABLE BEFORE THE UPDATE
        System.out.println("Debugging Purpose: Print Table after update");
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
                //delete(statement, catalog, storageManager);
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