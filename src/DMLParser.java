import java.util.ArrayList;
import java.util.List;

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
                            displayInfo(normalizedStatement);
                        } else {
                            throw new DMLParserException("Invalid display syntax: " + statement);
                        }
                    } else {
                        throw new DMLParserException("Incomplete display statement: " + statement);
                    }
                    break;
                case "select":
                    select(normalizedStatement);
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


    private void displayInfo(String statement) {
        // Implement
        // - table name
        // - table schema
        // - number of pages
        // - number of records
        // BASICALLY SAME AS ABOVE BUT SHORTER
        System.out.println("Table name:");
    }

    private void select(String statement) {
        // Implement
        // An error will be reported if the table does not exist.
    }
}
