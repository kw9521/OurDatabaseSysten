import java.util.ArrayList;
import java.nio.ByteBuffer;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.LinkedList;
import java.util.List;

public class BPlusNode {
    private boolean isLeaf;
    private boolean isRoot;
    private Attribute attr;
    private int tableID;
    private int pageID;
    private int order;
    private LinkedList<Object> keys;
    private LinkedList<BPlusNode> children;
    private LinkedList<Pair<Integer, Integer>> pointers;
    private BPlusNode parent;
    private BPlusNode nextLeaf;

    public BPlusNode(int order, boolean isRoot, int tableID, Attribute attr) {
        this.order = order;
        this.isRoot = isRoot;
        this.isLeaf = true;
        this.tableID = tableID;
        Table table = Main.getCatalog().getTable(tableID);
        ArrayList<Integer> freeSpaces = table.getFreeSpaces();
        if (freeSpaces.size() > 0)
            this.pageID = freeSpaces.remove(0);
        else
            this.pageID = table.getNumNodes();
        table.addTreeNode();
        this.parent = null;
        this.attr = attr;
        this.children = new LinkedList<BPlusNode>();
        this.keys = new LinkedList<>();
        this.pointers = new LinkedList<>();
    }

    // If this works I might be the actual GOAT
    public boolean insert(Record record, Object searchKey, int pointer, boolean intoInternal) {
        if (isLeaf || intoInternal) {
            for (int i = 0; i < keys.size(); i++) {
                Object key = keys.get(i);
                if (key != null) {
                    int cmp = compare(searchKey, key);
    
                    if (cmp == 0) {
                        System.err.println("Duplicate primary key, insert cancelled.");
                        return false;
                    }
    
                    if (cmp < 0) {
                        keys.add(i, searchKey);
                        System.out.println(searchKey + " < " + key);
    
                        if (isLeaf) {
                            Pair<Integer, Integer> nextPointer = pointers.get(i);
                            Page page = Main.getStorageManager().getPage(tableID, nextPointer.getPageNumber());
                            page.shiftRecordsAndAdd(record, nextPointer.getIndex());
    
                            // ✅ Maintain correct pointer size
                            pointers.add(i + 1, new Pair<>(nextPointer.pageNumber, nextPointer.index + 1));
                            pointers.set(i, new Pair<>(nextPointer.pageNumber, nextPointer.index));
    
                            if (pointers.size() >= i + 2) {
                                incrementPointerIndexes(nextPointer.getPageNumber(), i + 2);
                            }
    
                            BPlusNode rightNeighbor = getRightSibling();
                            while (rightNeighbor != null) {
                                if (!rightNeighbor.incrementPointerIndexes(nextPointer.getPageNumber(), 0)) {
                                    rightNeighbor = rightNeighbor.getRightSibling();
                                } else break;
                            }
    
                            if (page.isOverfull()) {
                                SplitResult splitResult = Main.getStorageManager().splitPage(page);
                                Attribute[] attrs = Main.getCatalog().getTable(tableID).getAttributes();
                                Object firstVal = splitResult.firstRecord.getAttributeValue(attr.getName(), attrs);
                                keys.add(i + 1, firstVal);
                                pointers.add(i + 2, new Pair<>(splitResult.newPageId, splitResult.firstIndex));
                            
                                // Update pointers for records moved to the new page
                                for (int j = i + 2; j < pointers.size(); j++) {
                                    Pair<Integer, Integer> ptr = pointers.get(j);
                                    if (ptr.getPageNumber() == nextPointer.getPageNumber()) {
                                        // Adjust index for records moved to new page
                                        int newIdx = ptr.getIndex() - page.getRecordCount();
                                        if (newIdx >= 0) {
                                            pointers.set(j, new Pair<>(splitResult.newPageId, newIdx));
                                        }
                                    }
                                }
                            
                                // Propagate updates to neighbors
                                BPlusNode neighbor = getLeftSiblingInclusive();
                                while (neighbor != null) {
                                    for (int j = 0; j < neighbor.pointers.size(); j++) {
                                        Pair<Integer, Integer> ptr = neighbor.pointers.get(j);
                                        if (ptr.getPageNumber() == nextPointer.getPageNumber() && 
                                            (j == 0 || compare(neighbor.keys.get(j - 1), firstVal) >= 0)) {
                                            int newIdx = ptr.getIndex() - page.getRecordCount();
                                            if (newIdx >= 0) {
                                                neighbor.pointers.set(j, new Pair<>(splitResult.newPageId, newIdx));
                                            }
                                        }
                                    }
                                    neighbor = neighbor.getRightSiblingInclusive();
                                }
                            }
                        }
    
                        System.out.println("Inserted: " + searchKey);
                        return true;
                    }
                }
            }
    
            // Insert at the end of the node
            System.out.println("End of node, inserting: " + searchKey);
            keys.add(searchKey);
    
            if (isLeaf) {
                if (pointers.isEmpty()) {
                    Page newPage = new Page(0, tableID, true);
                    newPage.addRecord(record);
                    Main.getCatalog().getTable(tableID).addPage(newPage);
            
                    // First insert: add pointers for the record and next position
                    pointers.add(new Pair<>(0, 0));
                    pointers.add(new Pair<>(0, 1));
                } else {
                    Pair<Integer, Integer> lastPointer = pointers.getLast();
                    Page page = Main.getStorageManager().getPage(tableID, lastPointer.getPageNumber());
                    page.addRecord(record);
            
                    // Add new pointer for the inserted record
                    pointers.add(new Pair<>(lastPointer.getPageNumber(), lastPointer.getIndex() + 1));
            
                    if (page.isOverfull()) {
                        SplitResult splitResult = Main.getStorageManager().splitPage(page);
            
                        // Debug logging
                        System.out.println(" Splitting (end-insert) — pageId=" + page.getPageId() +
                                          ", newPageId=" + splitResult.newPageId +
                                          ", firstRec=" + splitResult.firstRecord.getData());
            
                        Attribute[] attrs = Main.getCatalog().getTable(tableID).getAttributes();
                        System.out.println(" Attribute count: " + attrs.length);
                        for (int i = 0; i < attrs.length; i++) {
                            System.out.println("  [" + i + "] " + attrs[i].getName());
                        }
                        System.out.println("firstRec.getData().size() = " + splitResult.firstRecord.getData().size());
            
                        Object firstVal = splitResult.firstRecord.getAttributeValue(attr.getName(), attrs);
            
                        // Add key and pointer for the new page
                        keys.add(firstVal);
                        pointers.add(new Pair<>(splitResult.newPageId, splitResult.firstIndex));
            
                        // Update existing pointers for records moved to the new page
                        int originalPageRecords = page.getRecordCount();
                        for (int j = 0; j < pointers.size(); j++) {
                            Pair<Integer, Integer> ptr = pointers.get(j);
                            if (ptr.getPageNumber() == lastPointer.getPageNumber() && ptr.getIndex() >= originalPageRecords) {
                                int newIdx = ptr.getIndex() - originalPageRecords;
                                pointers.set(j, new Pair<>(splitResult.newPageId, newIdx));
                            }
                        }
            
                        // Update pointers in neighboring nodes
                        BPlusNode neighbor = getLeftSiblingInclusive();
                        while (neighbor != null && neighbor != this) {
                            boolean updated = false;
                            for (int j = 0; j < neighbor.pointers.size(); j++) {
                                Pair<Integer, Integer> ptr = neighbor.pointers.get(j);
                                if (ptr.getPageNumber() == lastPointer.getPageNumber() && 
                                    ptr.getIndex() >= originalPageRecords) {
                                    int newIdx = ptr.getIndex() - originalPageRecords;
                                    neighbor.pointers.set(j, new Pair<>(splitResult.newPageId, newIdx));
                                    updated = true;
                                }
                            }
                            if (!updated) break; // Stop if no pointers were updated
                            neighbor = neighbor.getRightSiblingInclusive();
                        }
            
                        // Debug pointers
                        System.out.println("After split: keys=" + keys + ", pointers=" + pointers);
                    }
                }
            }
    
            // Handle node splitting
            System.out.println("ORDER: " + order);
            if (keys.size() == order) {
                int splitIndex = (int) Math.ceil(order / 2.0);
                Object splitKey = keys.get(splitIndex);
    
                BPlusNode leftNode = new BPlusNode(order, this.isLeaf, tableID, attr);
                BPlusNode rightNode = new BPlusNode(order, this.isLeaf, tableID, attr);  
                
                System.out.println("splitting");

                leftNode.parent = parent;
                rightNode.parent = parent;
    
                // Distribute keys and pointers properly
                LinkedList<Object> leftKeys = new LinkedList<>(keys.subList(0, splitIndex));
                LinkedList<Object> rightKeys = new LinkedList<>(keys.subList(splitIndex, keys.size()));
                LinkedList<Pair<Integer, Integer>> leftPointers = new LinkedList<>(pointers.subList(0, splitIndex + 1)); // ✅ +1
                LinkedList<Pair<Integer, Integer>> rightPointers = new LinkedList<>(pointers.subList(splitIndex + 1, pointers.size()));
    
                leftNode.keys = leftKeys;
                leftNode.pointers = leftPointers;
                rightNode.keys = rightKeys;
                rightNode.pointers = rightPointers;
    
                // Link leaf nodes if it's a leaf split
                if (isLeaf) {
                    leftNode.isLeaf = true;
                    rightNode.isLeaf = true;
                    
                    // Set leaf linkage
                    System.out.println("Setting next leaf...");
                    leftNode.setNextLeaf(rightNode);
                    rightNode.setNextLeaf(this.getNextLeaf()); // preserve chain
                }

                if (isRoot) {
                    keys = new LinkedList<>();
                    insert(record, splitKey, pointer, true);
                    leftNode.parent = this;
                    rightNode.parent = this;
    
                    if (intoInternal) rightNode.keys.remove(splitKey);
    
                    children.add(leftNode);
                    children.add(rightNode);
                    pointers = new LinkedList<>();
                    pointers.add(new Pair<>(leftNode.pageID, -1));
                    pointers.add(new Pair<>(rightNode.pageID, -1));
                    isLeaf = false;
                } else {
                    if (intoInternal) rightNode.keys.remove(splitKey);
    
                    parent.children.add(leftNode);
                    parent.children.add(rightNode);
                    parent.pointers.add(new Pair<>(leftNode.pageID, -1));
                    parent.pointers.add(new Pair<>(rightNode.pageID, -1));
                    parent.insert(record, splitKey, 0, true);
    
                    parent.pointers.remove(parent.children.indexOf(this));
                    parent.children.remove(this);
                    Main.getCatalog().getTable(tableID).deleteTreeNode();
                }
            }
        } else {
            return search(searchKey).insert(record, searchKey, pointer, false);
        }
    
        return true;
    }    

