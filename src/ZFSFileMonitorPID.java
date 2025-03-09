import java.io.*;
import java.nio.file.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.*;

public class ZFSFileMonitorPID {
    private static final String WATCH_DIR = "/zfs";
    private static final String ZFS_POOL = "zfs";
    private static final Map<String, String> lastKnownHashes = new HashMap<>();
    private static final Map<String, FileMetadata> openedFileMetadata = new HashMap<>();
    private static String currentSnapshot = ""; // Speichert den aktuellen Snapshot-Namen

    // Datei-Metadaten (Hash + PID)
    static class FileMetadata {
        String hash;
        long pid;  // Prozess-ID

        FileMetadata(String hash, long pid) {
            this.hash = hash;
            this.pid = pid;
        }
    }

    public static void main(String[] args) throws IOException, NoSuchAlgorithmException {
        WatchService watchService = FileSystems.getDefault().newWatchService();
        Path path = Paths.get(WATCH_DIR);
        path.register(watchService, StandardWatchEventKinds.ENTRY_MODIFY, StandardWatchEventKinds.ENTRY_CREATE);

        System.out.println("Überwachung gestartet...");
        initializeHashes();
        createSnapshot(); // Initialen Snapshot erstellen

        while (true) {
            WatchKey key;
            try {
                key = watchService.take();
            } catch (InterruptedException e) {
                return;
            }

            for (WatchEvent<?> event : key.pollEvents()) {
                WatchEvent.Kind<?> kind = event.kind();
                Path filePath = path.resolve((Path) event.context());

                if (kind == StandardWatchEventKinds.ENTRY_CREATE || kind == StandardWatchEventKinds.ENTRY_MODIFY) {
                    handleFileChange(filePath);
                }
            }
            key.reset();
        }
    }

    private static void initializeHashes() throws IOException, NoSuchAlgorithmException {
        Files.walk(Paths.get(WATCH_DIR)).filter(Files::isRegularFile).forEach(file -> {
            try {
                String hash = calculateHash(file);
                lastKnownHashes.put(file.toString(), hash);
            } catch (IOException | NoSuchAlgorithmException e) {
                System.err.println("Fehler beim Initialisieren des Hashes: " + e.getMessage());
            }
        });
    }

    private static void handleFileChange(Path filePath) {
        try {
            String snapshotHash = getLatestSnapshotHash(filePath);
            FileMetadata metadata = openedFileMetadata.get(filePath.toString());  // Hole die gespeicherten Metadaten (Hash + PID)

            System.out.printf("[%s] Snapshot-Hash: %s vs gespeicherter Hash: %s (PID: %d)\n", filePath, snapshotHash, metadata != null ? metadata.hash : "null", metadata != null ? metadata.pid : -1);

            if (metadata != null && !metadata.hash.equals(snapshotHash) && !snapshotHash.isEmpty()) {
                System.out.println("WARNUNG: Datei wurde extern geändert! Rollback wird durchgeführt: " + filePath);
                rollbackSnapshot();  // Rollback durchführen
            } else {
                onFileSave(filePath);  // Snapshot erstellen
            }
        } catch (IOException | NoSuchAlgorithmException e) {
            System.err.println("Fehler beim Verarbeiten der Datei: " + e.getMessage());
        }
    }

    public static void onFileSave(Path filePath) {
        try {
            // Berechne den Hash der Datei
            String openedFileHash = calculateHash(filePath);

            // Hole die PID des aktuellen Prozesses
            long pid = ProcessHandle.current().pid();

            // Speichere die Datei-Metadaten (Hash + PID)
            openedFileMetadata.put(filePath.toString(), new FileMetadata(openedFileHash, pid));

            createSnapshot(); // Erstelle Snapshot nach Dateiänderung
            System.out.println("Snapshot nach erfolgreicher Speicherung erstellt: " + filePath);
            lastKnownHashes.put(filePath.toString(), openedFileHash);  // Speichere den Hash im lastKnownHashes
        } catch (IOException | NoSuchAlgorithmException e) {
            System.err.println("Fehler beim Speichern der Datei: " + e.getMessage());
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

    private static String getLatestSnapshotHash(Path filePath) throws IOException, NoSuchAlgorithmException {
        Path filePathInSnapshot = getLatestSnapshotPath(filePath);
        if (Files.exists(filePathInSnapshot)) {
            return calculateHash(filePathInSnapshot);
        } else {
            System.err.println("Kein Snapshot gefunden: " + filePathInSnapshot);
            return "";
        }
    }

    private static Path getLatestSnapshotPath(Path filePath) {
        return Paths.get("/zfs/.zfs/snapshot/" + currentSnapshot.split("@")[1] + "/" + filePath.getFileName());
    }

    private static String getTimestampWithNanoseconds() {
        long currentTimeMillis = System.currentTimeMillis();
        int nanoseconds = (int) (System.nanoTime() % 1_000_000_000); // Nanosekunden innerhalb der Sekunde
        String timeString = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss").format(new Date(currentTimeMillis));
        return timeString + "_" + String.format("%09d", nanoseconds); // Sorgt für 9-stellige Nanosekunden
    }

    private static void createSnapshot() {
        try {
            String timestamp = getTimestampWithNanoseconds();
            String snapshotName = ZFS_POOL + "@autosnap_" + timestamp;
            Process process = new ProcessBuilder("zfs", "snapshot", snapshotName).start();
            process.waitFor();
            currentSnapshot = snapshotName; // Speichern des aktuellen Snapshots
            System.out.println("Snapshot erstellt: " + snapshotName);
        } catch (IOException | InterruptedException e) {
            System.err.println("Fehler beim Erstellen des Snapshots: " + e.getMessage());
        }
    }

    private static void rollbackSnapshot() {
        try {
            Process process = new ProcessBuilder("zfs", "rollback", currentSnapshot).start();
            process.waitFor();
            System.out.println("Rollback durchgeführt: " + currentSnapshot);
        } catch (IOException | InterruptedException e) {
            System.err.println("Fehler beim Rollback: " + e.getMessage());
        }
    }
}
