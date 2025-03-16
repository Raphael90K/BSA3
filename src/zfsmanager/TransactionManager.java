package zfsmanager;

import java.io.*;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;


public class TransactionManager {
    private int rollbacks;
    private int commits;
    private static String latestSnapshot;
    private final ZFSFileManager zfsManager;
    private String directory;
    private Path filePath;
    private String fileHash;
    private BufferedWriter writer;
    private int commitFails;

    public TransactionManager(String pathToDir) {
        this.rollbacks = 0;
        this.commits = 0;
        this.commitFails = 0;
        this.zfsManager = new ZFSFileManager();
        this.directory = pathToDir;
    }

    public void start(Path filePath) {
        latestSnapshot = this.zfsManager.createSnapshot();
        this.filePath = filePath;
        try {
            this.fileHash = calculateHash(this.filePath);
        } catch (IOException | NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    public boolean commit(String content, boolean append) {
        String latestFileHash = "";

        // Versuche, die Datei systemweit zu sperren
        try (RandomAccessFile raf = new RandomAccessFile(filePath.toFile(), "rw");
             FileChannel fileChannel = raf.getChannel();
             FileLock lock = fileChannel.lock()) {

            // Exklusive Sperre für den ganzen Prozess
            System.out.println("Datei gesperrt!");

            // Datei schreiben
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(this.filePath.toFile(), append))) {
                writer.write(content);
            }

            // Neuen Datei-Hash berechnen
            latestFileHash = getLatestSnapshotHash(this.filePath);
            // Prüfe, ob Datei unverändert ist
            if (latestFileHash.equals(this.fileHash)) {
                zfsManager.createSnapshot();
                this.commits++;
                return true;
            } else {
                zfsManager.rollbackSnapshot(latestSnapshot);
                this.rollbacks++;
                return false;
            }

        } catch (IOException | NoSuchAlgorithmException e) {
            e.printStackTrace();
            this.commitFails++;
        }
        return false;
    }

    public int getRollbacks() {
        return rollbacks;
    }

    public int getCommits() {
        return commits;
    }

    public int getCommitFails() {
        return commitFails;
    }

    private String calculateHash(Path filePath) throws IOException, NoSuchAlgorithmException {
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

    private String getLatestSnapshotHash(Path filePath) throws IOException, NoSuchAlgorithmException {
        Path filePathInSnapshot = getLatestSnapshotPath(filePath);
        if (Files.exists(filePathInSnapshot)) {
            return calculateHash(filePathInSnapshot);
        } else {
            System.err.println("⚠ Kein Snapshot gefunden: " + filePathInSnapshot);
            return "";
        }
    }

    private Path getLatestSnapshotPath(Path filePath) throws IOException, NoSuchAlgorithmException {
        return Paths.get(filePath.getParent() + "/.zfs/snapshot/" + this.latestSnapshot.split("@")[1] + "/" + filePath.getFileName());
    }

    private void write(Path filePath, String content) {
        try {
            this.writer.write(content);
            System.out.println("Datei " + filePath + " geschrieben.");
        } catch (IOException e) {
            System.err.println("Fehler beim Schreiben der Datei: " + e.getMessage());
        }
    }
}
