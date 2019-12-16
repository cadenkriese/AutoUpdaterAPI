package cc.flogi.dev.autoupdater;

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
public final class InternalCore {
    public final static ProjectProperties PROPERTIES = ProjectProperties.from("autoupdater.properties");
    static boolean DEBUG = false;
    static boolean METRICS = true;

    private static InternalCore instance;
    @Getter private static Plugin plugin;
    @Getter private static File dataFolder;
    @Getter private static Logger logger = Logger.getLogger("AutoUpdaterAPI");

    public InternalCore(JavaPlugin javaPlugin) {
        instance = this;
        plugin = javaPlugin;
        dataFolder = new File(plugin.getDataFolder().getParent() + "/.auapi/");

        Properties properties = new Properties();

        Objects.requireNonNull(PROPERTIES);
    }

    public static InternalCore get() {
        return instance;
    }

    public void printError(Exception ex) {
        logger.warning("An error has occurred.");
        logger.warning("If you cannot figure out this error on your own please copy and paste " +
                               "\neverything from here to END ERROR and post it at " + PROPERTIES.REPO_URL + "issues.");
        logger.warning("\n============== BEGIN ERROR ==============");
        logger.warning("API VERSION: " + PROPERTIES.getTitle());
        logger.warning("\nERROR MESSAGE: " + ex.getMessage());
        logger.warning("\nSTACKTRACE: ");
        ex.printStackTrace();
        logger.warning("\n============== END ERROR ==============");
    }

    public void printError(Exception ex, String extraInfo) {
        logger.warning("An error has occurred.");
        logger.warning("If you cannot figure out this error on your own please copy and paste " +
                               "\neverything from here to END ERROR and post it at " + PROPERTIES.REPO_URL + "issues.");
        logger.warning("\n============== BEGIN ERROR ==============");
        logger.warning("API VERSION: " + PROPERTIES.getTitle());
        logger.warning("\nAPI MESSAGE: " + extraInfo);
        logger.warning("\nERROR MESSAGE: " + ex.getMessage());
        logger.warning("\nSTACKTRACE: ");
        ex.printStackTrace();
        logger.warning("\n============== END ERROR ==============");
    }

    public void printPluginError(String header, String message) {
        logger.warning("============== BEGIN ERROR ==============");
        logger.warning(header);
        logger.warning("\nAPI VERSION: " + PROPERTIES.getTitle());
        logger.warning("\nAPI MESSAGE: " + message);
        logger.warning("\n============== END ERROR ==============");
    }
}
