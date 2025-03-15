package zfsFileMonitor;

import java.io.*;
import java.nio.file.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.*;

public class ZFSFileMonitorPID {
    private static final String WATCH_DIR = "/zfs";
    private static final String ZFS_POOL = "zfs";
    private static final String PIPE_NAME = "/tmp/fanotify_pipe";
    private static final int CAPACITY = 2;

    private static final Map<String, Map<Integer, RingBuffer<String>>> processHashes = new HashMap<>();
    private static final RingBuffer<String> currentSnapshots = new RingBuffer<>(CAPACITY);

    private static final RingBuffer<Integer> lastPIDs = new RingBuffer<>(CAPACITY);
    private static final Timer timer = new Timer(500);

    private static int countRollbacks = 0;


    public static void main(String[] args) throws IOException, NoSuchAlgorithmException {
        WatchService watchService = FileSystems.getDefault().newWatchService();
        Path path = Paths.get(WATCH_DIR);
        path.register(watchService, StandardWatchEventKinds.ENTRY_MODIFY);
        PipeReader pr = new PipeReader(PIPE_NAME);

        System.out.println("🔍 Überwachung gestartet...");
        createSnapshot(); // Initialer Snapshot

        // id,path,EVENT,PID
        String line;

        while ((line = pr.readLine()) != null) {
            SemaphoreControl.sem_post();
            Event ev = Event.fromString(line);
            System.out.println("\n##############################");
            System.out.println(ev);
            if (ev.isTxt()) {
                timer.measureTime();
                // System.out.println("elapsed time:" + timer.getTimeDifference());

                handleFileChange(ev);
            }
            SemaphoreControl.sem_post();
        }
        pr.close();
    }

    private static void handleFileChange(Event ev) {
        try {
            // lastPIDs.enqueue(ev.getPID());
            // System.out.printf("lastPIDs: %s\n", lastPIDs);
            int index = timer.calcIndex();
            // System.out.printf("index: %d\n", index);
            String snapshotHash = getLatestSnapshotHash(Path.of(ev.getPath()), index);
            String currentHash = calculateHash(Path.of(ev.getPath()));
            // System.out.printf("snapshotHash: %s\n", snapshotHash);
            // System.out.printf("currentHash: %s\n", currentHash);
            // System.out.printf("snapshot %s\n", currentSnapshots.peekIndex(index));

            // Prüfe, ob ein Prozess die Datei verändert hat, während ein anderer sie nutzte
            if (ev.getType() == EventType.MODIFY && !snapshotHash.isEmpty()) {
                RingBuffer<String> lastProcessHashes = processHashes.getOrDefault(ev.getPath(), new HashMap<>()).get(ev.getPID());
                String processHash;
                try {
                    processHash = lastProcessHashes.peekIndex(index);
                } catch (Exception e) {
                    processHash = "";
                }
                //System.out.printf("processHash: %s\n", processHash);

                if (!processHash.equals(snapshotHash)) {
                    System.out.println("🚨 Inkonsistenz erkannt! Prozess " + ev.getPID() + " hat die Datei geändert, aber es gab parallele Änderungen. Rollback!");
                    rollbackSnapshot(index);
                    countRollbacks++;
                }
            }

            // Speichere den neuen Hash für diesen Prozess
            processHashes.computeIfAbsent(ev.getPath(), k -> new HashMap<>())
                    .computeIfAbsent(ev.getPID(), k -> new RingBuffer<>(CAPACITY))
                    .enqueue(currentHash);
            System.out.println(processHashes);
            System.out.printf("Rollbacks: %d\n", countRollbacks);
            createSnapshot();

        } catch (IOException | NoSuchAlgorithmException | NoSuchElementException e) {
            System.err.println("⚠ Fehler beim Verarbeiten der Datei: " + e.getMessage());
        }
    }

    private static String calculateHash(Path filePath) throws IOException, NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        try (InputStream fis = Files.newInputStream(filePath)) {
            byte[] buffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = fis.read(buffer)) != -1) {
                digest.update(buffer, 0, bytesRead);
            }
        }
        byte[] hashBytes = digest.digest();
        StringBuilder sb = new StringBuilder();
        for (byte b : hashBytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    private static String getLatestSnapshotHash(Path filePath, int index) throws IOException, NoSuchAlgorithmException {
        Path filePathInSnapshot = getLatestSnapshotPath(filePath, index);
        if (Files.exists(filePathInSnapshot)) {
            return calculateHash(filePathInSnapshot);
        } else {
            System.err.println("⚠ Kein Snapshot gefunden: " + filePathInSnapshot);
            return "";
        }
    }

    private static Path getLatestSnapshotPath(Path filePath, int index) {
        String currentSnapshot = currentSnapshots.peekIndex(index);
        return Paths.get(WATCH_DIR + "/.zfs/snapshot/" + currentSnapshot.split("@")[1] + "/" + filePath.getFileName());
    }

    private static void createSnapshot() {
        try {
            // Hole die aktuelle Zeit in Nanosekunden
            long nanoTime = System.nanoTime();
            String timestamp = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss").format(new Date());
            String snapshotName = ZFS_POOL + "@autosnap_" + timestamp + "_" + nanoTime;
            Process process = new ProcessBuilder("zfs", "snapshot", snapshotName).start();
            process.waitFor();
            currentSnapshots.enqueue(snapshotName);
            System.out.println("📸 Snapshot erstellt: " + snapshotName);
        } catch (IOException | InterruptedException e) {
            System.err.println("⚠ Fehler beim Erstellen des Snapshots: " + e.getMessage());
        }
    }

    private static void rollbackSnapshot(int index) {
        try {
            String currentSnapshot = currentSnapshots.peekIndex(index);
            Process process = new ProcessBuilder("zfs", "rollback", currentSnapshot).start();
            process.waitFor();
            System.out.println("🔄 Rollback durchgeführt: " + currentSnapshot);
        } catch (IOException | InterruptedException e) {
            System.err.println("⚠ Fehler beim Rollback: " + e.getMessage());
        }
    }
}

