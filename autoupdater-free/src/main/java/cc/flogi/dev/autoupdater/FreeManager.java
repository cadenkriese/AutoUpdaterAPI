package cc.flogi.dev.autoupdater;

import lombok.Getter;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * @author Caden Kriese (flogic)
 *
 * Created on 12/1/19
 */
public class FreeManager {
    @Getter private static Plugin plugin;
    @Getter private static AutoUpdaterAPI updaterAPI;

    public FreeManager(JavaPlugin javaPlugin) {
        plugin = javaPlugin;
        updaterAPI = new AutoUpdaterAPI(javaPlugin);
    }

    /**
     * Instantiate an updater for a normal resource.
     *
     * @param plugin     The plugin that should be updated.
     * @param initiator  The player that initiated the update (set to null if there is none).
     * @param resourceId The ID of the plugin on Spigot found in the url after the name.
     * @param locale     The locale file you want containing custom messages. Note most messages will be followed with a progress indicator like [DOWNLOADING].
     * @param deleteOld  Should the old version of the plugin be deleted and disabled.
     * @return The updater object for you to update yourself with.
     */
    public Updater createUpdater(Plugin plugin, Player initiator, int resourceId, UpdateLocale locale, boolean deleteOld) {
        return new Updater(plugin, initiator, resourceId, locale, deleteOld);
    }

    /**
     * Instantiate an updater for a normal resource.
     *
     * @param plugin     The plugin that should be updated.
     * @param initiator  The player that initiated the update (set to null if there is none).
     * @param resourceId The ID of the plugin on Spigot found in the url after the name.
     * @param locale     The locale file you want containing custom messages. Note most messages will be followed with a progress indicator like [DOWNLOADING].
     * @param deleteOld  Should the old version of the plugin be deleted and disabled.
     * @param endTask    Runnable that will run once the update has completed.
     * @return The updater object for you to update yourself with.
     */
    public Updater createUpdater(Plugin plugin, Player initiator, int resourceId, UpdateLocale locale, boolean deleteOld, UpdaterRunnable endTask) {
        return new Updater(plugin, initiator, resourceId, locale, deleteOld, endTask);
    }

    /**
     * Instantiate a self-updater for a normal resource.
     *
     * @param initiator  The player that initiated the update (set to null if there is none).
     * @param resourceId The ID of the plugin on Spigot found in the url after the name.
     * @param locale     The locale file you want containing custom messages. Note most messages will be followed with a progress indicator like [DOWNLOADING].
     * @param deleteOld  Should the old version of the plugin be deleted and disabled.
     * @return The updater object for you to update yourself with.
     */
    public Updater createSelfUpdater(Player initiator, int resourceId, UpdateLocale locale, boolean deleteOld) {
        return new Updater(plugin, initiator, resourceId, locale, deleteOld);
    }

    /**
     * Instantiate a self-updater for a normal resource.
     *
     * @param initiator  The player that initiated the update (set to null if there is none).
     * @param resourceId The ID of the plugin on Spigot found in the url after the name.
     * @param locale     The locale file you want containing custom messages. Note most messages will be followed with a progress indicator like [DOWNLOADING].
     * @param deleteOld  Should the old version of the plugin be deleted and disabled.
     * @param endTask    Runnable that will run once the update has completed.
     * @return The updater object for you to update yourself with.
     */
    public Updater createSelfUpdater(Player initiator, int resourceId, UpdateLocale locale, boolean deleteOld, UpdaterRunnable endTask) {
        return new Updater(plugin, initiator, resourceId, locale, deleteOld, endTask);
    }
}
