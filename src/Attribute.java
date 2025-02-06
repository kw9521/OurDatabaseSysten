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

    public void displayAttribute(){
        System.out.println(this.name);
        System.out.println(this.type);
        System.out.println("Constraints...");
    }
}
