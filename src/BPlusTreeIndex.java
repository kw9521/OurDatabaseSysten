import java.util.ArrayList;
import java.util.List;

// Class representing the B+ Tree index
public class BPlusTreeIndex {
    private BPlusNode root;
    private final int order; // Maximum number of keys in a node

    // Constructor
    public BPlusTreeIndex(int order) {
        this.order = order;
        this.root = new LeafNode();
    }

    public void printTree() {
        printNode(root, 0);
    }
    
    private void printNode(BPlusNode node, int level) {
        String indent = "  ".repeat(level);
        if (node instanceof LeafNode) {
            LeafNode leaf = (LeafNode) node;
            System.out.println(indent + "Leaf: keys=" + leaf.keys + ", pointers=" + leaf.pointers);
        } else {
            InternalNode internal = (InternalNode) node;
            System.out.println(indent + "Internal: keys=" + internal.keys);
            for (BPlusNode child : internal.children) {
                printNode(child, level + 1);
            }
        }
    }

    // Internal Node class
    private class InternalNode extends BPlusNode {
        private List<BPlusNode> children;

        public InternalNode() {
            super();
            this.children = new ArrayList<>();
        }

        @Override
        BPlusNode insert(int key, RecordPointer pointer) {
            // Find the child to insert into
            int i = 0;
            for(int nodeKey : keys){
                if(nodeKey <= key){
                    i++;
                }
            }
            BPlusNode child = children.get(i);
            BPlusNode newNode = child.insert(key, pointer);

            // If child split, handle the split
            if (newNode != null) {
                int midKey = newNode.keys.get(0);
                keys.add(i, midKey);
                children.add(i + 1, newNode);
            

                // If this node overflows, split it
                if (keys.size() > order) {
                    InternalNode sibling = new InternalNode();
                    int mid = keys.size() / 2;
                    int midKeyToPromote = keys.get(mid);

                    // Move keys and children to sibling
                    sibling.keys.addAll(keys.subList(mid + 1, keys.size()));
                    sibling.children.addAll(children.subList(mid + 1, children.size()));
                    keys.subList(mid, keys.size()).clear();
                    children.subList(mid + 1, children.size()).clear();

                    // If this is the root, create a new root
                    if (this == root) {
                        InternalNode newRoot = new InternalNode();
                        newRoot.keys.add(midKeyToPromote);
                        newRoot.children.add(this);
                        newRoot.children.add(sibling);
                        root = newRoot;
                        return null;
                    }
                    // Return sibling to parent
                    InternalNode newSplit = new InternalNode();
                    newSplit.keys.add(midKeyToPromote);
                    newSplit.children.add(sibling);
                    return sibling;
                }
            }
            return null;
        }

        @Override
        RecordPointer search(int key) {
            int i = 0;
            for(int nodeKey : keys){
                if(nodeKey <= key){
                    i++;
                }
            }
            if(i >= children.size()){
                return null;
            }
            return children.get(i).search(key);
        }
    }

    // Leaf Node class
    private class LeafNode extends BPlusNode {
        private List<RecordPointer> pointers; // Pointers to data locations
        private LeafNode next; // Pointer to next leaf node for range queries

        public LeafNode() {
            super();
            this.pointers = new ArrayList<>();
            this.next = null;
        }

        @Override
        BPlusNode insert(int key, RecordPointer pointer) {
            // Find position to insert
            int i = 0;
            while (i < keys.size() && keys.get(i) < key) {
                i++;
            }
            // Avoid duplicates (assumes unique primary keys)
            if (i < keys.size() && keys.get(i) == key) {
                pointers.set(i, pointer); // Update pointer
                return null;
            }
            keys.add(i, key);
            pointers.add(i, pointer);

            // If leaf overflows, split it
            if (keys.size() > order) {
                LeafNode sibling = new LeafNode();
                int mid = keys.size() / 2;

                // Move keys and pointers to sibling
                sibling.keys.addAll(keys.subList(mid, keys.size()));
                sibling.pointers.addAll(pointers.subList(mid, pointers.size()));
                keys.subList(mid, keys.size()).clear();
                pointers.subList(mid, pointers.size()).clear();

                // Link sibling
                sibling.next = this.next;
                this.next = sibling;

                // If this is the root, create a new root
                if (this == root) {
                    InternalNode newRoot = new InternalNode();
                    newRoot.keys.add(sibling.keys.get(0));
                    newRoot.children.add(this);
                    newRoot.children.add(sibling);
                    root = newRoot;
                    return null;
                }
                // Return sibling to parent
                return sibling;
            }
            return null;
        }

        @Override
        RecordPointer search(int key) {
            for (int i = 0; i < keys.size(); i++) {
                if (keys.get(i) == key) {
                    return pointers.get(i);
                }
            }
            return null; // Key not found
        }
    }

    // Public insert method
    public void insert(int key, int pageNumber, int index) {
        RecordPointer pointer = new RecordPointer(pageNumber, index);
        BPlusNode newNode = root.insert(key, pointer);
        // If root split, newNode is the new root
        if (newNode != null) {
            InternalNode newRoot = new InternalNode();
            newRoot.keys.add(newNode.keys.get(0));
            newRoot.children.add(root);
            // Cast newNode to InternalNode since it’s guaranteed to be an InternalNode after a split
            newRoot.children.add(((InternalNode) newNode).children.get(0));
            root = newRoot;
        }
    }

    // Public search method
    public RecordPointer search(int key) {
        return root.search(key);
    }
}