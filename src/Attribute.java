// Attributes used in table

public class Attribute {
    private String name; // Names can start with a alpha-character and contain alphanumeric characters.
    private String type; // Attribute types can only be integer, double, boolean, char(x), and varchar(x)
    // Constraints
    private boolean isNullable;
    private boolean primaryKey; // Primary keys are assumed to be automatically not null and unique.
    private boolean unique;
    private int size;

    public Attribute(String name, String type, boolean isNullable, boolean primaryKey, boolean unique, int size){
        this.name = name;
        this.type = type;
        this.isNullable = isNullable;
        this.primaryKey = primaryKey;
        this.unique = unique;
        this.size = size;
    }

    public void setName(String name){
        this.name = name;
    }

    public String getName(){
        return this.name;
    }

    public void setType(String type){
        this.type = type;
    }

    public String getType(){
        return this.type;
    }

    public void setNullable(boolean notNull){
        this.isNullable = notNull;
    }

    public boolean getNullable(){
        return this.isNullable;
    }

    public void setPrimaryKey(boolean primaryKey){
        this.primaryKey = primaryKey;
    }

    public boolean isPrimaryKey(){
        return this.primaryKey;
    }

    public void setUnique(boolean unique){
        this.unique = unique;
    }

    public boolean isUnique(){
        return this.unique;
    }

    public void setSize(int size){
        this.size = size;
    }

    public int getSize(){
        return this.size;
    }

    public static Attribute parse(String attributes){
        String[] tokens = attributes.split("\\s+");
        String name = tokens[0];
        String type = tokens[1];
        int size = 0;
        boolean notNull = attributes.contains("notnull");
        boolean primaryKey = attributes.contains("primarykey");
        boolean unique = attributes.contains("unique");

        // Manage char / varchar sizes
        if (type.startsWith("char") || type.startsWith("varchar")){
            // removes everything except digits "char(255)" -> "255"
            size = Integer.parseInt(type.replaceAll("[^0-9]", ""));
            // removes everything inside and including parentheses "varchar(100)" -> "varchar"
            type = type.replaceAll("\\(.*\\)", "");
        }
        else if (type.startsWith("integer")) size = Integer.BYTES;
        else if (type.startsWith("double")) size = Double.BYTES;
        else if (type.startsWith("boolean")) size = 1;

        return new Attribute(name, type, notNull, primaryKey, unique, size);
    }

    public void displayAttribute(){
        System.out.println(this.name);
        System.out.println(this.type);
        System.out.println("Constraints...");
    }
}
