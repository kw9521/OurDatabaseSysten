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

    public static DMLParser dmlParser = null;

    public static DMLParserInterface createParser(){
        dmlParser = new DMLParser();
        return dmlParser;
    }

    // Copy format from parseDDLStatement
    @Override
    public void parseDMLstatement(String statement) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'parseDMLstatement'");
    }





}
