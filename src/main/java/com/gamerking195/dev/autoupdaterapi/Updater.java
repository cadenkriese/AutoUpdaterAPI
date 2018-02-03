package com.gamerking195.dev.autoupdaterapi;

import com.gamerking195.dev.autoupdaterapi.util.UtilPlugin;
import com.gamerking195.dev.autoupdaterapi.util.UtilReader;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;

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

    private UpdaterRunnable endTask = (successful, ex) -> {};

    /**
     * Instantiate the updater for a regular resource.
     *
     * @param initiator     The player that initiated the update (set to null if there is none)
     * @param plugin        The outdated plugin.
     * @param resourceId    The ID of the plugin on Spigot found in the url after the name.
     * @param locale        The locale file you want containing custom messages. Note most messages will be followed with a progress indicator like [DOWNLOADING].
     * @param deleteUpdater Should the updater delete itself after the update fails / succeeds.
     * @param deleteOld     Should the old version of the plugin be deleted & disabled.
     */
    public Updater(Player initiator, Plugin plugin, int resourceId, UpdateLocale locale, boolean deleteUpdater, boolean deleteOld) {
        dataFolderPath = AutoUpdaterAPI.getInstance().getDataFolder().getPath();
        currentVersion = plugin.getDescription().getVersion();
        pluginName = locale.getPluginName().replace("%plugin%", plugin.getName()).replace("%old_version%", currentVersion);
        locale.setPluginName(locale.getPluginName().replace("%plugin%", plugin.getName()).replace("%old_version%", currentVersion));
        url = "https://api.spiget.org/v2/resources/" + resourceId;
        this.plugin = plugin;
        this.initiator = initiator;
        this.locale = locale;
        this.deleteUpdater = deleteUpdater;
        this.deleteOld = deleteOld;
        this.resourceId = String.valueOf(resourceId);
    }

    /**
     * Instantiate the updater for a regular resource.
     *
     * @param initiator     The player that initiated the update (set to null if there is none)
     * @param plugin        The outdated plugin.
     * @param resourceId    The ID of the plugin on Spigot found in the url after the name.
     * @param locale        The locale file you want containing custom messages. Note most messages will be followed with a progress indicator like [DOWNLOADING].
     * @param deleteUpdater Should the updater delete itself after the update fails / succeeds.
     * @param deleteOld     Should the old version of the plugin be deleted & disabled.
     * @param endTask       Runnable that will run once the update has completed.
     */
    public Updater(Player initiator, Plugin plugin, int resourceId, UpdateLocale locale, boolean deleteUpdater, boolean deleteOld, UpdaterRunnable endTask) {
        dataFolderPath = AutoUpdaterAPI.getInstance().getDataFolder().getPath();
        currentVersion = plugin.getDescription().getVersion();
        pluginName = locale.getPluginName().replace("%plugin%", plugin.getName()).replace("%old_version%", currentVersion);
        locale.setPluginName(locale.getPluginName().replace("%plugin%", plugin.getName()).replace("%old_version%", currentVersion));
        url = "https://api.spiget.org/v2/resources/" + resourceId;
        this.plugin = plugin;
        this.initiator = initiator;
        this.locale = locale;
        this.deleteUpdater = deleteUpdater;
        this.deleteOld = deleteOld;
        this.resourceId = String.valueOf(resourceId);
        this.endTask = endTask;
    }

    /**
     * Pings spigot to retrieve the latest version of a plugin.
     *
     * @return The latest version of the plugin as a string.
     */
    public String getLatestVersion() {
        try {
            return UtilReader.readFrom("https://api.spigotmc.org/legacy/update.php?resource="+resourceId);
        } catch (Exception exception) {
            AutoUpdaterAPI.getInstance().printError(exception);
            sendActionBarSync(initiator, locale.getUpdateFailed().replace("%plugin%", plugin.getName()).replace("%old_version%", currentVersion).replace("%new_version%", "&4NULL"));
        }

        return "";
    }

    public void update() {
        startingTime = System.currentTimeMillis();
        String newVersion = getLatestVersion();

        if (!newVersion.equalsIgnoreCase(currentVersion)) {

            pluginName = locale.getPluginName().replace("%plugin%", plugin.getName()).replace("%old_version%", currentVersion).replace("%new_version%", newVersion);
            locale.setFileName(locale.getFileName().replace("%plugin%", plugin.getName()).replace("%old_version%", currentVersion).replace("%new_version%", newVersion).replace(" ", "_"));

            sendActionBarSync(initiator, locale.getUpdating().replace("%plugin%", plugin.getName()).replace("%old_version%", currentVersion).replace("%new_version%", newVersion) + " &8[RETRIEVING FILES]");
            try {
                if (deleteOld) {
                    if (!new File(plugin.getClass().getProtectionDomain().getCodeSource().getLocation().toURI().getPath()).delete())
                        AutoUpdaterAPI.getInstance().printPluginError("Error occurred while updating " + pluginName + ".", "Could not delete old plugin jar.");

                    UtilPlugin.unload(plugin);
                }

                String s = AutoUpdaterAPI.getFileSeperator();

                new BukkitRunnable() {
                    @Override
                    public void run() {
                        try {
                            URL downloadUrl = new URL(url + "/download");
                            HttpURLConnection httpConnection = (HttpURLConnection) downloadUrl.openConnection();
                            httpConnection.setRequestProperty("User-Agent", "SpigetResourceUpdater");
                            long completeFileSize = httpConnection.getContentLength();

                            java.io.BufferedInputStream in = new java.io.BufferedInputStream(httpConnection.getInputStream());
                            java.io.FileOutputStream fos = new java.io.FileOutputStream(new File(dataFolderPath.substring(0, dataFolderPath.lastIndexOf(s)) + s + locale.getFileName() + ".jar"));
                            java.io.BufferedOutputStream bout = new BufferedOutputStream(fos, 1024);

                            byte[] data = new byte[1024];
                            long downloadedFileSize = 0;
                            int x;
                            while ((x = in.read(data, 0, 1024)) >= 0) {
                                downloadedFileSize += x;

                                if (downloadedFileSize % 5000 == 0) {
                                    final int currentProgress = (int) ((((double) downloadedFileSize) / ((double) completeFileSize)) * 15);

                                    final String currentPercent = String.format("%.2f", (((double) downloadedFileSize) / ((double) completeFileSize)) * 100);

                                    String bar = "&a:::::::::::::::";

                                    bar = bar.substring(0, currentProgress + 2) + "&c" + bar.substring(currentProgress + 2);

                                    sendActionBar(initiator, locale.getUpdatingDownload().replace("%plugin%", plugin.getName()).replace("%old_version%", currentVersion).replace("%new_version%", newVersion).replace("%download_bar%", bar).replace("%download_percent%", currentPercent + "%") + " &8[DOWNLOADING]");
                                }

                                bout.write(data, 0, x);
                            }

                            bout.close();
                            in.close();

                            new BukkitRunnable() {
                                @Override
                                public void run() {
                                    try {
                                        sendActionBarSync(initiator, locale.getUpdating().replace("%plugin%", plugin.getName()).replace("%old_version%", currentVersion).replace("%new_version%", newVersion) + " &8[INITIALIZING]");

                                        Bukkit.getPluginManager().loadPlugin(new File(dataFolderPath.substring(0, dataFolderPath.lastIndexOf(s)) + s + locale.getFileName() + ".jar"));
                                        Bukkit.getPluginManager().enablePlugin(Bukkit.getPluginManager().getPlugin(pluginName));

                                        endTask.run(true, null);
                                        double elapsedTimeSeconds = (double) (System.currentTimeMillis()-startingTime)/1000;
                                        sendActionBarSync(initiator, locale.getUpdateComplete().replace("%plugin%", plugin.getName()).replace("%old_version%", currentVersion).replace("%new_version%", newVersion).replace("%elapsed_time%", String.format("%.2f", elapsedTimeSeconds)));

                                        AutoUpdaterAPI.getInstance().resourceUpdated();

                                        delete();
                                    } catch(Exception ex) {
                                        AutoUpdaterAPI.getInstance().printError(ex, "Error occurred while initializing " + pluginName + ".");
                                        sendActionBarSync(initiator, locale.getUpdateFailed().replace("%plugin%", plugin.getName()).replace("%old_version%", currentVersion).replace("%new_version%", newVersion));
                                        endTask.run(false, ex);
                                        delete();
                                    }
                                }
                            }.runTask(AutoUpdaterAPI.getInstance());

                        } catch (Exception ex) {
                            AutoUpdaterAPI.getInstance().printError(ex, "Error occurred while updating " + pluginName + ".");
                            sendActionBar(initiator, locale.getUpdateFailed().replace("%plugin%", plugin.getName()).replace("%old_version%", currentVersion).replace("%new_version%", newVersion));
                            endTask.run(false, ex);
                            delete();
                        }
                    }
                }.runTaskAsynchronously(AutoUpdaterAPI.getInstance());

            } catch (Exception ex) {
                AutoUpdaterAPI.getInstance().printError(ex, "Error occurred while updating " + pluginName + ".");
                sendActionBarSync(initiator, locale.getUpdateFailed().replace("%plugin%", plugin.getName()).replace("%old_version%", currentVersion).replace("%new_version%", newVersion));
                endTask.run(false, ex);
                delete();
            }
        } else {
            AutoUpdaterAPI.getInstance().printPluginError("Error occurred while updating " + pluginName + "!", "Plugin is up to date!");
            sendActionBarSync(initiator, locale.getUpdateFailed().replace("%plugin%", plugin.getName()).replace("%old_version%", currentVersion).replace("%new_version%", newVersion) + " [PLUGIN IS UP TO DATE]");
            endTask.run(false, null);
            delete();
        }
    }

   /*
    * UTILITIES
    */

    //Fairly long method to call repetadly ¯\_(ツ)_/¯
    private void sendActionBar(Player player, String message) {
        new BukkitRunnable() {
            @Override
            public void run() {
                if (player != null) {
                    player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(ChatColor.translateAlternateColorCodes('&', message)));
                }
                if (AutoUpdaterAPI.getInstance().isDebug()) {
                    AutoUpdaterAPI.getInstance().getLogger().info(ChatColor.stripColor(ChatColor.translateAlternateColorCodes('&', message)));
                }

            }
        }.runTask(AutoUpdaterAPI.getInstance());
    }

    private void sendActionBarSync(Player player, String message) {
        if (player != null)
            player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(ChatColor.translateAlternateColorCodes('&', message)));
        if (AutoUpdaterAPI.getInstance().isDebug())
            AutoUpdaterAPI.getInstance().getLogger().info(ChatColor.stripColor(ChatColor.translateAlternateColorCodes('&', message)));
    }

    void delete() {
        if (deleteUpdater) {
            try {
                if (!new File(AutoUpdaterAPI.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath()).delete())
                    AutoUpdaterAPI.getInstance().printPluginError("Error occurred while updating " + pluginName + ".", "Could not delete updater jar.");

                UtilPlugin.unload(AutoUpdaterAPI.getInstance());
            } catch (Exception ex) {
                AutoUpdaterAPI.getInstance().printError(ex);
            }
        }
    }
}
