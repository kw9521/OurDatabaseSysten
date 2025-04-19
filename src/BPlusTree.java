import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.util.LinkedList;
import java.util.ArrayList;

/**
 * Class representing a B+ Tree index structure.
 */
public class BPlusTree {

    private BPlusNode root;
    private final int order; // Maximum number of keys in a node
    private final Attribute attr;

    public BPlusTree(Attribute attr, int tableID) {
        this.order = (int) Math.floor(Main.getPageSize() / (attr.getSize() + 2 * Integer.BYTES)) - 1;
        this.attr = attr;
        this.root = null;
    }

    /**
     * Searches for the leaf node that may contain the given key.
     */
    public BPlusNode search(Object key) {
        return root != null ? root.search(key) : null;
    }

    private boolean isEmpty() {
        return root == null;
    }

    /**
     * Inserts a new record into the B+ Tree.
     *
     * @param record  The record to insert.
     * @param key     The key associated with the record.
     * @param pointer The pointer value (page offset).
     * @return true if insertion succeeded, false otherwise.
     */
    public boolean insert(Record record, Object key, int pointer) {
        if (isEmpty()) {
            root = new BPlusNode(order, true, 0, attr);
        }
        return root.insert(record, key, pointer, false);
    }

    /**
     * Returns all leaf nodes in the tree in order.
     */
    public ArrayList<BPlusNode> getLeafNodes() {
        return root != null ? root.getLeafNodes() : new ArrayList<>();
    }

    /**
     * Deletes the given key from the B+ Tree.
     *
     * @param key The key to delete.
     * @return The deleted record, if it existed.
     */
    public Record delete(Object key) {
        if (isEmpty()) return null;

        Record record = root.delete(key, false);

        if (root.getChildren().size() == 1) {
            root = root.getChildren().get(0);
        }

        return record;
    }

    /**
     * Updates a record with a new key value.
     */
    public void update(Record record, Object keyToUpdate, Object key) {
        if (!isEmpty()) {
            root.update(record, keyToUpdate, key);
        }
    }

    /**
     * Serializes the tree and writes it to disk.
     */
    public void writeToFile() {
        if (root != null) {
            root.writeToFile();
        }
    }

    /**
     * Reconstructs a B+ Tree from its binary representation on disk.
     *
     * @param tableID The table ID associated with this index.
     * @param attr    The attribute used as the primary key.
     * @return A reconstructed BPlusTree, or null if loading fails.
     */
    public static BPlusTree fromFile(int tableID, Attribute attr) {
        String fileName = Main.getDBLocation() + "/BPIndex/" + tableID + ".bin";
        byte[] data = new byte[Main.getPageSize()];
    
        try (RandomAccessFile fileIn = new RandomAccessFile(fileName, "r")) {
            fileIn.read(data);
        } catch (Exception e) {
            System.err.println("Error reading B+ Tree from file: " + e.getMessage());
            return null;
        }
    
        ByteBuffer buffer = ByteBuffer.wrap(data);
        int numKeys = buffer.getInt();
    
        String attrType = attr.getType();
        int order = (int) Math.floor(Main.getPageSize() / (attr.getSize() + 2 * Integer.BYTES)) - 1;
    
        BPlusNode root = new BPlusNode(order, true, tableID, attr);
        LinkedList<Object> keys = new LinkedList<>();
        LinkedList<BPlusNode.Pair<Integer, Integer>> pointers = new LinkedList<>();
    
        // First pointer
        int pageNumber = buffer.getInt();
        int index = buffer.getInt();
        pointers.add(new BPlusNode.Pair<>(pageNumber, index));
    
        // Read keys and their corresponding pointers
        for (int i = 0; i < numKeys; i++) {
            Object key;
    
            switch (attrType.toLowerCase()) {
                case "varchar" -> {
                    int strLen = buffer.getInt();
                    byte[] strBytes = new byte[strLen];
                    buffer.get(strBytes, 0, strLen);
                    key = new String(strBytes);
                }
                case "char" -> {
                    int charSize = attr.getSize();
                    byte[] charBytes = new byte[charSize];
                    buffer.get(charBytes, 0, charSize);
                    key = new String(charBytes).trim();
                }
                case "integer" -> key = buffer.getInt();
                case "double" -> key = buffer.getDouble();
                case "boolean" -> key = buffer.get() == 1;
                default -> {
                    System.err.println("Unsupported attribute type: " + attrType);
                    return null;
                }
            }
    
            keys.add(key);
    
            pageNumber = buffer.getInt();
            index = buffer.getInt();
            pointers.add(new BPlusNode.Pair<>(pageNumber, index));
        }
    
        // Set root's reconstructed data
        root.setKeys(keys);
        root.setPointers(pointers);
    
        BPlusTree tree = new BPlusTree(attr, tableID);
        tree.root = root;
        return tree;
    }    

    /**
     * Displays the structure of the tree (debugging/visualization).
     */
    public void display() {
        if (root != null) {
            root.display();
        }
    }

    /**
     * Compares two key values based on the attribute type.
     * Used for determining insert/search positions.
     */
    public int compare(Object insertValue, Object existingValue) {
        if (attr.getType().equalsIgnoreCase("integer")) {
            return (int) insertValue - (int) existingValue;
        }
        return 0;
    }
}
