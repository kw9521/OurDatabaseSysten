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

    private void displaySchema(String statement, Catalog catalog) {
        // Implement
        System.out.println("DB Location:");
    }

    private void displayInfo(String statement) {
        // Implement
        // - table name
        // - table schema
        // - number of pages
        // - number of records
        System.out.println("Table name:");
    }

    private void select(String statement) {
        // Implement
        // An error will be reported if the table does not exist.
    }
}
