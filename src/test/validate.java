package test;

public class validate extends Thread {
    private static String path = "/zfs/1.txt";
    private String text;

    public validate(String text) {
        this.text = text;
    }

    @Override
    public void run() {
        test.write(path, this.text, 1000);
        System.out.println("PID: " + ProcessHandle.current().pid());
        System.out.printf("\"%s\" geschrieben.\n", text);
    }

    public static void main(String[] args) throws InterruptedException {
        Thread t1 = new validate("t1");
        Thread t2 = new validate("t2");
        t1.start();
        t1.join();
        Thread.sleep(1000);
        t2.start();
        t2.join();
    }
}
