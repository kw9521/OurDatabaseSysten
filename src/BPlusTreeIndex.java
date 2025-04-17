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
            int i = 0;
            for (int nodeKey : keys) {
                if (nodeKey <= key) {
                    i++;
                }
            }
            BPlusNode child = children.get(i);
            BPlusNode newNode = child.insert(key, pointer);

            if (newNode != null) {
                int midKey = newNode.keys.get(0);
                keys.add(i, midKey);
                children.add(i + 1, newNode);

                if (keys.size() > order) {
                    InternalNode sibling = new InternalNode();
                    int mid = keys.size() / 2;
                    int midKeyToPromote = keys.get(mid);

                    sibling.keys.addAll(keys.subList(mid + 1, keys.size()));
                    sibling.children.addAll(children.subList(mid + 1, children.size()));
                    keys.subList(mid, keys.size()).clear();
                    children.subList(mid + 1, children.size()).clear();

                    if (this == root) {
                        InternalNode newRoot = new InternalNode();
                        newRoot.keys.add(midKeyToPromote);
                        newRoot.children.add(this);
                        newRoot.children.add(sibling);
                        root = newRoot;
                        return null;
                    }
                    InternalNode newSplit = new InternalNode();
                    newSplit.keys.add(midKeyToPromote);
                    newSplit.children.add(sibling);
                    return newSplit;
                }
            }
            return null;
        }

        @Override
        RecordPointer search(int key) {
            int i = 0;
            for (int nodeKey : keys) {
                if (nodeKey <= key) {
                    i++;
                }
            }
            if (i >= children.size()) {
                return null;
            }
            return children.get(i).search(key);
        }

        @Override
        boolean delete(int key) {
            int i = 0;
            for (int nodeKey : keys) {
                if (nodeKey <= key) {
                    i++;
                }
            }
            BPlusNode child = children.get(i);
            boolean underflow = child.delete(key);

            if (underflow) {
                // Check if child is now underflowing
                int minKeys = (order % 2 == 0) ? order / 2 : (order + 1) / 2 - 1;
                if (child.keys.size() < minKeys || (child instanceof InternalNode && children.get(i).keys.size() < minKeys)) {
                    // Try borrowing from siblings
                    if (i > 0 && children.get(i - 1).keys.size() > minKeys) {
                        borrowFromLeft(i);
                    } else if (i < children.size() - 1 && children.get(i + 1).keys.size() > minKeys) {
                        borrowFromRight(i);
                    } else {
                        // Merge with a sibling
                        if (i > 0) {
                            mergeWithLeft(i);
                        } else {
                            mergeWithRight(i);
                        }
                    }
                }
            }
            return keys.size() < (order % 2 == 0 ? order / 2 : (order + 1) / 2 - 1);
        }

        private void borrowFromLeft(int childIndex) {
            BPlusNode child = children.get(childIndex);
            BPlusNode leftSibling = children.get(childIndex - 1);
            int parentKey = keys.get(childIndex - 1);

            if (child instanceof LeafNode) {
                LeafNode leafChild = (LeafNode) child;
                LeafNode leftLeaf = (LeafNode) leftSibling;
                // Move the last key and pointer from left sibling to child
                leafChild.keys.add(0, leftLeaf.keys.remove(leftLeaf.keys.size() - 1));
                leafChild.pointers.add(0, leftLeaf.pointers.remove(leftLeaf.pointers.size() - 1));
                // Update parent key
                keys.set(childIndex - 1, leafChild.keys.get(0));
            } else {
                InternalNode internalChild = (InternalNode) child;
                InternalNode leftInternal = (InternalNode) leftSibling;
                // Move the last child from left sibling to child
                internalChild.children.add(0, leftInternal.children.remove(leftInternal.children.size() - 1));
                // Move parent key to child
                internalChild.keys.add(0, parentKey);
                // Move last key from left sibling to parent
                keys.set(childIndex - 1, leftInternal.keys.remove(leftInternal.keys.size() - 1));
            }
        }

        private void borrowFromRight(int childIndex) {
            BPlusNode child = children.get(childIndex);
            BPlusNode rightSibling = children.get(childIndex + 1);
            int parentKey = keys.get(childIndex);

            if (child instanceof LeafNode) {
                LeafNode leafChild = (LeafNode) child;
                LeafNode rightLeaf = (LeafNode) rightSibling;
                // Move the first key and pointer from right sibling to child
                leafChild.keys.add(rightLeaf.keys.remove(0));
                leafChild.pointers.add(rightLeaf.pointers.remove(0));
                // Update parent key
                keys.set(childIndex, rightLeaf.keys.isEmpty() ? rightLeaf.keys.get(0) : rightLeaf.keys.get(0));
            } else {
                InternalNode internalChild = (InternalNode) child;
                InternalNode rightInternal = (InternalNode) rightSibling;
                // Move the first child from right sibling to child
                internalChild.children.add(rightInternal.children.remove(0));
                // Move parent key to child
                internalChild.keys.add(parentKey);
                // Move first key from right sibling to parent
                keys.set(childIndex, rightInternal.keys.remove(0));
            }
        }

        private void mergeWithLeft(int childIndex) {
            BPlusNode child = children.get(childIndex);
            BPlusNode leftSibling = children.get(childIndex - 1);
            int parentKey = keys.get(childIndex - 1);

            if (child instanceof LeafNode) {
                LeafNode leafChild = (LeafNode) child;
                LeafNode leftLeaf = (LeafNode) leftSibling;
                // Move all keys and pointers from child to left sibling
                leftLeaf.keys.addAll(leafChild.keys);
                leftLeaf.pointers.addAll(leafChild.pointers);
                leftLeaf.next = leafChild.next;
                // Remove child and parent key
                children.remove(childIndex);
                keys.remove(childIndex - 1);
            } else {
                InternalNode internalChild = (InternalNode) child;
                InternalNode leftInternal = (InternalNode) leftSibling;
                // Move parent key to left sibling
                leftInternal.keys.add(parentKey);
                // Move all keys and children from child to left sibling
                leftInternal.keys.addAll(internalChild.keys);
                leftInternal.children.addAll(internalChild.children);
                // Remove child and parent key
                children.remove(childIndex);
                keys.remove(childIndex - 1);
            }
        }

        private void mergeWithRight(int childIndex) {
            BPlusNode child = children.get(childIndex);
            BPlusNode rightSibling = children.get(childIndex + 1);
            int parentKey = keys.get(childIndex);

            if (child instanceof LeafNode) {
                LeafNode leafChild = (LeafNode) child;
                LeafNode rightLeaf = (LeafNode) rightSibling;
                // Move all keys and pointers from right sibling to child
                leafChild.keys.addAll(rightLeaf.keys);
                leafChild.pointers.addAll(rightLeaf.pointers);
                leafChild.next = rightLeaf.next;
                // Remove right sibling and parent key
                children.remove(childIndex + 1);
                keys.remove(childIndex);
            } else {
                InternalNode internalChild = (InternalNode) child;
                InternalNode rightInternal = (InternalNode) rightSibling;
                // Move parent key to child
                internalChild.keys.add(parentKey);
                // Move all keys and children from right sibling to child
                internalChild.keys.addAll(rightInternal.keys);
                internalChild.children.addAll(rightInternal.children);
                // Remove right sibling and parent key
                children.remove(childIndex + 1);
                keys.remove(childIndex);
            }
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
            int i = 0;
            while (i < keys.size() && keys.get(i) < key) {
                i++;
            }
            if (i < keys.size() && keys.get(i) == key) {
                pointers.set(i, pointer);
                return null;
            }
            keys.add(i, key);
            pointers.add(i, pointer);

            if (keys.size() > order) {
                LeafNode sibling = new LeafNode();
                int mid = keys.size() / 2;
                sibling.keys.addAll(keys.subList(mid, keys.size()));
                sibling.pointers.addAll(pointers.subList(mid, pointers.size()));
                keys.subList(mid, keys.size()).clear();
                pointers.subList(mid, pointers.size()).clear();
                sibling.next = this.next;
                this.next = sibling;

                if (this == root) {
                    InternalNode newRoot = new InternalNode();
                    newRoot.keys.add(sibling.keys.get(0));
                    newRoot.children.add(this);
                    newRoot.children.add(sibling);
                    root = newRoot;
                    return null;
                }
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
            return null;
        }

        @Override
        boolean delete(int key) {
            for (int i = 0; i < keys.size(); i++) {
                if (keys.get(i) == key) {
                    keys.remove(i);
                    pointers.remove(i);
                    // Check for underflow: minimum keys is ⌈order/2⌉
                    int minKeys = (order % 2 == 0) ? order / 2 : (order + 1) / 2;
                    return keys.size() < minKeys;
                }
            }
            return false; // Key not found, no underflow
        }
    }

    // Public insert method
    public void insert(int key, int pageNumber, int index) {
        RecordPointer pointer = new RecordPointer(pageNumber, index);
        BPlusNode newNode = root.insert(key, pointer);
        if (newNode != null) {
            InternalNode newRoot = new InternalNode();
            newRoot.keys.add(newNode.keys.get(0));
            newRoot.children.add(root);
            newRoot.children.add(newNode);
            root = newRoot;
        }
    }

    // Public delete method
    public void delete(int key) {
        boolean underflow = root.delete(key);
        if (root instanceof InternalNode && root.keys.size() == 0 && ((InternalNode) root).children.size() == 1) {
            root = ((InternalNode) root).children.get(0);
        }
    }

    // Public search method
    public RecordPointer search(int key) {
        return root.search(key);
    }
}