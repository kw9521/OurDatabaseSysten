import java.util.*;

public class Node {
    private Node leftLeaf = null;
    private Node rightLeaf = null;
    private String value;

    public Node(String value){
        this.value = value;
    }

    public void setLeftLeaf(Node node){
        this.leftLeaf = node;
    }

    public void setRightLeaf(Node node){
        this.rightLeaf = node;
    }

    public Node getLeftLeaf(){
        return leftLeaf;
    }

    public Node getRightLeaf(){
        return rightLeaf;
    }

    public String getValue(){
        return value;
    }

    public boolean evaluate(List<Object> record, List<String> columnNames) throws Exception{
        boolean rightEval = false;
        boolean leftEval = false;
        switch(value){
            case "=":
            return comparison(record, columnNames, "=");
            
            case ">":
            return comparison(record, columnNames, ">");

            case "<":
            return comparison(record, columnNames, "<");

            case ">=":
            return comparison(record, columnNames, ">=");

            case "<=":
            return comparison(record, columnNames, "<=");

            case "!=":
            return comparison(record, columnNames, "!=");

            case "and":
            rightEval = rightLeaf.evaluate(record, columnNames);
            leftEval = leftLeaf.evaluate(record, columnNames);
            return rightEval && leftEval;

            case "or":
            rightEval = rightLeaf.evaluate(record, columnNames);
            leftEval = leftLeaf.evaluate(record, columnNames);
            return rightEval || leftEval;
        }

        return false;
    }

    public void debugPrint(){
        System.out.println(this.value);
        if(this.leftLeaf != null){
            System.out.println("LEFT");
            this.leftLeaf.debugPrint();
        }
        if(this.rightLeaf != null){
            System.out.println("RIGHT");
            this.rightLeaf.debugPrint();
        }
    }

    private boolean isNumber(String val){
        try{
            Double.parseDouble(val);
            return true;
        }
        catch(NumberFormatException e){
            return false;
        }
    }

    private double convertObjToDouble(Object obj) throws Exception{
        if(obj instanceof Integer){
            return (double)(Integer)obj;
        }
        else if(obj instanceof Double){
            return (double)obj;
        }
        else{
            throw new Exception("Type missmatch!");
        }
    }

    //Takes in the columnNames list and returns the index of the column name if found otherwise i will be larger than size of array
    private int getIndex(List<String> columnNames, String val, boolean isMultiTables) throws Exception{
        int i = 0;
        if(val.split("\\.").length == 2){
            while(i < columnNames.size() && !(columnNames.get(i).equals(val))){
                i++;
            }
        }
        else{
            if(isMultiTables){
                throw new Exception("Invalid syntax: should include table.column name not just column name!");
            }
            while(i < columnNames.size() && !(columnNames.get(i).split("\\.")[1].equals(val))){
                i++;
            }
        }
        return i;
    }

    //Determine if there are multiple tables being looked through here or not
    private boolean isMultipleTables(List<String> columnNames){
        List<String> tables = new ArrayList();
        for(String name : columnNames){
            String temp = name.split("\\.")[0];
            if(!tables.contains(temp)){
                tables.add(temp);
            }
        }

        if(tables.size() > 1){
            return true;
        }

        return false;

    }

    //Compares objects based on equality
    private boolean compareObjectForEquality(Object r1, Object r2) throws Exception{
        if(r1 instanceof Integer && r2 instanceof Integer){
            return (Integer)r1 == (Integer)r2;
        }
        else if(r1 instanceof Double && r2 instanceof Double){
            return (Double)r1 == (Double)r2;
        }
        else if(r1 instanceof Boolean && r2 instanceof Boolean){
            return (Boolean)r1 == (Boolean)r2;
        }
        else if(r1 instanceof String && r2 instanceof String){
            return ((String)r1).equals((String)r2);
        }
        else{
            throw new Exception("Invalid: Columns are not of same type!");
        }
    }

    //Compares objects based on greater than
    private boolean compareObjectForGreater(Object r1, Object r2) throws Exception{
        if(r1 instanceof Integer && r2 instanceof Integer){
            return (Integer)r1 > (Integer)r2;
        }
        else if(r1 instanceof Double && r2 instanceof Double){
            return (Double)r1 > (Double)r2;
        }
        else if(r1 instanceof Boolean && r2 instanceof Boolean){
            if(Boolean.compare((Boolean)r1, (Boolean)r2) > 0){
                return true;
            }
            else{
                return false;
            }
        }
        else if(r1 instanceof String && r2 instanceof String){
            if(((String)r1).compareTo((String)r2) > 0){
                return true;
            }
            else{
                return false;
            }
        }
        else{
            throw new Exception("Invalid: Columns are not of same type!");
        }
    }

