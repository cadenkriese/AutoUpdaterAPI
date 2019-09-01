package cc.flogi.dev.autoupdaterapi;

import cc.flogi.dev.autoupdaterapi.util.UtilPlugin;
import cc.flogi.dev.autoupdaterapi.util.UtilReader;
import cc.flogi.dev.autoupdaterapi.util.UtilUI;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author Caden Kriese (flogic)
 *
 * Created on 6/14/17
 */
public class Updater {
    private Player initiator;

    private Plugin plugin;

    private String dataFolderPath;
    private String currentVersion;
    private String url;
    private String resourceId;
    private String pluginName;

    private UpdateLocale locale;

    private boolean deleteUpdater;
    private boolean deleteOld;

    private long startingTime;

    private UpdaterRunnable endTask = (successful, ex, updatedPlugin, pluginName) -> {};

    /**
     * Instantiate the updater for a regular resource.
     *
     * @param initiator     The player that initiated the update (set to null if there is none)
     * @param plugin        The outdated plugin.
     * @param resourceId    The ID of the plugin on Spigot found in the url after the name.
     * @param locale        The locale file you want containing custom messages. Note most messages will be followed with a progress indicator like [DOWNLOADING].
     * @param deleteUpdater Should the updater delete itself after the update fails / succeeds.
     * @param deleteOld     Should the old version of the plugin be deleted and disabled.
     */
    public Updater(Player initiator, Plugin plugin, int resourceId, UpdateLocale locale, boolean deleteUpdater, boolean deleteOld) {
        dataFolderPath = AutoUpdaterAPI.get().getDataFolder().getPath();
        currentVersion = plugin.getDescription().getVersion();
        url = "https://api.spiget.org/v2/resources/" + resourceId;
        this.plugin = plugin;
        this.initiator = initiator;
        this.locale = locale;
        this.deleteUpdater = deleteUpdater;
        this.deleteOld = deleteOld;
        this.resourceId = String.valueOf(resourceId);

        if (locale.getPluginName() != null) {
            pluginName = locale.getPluginName().replace("%plugin%", plugin.getName()).replace("%old_version%", currentVersion);
            locale.setPluginName(locale.getPluginName().replace("%plugin%", plugin.getName()).replace("%old_version%", currentVersion));
        } else {
            pluginName = null;
        }
    }

    /**
     * Instantiate the updater for a regular resource.
     *
     * @param initiator     The player that initiated the update (set to null if there is none)
     * @param plugin        The outdated plugin.
     * @param resourceId    The ID of the plugin on Spigot found in the url after the name.
     * @param locale        The locale file you want containing custom messages. Note most messages will be followed with a progress indicator like [DOWNLOADING].
     * @param deleteUpdater Should the updater delete itself after the update fails / succeeds.
     * @param deleteOld     Should the old version of the plugin be deleted and disabled.
     * @param endTask       Runnable that will run once the update has completed.
     */
    public Updater(Player initiator, Plugin plugin, int resourceId, UpdateLocale locale, boolean deleteUpdater, boolean deleteOld, UpdaterRunnable endTask) {
        dataFolderPath = AutoUpdaterAPI.get().getDataFolder().getPath();
        currentVersion = plugin.getDescription().getVersion();
        url = "https://api.spiget.org/v2/resources/" + resourceId;
        this.plugin = plugin;
        this.initiator = initiator;
        this.locale = locale;
        this.deleteUpdater = deleteUpdater;
        this.deleteOld = deleteOld;
        this.resourceId = String.valueOf(resourceId);
        this.endTask = endTask;

        if (locale.getPluginName() != null) {
            pluginName = locale.getPluginName().replace("%plugin%", plugin.getName()).replace("%old_version%", currentVersion);
            locale.setPluginName(locale.getPluginName().replace("%plugin%", plugin.getName()).replace("%old_version%", currentVersion));
        } else {
            pluginName = null;
        }
    }

    /**
     * Pings spigot to retrieve the latest version of a plugin.
     *
     * @return The latest version of the plugin as a string.
     */
    public String getLatestVersion() {
        try {
            return UtilReader.readFrom("https://api.spigotmc.org/legacy/update.php?resource=" + resourceId);
        } catch (Exception exception) {
            AutoUpdaterAPI.get().printError(exception);
            UtilUI.sendActionBar(initiator, locale.getUpdateFailed().replace("%plugin%", plugin.getName()).replace("%old_version%", currentVersion).replace("%new_version%", "&4NULL"));
        }

        return "";
    }