    // Increments the index of all pointers starting from the given index
    // if their page number matches the specified page number.
    public boolean incrementPointerIndexes(int pageNum, int indexInPointerList) {
        for (int i = indexInPointerList; i < pointers.size(); i++) {
            Pair<Integer, Integer> pointer = pointers.get(i);
    
            if (pointer.getPageNumber() == pageNum) {
                pointers.set(i, new Pair<>(pointer.getPageNumber(), pointer.getIndex() + 1));
            } else {
                return true; // Stop early if a different page is encountered
            }
        }
        return false; // All matching page numbers were updated
    }

    // Increments pointers for entries that come after a split page.
    // If a pointer shares the same pageNum and its key is greater than the split key,
    // update its page number and index.
    public int incrementPointers(int pageNum, Object searchKey, int newIndex) {
        int offset = 0;
        for (int i = 0; i < pointers.size(); i++) {
            Pair<Integer, Integer> pointer = pointers.get(i);
            if (pointer.getPageNumber() == pageNum && (i == 0 || compare(keys.get(i - 1), searchKey) > 0)) {
                pointers.set(i, new Pair<>(pageNum + 1, newIndex + offset));
                offset++;
            }
        }
        return offset;
    }

    // Checks if this node has any pointers that share the given page number.
    public boolean doesNodeSharePage(int pageNum) {
        for (Pair<Integer, Integer> pointer : pointers) {
            if (pointer.getPageNumber() == pageNum) {
                return true;
            }
        }
        return false;
    }

