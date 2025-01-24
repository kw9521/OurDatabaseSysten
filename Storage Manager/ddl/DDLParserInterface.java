package ddl;

public interface DDLParserInterface {
    void parseDDLstatement(String statement) throws DDLParserException;
}