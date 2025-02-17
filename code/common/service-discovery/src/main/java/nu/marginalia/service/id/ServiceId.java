package nu.marginalia.service.id;

public enum ServiceId {

    Assistant("assistant-service"),
    Api("api-service"),
    Search("search-service"),
    Index("index-service"),
    Query("query-service"),
    Executor("executor-service"),

    Control("control-service"),

    Dating("dating-service"),
    Explorer("explorer-service");

    public final String name;
    ServiceId(String name) {
        this.name = name;
    }

    public String withNode(int node) {
        return name + ":" + node;
    }

    public static ServiceId byName(String name) {
        for (ServiceId id : values()) {
            if (id.name.equals(name)) {
                return id;
            }
        }
        return null;
    }
}
