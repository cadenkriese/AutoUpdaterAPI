package cc.flogi.dev.autoupdater.internal;

import cc.flogi.dev.autoupdater.api.PluginUpdater;
import cc.flogi.dev.autoupdater.api.UpdateLocale;
import cc.flogi.dev.autoupdater.api.UpdaterRunnable;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
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
        new AutoUpdaterInternal(plugin);

        if (premiumSupport) {
            UtilThreading.async(() -> {
                try {
                    UtilLibraries.downloadPremiumSupport(new File(AutoUpdaterInternal.getDataFolder().getPath() + "/libs/"));
                    PremiumSpigotPluginUpdater.init(plugin);
                } catch (IOException ex) {
                    AutoUpdaterInternal.get().printError(ex, "Error occurred while setting up support for premium resources.");
                }
            });
        }
    }

    /**
     * Places cached plugins in the plugins' folder but does not enable them
     *
     * @throws IOException If files are corrupt.
     * @apiNote You likely want to call this on your ${@link JavaPlugin#onDisable()}
     */
    public static void applyCachedUpdates() throws IOException {
        if (!AutoUpdaterInternal.getCacheFolder().exists())
            return;

        File cacheFolder = AutoUpdaterInternal.getCacheFolder();

        HashMap<File, File> updates = new HashMap<>();

        Arrays.stream(cacheFolder.listFiles())
                .filter(f -> !f.getName().endsWith(".meta"))
                .forEach(f -> updates.put(f, new File(cacheFolder, f.getName() + ".meta")));

        for (File file : updates.keySet()) {
            File meta = updates.get(file);

            Type type = new TypeToken<Map<String, String>>() {}.getType();
            Map<String, String> map = new Gson().fromJson(UtilIO.readFromFile(meta), type);
            if (Boolean.parseBoolean(map.get("replace")))
                new File(map.get("old-file")).delete();

            Files.move(file.toPath(), Paths.get(map.get("destination")));
            meta.delete();
        }

        Files.deleteIfExists(cacheFolder.toPath());
    }

    /**
     * Resets the current user used in {@link PremiumSpigotPluginUpdater}
     *
     * @apiNote Requires premiumSupport to be set to true on startup.
     * @since 3.0.1
     */
    public static void resetUser() {
        PremiumSpigotPluginUpdater.resetUser();
    }

    /**
     * Controls if the API should print large amounts of debug data.
     *
     * @param enabled Should debug logging be enabled or not.
     */
    public static void setDebug(boolean enabled) {
        AutoUpdaterInternal.DEBUG = enabled;

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
        AutoUpdaterInternal.METRICS = false;
    }

    /**
     * Prompts a player to login to their Spigot account and encrypts their credentials.
     *
     * @param player The player to prompt for their login info.
     * @apiNote Requires premiumSupport to be set to true on startup.
     * @apiNote This method will make minecraft version sensitive calls,
     * please ensure that the version you're working on is supported by {@link net.wesjd.anvilgui.AnvilGUI}.
     * @since 3.0.1
     */
    public static void promptLogin(Player player) {
        new PremiumSpigotPluginUpdater(null, null, 1, PluginUpdateLocale.builder().build(), false, false).authenticate(false);
    }

    /**
     * Instantiate a {@link cc.flogi.dev.autoupdater.internal.PublicPluginUpdater}.
     *
     * @param plugin     The plugin that should be updated.
     * @param initiator  The player that initiated the update (set to null if there is none).
     * @param url        The URL where the jar can be downloaded from.
     * @param initialize Should the plugin be initialized now or after a server restart.
     * @param locale     The locale file you want containing custom messages.
     *                   Note most messages will be followed with a progress indicator like [DOWNLOADING].
     * @param replace    Should the old version of the plugin be deleted and disabled.
     * @param newVersion The latest version of the resource.
     * @return An instantiated {@link cc.flogi.dev.autoupdater.internal.PublicPluginUpdater}.
     * @since 3.0.1
     */
    public PluginUpdater createPublicPluginUpdater(Plugin plugin, Player initiator, String url, UpdateLocale locale, boolean initialize, boolean replace, String newVersion) {
        return new PublicPluginUpdater(plugin, initiator, url, (PluginUpdateLocale) locale, initialize, replace, newVersion);
    }

    /**
     * Instantiate a {@link cc.flogi.dev.autoupdater.internal.PublicPluginUpdater}.
     *
     * @param plugin     The plugin that should be updated.
     * @param initiator  The player that initiated the update (set to null if there is none).
     * @param url        The URL where the jar can be downloaded from.
     * @param locale     The locale file you want containing custom messages.
     *                   Note most messages will be followed with a progress indicator like [DOWNLOADING].
     * @param initialize Should the plugin be initialized now or after a server restart.
     * @param replace    Should the old version of the plugin be deleted and disabled.
     * @param endTask    Runnable that will run once the update has completed.
     * @param newVersion The latest version of the resource.
     * @return An instantiated {@link cc.flogi.dev.autoupdater.internal.PublicPluginUpdater}.
     * @since 3.0.1
     */
    public PluginUpdater createPublicPluginUpdater(Plugin plugin, Player initiator, String url, UpdateLocale locale, boolean initialize, boolean replace, String newVersion, UpdaterRunnable endTask) {
        return new PublicPluginUpdater(plugin, initiator, url, (PluginUpdateLocale) locale, initialize, replace, newVersion, endTask);
    }

    /*
     * PREMIUM PLUGINS
     */

    /**
     * Instantiate a {@link PublicSpigotPluginUpdater}.
     *
     * @param plugin     The plugin that should be updated.
     * @param initiator  The player that initiated the update (set to null if there is none).
     * @param resourceId The ID of the plugin on Spigot found in the url after the name.
     * @param locale     The locale file you want containing custom messages.
     *                   Note most messages will be followed with a progress indicator like [DOWNLOADING].
     * @param initialize Should the plugin be initialized now or after a server restart.
     * @param replace    Should the old version of the plugin be deleted and disabled.
     * @return An instantiated {@link PublicSpigotPluginUpdater}.
     * @since 3.0.1
     */
    public PublicSpigotPluginUpdater createSpigotPluginUpdater(Plugin plugin, Player initiator, int resourceId, UpdateLocale locale, boolean initialize, boolean replace) {
        return new PublicSpigotPluginUpdater(plugin, initiator, resourceId, (PluginUpdateLocale) locale, initialize, replace);
    }

    /**
     * Instantiate a {@link PublicSpigotPluginUpdater}.
     *
     * @param plugin     The plugin that should be updated (If updating yourself).
     * @param initiator  The player that initiated the update (set to null if there is none).
     * @param resourceId The ID of the plugin on Spigot found in the url after the name.
     * @param locale     The locale file you want containing custom messages.
     *                   Note most messages will be followed with a progress indicator like [DOWNLOADING].
     * @param initialize Should the plugin be initialized now or after a server restart.
     * @param replace    Should the old version of the plugin be deleted and disabled.
     * @param endTask    Runnable that will run once the update has completed.
     * @return An instantiated {@link PublicSpigotPluginUpdater}.
     * @since 3.0.1
     */
    public PublicSpigotPluginUpdater createSpigotPluginUpdater(Plugin plugin, Player initiator, int resourceId, UpdateLocale locale, boolean initialize, boolean replace, UpdaterRunnable endTask) {
        return new PublicSpigotPluginUpdater(plugin, initiator, resourceId, (PluginUpdateLocale) locale, replace, initialize, endTask);
    }

    /**
     * Instantiate a {@link PremiumSpigotPluginUpdater}.
     *
     * @param initiator  The player that started this action (if there is none set to null).
     * @param plugin     The instance of the outdated plugin.
     * @param resourceId The ID of the plugin on Spigot found in the url after the name.
     * @param locale     The locale file you want containing custom messages.
     *                   Note most messages will be followed with a progress indicator like [DOWNLOADING].
     * @param initialize Should the plugin be initialized now or after a server restart.
     * @param replace    Should the old version of the plugin be deleted and disabled.
     * @return An instantiated {@link PremiumSpigotPluginUpdater}.
     * @apiNote Requires premiumSupport to be set to true on startup.
     * @since 3.0.1
     */
    public PluginUpdater createPremiumSpigotPluginUpdater(Plugin plugin, Player initiator, int resourceId, UpdateLocale locale, boolean initialize, boolean replace) {
        return new PremiumSpigotPluginUpdater(initiator, plugin, resourceId, (PluginUpdateLocale) locale, initialize, replace);
    }

    /**
     * Instantiate PremiumUpdater
     *
     * @param initiator  The player that started this action (if there is none set to null).
     * @param plugin     The instance of the outdated plugin.
     * @param resourceId The ID of the plugin on Spigot found in the url after the name.
     * @param locale     The locale file you want containing custom messages.
     *                   Note most messages will be followed with a progress indicator like [DOWNLOADING].
     * @param initialize Should the plugin be initialized now or after a server restart.
     * @param replace    Should the old version of the plugin be deleted and disabled.
     * @param endTask    Runnable that will run once the update has completed.
     * @return An instantiated {@link PremiumSpigotPluginUpdater}.
     * @apiNote Requires premiumSupport to be set to true on startup.
     * @since 3.0.1
     */
    public PluginUpdater createPremiumSpigotPluginUpdater(Plugin plugin, Player initiator, int resourceId, UpdateLocale locale, boolean initialize, boolean replace, UpdaterRunnable endTask) {
        return new PremiumSpigotPluginUpdater(initiator, plugin, resourceId, (PluginUpdateLocale) locale, initialize, replace, endTask);
    }

    /**
     * Creates a builder for you to construct your locale from.
     *
     * @return Builder class for a locale file.
     */
    public UpdateLocale.LocaleBuilder createLocaleBuilder() {
        return PluginUpdateLocale.builder();
    }
}
