/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package cloud.bernarpa.jphoto;

import com.adobe.internal.xmp.XMPException;
import com.drew.imaging.ImageMetadataReader;
import com.drew.imaging.ImageProcessingException;
import com.drew.metadata.Directory;
import com.drew.metadata.Metadata;
import com.drew.metadata.Tag;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.io.IOException;
import java.util.List;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import javax.imageio.ImageIO;

/**
 *
 * @author rnd
 */
public class PhotoOrganizer {

    private static final boolean PRINT = false;
    private static final DateTimeFormatter QUICK_TIME_FORMATTER = DateTimeFormatter.ofPattern("E LLL dd H:m:s VV yyyy");
    private static final DateTimeFormatter MP4_FORMATTER = QUICK_TIME_FORMATTER;
    private static final DateTimeFormatter EXIF_FORMATTER = DateTimeFormatter.ofPattern("yyyy:MM:dd H:m:s");
    private static final DateTimeFormatter FINGERPRINT_FORMATTER = DateTimeFormatter.ofPattern("yyyy:MM:dd H-m-s");

    private final Path dirCollection;
    private final Path dirImport;

    public PhotoOrganizer(String dirCollection, String dirImport) throws Exception {
        this.dirCollection = Path.of(dirCollection);
        if (dirCollection.length() == 0 || !Files.exists(this.dirCollection)) {
            throw new Exception("Collection directory not found: " + dirCollection);
        }
        this.dirImport = Path.of(dirImport);
        if (dirImport.length() == 0 || !Files.exists(this.dirImport)) {
            throw new Exception("Import directory not found: " + dirImport);
        }
    }

    private boolean isPhoto(Path file) {
        String normalized = file.toString().toLowerCase();
        return normalized.endsWith(".jpg")
                || normalized.endsWith(".jpeg")
                || normalized.endsWith(".heic")
                || normalized.endsWith(".mov")
                || normalized.endsWith(".mp4");
    }