    public Record delete(Object searchKey, boolean intoInternal) {
        if (isLeaf || intoInternal) {
            // Locate and remove key
            int index = keys.indexOf(searchKey);
            if (index == -1)
                return null; // Key not found

            keys.remove(index);
            Pair<Integer, Integer> pointer = pointers.remove(index);

            // Remove the record from its page
            Page page = Main.getStorageManager().getPage(tableID, pointer.getPageNumber());
            Record record = page.getRecords().get(pointer.getIndex());
            page.deleteRecord(record, pointer.getIndex());

            // If the page becomes empty, drop it; otherwise, update it
            if (page.getRecordCount() == 0) {
                Main.getCatalog().getTable(tableID).dropPage(page.getPageId());
            } else {
                Main.getBuffer().updatePage(page);
            }

            // Handle underflow: try to borrow or merge
            if (children.size() > Math.ceil(order / 2.0) || (isRoot && !children.isEmpty())) {
                if (borrowFrom()) {
                    return record;
                }
                if (isRoot) {
                    children.get(0).merge();
                } else {
                    merge();
                }
            }

            return record;
        } else {
            // Recurse to appropriate child and delete
            BPlusNode target = search(searchKey);
            if (target != null) {
                return target.delete(searchKey, false);
            }
        }

        return null;
    }

