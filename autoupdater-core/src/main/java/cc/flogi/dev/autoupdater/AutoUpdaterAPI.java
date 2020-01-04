package cc.flogi.dev.autoupdater;

import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * AutoUpdaterAPI
 *
 * @author Caden Kriese (flogic)
 *
 * This resource is licensed under the Apache License Version 2.0.
 * Full license information in the LICENSE file.
 */
public class AutoUpdaterAPI {
    /**
     * Instantiates the API.
     *
     * @param plugin         The plugin running the API.
     * @param premiumSupport Should this API instance support premium plugins.
     * @apiNote If you do not plan on updating premium plugins make sure premiumSupport is set to false,
     * it will create a ton of overhead logic you don't want getting in the way of a relatively simple task.
     */
    public AutoUpdaterAPI(JavaPlugin plugin, boolean premiumSupport) {
        new InternalCore(plugin);

        if (premiumSupport) {
            UtilThreading.async(() -> {
                try {
                    UtilLibraries.downloadPremiumSupport(new File(InternalCore.getDataFolder().getPath() + "/libs/"));
                    PremiumSpigotUpdater.init(plugin);
                } catch (IOException ex) {
                    InternalCore.get().printError(ex, "Error occurred while setting up support for premium resources.");
                }
            });
        }
    }

    /**
     * Resets the current user used in {@link PremiumSpigotUpdater}
     *
     * @apiNote Requires premiumSupport to be set to true on startup.
     * @since 3.0.1
     */
    public static void resetUser() {
        PremiumSpigotUpdater.resetUser();
    }

    /**
     * Controls if the API should print large amounts of debug data.
     *
     * @param enabled Should debug logging be enabled or not.
     */
    public static void setDebug(boolean enabled) {
        InternalCore.DEBUG = enabled;

        if (enabled) {
            Logger.getLogger("org.apache.commons.httpclient").setLevel(Level.INFO);
            Logger.getLogger("com.gargoylesoftware").setLevel(Level.INFO);
        }
    }

    /**
     * Disable internal metrics reporting from the API.
     *
     * Note that all metrics reporting are anonymous and web requests
     * made have a minimal impact on performance.
     */
    public static void disableMetrics() {
        InternalCore.METRICS = false;
    }

    /**
     * Prompts a player to login to their Spigot account and encrypts their credentials.
     *
     * @param player The player to prompt for their login info.
     * @apiNote Requires premiumSupport to be set to true on startup.
     * @apiNote This method will make minecraft version sensitive calls, please ensure that the version you're working on is supported by {@link net.wesjd.anvilgui.AnvilGUI}.
     * @since 3.0.1
     */
    public static void promptLogin(Player player) {
        new PremiumSpigotUpdater(null, null, 1, new UpdateLocale(), false).authenticate(false);
    }

    /**
     * Instantiate a {@link PublicUpdater}.
     *
     * @param plugin     The plugin that should be updated.
     * @param initiator  The player that initiated the update (set to null if there is none).
     * @param url        The URL where the jar can be downloaded from.
     * @param locale     The locale file you want containing custom messages. Note most messages will be followed with a progress indicator like [DOWNLOADING].
     * @param replace    Should the old version of the plugin be deleted and disabled.
     * @param newVersion The latest version of the resource.
     * @return An instantiated {@link PublicUpdater}.
     * @since 3.0.1
     */
    public Updater createPublicUpdater(Plugin plugin, Player initiator, String url, UpdateLocale locale, boolean replace, String newVersion) {
        return new PublicUpdater(plugin, initiator, url, locale, replace, newVersion);
    }

