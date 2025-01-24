package ddl;
public class DDLParser implements DDLParserInterface{
    //Example creation:
    //JottQL> create table foo(
    //num integer primarykey);
    //SUCCESS
    
    private final String CREATE_TABLE_STATMENT = "create table";
    private final String ALTER_TABLE_STATEMENT = "alter table";
    private final String DROP_TABLE_STATEMENT = "drop table";

    private final String INVALID_STATEMENT = "An invalid statement has been parsed.";

    public static DDLParser ddlParser = null;

    public static DDLParserInterface createParser(){
        ddlParser = new DDLParser();
        return ddlParser;
    }

    @Override
    public void parseDDLstatement(String statement) throws DDLParserException {
        // Lowercase + strip leading whitespace
        statement = statement.toLowerCase().stripLeading();
    
        try {
            if (statement.startsWith(CREATE_TABLE_STATMENT)) {
                // Call and implement method
            } else if (statement.startsWith(ALTER_TABLE_STATEMENT)) {
                // Call and implement method
            } else if (statement.startsWith(DROP_TABLE_STATEMENT)) {
                // Call and implement method
            } else {
                throw new DDLParserException(String.format(INVALID_STATEMENT, statement));
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
    





}
