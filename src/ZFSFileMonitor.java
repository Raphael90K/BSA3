import java.io.*;
import java.nio.file.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;

public class ZFSFileMonitor {
    private static final String WATCH_DIR = "/zfs";
    private static final String ZFS_DATASET = "zfs";
    private static final Map<String, String> initialHashes = new HashMap<>();
    private static final Map<String, String> lastKnownHashes = new HashMap<>();

    public static void main(String[] args) throws IOException, NoSuchAlgorithmException {
        WatchService watchService = FileSystems.getDefault().newWatchService();
        Path path = Paths.get(WATCH_DIR);
        path.register(watchService, StandardWatchEventKinds.ENTRY_MODIFY, StandardWatchEventKinds.ENTRY_CREATE);

        System.out.println("Überwachung gestartet...");
        createSnapshot();
        initializeHashes();

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
                    onFileSave(filePath);
                }
            }
            key.reset();
        }
    }

    private static void initializeHashes() throws IOException, NoSuchAlgorithmException {
        Files.walk(Paths.get(WATCH_DIR)).filter(Files::isRegularFile).forEach(file -> {
            try {
                String hash = calculateHash(file);
                initialHashes.put(file.toString(), hash);
                lastKnownHashes.put(file.toString(), hash);
            } catch (IOException | NoSuchAlgorithmException e) {
                System.err.println("Fehler beim Initialisieren des Hashes: " + e.getMessage());
            }
        });
    }

    private static void handleFileChange(Path filePath) {
        try {
            String newHash = calculateHash(filePath);
            String lastHash = lastKnownHashes.get(filePath.toString());

            if (lastHash != null && !lastHash.equals(newHash)) {
                System.out.println("WARNUNG: Datei wurde extern geändert: " + filePath);
            }
            lastKnownHashes.put(filePath.toString(), newHash);
        } catch (IOException | NoSuchAlgorithmException e) {
            System.err.println("Fehler beim Verarbeiten der Datei: " + e.getMessage());
        }
    }

    public static void onFileSave(Path filePath) {
        try {
            String savedHash = calculateHash(filePath);
            String initialHash = initialHashes.get(filePath.toString());
            String lastHash = lastKnownHashes.get(filePath.toString());

            if (lastHash != null && !lastHash.equals(savedHash) && !lastHash.equals(initialHash)) {
                System.out.println("Externe Änderung erkannt! Rollback wird durchgeführt: " + filePath);
                rollbackSnapshot();
            }
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

    private static void createSnapshot() {
        try {
            Process process = new ProcessBuilder("zfs", "snapshot", ZFS_DATASET + "@autosnap").start();
            process.waitFor();
            System.out.println("Snapshot erstellt.");
        } catch (IOException | InterruptedException e) {
            System.err.println("Fehler beim Erstellen des Snapshots: " + e.getMessage());
        }
    }

    private static void rollbackSnapshot() {
        try {
            Process process = new ProcessBuilder("zfs", "rollback", ZFS_DATASET + "@autosnap").start();
            process.waitFor();
            System.out.println("Rollback durchgeführt.");
        } catch (IOException | InterruptedException e) {
            System.err.println("Fehler beim Rollback: " + e.getMessage());
        }
    }
}