    //Compares objects based on less than
    private boolean compareObjectForLess(Object r1, Object r2) throws Exception{
        if(r1 instanceof Integer && r2 instanceof Integer){
            return (Integer)r1 < (Integer)r2;
        }
        else if(r1 instanceof Double && r2 instanceof Double){
            return (Double)r1 < (Double)r2;
        }
        else if(r1 instanceof Boolean && r2 instanceof Boolean){
            if(Boolean.compare((Boolean)r1, (Boolean)r2) < 0){
                return true;
            }
            else{
                return false;
            }
        }
        else if(r1 instanceof String && r2 instanceof String){
            if(((String)r1).compareTo((String)r2) < 0){
                return true;
            }
            else{
                return false;
            }
        }
        else{
            throw new Exception("Invalid: Columns are not of same type!");
        }
    }

    //Compares objects based on greater than or equal to
    private boolean compareObjectForGreaterEquality(Object r1, Object r2) throws Exception{
        if(r1 instanceof Integer && r2 instanceof Integer){
            return (Integer)r1 >= (Integer)r2;
        }
        else if(r1 instanceof Double && r2 instanceof Double){
            return (Double)r1 >= (Double)r2;
        }
        else if(r1 instanceof Boolean && r2 instanceof Boolean){
            if(Boolean.compare((Boolean)r1, (Boolean)r2) >= 0){
                return true;
            }
            else{
                return false;
            }
        }
        else if(r1 instanceof String && r2 instanceof String){
            if(((String)r1).compareTo((String)r2) >= 0){
                return true;
            }
            else{
                return false;
            }
        }
        else{
            throw new Exception("Invalid: Columns are not of same type!");
        }
    }

    //Compares objects based on less than or equal to
    private boolean compareObjectForLessEquality(Object r1, Object r2) throws Exception{
        if(r1 instanceof Integer && r2 instanceof Integer){
            return (Integer)r1 <= (Integer)r2;
        }
        else if(r1 instanceof Double && r2 instanceof Double){
            return (Double)r1 <= (Double)r2;
        }
        else if(r1 instanceof Boolean && r2 instanceof Boolean){
            if(Boolean.compare((Boolean)r1, (Boolean)r2) <= 0){
                return true;
            }
            else{
                return false;
            }
        }
        else if(r1 instanceof String && r2 instanceof String){
            if(((String)r1).compareTo((String)r2) <= 0){
                return true;
            }
            else{
                return false;
            }
        }
        else{
            throw new Exception("Invalid: Columns are not of same type!");
        }
    }

    //Compares objects based on not equal to
    private boolean compareObjectForNotEquality(Object r1, Object r2) throws Exception{
        if(r1 instanceof Integer && r2 instanceof Integer){
            return (Integer)r1 != (Integer)r2;
        }
        else if(r1 instanceof Double && r2 instanceof Double){
            return (Double)r1 != (Double)r2;
        }
        else if(r1 instanceof Boolean && r2 instanceof Boolean){
            if(Boolean.compare((Boolean)r1, (Boolean)r2) != 0){
                return true;
            }
            else{
                return false;
            }
        }
        else if(r1 instanceof String && r2 instanceof String){
            if(((String)r1).compareTo((String)r2) != 0){
                return true;
            }
            else{
                return false;
            }
        }
        else{
            throw new Exception("Invalid: Columns are not of same type!");
        }
    }

