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

    private void createTable(String statement, Catalog catalog) {
        // Logic to parse and create a table
        System.out.println("Creating table...");
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
