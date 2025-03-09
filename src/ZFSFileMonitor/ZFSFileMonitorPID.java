package ZFSFileMonitor;

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
    private static final Map<String, Map<Integer, String>> processHashes = new HashMap<>();
    private static String currentSnapshot = "";

    public static void main(String[] args) throws IOException, NoSuchAlgorithmException {
        WatchService watchService = FileSystems.getDefault().newWatchService();
        Path path = Paths.get(WATCH_DIR);
        path.register(watchService, StandardWatchEventKinds.ENTRY_MODIFY);
        PipeReader pr = new PipeReader(PIPE_NAME);

        System.out.println("🔍 Überwachung gestartet...");
        createSnapshot(); // Initialer Snapshot

        String line;

        while ((line = pr.readLine()) != null) {
            System.out.println(line);
        }
    }

    private static void handleFileChange(Path filePath) {
        try {
            Set<Integer> processes = getProcessesUsingFile(filePath);
            String snapshotHash = getLatestSnapshotHash(filePath);

            // Wenn keine Prozesse die Datei nutzen, gibt es nichts zu prüfen
            if (processes.isEmpty()) {
                return;
            }

            // Prüfe, ob ein Prozess die Datei verändert hat, während ein anderer sie nutzte
            for (int pid : processes) {
                String lastProcessHash = processHashes
                        .getOrDefault(filePath.toString(), new HashMap<>())
                        .get(pid);
                String currentHash = calculateHash(filePath);

                if (lastProcessHash != null && !lastProcessHash.equals(snapshotHash) && !lastProcessHash.equals(currentHash)) {
                    System.out.println("🚨 Inkonsistenz erkannt! Prozess " + pid + " hat die Datei geändert, aber es gab parallele Änderungen. Rollback!");
                    rollbackSnapshot();
                    return;
                }

                // Speichere den neuen Hash für diesen Prozess
                processHashes.computeIfAbsent(filePath.toString(), k -> new HashMap<>()).put(pid, currentHash);
            }

        } catch (IOException | NoSuchAlgorithmException e) {
            System.err.println("⚠ Fehler beim Verarbeiten der Datei: " + e.getMessage());
        }
    }

    private static Set<Integer> getProcessesUsingFile(Path filePath) throws IOException {
        Set<Integer> pids = new HashSet<>();
        Process process = new ProcessBuilder("lsof", "-t", filePath.toString()).start();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                try {
                    pids.add(Integer.parseInt(line.trim()));
                } catch (NumberFormatException ignored) {
                }
            }
        }
        return pids;
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
            System.err.println("⚠ Kein Snapshot gefunden: " + filePathInSnapshot);
            return "";
        }
    }

    private static Path getLatestSnapshotPath(Path filePath) {
        return Paths.get("/zfs/.zfs/snapshot/" + currentSnapshot.split("@")[1] + "/" + filePath.getFileName());
    }

    private static void createSnapshot() {
        try {
            String timestamp = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss").format(new Date());
            String snapshotName = ZFS_POOL + "@autosnap_" + timestamp;
            Process process = new ProcessBuilder("zfs", "snapshot", snapshotName).start();
            process.waitFor();
            currentSnapshot = snapshotName;
            System.out.println("📸 Snapshot erstellt: " + snapshotName);
        } catch (IOException | InterruptedException e) {
            System.err.println("⚠ Fehler beim Erstellen des Snapshots: " + e.getMessage());
        }
    }

    private static void rollbackSnapshot() {
        try {
            Process process = new ProcessBuilder("zfs", "rollback", currentSnapshot).start();
            process.waitFor();
            System.out.println("🔄 Rollback durchgeführt: " + currentSnapshot);
        } catch (IOException | InterruptedException e) {
            System.err.println("⚠ Fehler beim Rollback: " + e.getMessage());
        }
    }
}