    private boolean comparison(List<Object> record, List<String> columnNames, String operator) throws Exception{
        boolean isMultiTables = isMultipleTables(columnNames);
        String leftVal = leftLeaf.getValue();
        String rightVal = rightLeaf.getValue();

        if(isNumber(leftVal)){
            //If Both leafs are numbers
            if(isNumber(rightVal)){
                double leftNum = Double.parseDouble(leftVal);
                double rightNum = Double.parseDouble(rightVal);
                if(leftNum == rightNum){
                    return true;
                }
                else{
                    return false;
                }
            }
            //If just left leaf is a number
            else{
                double leftNum = Double.parseDouble(leftVal);
                
                //Find the column name and get the index so we can check the value
                int i = getIndex(columnNames, rightVal, isMultiTables);

                //If we didn't find the column throw and exception
                if(i >= record.size()){
                    throw new Exception("Column Name not found!");
                }

                try{
                    if(record.get(i) == null){
                        return false;
                    }
                    switch(operator){
                    case "=":
                    return leftNum == convertObjToDouble(record.get(i));
                    case ">":
                    return leftNum > convertObjToDouble(record.get(i));
                    case "<":
                    return leftNum < convertObjToDouble(record.get(i));
                    case ">=":
                    return leftNum >= convertObjToDouble(record.get(i));
                    case "<=":
                    return leftNum <= convertObjToDouble(record.get(i));
                    case "!=":
                    return leftNum != convertObjToDouble(record.get(i));
                    default:
                    return false;
                    }
                }
                catch(Exception e){
                    throw new Exception("Column type not a number!");
                }
            }
        }
        //If just the right leaf is a number
        else if(isNumber(rightVal)){
            double rightNum = Double.parseDouble(rightVal);
            //Find the column name and get the index so we can check the value
            int i = getIndex(columnNames, leftVal, isMultiTables);

            //If we didn't find the column throw and exception
            if(i >= columnNames.size()){
                throw new Exception("Column Name not found!");
            }
            try{
                if(record.get(i) == null){
                    return false;
                }
                switch(operator){
                    case "=":
                    return rightNum == convertObjToDouble(record.get(i));
                    case ">":
                    return convertObjToDouble(record.get(i)) > rightNum;
                    case "<":
                    return convertObjToDouble(record.get(i)) < rightNum;
                    case ">=":
                    return convertObjToDouble(record.get(i)) >= rightNum;
                    case "<=":
                    return convertObjToDouble(record.get(i)) <= rightNum;
                    case "!=":
                    return convertObjToDouble(record.get(i)) != rightNum;
                    default:
                    return false;
                }
            }
            catch(Exception e){
                throw new Exception("Column type not a number!");
            }
        }
        //If neither are numbers
        else{
            if(leftVal.equals("true") || leftVal.equals("false")){
                boolean leftBool;
                if(leftVal.equals("true")){
                    leftBool = true;
                }
                else{
                    leftBool = false;
                }
                int i = getIndex(columnNames, rightVal, isMultiTables);

                //If we didn't find the column throw and exception
                if(i >= record.size()){
                    throw new Exception("Column Name not found!");
                }

                try{
                    if(record.get(i) == null){
                        return false;
                    }
                    Object temp = leftBool;
                    switch(operator){
                        case "=":
                        return compareObjectForEquality(temp, record.get(i));
                        case ">":
                        return compareObjectForGreater(temp, record.get(i));
                        case "<":
                        return compareObjectForLess(temp, record.get(i));
                        case ">=":
                        return compareObjectForGreaterEquality(temp, record.get(i));
                        case "<=":
                        return compareObjectForLessEquality(temp, record.get(i));
                        case "!=":
                        return compareObjectForNotEquality(temp, record.get(i));
                        default:
                        return false;
                    }
                }
                catch(Exception e){
                    throw new Exception("Column type not a boolean!");
                }
            }
            //If the left leafs value is null
            else if(leftVal.equals("null")){
                int i = getIndex(columnNames, rightVal, isMultiTables);

                //If we didn't find the column throw and exception
                if(i >= record.size()){
                    throw new Exception("Column Name not found!");
                }

                if(record.get(i) == null){
                    return true;
                }
                return false;
            }
            //If the left leafs value is a string
            else if(leftVal.split("\"").length == 2){
                String leftString = leftVal.split("\"")[1];

                int i = getIndex(columnNames, rightVal, isMultiTables);

                //If we didn't find the column throw and exception
                if(i >= record.size()){
                    throw new Exception("Column Name not found!");
                }

                try{
                    if(record.get(i) == null){
                        return false;
                    }
                    switch(operator){
                        case "=":
                        return compareObjectForEquality(leftString, record.get(i));
                        case ">":
                        return compareObjectForGreater(leftString, record.get(i));
                        case "<":
                        return compareObjectForLess(leftString, record.get(i));
                        case ">=":
                        return compareObjectForGreaterEquality(leftString, record.get(i));
                        case "<=":
                        return compareObjectForLessEquality(leftString, record.get(i));
                        case "!=":
                        return compareObjectForNotEquality(leftString, record.get(i));
                        default:
                        return false;
                    }
                }
                catch(Exception e){
                    throw new Exception("Column type not a string!");
                }

            }
            //If the right leafs value is a boolean
            else if(rightVal.equals("true") || rightVal.equals("false")){
                boolean rightBool;
                if(rightVal.equals("true")){
                    rightBool = true;
                }
                else{
                    rightBool = false;
                }
                int i = getIndex(columnNames, leftVal, isMultiTables);

                //If we didn't find the column throw and exception
                if(i >= record.size()){
                    throw new Exception("Column Name not found!");
                }

                try{
                    if(record.get(i) == null){
                        return false;
                    }
                    Object temp = rightBool;
                    switch(operator){
                        case "=":
                        return compareObjectForEquality(record.get(i), temp);
                        case ">":
                        return compareObjectForGreater(record.get(i), temp);
                        case "<":
                        return compareObjectForLess(record.get(i), temp);
                        case ">=":
                        return compareObjectForGreaterEquality(record.get(i), temp);
                        case "<=":
                        return compareObjectForLessEquality(record.get(i), temp);
                        case "!=":
                        return compareObjectForNotEquality(record.get(i), temp);
                        default:
                        return false;
                    }
                }
                catch(Exception e){
                    throw new Exception("Column type not a boolean!");
                }
            }
            //If the right leafs value is null
            else if(rightVal.equals("null")){
                int i = getIndex(columnNames, leftVal, isMultiTables);

                //If we didn't find the column throw and exception
                if(i >= record.size()){
                    throw new Exception("Column Name not found!");
                }

                if(record.get(i) == null){
                    return true;
                }
                return false;
            }
            //If the right leafs value is a string
            else if(rightVal.split("\"").length == 2){
                String rightString = rightVal.split("\"")[1];

                int i = getIndex(columnNames, leftVal, isMultiTables);

                //If we didn't find the column throw and exception
                if(i >= record.size()){
                    throw new Exception("Column Name not found!");
                }

                try{
                    if(record.get(i) == null){
                        return false;
                    }
                    switch(operator){
                        case "=":
                        return compareObjectForEquality(record.get(i), rightString);
                        case ">":
                        return compareObjectForGreater(record.get(i), rightString);
                        case "<":
                        return compareObjectForLess(record.get(i), rightString);
                        case ">=":
                        return compareObjectForGreaterEquality(record.get(i), rightString);
                        case "<=":
                        return compareObjectForLessEquality(record.get(i), rightString);
                        case "!=":
                        return compareObjectForNotEquality(record.get(i), rightString);
                        default:
                        return false;
                    }
                }
                catch(Exception e){
                    throw new Exception("Column type not a String!");
                }
            }
            //If both are column names
            else{
                //Check to make sure we have correct syntax for column names
                if(leftVal.split("\\.").length == 2 && rightVal.split("\\.").length == 2){
                    String column1 = leftVal.split("\\.")[1];
                    String column2 = rightVal.split("\\.")[1];
                    if(column1.equals(column2)){
                        throw new Exception("Can not compare on the same columns!");
                    }

                    int c1 = getIndex(columnNames, leftVal, isMultiTables);
                    int c2 = getIndex(columnNames, rightVal, isMultiTables);

                    //If we didn't find the column throw and exception
                    if(c1 >= columnNames.size() || c2 >= columnNames.size()){
                        throw new Exception("Column Name not found!");
                    }

                    try{
                        switch(operator){
                            case "=":
                            return compareObjectForEquality(record.get(c1), record.get(c2));
                            case ">":
                            return compareObjectForGreater(record.get(c1), record.get(c2));
                            case "<":
                            return compareObjectForLess(record.get(c1), record.get(c2));
                            case ">=":
                            return compareObjectForGreaterEquality(record.get(c1), record.get(c2));
                            case "<=":
                            return compareObjectForLessEquality(record.get(c1), record.get(c2));
                            case "!=":
                            return compareObjectForNotEquality(record.get(c1), record.get(c2));
                            default:
                            return false;
                        }
                    }
                    catch(Exception e){
                        throw new Exception("Column type not a string!");
                    }
                }
                else{
                    throw new Exception("Invalid syntax!");
                }
            }
        }
    }
}