    public boolean borrowFrom() {
        BPlusNode toBorrowFrom = null;
        Object key = null;

        // Attempt to find a node to borrow from
        if (isRoot && !children.isEmpty()) {
            toBorrowFrom = children.get(children.size() - 1);
            key = toBorrowFrom.keys.get(0);
        } else if (parent != null && parent.isRoot) {
            toBorrowFrom = parent;
            key = parent.keys.get(0);
        } else if (getLeftSibling() != null) {
            toBorrowFrom = getLeftSibling();
            key = toBorrowFrom.keys.get(toBorrowFrom.keys.size() - 1);
            toBorrowFrom.isRoot = false;
        } else if (getRightSibling() != null) {
            toBorrowFrom = getRightSibling();
            key = toBorrowFrom.keys.get(0);
        }

        // If no suitable node found or it's too small to borrow from, fail
        if (toBorrowFrom == null || toBorrowFrom.keys.size() <= Math.ceil(order / 2.0)) {
            return false;
        }

        // Special case for root redistribution
        if (isRoot) {
            int keyIndex = toBorrowFrom.keys.indexOf(key);
            if (keyIndex != -1 && keyIndex < toBorrowFrom.children.size()) {
                BPlusNode nodeToRedistribute = toBorrowFrom.children.get(keyIndex);
                toBorrowFrom.children.remove(nodeToRedistribute);
                int currentIndex = children.indexOf(toBorrowFrom);
                if (currentIndex > 0) {
                    children.get(currentIndex - 1).children.add(nodeToRedistribute);
                }
            }
        }

        // Insert borrowed key into current node
        insert(null, key, 0, !isLeaf);
        toBorrowFrom.delete(key, !toBorrowFrom.isLeaf);

        return true;
    }

