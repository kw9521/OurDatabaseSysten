package ddl;

public class DDLParser {
    private static final String CREATE_TABLE_STATEMENT = "create table";
    private static final String ALTER_TABLE_STATEMENT = "alter table";
    private static final String DROP_TABLE_STATEMENT = "drop table";

    private static final String INVALID_STATEMENT = "An invalid statement has been parsed.";

    public void parseDDLstatement(String statement) throws DDLParserException {
        String normalizedStatement = statement.toLowerCase().stripLeading();

        if (normalizedStatement.startsWith(CREATE_TABLE_STATEMENT)) {
            createTable(normalizedStatement);
        } else if (normalizedStatement.startsWith(ALTER_TABLE_STATEMENT)) {
            alterTable(normalizedStatement);
        } else if (normalizedStatement.startsWith(DROP_TABLE_STATEMENT)) {
            dropTable(normalizedStatement);
        } else {
            throw new DDLParserException(INVALID_STATEMENT + ": " + statement);
        }
    }

    private void createTable(String statement) {
        // Logic to parse and create a table
        System.out.println("Creating table...");
    }

    private void alterTable(String statement) {
        // Logic to parse and alter a table
        System.out.println("Altering table...");
    }

    private void dropTable(String statement) {
        // Logic to parse and drop a table
        System.out.println("Dropping table...");
    }
}
