import java.io.*;
import java.nio.file.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.*;

public class ZFSFileMonitor {
    private static final String WATCH_DIR = "/zfs";
    private static final String ZFS_POOL = "zfs";
    private static final Map<String, String> lastKnownHashes = new HashMap<>();
    private static String currentSnapshot = ""; // Speichert den aktuellen Snapshot-Namen

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
            String lastHash = lastKnownHashes.get(filePath.toString());

            System.out.printf("[%s] %s: %s\n", filePath, snapshotHash, lastHash);
            if (lastHash != null && !lastHash.equals(snapshotHash) && !snapshotHash.isEmpty()) {
                System.out.println("WARNUNG: Datei wurde extern geändert! Rollback wird durchgeführt: " + filePath);
                rollbackSnapshot();
            } else {
                onFileSave(filePath);
            }
        } catch (IOException | NoSuchAlgorithmException e) {
            System.err.println("Fehler beim Verarbeiten der Datei: " + e.getMessage());
        }
    }

    public static void onFileSave(Path filePath) {
        createSnapshot(); // Erstelle Snapshot nach Dateiänderung
        System.out.println("Snapshot nach erfolgreicher Speicherung erstellt: " + filePath);
        try {
            String savedHash = calculateHash(filePath);
            lastKnownHashes.put(filePath.toString(), savedHash);
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
        System.out.println(filePathInSnapshot);
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
