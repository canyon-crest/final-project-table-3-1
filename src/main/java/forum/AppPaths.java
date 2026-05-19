package forum;

import java.io.File;
import java.net.URISyntaxException;
import java.security.CodeSource;

/**
 * Resolves runtime file locations for IDE runs, JAR runs, and jpackage EXE runs.
 */
public final class AppPaths {

    private AppPaths() {
    }

    /**
     * Finds forum.properties from common runtime locations.
     *
     * @return properties file if found, otherwise null
     */
    public static File resolveForumPropertiesFile() {
        String explicit = System.getProperty("forum.properties.path", "").trim();
        if (!explicit.isEmpty()) {
            File explicitFile = new File(explicit);
            if (explicitFile.isFile()) {
                return explicitFile;
            }
        }

        File[] candidates = new File[] {
                new File("forum.properties"),
                fromAppDir("forum.properties"),
                fromAppDir("app/forum.properties"),
                fromCodeSourceDir("forum.properties"),
                fromCodeSourceDir("app/forum.properties")
        };
        return firstFile(candidates);
    }

    /**
     * Finds an asset file from common runtime locations.
     *
     * @param relativePath path like assets/teacher-wide.jpg
     * @return asset file if found, otherwise null
     */
    public static File resolveAssetFile(String relativePath) {
        if (relativePath == null || relativePath.isBlank()) {
            return null;
        }
        File[] candidates = new File[] {
                new File(relativePath),
                fromAppDir(relativePath),
                fromAppDir("app/" + relativePath),
                fromCodeSourceDir(relativePath),
                fromCodeSourceDir("app/" + relativePath)
        };
        return firstFile(candidates);
    }

    private static File firstFile(File[] candidates) {
        for (File f : candidates) {
            if (f != null && f.isFile()) {
                return f;
            }
        }
        return null;
    }

    private static File fromAppDir(String relativePath) {
        File appDir = detectAppDir();
        return appDir == null ? null : new File(appDir, relativePath);
    }

    private static File detectAppDir() {
        String appPath = System.getProperty("jpackage.app-path", "").trim();
        if (!appPath.isEmpty()) {
            File exe = new File(appPath);
            File parent = exe.getParentFile();
            if (parent != null) {
                return parent;
            }
        }
        return null;
    }

    private static File fromCodeSourceDir(String relativePath) {
        File dir = detectCodeSourceDir();
        return dir == null ? null : new File(dir, relativePath);
    }

    private static File detectCodeSourceDir() {
        try {
            CodeSource src = AppPaths.class.getProtectionDomain().getCodeSource();
            if (src == null || src.getLocation() == null) {
                return null;
            }
            File codeLocation = new File(src.getLocation().toURI());
            if (codeLocation.isFile()) {
                return codeLocation.getParentFile();
            }
            return codeLocation;
        } catch (URISyntaxException ex) {
            return null;
        }
    }
}
