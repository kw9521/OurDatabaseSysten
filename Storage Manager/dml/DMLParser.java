package dml;

public class DMLParser implements DMLParserInterface{
    //Example creation:
    //JottQL> create table foo(
    //num integer primarykey);
    //SUCCESS
    
    private final String INSERT_INTO_STATEMENT = "insert into";
    private final String DISPLAY_SCHEMA_STATEMENT = "display schema";
    private final String DISPLAY_INFO_STATEMENT = "display info";
    private final String SELECT_STATEMENT = "select";

    private final String INVALID_STATEMENT = "An invalid statement has been parsed.";

    public static DMLParser dmlParser = null;

    public static DMLParserInterface createParser(){
        dmlParser = new DMLParser();
        return dmlParser;
    }

    @Override
    public void parseDMLstatement(String statement) throws DMLParserException {
        // Lowercase + strip leading whitespace
        statement = statement.toLowerCase().stripLeading();
    
        try {
            if (statement.startsWith(INSERT_INTO_STATEMENT)) {
                insertInto(statement);
            } else if (statement.startsWith(DISPLAY_SCHEMA_STATEMENT)) {
                displaySchema(statement);
            } else if (statement.startsWith(DISPLAY_INFO_STATEMENT)) {
                displayInfo(statement);
            } else if (statement.startsWith(SELECT_STATEMENT)){
                select(statement);
            } else {
                throw new DMLParserException(String.format(INVALID_STATEMENT, statement));
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

    private void insertInto(String statement){
        //implement
        System.out.println("Inserting into table...");
    }

    private void displaySchema(String statement){
        //implement
        System.out.println("DB Location:");
    }

    private void displayInfo(String statement){
        //implement
        //• table name
        //• table schema
        //• number of pages
        //• number of records
        System.out.println("Table name:");
    }

    private void select(String statement){
        //implement
        //An error will be reported if the table does not exist.
    }

}
