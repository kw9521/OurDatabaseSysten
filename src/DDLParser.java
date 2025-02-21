import java.util.ArrayList;
import java.util.List;

public class DDLParser {
    private static final String CREATE_TABLE_STATEMENT = "create table";
    private static final String ALTER_TABLE_STATEMENT = "alter table";
    private static final String DROP_TABLE_STATEMENT = "drop table";

    private static final String INVALID_STATEMENT = "An invalid statement has been parsed.";

    public void parseDDLstatement(String statement, Catalog catalog, StorageManager storageManager)
            throws DDLParserException {
        // Lowercase + strip leading whitespace
        String normalizedStatement = statement.stripLeading();
        // Split into command and args
        String[] tokens = normalizedStatement.split("\s+", 2);

        if (tokens.length == 0) {
            throw new DDLParserException(INVALID_STATEMENT + ": " + statement);
        }

        String command = tokens[0].toLowerCase();

        try {
            switch (command) {
                case "create":
                    if (tokens.length > 1 && tokens[1].startsWith("table")) {
                        createTable(normalizedStatement, catalog, storageManager);
                    } else {
                        throw new DDLParserException("Invalid create syntax: " + statement);
                    }
                    break;
                case "alter":
                    if (tokens.length > 1 && tokens[1].startsWith("table")) {
                        alterTable(normalizedStatement, catalog, storageManager);
                    } else {
                        throw new DDLParserException("Invalid alter syntax: " + statement);
                    }
                    break;
                case "drop":
                    if (tokens.length > 1 && tokens[1].startsWith("table")) {
                        dropTable(normalizedStatement, catalog);
                    } else {
                        throw new DDLParserException("Invalid drop syntax: " + statement);
                    }
                    break;
                default:
                    throw new DDLParserException(INVALID_STATEMENT + ": " + statement);
            }
        } catch (DDLParserException e) {
            // Handle custom DDLParserException
            System.err.println("DDLParserException: " + e.getMessage());
            throw e; // Re-throwing the exception if needed
        } catch (Exception e) {
            // Handle any other unexpected exceptions
            System.err.println("An unexpected error occurred: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // Examples:
    // CREATE TABLE BAZZLE( baz double PRIMARYKEY );
    //
    // create table foo(
    // baz integer primarykey,
    // bar Double notnull,
    // bazzle char(10) unique notnull
    // );

    private static void createTable(String statement, Catalog catalog, StorageManager storageManager) {
        String[] parts = statement.split("\\s+", 3);

        // Validate command structure
        if (parts.length < 3 || !parts[0].equalsIgnoreCase("create") || !parts[1].equalsIgnoreCase("table")) {
            System.err.println("Syntax error: Expected 'CREATE TABLE <table_name> (<attributes>);'");
            return;
        }

        String tableDefinition = parts[2].trim();
        int tableNameEndIdx = tableDefinition.indexOf('(');

        // Validate presence of '('
        if (tableNameEndIdx == -1) {
            System.err.println("Syntax error: Expected '(' after table name.");
            return;
        }

        String tableName = tableDefinition.substring(0, tableNameEndIdx).trim();
        String attributesSection = tableDefinition.substring(tableNameEndIdx).trim();

        // Ensure attributes section ends properly
        if (!attributesSection.endsWith(");")) {
            System.err.println("Syntax error: Expected ');' at the end of the CREATE TABLE command.");
            return;
        }

        // Extract attribute definitions by removing surrounding '()'
        attributesSection = attributesSection.substring(1, attributesSection.length() - 2);
        String[] attributeTokens = attributesSection.split(",\\s*");

        List<Attribute> attributes = new ArrayList<>();
        Attribute primaryKey = null;

        // Parse attributes
        for (String token : attributeTokens) {
            Attribute attr = Attribute.parse(token.trim());
            attributes.add(attr);
            if (attr.isPrimaryKey()) {
                if (primaryKey != null) {
                    System.err.println("Error: Multiple primary keys detected. Only one primary key is allowed.");
                    return;
                }
                primaryKey = attr;
            }
        }

        // Ensure exactly one primary key exists
        if (primaryKey == null) {
            System.out.println("No primary key defined");
            System.out.println("ERROR\n");
            return;
        }

        // Check if table already exists NEED getTables()
        boolean tableExists = catalog.getTables().stream()
                .anyMatch(table -> table.getName().equals(tableName));
        if (tableExists) {
            System.err.println("Error: Table '" + tableName + "' already exists.");
            return;
        }

        // Create and add table to catalog
        Table newTable = new Table(
                tableName,
                catalog.getNextTableID(),
                attributes.size(),
                attributes);
        catalog.addTable(newTable);
        storageManager.createTableFile(newTable.getTableID()); //Create the .bin file for the table

        System.out.println("Table '" + tableName + "' created successfully.");
    }

    private static void alterTable(String statement, Catalog catalog, StorageManager storageManager) {
        String[] parts = statement.split("\\s+", 5);
    
        if (parts.length < 5 || !parts[0].equalsIgnoreCase("alter") || !parts[1].equalsIgnoreCase("table")) {
            System.out.println("Syntax error in ALTER TABLE command.");
            return;
        }
    
        String tableName = parts[2];
        Table table = catalog.getTableByName(tableName);
    
        if (table == null) {
            System.out.println("Table " + tableName + " not found.");
            return;
        }
    
        String operation = parts[3].toLowerCase();
        String definition = parts[4].replace(";", "");
    
        if (operation.equals("add")) {
            Attribute newAttr = Attribute.parse(definition);
            table.addAttribute(newAttr);
    
            List<Attribute> attributes = table.getAttributes();
            int newAttributeIndex = attributes.size() - 1;
            String defaultValue = null;
    
            String[] defParts = definition.split("\\s+");
            for (int i = 0; i < defParts.length - 1; i++) {
                if (defParts[i].equalsIgnoreCase("default")) {
                    defaultValue = defParts[i + 1];
                    attributes.get(newAttributeIndex).setDefaultValue(defaultValue);
                    break;
                }
            }
    
            List<Page> pages = storageManager.getPages(table.getTableID());
            for (Page page : pages) {
                for (Record record : page.getRecords()) {
                    int sizeAdded = record.addValue(
                        attributes.get(newAttributeIndex).getDefaultValue(),
                        newAttributeIndex,
                        attributes.get(newAttributeIndex)
                    );
                    page.setSize(page.getSize() + sizeAdded);
                }
    
                if (page.getSize() > Main.getPageSize()) {
                    storageManager.splitPage(page);
                }
            }
    
            System.out.println("Attribute " + newAttr.getName() + " added to table " + tableName + ".");
        } else if (operation.equals("drop")) {
            List<Attribute> attributes = table.getAttributes();
            int attributeIndex = -1;
    
            for (int i = 0; i < attributes.size(); i++) {
                if (attributes.get(i).getName().equals(definition)) {
                    attributeIndex = i;
                    break;
                }
            }
    
            if (attributeIndex == -1) {
                System.out.println("Attribute " + definition + " not found in table.");
                return;
            }
    
            List<Page> pages = storageManager.getPages(table.getTableID());
            for (Page page : pages) {
                for (Record record : page.getRecords()) {
                    int sizeLost = record.removeValue(attributeIndex, attributes.get(attributeIndex));
                    page.setSize(page.getSize() - sizeLost);
                }
            }
    
            table.dropAttribute(definition);
            System.out.println("Attribute " + definition + " dropped from table " + tableName + ".");
        } else {
            System.out.println("Unsupported ALTER TABLE operation: " + operation);
        }
    }
    

    private void dropTable(String statement, Catalog catalog) {
        System.out.println("Dropping table...");

        String[] parts = statement.split("\\s+", 3);
        if (parts.length != 3){
            System.out.println("Syntax Error: Expected 'DROP TABLE <table_name>'");
            return;
        }
        String tableName = parts[2].replace(";", "");
        catalog.dropTable(tableName);
        System.out.println("Table '" + tableName + "' dropped successfully.");
    }
}
