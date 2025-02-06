import java.util.ArrayList;
import java.util.List;

public class DDLParser {
    private static final String CREATE_TABLE_STATEMENT = "create table";
    private static final String ALTER_TABLE_STATEMENT = "alter table";
    private static final String DROP_TABLE_STATEMENT = "drop table";

    private static final String INVALID_STATEMENT = "An invalid statement has been parsed.";

    public void parseDDLstatement(String statement, Catalog catalog, StorageManager storageManager) throws DDLParserException {
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
                        createTable(normalizedStatement, catalog);
                    } else {
                        throw new DDLParserException("Invalid create syntax: " + statement);
                    }
                    break;
                case "alter":
                    if (tokens.length > 1 && tokens[1].startsWith("table")) {
                        alterTable(normalizedStatement, catalog);
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
    //     baz integer primarykey,
    //     bar Double notnull,
    //     bazzle char(10) unique notnull
    // );

private static void handleCreateCommand(String inputLine, Catalog catalog) {
    String[] parts = inputLine.split("\\s+", 3);
    
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
        System.err.println("Error: A primary key must be specified.");
        return;
    }

    // Check if table already exists  NEED getTables()
    boolean tableExists = catalog.getTables().stream()
                                 .anyMatch(table -> table.getName().equals(tableName));
    if (tableExists) {
        System.err.println("Error: Table '" + tableName + "' already exists.");
        return;
    }

    // Create and add table to catalog  NEED getNextTableNumber() + addTable()
    Table newTable = new Table(
        attributes.size(),
        tableName,
        catalog.getNextTableNumber(),
        attributes.toArray(new Attribute[0])
    );
    catalog.addTable(newTable);

    System.out.println("Table '" + tableName + "' created successfully.");
}


    private void alterTable(String statement, Catalog catalog) {
        // Logic to parse and alter a table
        System.out.println("Altering table...");
    }

    private void dropTable(String statement, Catalog catalog) {
        // Logic to parse and drop a table
        System.out.println("Dropping table...");
    }
}
