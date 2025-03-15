package test;

import java.io.*;
import java.nio.file.*;

public class ProcessB {
    private static final String OUTPUT_FILE = "/zfs/1000.txt";
    private static final int SLEEP_MILLIS = 1000;
    private static final int iterations = 20;

    public static void main(String[] args) {
        for (int i = 0; i < iterations; i++) {
            process();
        }
    }

    public static void process() {
        try {
            // 1. Warten, bis A geöffnet hat
            while (!('B' == getStatus())) {
                Thread.sleep(SLEEP_MILLIS);
            }
            // 2. Datei öffnen
            FileWriter writer = new FileWriter(OUTPUT_FILE, true);
            Thread.sleep(SLEEP_MILLIS);

            // 3. Schreiben in die Datei
            writer.write("B\n");
            writer.close();
            Thread.sleep(SLEEP_MILLIS);
            // 4. Status setzen, damit A schreiben kann
            setStatus('A');

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static char getStatus() throws Exception {
        return SimpleWrite.read_mem();
    }

    private static void setStatus(char status) throws Exception {
        SimpleWrite.write_mem(status);
    }
}
