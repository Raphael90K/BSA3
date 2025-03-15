package zfsFileMonitor;

public class Timer {
    private long lastEventTime = 0;
    private long secondLastEventTime = 0;
    private final int maxTDinMs;

    public Timer(int maxTDinMs) {
        this.maxTDinMs = maxTDinMs;
    }

    public void measureTime() {
        secondLastEventTime = lastEventTime;
        lastEventTime = System.currentTimeMillis();
    }

    public long getTimeDifference() {
        return lastEventTime - secondLastEventTime;
    }

    public boolean maxTDNotReached(){
        return (lastEventTime - secondLastEventTime) < maxTDinMs;
    }

    public int calcIndex(){
        return (lastEventTime - secondLastEventTime) < maxTDinMs ? 1 : 0;
    }

}
