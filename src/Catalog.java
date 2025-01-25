public class Catalog {
    private String dbLocation;
    private int pageSize;
    private int bufferSize;

    // private List<Table> tables;

    public Catalog(String dbLocation, int pageSize, int bufferSize){
        this.dbLocation = dbLocation;
        this.pageSize = pageSize;
        this.bufferSize = bufferSize;
    }

    public String getDbLocation(){
        return this.dbLocation;
    }

    public int getPageSize(){
        return this.pageSize;
    }

    public int getBufferSize(){
        return this.bufferSize;
    }
}