    /**
     * Instantiate a {@link PublicUpdater}.
     *
     * @param plugin     The plugin that should be updated.
     * @param initiator  The player that initiated the update (set to null if there is none).
     * @param url        The URL where the jar can be downloaded from.
     * @param locale     The locale file you want containing custom messages. Note most messages will be followed with a progress indicator like [DOWNLOADING].
     * @param replace    Should the old version of the plugin be deleted and disabled.
     * @param endTask    Runnable that will run once the update has completed.
     * @param newVersion The latest version of the resource.
     * @return An instantiated {@link PublicUpdater}.
     * @since 3.0.1
     */
    public Updater createPublicUpdater(Plugin plugin, Player initiator, String url, UpdateLocale locale, boolean replace, String newVersion, UpdaterRunnable endTask) {
        return new PublicUpdater(plugin, initiator, url, locale, replace, newVersion, endTask);
    }

    /*
     * PREMIUM PLUGINS
     */

    /**
     * Instantiate a {@link PublicSpigotUpdater}.
     *
     * @param plugin     The plugin that should be updated.
     * @param initiator  The player that initiated the update (set to null if there is none).
     * @param resourceId The ID of the plugin on Spigot found in the url after the name.
     * @param locale     The locale file you want containing custom messages. Note most messages will be followed with a progress indicator like [DOWNLOADING].
     * @param replace    Should the old version of the plugin be deleted and disabled.
     * @return An instantiated {@link PublicSpigotUpdater}.
     * @since 3.0.1
     */
    public Updater createSpigotUpdater(Plugin plugin, Player initiator, int resourceId, UpdateLocale locale, boolean replace) {
        return new PublicSpigotUpdater(plugin, initiator, resourceId, locale, replace);
    }

    /**
     * Instantiate a {@link PublicSpigotUpdater}.
     *
     * @param plugin     The plugin that should be updated (If updating yourself).
     * @param initiator  The player that initiated the update (set to null if there is none).
     * @param resourceId The ID of the plugin on Spigot found in the url after the name.
     * @param locale     The locale file you want containing custom messages. Note most messages will be followed with a progress indicator like [DOWNLOADING].
     * @param replace    Should the old version of the plugin be deleted and disabled.
     * @param endTask    Runnable that will run once the update has completed.
     * @return An instantiated {@link PublicSpigotUpdater}.
     * @since 3.0.1
     */
    public Updater createSpigotUpdater(Plugin plugin, Player initiator, int resourceId, UpdateLocale locale, boolean replace, UpdaterRunnable endTask) {
        return new PublicSpigotUpdater(plugin, initiator, resourceId, locale, replace, endTask);
    }

    /**
     * Instantiate a {@link PremiumSpigotUpdater}.
     *
     * @param initiator  The player that started this action (if there is none set to null).
     * @param plugin     The instance of the outdated plugin.
     * @param resourceId The ID of the plugin on Spigot found in the url after the name.
     * @param locale     The locale file you want containing custom messages. Note most messages will be followed with a progress indicator like [DOWNLOADING].
     * @param replace    Should the old version of the plugin be deleted and disabled.
     * @return An instantiated {@link PremiumSpigotUpdater}.
     * @apiNote Requires premiumSupport to be set to true on startup.
     * @since 3.0.1
     */
    public Updater createPremiumSpigotUpdater(Plugin plugin, Player initiator, int resourceId, UpdateLocale locale, boolean replace) {
        return new PremiumSpigotUpdater(initiator, plugin, resourceId, locale, replace);
    }

    /**
     * Instantiate PremiumUpdater
     *
     * @param initiator  The player that started this action (if there is none set to null).
     * @param plugin     The instance of the outdated plugin.
     * @param resourceId The ID of the plugin on Spigot found in the url after the name.
     * @param locale     The locale file you want containing custom messages. Note most messages will be followed with a progress indicator like [DOWNLOADING].
     * @param replace    Should the old version of the plugin be deleted and disabled.
     * @param endTask    Runnable that will run once the update has completed.
     * @return An instantiated {@link PremiumSpigotUpdater}.
     * @apiNote Requires premiumSupport to be set to true on startup.
     * @since 3.0.1
     */
    public Updater createPremiumSpigotUpdater(Plugin plugin, Player initiator, int resourceId, UpdateLocale locale, boolean replace, UpdaterRunnable endTask) {
        return new PremiumSpigotUpdater(initiator, plugin, resourceId, locale, replace, endTask);
    }
}
