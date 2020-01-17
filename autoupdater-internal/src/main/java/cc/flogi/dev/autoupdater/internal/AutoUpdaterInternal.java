package cc.flogi.dev.autoupdater.internal;

import lombok.Getter;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.Objects;
import java.util.Properties;
import java.util.logging.Logger;


/**
 * Class holding internal API information.
 */
public final class AutoUpdaterInternal {
    public final static ProjectProperties PROPERTIES = ProjectProperties.from("autoupdater.properties");
    public static boolean DEBUG = false;
    public static boolean METRICS = true;

    private static AutoUpdaterInternal instance;
    @Getter private static Plugin plugin;
    @Getter private static File dataFolder;
    @Getter private static File cacheFolder;
    @Getter private static Logger logger = Logger.getLogger("AutoUpdaterAPI");

    AutoUpdaterInternal(JavaPlugin javaPlugin) {
        instance = this;
        plugin = javaPlugin;
        dataFolder = new File(plugin.getDataFolder().getParent() + "/.auapi/");
        cacheFolder = new File(dataFolder, "caches");

        dataFolder.mkdirs();

        Properties properties = new Properties();

        Objects.requireNonNull(PROPERTIES);
    }

    /**
     * Provide internal API utilities.
     *
     * @return Instance of internal API utility class.
     */
    public static AutoUpdaterInternal get() {
        return instance;
    }

    void printError(Exception ex) {
        printError(ex, null);
    }

    void printError(Exception ex, String extraInfo) {
        logger.warning("An error has occurred.");
        logger.warning("If you cannot figure out this error on your own please copy and paste " +
                "\neverything from here to END ERROR and post it at " + PROPERTIES.REPO_URL + "issues.");
        logger.warning("\n============== BEGIN ERROR ==============");
        logger.warning("API VERSION: " + PROPERTIES.getTitle());
        if (extraInfo != null)
            logger.warning("\nAPI MESSAGE: " + extraInfo);
        logger.warning("\nERROR MESSAGE: " + ex.getMessage());
        logger.warning("\nSTACKTRACE: ");
        ex.printStackTrace();
        logger.warning("\n============== END ERROR ==============");
    }

    void printPluginError(String header, String message) {
        logger.warning("============== BEGIN ERROR ==============");
        logger.warning(header);
        logger.warning("\nAPI VERSION: " + PROPERTIES.getTitle());
        logger.warning("\nAPI MESSAGE: " + message);
        logger.warning("\n============== END ERROR ==============");
    }
}
