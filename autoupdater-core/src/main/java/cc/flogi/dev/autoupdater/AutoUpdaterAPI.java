package cc.flogi.dev.autoupdater;

import cc.flogi.dev.autoupdater.util.ProjectProperties;
import lombok.Getter;
import lombok.NonNull;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Objects;
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
    @NonNull public final static ProjectProperties PROPERTIES = ProjectProperties.from("autoupdater.properties");
    public static final boolean DEBUG = false;

    private static AutoUpdaterAPI instance;
    @Getter private static Plugin plugin;
    @Getter private Logger logger = Logger.getLogger("AutoUpdaterAPI");

    public AutoUpdaterAPI(JavaPlugin javaPlugin) {
        instance = this;
        plugin = javaPlugin;

        Objects.requireNonNull(PROPERTIES);
    }

    public static AutoUpdaterAPI get() {
        return instance;
    }

    public void printError(Exception ex) {
        this.logger.warning("An error has occurred.");
        this.logger.warning("If you cannot figure out this error on your own (e.g. a config error) please copy and paste everything from here to END ERROR and post it at " + PROPERTIES.REPO_URL + "issues.");
        this.logger.warning("\n============== BEGIN ERROR ==============");
        this.logger.warning("API VERSION: " + PROPERTIES.getTitle());
        this.logger.warning("\nERROR MESSAGE: " + ex.getMessage());
        this.logger.warning("\nSTACKTRACE: ");
        ex.printStackTrace();
        this.logger.warning("\n============== END ERROR ==============");
    }

    public void printError(Exception ex, String extraInfo) {
        this.logger.warning("An error has occurred.");
        this.logger.warning("If you cannot figure out this error on your own (e.g. a config error) please copy and paste everything from here to END ERROR and post it at " + PROPERTIES.REPO_URL + "issues.");
        this.logger.warning("\n============== BEGIN ERROR ==============");
        this.logger.warning("API VERSION: " + PROPERTIES.getTitle());
        this.logger.warning("\nAPI MESSAGE: " + extraInfo);
        this.logger.warning("\nERROR MESSAGE: " + ex.getMessage());
        this.logger.warning("\nSTACKTRACE: ");
        ex.printStackTrace();
        this.logger.warning("\n============== END ERROR ==============");
    }

    public void printPluginError(String header, String message) {
        this.logger.warning("============== BEGIN ERROR ==============");
        this.logger.warning(header);
        this.logger.warning("\nAPI VERSION: " + PROPERTIES.getTitle());
        this.logger.warning("\nAPI MESSAGE: " + message);
        this.logger.warning("\n============== END ERROR ==============");
    }
}
