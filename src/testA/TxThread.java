package testA;

import zfsmanager.TransactionManager;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;


public class TxThread extends Thread {
    private static final String DIR = "/zfs";
    private TransactionManager tm;
    private AtomicBoolean running;
    private Random rand;
    private int[] commits;
    private int[] rollbacks;
    private int[] errors;
    private int id;

    public TxThread(AtomicBoolean running, Random random, int[] commits, int[] rollbacks, int[] errors, int id) {
        this.tm = new TransactionManager(DIR);
        this.running = running;
        this.rand = random;
        this.commits = commits;
        this.rollbacks = rollbacks;
        this.errors = errors;
        this.id = id;
    }

    public void run() {

        while (running.get()) {
            // select random File
            String fileName = this.rand.nextInt(10) + ".txt";
            Path filePath = Paths.get(DIR, fileName);

            // start transaction
            this.tm.start(filePath);

            // wait random time
            try {
                Thread.sleep(this.rand.nextInt(500));
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }

            // add random char to idea file
            char randomChar = (char) ('A' + this.rand.nextInt(26));
            this.tm.commit(randomChar + "\n", true, false);
        }

        this.commits[id] = this.tm.getCommits();
        this.rollbacks[id] = this.tm.getRollbacks();
        this.errors[id] = this.tm.getCommitFails();
        System.out.println("TxThread " + id + " stopped");
        System.out.printf("Commits: %d\n", this.tm.getCommits());
        System.out.printf("Rollbacks: %d\n", this.tm.getRollbacks());
        System.out.printf("Failures: %d\n", this.tm.getCommitFails());
    }
}
