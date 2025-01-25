package dml;

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

    public void parseDMLstatement(String statement) throws DMLParserException {
        // Lowercase + strip leading whitespace
        String normalizedStatement = statement.toLowerCase().stripLeading();

        try {
            if (normalizedStatement.startsWith(INSERT_INTO_STATEMENT)) {
                insertInto(normalizedStatement);
            } else if (normalizedStatement.startsWith(DISPLAY_SCHEMA_STATEMENT)) {
                displaySchema(normalizedStatement);
            } else if (normalizedStatement.startsWith(DISPLAY_INFO_STATEMENT)) {
                displayInfo(normalizedStatement);
            } else if (normalizedStatement.startsWith(SELECT_STATEMENT)) {
                select(normalizedStatement);
            } else {
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

    private void displaySchema(String statement) {
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