    private List<Path> listPhotos(Path dir) throws IOException {
        List<Path> fileList = new ArrayList<>();
        Files.walkFileTree(dir, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs)
                    throws IOException {
                if (!Files.isDirectory(file) && isPhoto(file)) {
                    fileList.add(file);
                }
                return FileVisitResult.CONTINUE;
            }
        });
        return fileList;
    }

    public LocalDateTime getDateTime(Metadata meta) {
        LocalDateTime quickTimeCreation = null;
        LocalDateTime mp4Creation = null;
        LocalDateTime exifOriginal = null;
        LocalDateTime exifDigitized = null;
        for (Directory directory : meta.getDirectories()) {
            for (Tag tag : directory.getTags()) {
                if (PRINT) {
                    System.out.format("[%s] - %s = %s\n", directory.getName(), tag.getTagName(), tag.getDescription());
                }
                try {
                    if (directory.getName().equals("QuickTime") && tag.getTagName().equals("Creation Time")) {
                        quickTimeCreation = LocalDateTime.parse(tag.getDescription().replace("CEST", "CET"), QUICK_TIME_FORMATTER);
                    } else if (directory.getName().equals("MP4") && tag.getTagName().equals("Creation Time")) {
                        mp4Creation = LocalDateTime.parse(tag.getDescription().replace("CEST", "CET"), MP4_FORMATTER);
                    } else if (directory.getName().equals("Exif SubIFD") && tag.getTagName().equals("Date/Time Original")) {
                        exifOriginal = LocalDateTime.parse(tag.getDescription(), EXIF_FORMATTER);
                    } else if (directory.getName().equals("Exif SubIFD") && tag.getTagName().equals("Date/Time Digitized")) {
                        exifDigitized = LocalDateTime.parse(tag.getDescription(), EXIF_FORMATTER);
                    }
                } catch (DateTimeParseException ex) {
                    if (PRINT) {
                        System.err.println(ex);
                        ex.printStackTrace();
                    }
                }
            }
        }
        if (PRINT) {
            System.out.printf("quickTimeCreation = %s\n", quickTimeCreation);
            System.out.printf("mp4Creation = %s\n", mp4Creation);
            System.out.printf("exifOriginal = %s\n", exifOriginal);
            System.out.printf("exifDigitized = %s\n", exifDigitized);
        }
        if (exifOriginal != null) {
            return exifOriginal;
        } else if (exifDigitized != null) {
            return exifDigitized;
        } else if (quickTimeCreation != null) {
            return quickTimeCreation;
        } else if (mp4Creation != null) {
            return mp4Creation;
        } else {
            return null;
        }
    }

    private void fixPhotos() throws IOException, ImageProcessingException, XMPException {
        List<Path> toImport = listPhotos(dirImport);
        Path dirNoExif = dirImport.resolve("NoExif");
        Path dirOrganized = dirImport.resolve("Organized");
        for (Path photo : toImport) {
            Metadata meta = null;
            try {
                meta = ImageMetadataReader.readMetadata(photo.toFile());
            } catch (ImageProcessingException ex) {
                System.err.println("ImageProcessingException: " + photo);
                continue;
            }
            LocalDateTime dateTime = getDateTime(meta);
            if (dateTime != null) {
                String newDirName = String.format("%d-%02d-%02d",
                        dateTime.getYear(),
                        dateTime.getMonthValue(),
                        dateTime.getDayOfMonth());
                String photoStr = photo.toString();
                String extension = photoStr.substring(photoStr.lastIndexOf(".") + 1);
                String newFileName = String.format("%d-%02d-%02d_%02d-%02d-%02d.%s",
                        dateTime.getYear(),
                        dateTime.getMonthValue(),
                        dateTime.getDayOfMonth(),
                        dateTime.getHour(),
                        dateTime.getMinute(),
                        dateTime.getSecond(),
                        extension.toLowerCase());
                Path newDir = dirOrganized.resolve(newDirName);
                if (!Files.exists(newDir)) {
                    Files.createDirectories(newDir);
                }
                Path renamed = newDir.resolve(newFileName);
                Files.move(photo, renamed, StandardCopyOption.REPLACE_EXISTING);
            } else {
                if (!Files.exists(dirNoExif)) {
                    Files.createDirectory(dirNoExif);
                }
                Path renamed = dirNoExif.resolve(photo.getFileName());
                Files.move(photo, renamed, StandardCopyOption.REPLACE_EXISTING);
            }
        }
    }

    private static String bytesToHex(byte[] hash) {
        StringBuilder hexString = new StringBuilder(2 * hash.length);
        for (int i = 0; i < hash.length; i++) {
            String hex = Integer.toHexString(0xff & hash[i]);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        return hexString.toString();
    }

    private String getPhotoFingerprintPixel(Path photo) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            BufferedImage image = ImageIO.read(photo.toFile());
            byte[] pixels = ((DataBufferByte) image.getRaster().getDataBuffer()).getData();
            byte[] hash = digest.digest(pixels);
            return String.format("%d-%d-%s", image.getWidth(), image.getHeight(), bytesToHex(hash));
        } catch (NoSuchAlgorithmException ex) {
            System.err.println("HASHING ALGORITHM NOT FOUND: " + ex.getMessage());
            return null;
        } catch (Exception ex) {
            System.err.println("PHOTO FILE ERROR " + photo + ": " + ex.getMessage());
            return null;
        }
    }

    private String getPhotoFingerprint(Path photo) {
        try {
            Metadata meta = ImageMetadataReader.readMetadata(photo.toFile());
            LocalDateTime dateTime = getDateTime(meta);
            return dateTime.format(FINGERPRINT_FORMATTER);
        } catch (Exception ex) {
            System.err.println("PHOTO FILE ERROR " + photo + ": " + ex.getMessage());
            return null;
        }
    }

    private void updateFingerprints(Path dir, MainFrame mainFrame) throws IOException, SQLException {
        PhotoDb db = new PhotoDb();
        List<Path> photos = listPhotos(dir);
        int photoCount = photos.size();
        AtomicInteger counter = new AtomicInteger(0);
        int size = photos.size();
        Set<String> fingerprinted = db.getFingerprintedFiles();
        mainFrame.setStatusMessage("Computing fingerprints...");
        List<String> fingerprints = photos.stream()
                .parallel()
                .map(photo -> {
                    String res = null;
                    int count = counter.getAndIncrement();
                    if (!mainFrame.closing.get() && !fingerprinted.contains(photo.toString())) {
                        res = getPhotoFingerprint(photo);
                        String message = String.format("%d / %d", count, size);
                        mainFrame.setProgressBar(0, photoCount, count, message);
                    }
                    return res;
                })
                .collect(Collectors.toList());
        if (mainFrame.closing.get()) {
            return;
        }
        mainFrame.setStatusMessage("Storing fingerprints...");
        for (int i = 0; i < size; ++i) {
            String fingerprint = fingerprints.get(i);
            Path photo = photos.get(i);
            if (fingerprint != null && db.getFingerprint(photo) == null) {
                db.insertFingerprint(photo, fingerprint);
                String message = String.format("%d / %d", i + 1, size);
                mainFrame.setProgressBar(0, photoCount, i + 1, message);
            }
        }
        mainFrame.resetProgressBar();
    }

    private void removeDuplicates(MainFrame mainFrame) throws IOException, SQLException {
        mainFrame.setStatusMessage("Detecting duplicates...");
        PhotoDb db = new PhotoDb();
        List<Path> photos = listPhotos(dirImport);
        Path dirDuplicates = dirImport.resolve("Duplicates");
        for (Path photo : photos) {
            String fingerprint = getPhotoFingerprint(photo);
            String duplicate = db.findDuplicate(photo, fingerprint);
            if (duplicate != null) {
                if (!Files.exists(dirDuplicates)) {
                    Files.createDirectory(dirDuplicates);
                }
                Path newPath = dirDuplicates.resolve(photo.getFileName());
                Files.move(photo, newPath, StandardCopyOption.REPLACE_EXISTING);
                Path oldDir = photo.getParent();
                if (listPhotos(oldDir).isEmpty()) {
                    Files.delete(oldDir);
                }
            }
        }
    }

    public void organizePhotos(MainFrame mainFrame) throws IOException, SQLException, ImageProcessingException, XMPException {
        fixPhotos();
        //updateFingerprints(dirImport, mainFrame);
        updateFingerprints(dirCollection, mainFrame);
        removeDuplicates(mainFrame);
        mainFrame.setStatusMessage("That's all, folks!");
    }
}
