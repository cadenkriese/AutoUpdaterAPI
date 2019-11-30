package cc.flogi.dev.autoupdater;

import cc.flogi.dev.autoupdater.util.ProjectProperties;
import lombok.Getter;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.io.File;
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
    private static AutoUpdaterAPI instance;
    public static AutoUpdaterAPI get() {
        return instance;
    }

    @Getter private static Plugin plugin;
    @Getter private Logger logger;
    @Getter private File privateDataFolder;

    public final static ProjectProperties PROPERTIES = ProjectProperties.from("autoupdater.properties");
    public static final boolean DEBUG = false;

    /**
     * Instantiate AutoUpdaterAPI
     *
     * @param javaPlugin the plugin running the API.
     */
    public AutoUpdaterAPI(Plugin javaPlugin) {
        //General setup
        plugin = javaPlugin;
        instance = this;
        logger = javaPlugin.getLogger();
        privateDataFolder = new File(plugin.getDataFolder().getParent() + "/.auapi/");
    }

    /**
     * Instantiate a self-updater for a normal resource.
     *
     * @param initiator     The player that initiated the update (set to null if there is none).
     * @param resourceId    The ID of the plugin on Spigot found in the url after the name.
     * @param locale        The locale file you want containing custom messages. Note most messages will be followed with a progress indicator like [DOWNLOADING].
     * @param deleteOld     Should the old version of the plugin be deleted and disabled.
     */
    public Updater createSelfUpdater(Player initiator, int resourceId, UpdateLocale locale, boolean deleteOld) {
        return new Updater(plugin, initiator, resourceId, locale, deleteOld);
    }

    /**
     * Instantiate a self-updater for a normal resource.
     *
     * @param initiator     The player that initiated the update (set to null if there is none).
     * @param resourceId    The ID of the plugin on Spigot found in the url after the name.
     * @param locale        The locale file you want containing custom messages. Note most messages will be followed with a progress indicator like [DOWNLOADING].
     * @param deleteOld     Should the old version of the plugin be deleted and disabled.
     * @param endTask       Runnable that will run once the update has completed.
     */
    public Updater createSelfUpdater(Player initiator, int resourceId, UpdateLocale locale, boolean deleteOld, UpdaterRunnable endTask) {
        return new Updater(plugin, initiator, resourceId, locale, deleteOld, endTask);
    }

    protected void printError(Exception ex) {
        this.logger.severe("A severe error has occurred.");
        this.logger.severe("If you cannot figure out this error on your own (e.g. a config error) please copy and paste everything from here to END ERROR and post it at "+PROPERTIES.REPO_URL +"issues.");
        this.logger.severe("");
        this.logger.severe("============== BEGIN ERROR ==============");
        this.logger.severe("API VERSION: "+PROPERTIES.getTitle());
        this.logger.severe("");
        this.logger.severe("ERROR MESSAGE: " + ex.getMessage());
        this.logger.severe("");
        this.logger.severe("STACKTRACE: ");
        ex.printStackTrace();
        this.logger.severe("");
        this.logger.severe("============== END ERROR ==============");
    }

    public void printError(Exception ex, String extraInfo) {
        this.logger.severe("A severe error has occurred.");
        this.logger.severe("If you cannot figure out this error on your own (e.g. a config error) please copy and paste everything from here to END ERROR and post it at "+PROPERTIES.REPO_URL +"issues.");
        this.logger.severe("");
        this.logger.severe("============== BEGIN ERROR ==============");
        this.logger.severe("API VERSION: "+PROPERTIES.getTitle());
        this.logger.severe("");
        this.logger.severe("API MESSAGE: " + extraInfo);
        this.logger.severe("");
        this.logger.severe("ERROR MESSAGE: " + ex.getMessage());
        this.logger.severe("");
        this.logger.severe("STACKTRACE: ");
        ex.printStackTrace();
        this.logger.severe("");
        this.logger.severe("============== END ERROR ==============");
    }

    protected void printPluginError(String header, String message) {
        this.logger.severe("============== BEGIN ERROR ==============");
        this.logger.severe(header);
        this.logger.severe("");
        this.logger.severe("API VERSION: "+PROPERTIES.getTitle());
        this.logger.severe("");
        this.logger.severe("API MESSAGE: " + message);
        this.logger.severe("");
        this.logger.severe("============== END ERROR ==============");
    }
}
