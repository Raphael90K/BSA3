package testA;

import java.io.*;
import java.nio.channels.OverlappingFileLockException;
import java.nio.file.*;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

public class AutomaticTest {

    private static final String DIRECTORY = "/zfs";
    private static final int RUN_DURATION = 10; // in Sekunden
    private static final Random RANDOM = new Random();
    private static final AtomicBoolean running = new AtomicBoolean(true);

    public static void main(String[] args) {
        // 10 Dateien erstellen (0.txt - 9.txt)
        for (int i = 0; i < 10; i++) {
            try {
                Files.createFile(Paths.get(DIRECTORY, i + ".txt"));
            } catch (IOException e) {
                // Falls Datei schon existiert, ignorieren
            }
        }
        ArrayList<String> results = new ArrayList<>(5);
        for (int i = 2; i < 17; i *= 2) {
            running.set(true);
            runTest(i, results);
        }
        System.out.println("############### Summary ###############");
        for (String result : results) {
            System.out.println(result);
        }
    }

    public static void runTest(int nThreads, ArrayList<String> results) {



        int[] commits = new int[nThreads];
        int[] rollbacks = new int[nThreads];
        int[] errors = new int[nThreads];

        Thread[] threads = new Thread[nThreads];

        for (int i = 0; i < nThreads; i++) {
            threads[i] = new TxThread(running, RANDOM, commits, rollbacks, errors, i);
            threads[i].start();
        }
        // Programm für 60 Sekunden laufen lassen
        try {
            Thread.sleep(RUN_DURATION * 1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        running.set(false);

        for (int i = 0; i < nThreads; i++) {
            try {
                threads[i].join();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }

        int c = Arrays.stream(commits).sum();
        int r = Arrays.stream(rollbacks).sum();
        int e = Arrays.stream(errors).sum();
        StringBuilder s = new StringBuilder();

        s.append("############################\n");
        s.append("Threads: " + nThreads + "\n");

        s.append("Commits: " + c + "\n");
        s.append("Rollbacks: " + r +"\n");
        s.append("Errors: " + e + "\n");

        results.add(s.toString());
    }

}

