public class SplitResult {
    public Record firstRecord;
    public int newPageId;
    public int firstIndex;
    public SplitResult(Record firstRecord, int newPageId, int firstIndex) {
        this.firstRecord = firstRecord;
        this.newPageId = newPageId;
        this.firstIndex = firstIndex;
    }
}