
package chatty.gui.components.updating;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.TemporalUnit;
import java.util.logging.Logger;
import java.util.regex.Pattern;

/**
 *
 * @author tduva
 */
public class Stuff {
    
    private static final Logger LOGGER = Logger.getLogger(Stuff.class.getName());
    
    private static boolean initialized = false;
    private static Path javaHome = null;
    private static Path javawExe = null;
    private static Path chattyExe = null;
    private static Path chattyExeDir = null;
    private static Path jarPath = null;
    private static Path jarDir = null;
    private static Path tempDir = null;
    
    public static synchronized Path getTempDir() {
        checkInitialized();
        return tempDir;
    }
    
    public static synchronized boolean isStandalone() {
        checkInitialized();
        return chattyExe != null && javawExe == null;
    }
    
    public static synchronized boolean installPossible() {
        checkInitialized();
        return jarDir != null && tempDir != null;
    }
    
    public static synchronized Path getInstallDir(boolean standaloneInstaller) {
        checkInitialized();
        if (standaloneInstaller) {
            return chattyExeDir;
        }
        return jarDir;
    }
    
    public static synchronized Path getJarPath() {
        checkInitialized();
        return jarPath;
    }
    
    public static synchronized Path getChattyExeDir() {
        checkInitialized();
        return chattyExeDir;
    }
    
    public static synchronized Path getChattyExe() {
        checkInitialized();
        return chattyExe;
    }
    
    public static synchronized Path getJavawExe() {
        checkInitialized();
        return javawExe;
    }
    
    private static void checkInitialized() {
        if (!initialized) {
            throw new IllegalStateException("Not initialized");
        }
    }
    
    public static synchronized void init() {
        Path jarPathTemp = null;
        try {
            jarPathTemp = Paths.get(RunUpdater.class.getProtectionDomain().getCodeSource().getLocation().toURI());
            if (!jarPathTemp.toString().endsWith(".jar")
                    || !Files.exists(jarPathTemp)
                    || !Files.isRegularFile(jarPathTemp)) {
                jarPathTemp = null;
            }
        } catch (URISyntaxException ex) {
            LOGGER.warning("jar: "+ex);
        }
        init(jarPathTemp);
    }
    
    public static synchronized void init(Path jarPathTemp) {
        try {
            javaHome = Paths.get(System.getProperty("java.home"));

            Path javawExeTemp = javaHome.resolve("bin").resolve("javaw.exe");
            if (Files.exists(javawExeTemp)) {
                javawExe = javawExeTemp;
            } else {
                javawExe = null;
            }

            jarPath = jarPathTemp;
            if (jarPath != null) {
                jarDir = jarPath.getParent();
            } else {
                jarDir = null;
            }

            if (jarDir != null) {
                Path chattyExeTemp = javaHome.getParent().resolve("Chatty.exe");
                Path chattyExeTemp2 = jarDir.getParent().resolve("Chatty.exe");
                if (Files.exists(chattyExeTemp) && chattyExeTemp.equals(chattyExeTemp2)) {
                    chattyExe = chattyExeTemp;
                    chattyExeDir = chattyExe.getParent();
                } else {
                    chattyExe = null;
                }
            }

            Path tempDirTemp = Paths.get(System.getProperty("java.io.tmpdir"));
            if (Files.exists(tempDirTemp) && Files.isWritable(tempDirTemp)) {
                tempDir = tempDirTemp;
            }
        } catch (Exception ex) {
            LOGGER.warning("Error initializing stuff: " + ex);
        }
        LOGGER.info(String.format("Updating Stuff: javaHome: %s / javawExe: %s / jarPath: %s / chattyExe: %s / tempDir: %s",
                javaHome, javawExe != null ? "yes" : "no", jarPath, chattyExe, tempDir));
        initialized = true;
    }
    
    public static Path getTempFilePath(String name) {
        return Stuff.getTempDir().resolve(name+"_"+System.currentTimeMillis()+".exe");
    }
    
    public static void clearOldSetups() {
        checkInitialized();
        if (getTempDir() == null) {
            LOGGER.warning("Failed to delete old setup files: Invalid temp dir");
            return;
        }
        Instant oldIfBefore = Instant.now().minus(Duration.ofDays(7));
        Pattern fileNameCheck = Pattern.compile("Chatty_.*installer.*[0-9]+\\.exe");
        LOGGER.info("Checking if old setup files should be deleted..");
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(
                getTempDir(),
                file -> {
                    if (fileNameCheck.matcher(file.getFileName().toString()).matches()) {
                        System.out.println(file.getFileName());
                    }
                    return fileNameCheck.matcher(file.getFileName().toString()).matches()
                        && Files.getLastModifiedTime(file).toInstant().isBefore(oldIfBefore);
                            })) {
            for (Path file : stream) {
                try {
                    Files.delete(file);
                    LOGGER.info("Deleted old setup file: "+file);
                } catch (IOException ex) {
                    LOGGER.warning("Failed to delete old setup file: "+ex);
                }
            }
        } catch (IOException ex) {
            LOGGER.warning("Failed to delete old setup files: "+ex);
        }
    }
    
}
