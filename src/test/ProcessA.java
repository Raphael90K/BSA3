package test;

import java.io.*;
import java.nio.file.*;

public class ProcessA {
    private static final String STATUS_FILE = "status.txt";
    private static final String OUTPUT_FILE = "/zfs/test.txt";
    private static final int SLEEP_MILLIS = 2000;
    private static final int iterations = 10;

    public static void main(String[] args) throws InterruptedException {
        for (int i = 0; i < iterations; i++) {
            System.out.printf("iteration: %d\n", i);
            process();
            Thread.sleep(SLEEP_MILLIS);
        }
    }

    public static void process() {
        try {
            // 1. Datei öffnen
            System.out.println("Programm A hat die Datei geöffnet.");
            FileWriter writer = new FileWriter(OUTPUT_FILE, true);
            Thread.sleep(SLEEP_MILLIS);

            setStatus("A_OPEN");
            // 2. Warten, bis B die Datei geöffnet hat
            while (!"B_OPEN".equals(getStatus())) {
                Thread.sleep(500);
            }
            // 3. Warten, bis B fertig geschrieben hat
            while (!"B_DONE".equals(getStatus())) {
                Thread.sleep(500);
            }
            // 4. Schreiben in die Datei
            writer.write("Programm A schreibt und schließt.\n");
            writer.close();
            Thread.sleep(SLEEP_MILLIS);
            System.out.println("Programm A hat geschrieben.");
            // 5. Abschlussstatus setzen
            setStatus("A_DONE");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static String getStatus() throws IOException {
        return Files.readString(Path.of(STATUS_FILE)).trim();
    }

    private static void setStatus(String status) throws IOException {
        Files.writeString(Path.of(STATUS_FILE), status);
    }
}
