// Attributes used in table

public class Attribute {
    private String name; // Names can start with a alpha-character and contain alphanumeric characters.
    private String type; // Attribute types can only be integer, double, boolean, char(x), and varchar(x)
    // Constraints
    private boolean notNull;
    private boolean primaryKey; // Primary keys are assumed to be automatically not null and unique.
    private boolean unique;

    public Attribute(String name, String type, boolean notNull, boolean primaryKey, boolean unique){
        this.name = name;
        this.type = type;
        this.notNull = notNull;
        this.primaryKey = primaryKey;
        this.unique = unique;
    }

    public void displayAttribute(){
        System.out.println(this.name);
        System.out.println(this.type);
        System.out.println("Constraints...");
    }
}
