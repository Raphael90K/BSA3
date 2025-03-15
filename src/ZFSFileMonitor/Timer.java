package ZFSFileMonitor;

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

    public static void main (String[] args) throws InterruptedException {
        Timer timer = new Timer(500);
        timer.measureTime();
        timer.measureTime();
        System.out.println(timer.getTimeDifference());
    }
}