    public void update() {
        startingTime = System.currentTimeMillis();
        String newVersion = getLatestVersion();

        if (!newVersion.equalsIgnoreCase(currentVersion)) {

            if (pluginName == null) {
                locale.setFileName(locale.getFileName().replace("%old_version%", currentVersion).replace("%new_version%", newVersion).replace(" ", "_"));
            } else {
                pluginName = locale.getPluginName().replace("%plugin%", plugin.getName()).replace("%old_version%", currentVersion).replace("%new_version%", newVersion);
                locale.setFileName(locale.getFileName().replace("%plugin%", plugin.getName()).replace("%old_version%", currentVersion).replace("%new_version%", newVersion).replace(" ", "_"));
            }

            UtilUI.sendActionBar(initiator, locale.getUpdating().replace("%plugin%", plugin.getName()).replace("%old_version%", currentVersion).replace("%new_version%", newVersion) + " &8[RETRIEVING FILES]");
            try {
                if (deleteOld) {
                    File pluginFile = new File(plugin.getClass().getProtectionDomain().getCodeSource().getLocation().toURI().getPath());

                    UtilPlugin.unload(plugin);

                    if (!pluginFile.delete())
                        AutoUpdaterAPI.get().printPluginError("Error occurred while updating " + pluginName + ".", "Could not delete old plugin jar.");
                }

                new BukkitRunnable() {
                    @Override
                    public void run() {
                        try {
                            URL downloadUrl = new URL(url + "/download");
                            HttpURLConnection httpConnection = (HttpURLConnection) downloadUrl.openConnection();
                            httpConnection.setRequestProperty("User-Agent", "SpigetResourceUpdater");
                            long completeFileSize = httpConnection.getContentLength();

                            int grabSize = 2048;

                            BufferedInputStream in = new BufferedInputStream(httpConnection.getInputStream());
                            FileOutputStream fos = new FileOutputStream(new File(dataFolderPath.substring(0, dataFolderPath.lastIndexOf("/")) + "/" + locale.getFileName() + ".jar"));
                            BufferedOutputStream bout = new BufferedOutputStream(fos, grabSize);

                            byte[] data = new byte[grabSize];
                            long downloadedFileSize = 0;
                            int grab;
                            while ((grab = in.read(data, 0, grabSize)) >= 0) {
                                downloadedFileSize += grab;

                                if (downloadedFileSize % (grabSize * 5) == 0) {
                                    String bar = UtilUI.progressBar(15, downloadedFileSize, completeFileSize, ':', ChatColor.RED, ChatColor.GREEN);
                                    final String currentPercent = String.format("%.2f", (((double) downloadedFileSize) / ((double) completeFileSize)) * 100);

                                    UtilUI.sendActionBar(initiator, locale.getUpdatingDownload().replace("%plugin%", plugin.getName()).replace("%old_version%", currentVersion).replace("%new_version%", newVersion).replace("%download_bar%", bar).replace("%download_percent%", currentPercent + "%") + " &8[DOWNLOADING]");
                                }

                                bout.write(data, 0, grab);
                            }

                            bout.close();
                            in.close();
                            fos.close();

                            new BukkitRunnable() {
                                @Override
                                public void run() {
                                    try {
                                        UtilUI.sendActionBar(initiator, locale.getUpdating().replace("%plugin%", plugin.getName()).replace("%old_version%", currentVersion).replace("%new_version%", newVersion) + " &8[INITIALIZING]");

                                        List<Plugin> beforePlugins = new ArrayList<>(Arrays.asList(Bukkit.getPluginManager().getPlugins()));

                                        Plugin updated = Bukkit.getPluginManager().loadPlugin(new File(dataFolderPath.substring(0, dataFolderPath.lastIndexOf("/")) + "/" + locale.getFileName() + ".jar"));

                                        if (pluginName == null) {
                                            List<Plugin> afterPlugins = new ArrayList<>(Arrays.asList(Bukkit.getPluginManager().getPlugins()));
                                            afterPlugins.removeAll(beforePlugins);
                                            pluginName = afterPlugins.get(0).getName();
                                        }

                                        Bukkit.getPluginManager().enablePlugin(updated);

                                        endTask.run(true, null, updated, pluginName);
                                        double elapsedTimeSeconds = (double) (System.currentTimeMillis() - startingTime) / 1000;
                                        UtilUI.sendActionBar(initiator, locale.getUpdateComplete().replace("%plugin%", plugin.getName()).replace("%old_version%", currentVersion).replace("%new_version%", newVersion).replace("%elapsed_time%", String.format("%.2f", elapsedTimeSeconds)));
                                    } catch (Exception ex) {
                                        AutoUpdaterAPI.get().printError(ex, "Error occurred while initializing " + pluginName + ".");
                                        UtilUI.sendActionBar(initiator, locale.getUpdateFailed().replace("%plugin%", plugin.getName()).replace("%old_version%", currentVersion).replace("%new_version%", newVersion));
                                        if (pluginName != null)
                                            endTask.run(false, ex, Bukkit.getPluginManager().getPlugin(pluginName), pluginName);
                                    }
                                }
                            }.runTask(AutoUpdaterAPI.getPlugin());

                        } catch (Exception ex) {
                            AutoUpdaterAPI.get().printError(ex, "Error occurred while updating " + pluginName + ".");
                            UtilUI.sendActionBar(initiator, locale.getUpdateFailed().replace("%plugin%", plugin.getName()).replace("%old_version%", currentVersion).replace("%new_version%", newVersion));
                            if (pluginName != null)
                                endTask.run(false, ex, Bukkit.getPluginManager().getPlugin(pluginName), pluginName);
                        }
                    }
                }.runTaskAsynchronously(AutoUpdaterAPI.getPlugin());

            } catch (Exception ex) {
                AutoUpdaterAPI.get().printError(ex, "Error occurred while updating " + pluginName + ".");
                UtilUI.sendActionBar(initiator, locale.getUpdateFailed().replace("%plugin%", plugin.getName()).replace("%old_version%", currentVersion).replace("%new_version%", newVersion));
                if (pluginName != null)
                    endTask.run(false, ex, Bukkit.getPluginManager().getPlugin(pluginName), pluginName);
            }
        } else {
            AutoUpdaterAPI.get().printPluginError("Error occurred while updating " + pluginName + "!", "Plugin is up to date!");
            UtilUI.sendActionBar(initiator, locale.getUpdateFailed().replace("%plugin%", plugin.getName()).replace("%old_version%", currentVersion).replace("%new_version%", newVersion) + " [PLUGIN IS UP TO DATE]");
            endTask.run(false, null, Bukkit.getPluginManager().getPlugin(pluginName), pluginName);
        }
    }
}