    // Returns the immediate right sibling of this node, if it exists.
    public BPlusNode getRightSibling() {
        if (parent == null) return null;

        int index = parent.children.indexOf(this);
        if (index + 1 < parent.children.size()) {
            return parent.children.get(index + 1);
        }
        return null;
    }

    // Returns the leftmost leaf node of the right sibling subtree.
    // Recursively ascends if no immediate right sibling is available.
    public BPlusNode getRightSiblingInclusive() {
        if (parent == null) return null;

        int index = parent.children.indexOf(this);
        if (index + 1 < parent.children.size()) {
            return parent.children.get(index + 1).getLeftmostLeaf();
        }
        return parent.getRightSiblingInclusive();
    }

    // Returns the immediate left sibling of this node, if it exists.
    public BPlusNode getLeftSibling() {
        if (parent == null) return null;

        int index = parent.children.indexOf(this);
        if (index > 0) {
            return parent.children.get(index - 1);
        }
        return null;
    }

    // Returns the rightmost leaf node of the left sibling subtree.
    // Recursively ascends if no immediate left sibling is available.
    public BPlusNode getLeftSiblingInclusive() {
        if (parent == null) return null;

        int index = parent.children.indexOf(this);
        if (index > 0) {
            return parent.children.get(index - 1).getRightmostLeaf();
        }
        return parent.getLeftSiblingInclusive();
    }

    // Recursively traverses to the leftmost leaf from this node.
    private BPlusNode getLeftmostLeaf() {
        BPlusNode current = this;
        while (!current.isLeaf()) {
            current = current.children.get(0);
        }
        return current;
    }

    // Recursively traverses to the rightmost leaf from this node.
    private BPlusNode getRightmostLeaf() {
        BPlusNode current = this;
        while (!current.isLeaf()) {
            current = current.children.get(current.children.size() - 1);
        }
        return current;
    }

    // Merges this node with its right sibling.
    public boolean mergeRight() {
        int index = parent.children.indexOf(this);
        BPlusNode rightSibling = getRightSibling();

        if (rightSibling == null) {
            return false;
        }

        // Create a new merged node
        BPlusNode merged = new BPlusNode(order, isRoot, tableID, attr);

        // Merge keys
        for (Object key : this.keys) {
            merged.insert(null, key, 0, true);
        }
        for (Object key : rightSibling.keys) {
            merged.insert(null, key, 0, true);
        }

        // Merge children
        merged.children.addAll(this.children);
        merged.children.addAll(rightSibling.children);
        merged.parent = parent;

        // Update parent
        parent.children.remove(this);
        parent.children.remove(rightSibling);
        parent.children.add(index, merged);

        // If this was the last internal split, promote merged node to root
        if (parent.children.size() == 1 && parent.isRoot) {
            merged.isRoot = true;
            merged.parent = null;
            merged.isLeaf = false;
            return true;
        }

        // Delete parent separator key that used to separate the two nodes
        if (index < parent.keys.size()) {
            parent.delete(parent.keys.get(index), true);
        }

        return true;
    }

    // Merges this node with its left sibling.
    public boolean mergeLeft() {
        BPlusNode leftSibling = getLeftSibling();
        if (leftSibling == null) {
            return false;
        }

        int index = parent.children.indexOf(leftSibling);
        BPlusNode merged = new BPlusNode(order, isRoot, tableID, attr);

        // Merge keys
        for (Object key : leftSibling.keys) {
            merged.insert(null, key, 0, true);
        }
        for (Object key : this.keys) {
            merged.insert(null, key, 0, true);
        }

        // Merge children
        merged.children.addAll(leftSibling.children);
        merged.children.addAll(this.children);
        merged.parent = parent;

        // Update parent
        parent.children.remove(this);
        parent.children.remove(leftSibling);
        parent.children.add(index, merged);

        // Promote merged node to root if needed
        if (parent.children.size() == 1 && parent.isRoot) {
            merged.isRoot = true;
            merged.parent = null;
            merged.isLeaf = false;
            return true;
        }

        // Delete separator key in parent
        if (index < parent.keys.size()) {
            parent.delete(parent.keys.get(index), true);
        }

        return true;
    }

