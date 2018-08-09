package map.poc2.constants;

public enum CacheType {
    HAZELCAST_CACHE("hazelcast-cache"),
    BUY_SHEET("buy-sheet-cache-9"),
    BUY_SHEET_1("buy-sheet-cache-1");

    CacheType(String value) {
        this.value = value;
    }

    private String value;

    public String getValue() {
        return value;
    }
}
