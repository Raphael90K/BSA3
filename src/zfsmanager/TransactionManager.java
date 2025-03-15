package zfsmanager;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.NoSuchElementException;

public class TransactionManager {
    private ZFSFileManager fileManager;
    private int rollbacks;
    private String currentSnapshot;
    private ZFSFileManager zfsManager;
    private Path path;
    private String fileHash;

    public TransactionManager(ZFSFileManager fileManager) {
        this.rollbacks = 0;
        this.fileManager = fileManager;
        this.zfsManager = new ZFSFileManager();
    }

    public void start(Path path) {
        this.zfsManager.createSnapshot();
        this.path = path;
        try {
            this.fileHash = calculateHash(path);
        } catch (IOException | NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    public boolean commit() {

        return false;
    }

    public int getRollbacks() {
        return rollbacks;
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
        return Paths.get(filePath.getParent() + "/.zfs/snapshot/" + currentSnapshot.split("@")[1] + "/" + filePath.getFileName());
    }
}
