package ZFSFileMonitor;

public class Event {
    private int id;
    private EventType type;
    private String path;
    private int PID;

    Event(int id, EventType type, String fileName, int PID) {
        this.id = id;
        this.type = type;
        this.path = fileName;
        this.PID = PID;
    }

    public static Event fromString(String entry) {
        String[] parts = entry.split(",");
        Event event = new Event(Integer.parseInt(parts[0]), EventType.valueOf(parts[1]), parts[2], Integer.parseInt(parts[3]));
        return event;
    }

    public int getId() {
        return id;
    }

    public EventType getType() {
        return type;
    }

    public String getPath() {
        return path;
    }

    public int getPID() {
        return PID;
    }

    public boolean isTxt() {
        return this.path.endsWith(".txt");
    }

    @Override
    public String toString() {
        return "Event{" +
                "id=" + id +
                ", type=" + type +
                ", fileName='" + path + '\'' +
                ", PID=" + PID +
                '}';
    }
}
