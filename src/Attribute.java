// Attributes used in table

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class Attribute {
    private String name; // Names can start with a alpha-character and contain alphanumeric characters.
    private String type; // Attribute types can only be integer, double, boolean, char(x), and varchar(x)
    // Constraints
    private boolean nonNull;
    private boolean primaryKey; // Primary keys are assumed to be automatically not null and unique.
    private boolean unique;
    private int size;

    private Object defaultValue;

    public Attribute(String name, String type, boolean nonNull, boolean primaryKey, boolean unique, int size){
        this.name = name;
        this.type = type;
        this.nonNull = nonNull;
        this.primaryKey = primaryKey;
        this.unique = unique;
        this.size = size;
        this.defaultValue = null;
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

    public String getTypeFancy(){
        if(this.type.equals("char") || this.type.equals("varchar")){
            return this.type + "(" + this.size + ")";
        }
        return this.type;
    }

    public void setNotNull(boolean notNull){
        this.nonNull = notNull;
    }

    public boolean isnonNull(){
        return this.nonNull;
    }

    public void setPrimaryKey(boolean primaryKey){
        this.primaryKey = primaryKey;
    }

    public boolean isPrimaryKey(){
        return this.primaryKey;
    }

    public boolean getPrimaryKey(){
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

    public void setDefaultValue(Object defaultValue){
        this.defaultValue = defaultValue;
    }

    public Object getDefaultValue(){
        return this.defaultValue;
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
        else{
            return null;
        }

        return new Attribute(name, type, notNull, primaryKey, unique, size);
    }

    public void displayAttribute(){
        System.out.println(this.name);
        System.out.println(this.type);
        System.out.println("Constraints...");
    }

    public void writeToStream(DataOutputStream dos) throws IOException {
        dos.writeUTF(this.name);
        dos.writeUTF(this.type);
        dos.writeBoolean(this.nonNull);
        dos.writeBoolean(this.primaryKey);
        dos.writeBoolean(this.unique);
        dos.writeInt(this.size);
    }

    public static Attribute readFromStream(DataInputStream dis) throws IOException {
        String name = dis.readUTF();
        String type = dis.readUTF();
        boolean nonNull = dis.readBoolean();
        boolean primaryKey = dis.readBoolean();
        boolean unique = dis.readBoolean();
        int size = dis.readInt();

        return new Attribute(name, type, unique, nonNull, primaryKey, size);
    }

    public int calcByteSize(){
        int totalSize = 0;
        byte[] nameBytes = this.name.getBytes(StandardCharsets.UTF_8);
        totalSize += nameBytes.length;
        totalSize += 4; //For the int length of the string
        
        byte[] typeBytes = this.type.getBytes(StandardCharsets.UTF_8);
        totalSize += typeBytes.length;
        totalSize += 4; //For the int length of the string

        totalSize += 4; //For the int size of the attribute
        
        totalSize += 3; //For the 3 boolean values
        return totalSize;
    }

}
