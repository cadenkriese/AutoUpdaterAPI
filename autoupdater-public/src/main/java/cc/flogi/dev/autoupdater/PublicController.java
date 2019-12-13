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
public class PublicController {
    @Getter private static Plugin plugin;
    @Getter private static AutoUpdaterAPI updaterAPI;

    public PublicController(JavaPlugin javaPlugin) {
        plugin = javaPlugin;
        updaterAPI = new AutoUpdaterAPI(javaPlugin);
    }

    /**
     * Instantiate an updater for a normal resource.
     *
     * @param plugin     The plugin that should be updated.
     * @param initiator  The player that initiated the update (set to null if there is none).
     * @param url        The URL where the jar can be downloaded from.
     * @param locale     The locale file you want containing custom messages. Note most messages will be followed with a progress indicator like [DOWNLOADING].
     * @param replace    Should the old version of the plugin be deleted and disabled.
     *
     * @return The updater object for you to update the plugin with.
     */
    public PublicUpdater createPublicUpdater(Plugin plugin, Player initiator, String url, UpdateLocale locale, boolean replace) {
        return new PublicUpdater(plugin, initiator, url, locale, replace);
    }

    /**
     * Instantiate an updater for a normal resource.
     *
     * @param plugin     The plugin that should be updated.
     * @param initiator  The player that initiated the update (set to null if there is none).
     * @param url        The URL where the jar can be downloaded from.
     * @param locale     The locale file you want containing custom messages. Note most messages will be followed with a progress indicator like [DOWNLOADING].
     * @param replace    Should the old version of the plugin be deleted and disabled.
     * @param endTask    Runnable that will run once the update has completed.
     *
     * @return The updater object for you to update the plugin with.
     */
    public PublicUpdater createPublicUpdater(Plugin plugin, Player initiator, String url, UpdateLocale locale, boolean replace, UpdaterRunnable endTask) {
        return new PublicUpdater(plugin, initiator, url, locale, replace, endTask);
    }

    /**
     * Instantiate an updater for a normal resource.
     *
     * @param plugin     The plugin that should be updated.
     * @param initiator  The player that initiated the update (set to null if there is none).
     * @param resourceId The ID of the plugin on Spigot found in the url after the name.
     * @param locale     The locale file you want containing custom messages. Note most messages will be followed with a progress indicator like [DOWNLOADING].
     * @param replace    Should the old version of the plugin be deleted and disabled.
     *
     * @return The updater object for you to update the plugin with.
     */
    public PublicUpdater createSpigotUpdater(Plugin plugin, Player initiator, int resourceId, UpdateLocale locale, boolean replace) {
        return new PublicSpigotUpdater(plugin, initiator, resourceId, locale, replace);
    }

    /**
     * Instantiate an updater for a normal resource.
     *
     * @param plugin     The plugin that should be updated (If updating yourself).
     * @param initiator  The player that initiated the update (set to null if there is none).
     * @param resourceId The ID of the plugin on Spigot found in the url after the name.
     * @param locale     The locale file you want containing custom messages. Note most messages will be followed with a progress indicator like [DOWNLOADING].
     * @param replace    Should the old version of the plugin be deleted and disabled.
     * @param endTask    Runnable that will run once the update has completed.
     *
     * @return The updater object for you to update the plugin with.
     */
    public PublicUpdater createSpigotUpdater(Plugin plugin, Player initiator, int resourceId, UpdateLocale locale, boolean replace, UpdaterRunnable endTask) {
        return new PublicSpigotUpdater(plugin, initiator, resourceId, locale, replace, endTask);
    }
}
