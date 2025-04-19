// Class representing a pointer to a database record's location
class RecordPointer {
    private final int pageNumber; // Disk page number
    private final int index;      // Offset within the page

    public RecordPointer(int pageNumber, int index) {
        this.pageNumber = pageNumber;
        this.index = index;
    }

    @Override
    public String toString() {
        return "(Page: " + pageNumber + ", Index: " + index + ")";
    }

    public int getPageNumber() {
        return pageNumber;
    }
    
    public int getIndex() {
        return index;
    }
    
}