    // merge function
    public void merge() {
        if (mergeLeft())
            return;
        if (mergeRight())
            return;
    }

    public void update(Record record, Object keyToUpdate, Object key) {
        delete(key, false);
        insert(record, keyToUpdate, 0, false);
    }

    public int getPageID() {
        return pageID;
    }

    public LinkedList<BPlusNode> getChildren() {
        return this.children;
    }

    public LinkedList<Object> getKeys() {
        return this.keys;
    }

    public boolean isLeaf() {
        return this.isLeaf;
    }

    // Searches the B+ Tree for the leaf node where the given key would reside.
    public BPlusNode search(Object key) {
        if (!isLeaf) {
            BPlusNode childNode = getChildNodeForKey(key);
            if (childNode != null) {
                return childNode.isLeaf ? childNode : childNode.search(key);
            }
        }
        return null; // Should only happen if structure is broken or key is out of bounds
    }

    // Determines the correct child node for a given key based on the current node's keys.
    private BPlusNode getChildNodeForKey(Object key) {
        for (int i = 0; i < keys.size(); i++) {
            if (i == 0 && compare(key, keys.get(i)) < 0) {
                // Key is less than the smallest key
                return children.get(0);
            }

            if (i == keys.size() - 1 && compare(key, keys.get(i)) >= 0) {
                // Key is greater than or equal to the largest key
                return children.get(i + 1);
            }

            if (i > 0 && compare(key, keys.get(i - 1)) >= 0 && compare(key, keys.get(i)) < 0) {
                // Key falls between keys[i - 1] and keys[i]
                return children.get(i);
            }
        }

        return null; // Fallback case
    }

    public ArrayList<BPlusNode> getLeafNodes() {
        ArrayList<BPlusNode> leafNodes = new ArrayList<>();
        collectLeafNodes(this, leafNodes);
        return leafNodes;
    }

    private void collectLeafNodes(BPlusNode node, ArrayList<BPlusNode> leafNodes) {
        if (node.isLeaf) {
            leafNodes.add(node);
        } else {
            for (BPlusNode child : node.children) {
                collectLeafNodes(child, leafNodes);
            }
        }
    }

    public void setNextLeaf(BPlusNode nextLeaf) {
        this.nextLeaf = nextLeaf;
    }
    
    public BPlusNode getNextLeaf() {
        return this.nextLeaf;
    }

    public void setKeys(LinkedList<Object> keys) {
        this.keys = keys;
    }
    
    public void setPointers(LinkedList<Pair<Integer, Integer>> pointers) {
        this.pointers = pointers;
    }
    
