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

    //TODO
    public boolean evaluate(){
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
}
