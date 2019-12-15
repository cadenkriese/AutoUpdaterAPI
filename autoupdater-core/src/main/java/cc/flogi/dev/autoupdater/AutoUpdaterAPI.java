package cc.flogi.dev.autoupdater;

import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.IOException;

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
     *
     * @apiNote If you do not plan on updating premium plugins make sure premiumSupport is set to false,
     * it will create a ton of overhead logic you don't want getting in the way of a relatively simple task.
     */
    public AutoUpdaterAPI(JavaPlugin plugin, boolean premiumSupport) {
        new InternalCore(plugin);

        if (premiumSupport) {
            UtilThreading.async(() -> {
                try {
                    UtilLibraries.downloadPremiumSupport();
                    PremiumUpdater.init(plugin);
                } catch (IOException ex) {
                    InternalCore.get().printError(ex);
                }
            });
        }
    }

    /**
     * Resets the current user used in {@link PremiumUpdater}
     *
     * @apiNote Requires premiumSupport to be set to true on startup.
     */
    public static void resetUser() {
        PremiumUpdater.resetUser();
    }

    /**
     * Prompts a player to login to their Spigot account and encrypts their credentials.
     *
     * @param player The player to prompt for their login info.
     *
     * @apiNote Requires premiumSupport to be set to true on startup.
     * @apiNote This method will make minecraft version sensitive calls, please ensure that the version you're working on is supported by {@link net.wesjd.anvilgui.AnvilGUI}.
     */
    public static void promptLogin(Player player) {
        new PremiumUpdater(null, null, 1, new UpdateLocale(), false).authenticate(false);
    }

    /**
     * Instantiate a {@link PublicUpdater}.
     *
     * @param plugin    The plugin that should be updated.
     * @param initiator The player that initiated the update (set to null if there is none).
     * @param url       The URL where the jar can be downloaded from.
     * @param locale    The locale file you want containing custom messages. Note most messages will be followed with a progress indicator like [DOWNLOADING].
     * @param replace   Should the old version of the plugin be deleted and disabled.
     *
     * @return An instantiated {@link PublicUpdater}.
     */
    public PublicUpdater createPublicUpdater(Plugin plugin, Player initiator, String url, UpdateLocale locale, boolean replace) {
        return new PublicUpdater(plugin, initiator, url, locale, replace);
    }

    /**
     * Instantiate a {@link PublicUpdater}.
     *
     * @param plugin    The plugin that should be updated.
     * @param initiator The player that initiated the update (set to null if there is none).
     * @param url       The URL where the jar can be downloaded from.
     * @param locale    The locale file you want containing custom messages. Note most messages will be followed with a progress indicator like [DOWNLOADING].
     * @param replace   Should the old version of the plugin be deleted and disabled.
     * @param endTask   Runnable that will run once the update has completed.
     *
     * @return An instantiated {@link PublicUpdater}.
     */
    public PublicUpdater createPublicUpdater(Plugin plugin, Player initiator, String url, UpdateLocale locale, boolean replace, UpdaterRunnable endTask) {
        return new PublicUpdater(plugin, initiator, url, locale, replace, endTask);
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
     *
     * @return An instantiated {@link PublicSpigotUpdater}.
     */
    public PublicSpigotUpdater createSpigotUpdater(Plugin plugin, Player initiator, int resourceId, UpdateLocale locale, boolean replace) {
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
     *
     * @return An instantiated {@link PublicSpigotUpdater}.
     */
    public PublicSpigotUpdater createSpigotUpdater(Plugin plugin, Player initiator, int resourceId, UpdateLocale locale, boolean replace, UpdaterRunnable endTask) {
        return new PublicSpigotUpdater(plugin, initiator, resourceId, locale, replace, endTask);
    }

    /**
     * Instantiate a {@link PremiumUpdater}.
     *
     * @param initiator  The player that started this action (if there is none set to null).
     * @param plugin     The instance of the outdated plugin.
     * @param resourceId The ID of the plugin on Spigot found in the url after the name.
     * @param locale     The locale file you want containing custom messages. Note most messages will be followed with a progress indicator like [DOWNLOADING].
     * @param replace    Should the old version of the plugin be deleted and disabled.
     *
     * @return An instantiated {@link PremiumUpdater}.
     * @apiNote Requires premiumSupport to be set to true on startup.
     * @since 3.0.1
     */
    public PremiumUpdater createPremiumUpdater(Plugin plugin, Player initiator, int resourceId, UpdateLocale locale, boolean replace) {
        return new PremiumUpdater(initiator, plugin, resourceId, locale, replace);
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
     *
     * @return An instantiated {@link PremiumUpdater}.
     * @apiNote Requires premiumSupport to be set to true on startup.
     * @since 3.0.1
     */
    public PremiumUpdater createPremiumUpdater(Plugin plugin, Player initiator, int resourceId, UpdateLocale locale, boolean replace, UpdaterRunnable endTask) {
        return new PremiumUpdater(initiator, plugin, resourceId, locale, replace, endTask);
    }
}