    public void writeToFile() {
        int estimatedSize = 4 + pointers.size() * 8 + keys.size() * 12;
        ByteBuffer buffer = ByteBuffer.allocate(Math.max(Main.getPageSize(), estimatedSize));
        
        // Defensive check
        if (pointers.size() != keys.size() + 1) {
            throw new IllegalStateException("Pointer-key mismatch in node: expected " +
                (keys.size() + 1) + " pointers but found " + pointers.size());
        }

        // Write number of keys
        buffer.putInt(keys.size());

        // Write first pointer
        Pair<Integer, Integer> pointer = pointers.get(0);
        buffer.putInt(pointer.getPageNumber());
        buffer.putInt(pointer.getIndex());

        // Write each key and corresponding next pointer
        for (int i = 0; i < keys.size(); i++) {
            Object key = keys.get(i);

            switch (attr.getType().toLowerCase()) {
                case "varchar" -> {
                    String varcharValue = (String) key;
                    byte[] varcharBytes = varcharValue.getBytes();
                    buffer.putInt(varcharBytes.length);
                    buffer.put(varcharBytes);
                }
                case "char" -> {
                    String charValue = (String) key;
                    String padded = String.format("%-" + attr.getSize() + "s", charValue);
                    buffer.put(padded.getBytes());
                }
                case "integer" -> buffer.putInt((int) key);
                case "double" -> buffer.putDouble((double) key);
                case "boolean" -> buffer.put((byte) ((boolean) key ? 1 : 0));
                default -> throw new IllegalArgumentException("Unsupported attribute type: " + attr.getType());
            }

            if (i + 1 >= pointers.size()) {
                System.err.println("Tried to access pointers[" + (i + 1) + "] but size is " + pointers.size());
            }            

            Pair<Integer, Integer> nextPointer = pointers.get(i + 1); // Safe now
            buffer.putInt(nextPointer.getPageNumber()); // Line 692
            buffer.putInt(nextPointer.getIndex());
        }

        // Ensure the directory exists
        String fileName = Main.getDBLocation() + "/BPIndex/" + tableID + ".bin";
        File file = new File(fileName);
        File directory = file.getParentFile();
        if (directory != null && !directory.exists()) {
            directory.mkdirs(); // ?
        }

        // Write to file
        try (RandomAccessFile fileOut = new RandomAccessFile(file, "rw")) {
            fileOut.seek(pageID * Main.getPageSize());

            System.out.println("Total buffer capacity: " + buffer.capacity());
            System.out.println("Buffer position before write: " + buffer.position());

            fileOut.write(buffer.array());
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Write children to file
        for (BPlusNode child : children) {
            child.writeToFile();
        }
    }
    
    public void display() {
        if (this.isRoot) {
            System.out.print("root: ");
        }
        if (this.isLeaf) {
            System.out.print("leaf: ");
        } else {
            System.out.print("internal: ");
        }
        for (Object key : this.keys) {
            System.out.print("| " + key);
        }

        System.out.print(" |\n");
        for (BPlusNode child : children) {
            child.display();
        }
    }

    public int compare(Object insertValue, Object existingValue) { // used for finding where to insert search keys
        if (attr.getType().equalsIgnoreCase("integer")) {
            return ((Integer) insertValue).compareTo((Integer) existingValue);
        } else if (attr.getType().equalsIgnoreCase("double")) {
            return ((Double) insertValue).compareTo((Double) existingValue);
        } else {
            return ((String) insertValue).compareTo((String) existingValue);
        }
    }

    static class Pair<K, V> {
        private K pageNumber;
        private final V index;

        public Pair(K pageNumber, V index) {
            this.pageNumber = pageNumber;
            this.index = index;
        }

        public K getPageNumber() {
            return pageNumber;
        }

        public void setPageNumber(K value) {
            pageNumber = value;
        }

        public V getIndex() {
            return index;
        }
    }

    public Pair<Integer, Integer> deleteAndReturnPointer(Object key) {
        if (isLeaf) {
            int index = keys.indexOf(key);
            if (index == -1) return null;
    
            // Remove key and pointer
            keys.remove(index);
            Pair<Integer, Integer> pointer = pointers.remove(index);
    
            // Delete record from page directly
            Page page = Main.getStorageManager().getPage(tableID, pointer.getPageNumber());
            Record record = page.getRecords().get(pointer.getIndex());
            page.deleteRecord(record, pointer.getIndex());
    
            if (page.getRecordCount() == 0) {
                Main.getCatalog().getTable(tableID).dropPage(page.getPageId());
            } else {
                Main.getBuffer().updatePage(page);
            }
    
            return pointer;
        } else {
            BPlusNode child = search(key);
            return child != null ? child.deleteAndReturnPointer(key) : null;
        }
    }
    
    public List<Pair<Integer, Integer>> getPointers() {
        return pointers;
    }

}