package cc.flogi.dev.autoupdater;

import cc.flogi.dev.autoupdater.util.ProjectProperties;
import lombok.Getter;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.Objects;
import java.util.Properties;
import java.util.logging.Logger;

/*
 * AutoUpdaterAPI
 *
 * Author: Caden Kriese (flogic)
 *
 * This resource is licensed under the Apache License Version 2.0.
 * Full license information in the LICENSE file.
 *
 */

public class AutoUpdaterAPI {
    public final static ProjectProperties PROPERTIES = ProjectProperties.from("autoupdater.properties");
    public static final boolean DEBUG = false;
    public static final boolean METRICS = true;

    private static AutoUpdaterAPI instance;
    @Getter private static Plugin plugin;
    @Getter private static File dataFolder;
    @Getter private static Logger logger = Logger.getLogger("AutoUpdaterAPI");

    public AutoUpdaterAPI(JavaPlugin javaPlugin) {
        instance = this;
        plugin = javaPlugin;
        dataFolder = new File(plugin.getDataFolder().getParent() + "/.auapi/");

        Properties properties = new Properties();

        Objects.requireNonNull(PROPERTIES);
    }

    public static AutoUpdaterAPI get() {
        return instance;
    }

    public void printError(Exception ex) {
        logger.warning("An error has occurred.");
        logger.warning("If you cannot figure out this error on your own (e.g. a config error) please copy and paste everything from here to END ERROR and post it at " + PROPERTIES.REPO_URL + "issues.");
        logger.warning("\n============== BEGIN ERROR ==============");
        logger.warning("API VERSION: " + PROPERTIES.getTitle());
        logger.warning("\nERROR MESSAGE: " + ex.getMessage());
        logger.warning("\nSTACKTRACE: ");
        ex.printStackTrace();
        logger.warning("\n============== END ERROR ==============");
    }

    public void printError(Exception ex, String extraInfo) {
        logger.warning("An error has occurred.");
        logger.warning("If you cannot figure out this error on your own (e.g. a config error) please copy and paste everything from here to END ERROR and post it at " + PROPERTIES.REPO_URL + "issues.");
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
