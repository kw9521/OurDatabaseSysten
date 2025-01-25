public class Catalog {
    private String dbLocation;
    private int pageSize;
    private int bufferSize;

    public Catalog(String dbLocation, int pageSize, int bufferSize){
        this.dbLocation = dbLocation;
        this.pageSize = pageSize;
        this.bufferSize = bufferSize;
    }
}
