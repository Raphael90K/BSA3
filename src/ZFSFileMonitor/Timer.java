package ZFSFileMonitor;

public class Timer {
    private long lastEventTime = 0;
    private long secondLastEventTime = 0;
    private final int maxTDinNs;

    public Timer(int maxTDinMs) {
        this.maxTDinNs = maxTDinMs;
    }

    public void measureTime() {
        secondLastEventTime = lastEventTime;
        lastEventTime = System.currentTimeMillis();
    }

    public long getTimeDifference() {
        return lastEventTime - secondLastEventTime;
    }

    public boolean maxTDNotReached(){
        return (lastEventTime - secondLastEventTime) < maxTDinNs;
    }

    public static void main (String[] args) throws InterruptedException {
        Timer timer = new Timer(10);

        timer.measureTime();
        Thread.sleep(5);
        timer.measureTime();
        System.out.println(timer.getTimeDifference());
        System.out.println(timer.maxTDNotReached());
    }
}
