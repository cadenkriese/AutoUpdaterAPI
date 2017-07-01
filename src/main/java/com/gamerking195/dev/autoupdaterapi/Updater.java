package com.gamerking195.dev.autoupdaterapi;

import com.gamerking195.dev.autoupdaterapi.util.UtilPlugin;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.*;
import java.lang.reflect.Type;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.Charset;

public class Updater {
    private Player initiator;

    private JavaPlugin plugin;

    private String dataFolderPath;
    private String currentVersion;
    private String url;
    private String pluginName;

    private UpdateLocale locale;

    private boolean deleteUpdater;
    private boolean deleteOld;

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
    public Updater(Player initiator, JavaPlugin plugin, int resourceId, UpdateLocale locale, boolean deleteUpdater, boolean deleteOld) {
        dataFolderPath = AutoUpdaterAPI.getInstance().getDataFolder().getPath();
        currentVersion = plugin.getDescription().getVersion();
        pluginName = locale.getPluginName();
        url = "https://api.spiget.org/v2/resources/" + resourceId;
        this.plugin = plugin;
        this.initiator = initiator;
        this.locale = locale;
        this.deleteUpdater = deleteUpdater;
        this.deleteOld = deleteOld;
    }

    /**
     * Pings spigot to retrieve the latest version of a plugin.
     *
     * @return The latest version of the plugin as a string.
     */
    public String getLatestVersion() {
        Gson gson = new Gson();

        try {
            String latestVersion = readFrom(url + "/versions/latest");
            Type type = new TypeToken<JsonObject>() {
            }.getType();
            JsonObject object = gson.fromJson(latestVersion, type);

            if (object.get("error") != null) {
                AutoUpdaterAPI.getInstance().printPluginError("Error occurred while retrieving resource info from Spiget.", object.get("error").getAsString());
                sendActionBarSync(initiator, locale.getUpdateFailed().replace("%plugin%", pluginName).replace("%old_version%", currentVersion).replace("%new_version%", "&4NULL"));
                delete();
            }

            return object.get("name").getAsString();
        } catch (Exception exception) {
            AutoUpdaterAPI.getInstance().printError(exception);
            sendActionBarSync(initiator, locale.getUpdateFailed().replace("%plugin%", pluginName).replace("%old_version%", currentVersion).replace("%new_version%", "&4NULL"));
        }

        return "";
    }

    public void update() {
        String newVersion = getLatestVersion();

        if (!newVersion.equalsIgnoreCase(currentVersion)) {
            sendActionBarSync(initiator, locale.getUpdating().replace("%plugin%", pluginName).replace("%old_version%", currentVersion).replace("%new_version%", newVersion) + " &8[RETRIEVING FILES]");
            try {
                if (deleteOld) {
                    if (!new File(plugin.getClass().getProtectionDomain().getCodeSource().getLocation().toURI().getPath()).delete())
                        AutoUpdaterAPI.getInstance().printPluginError("Error occurred while updating " + pluginName + ".", "Could not deleteUpdater old plugin jar.");

                    UtilPlugin.unload(plugin);
                }

                new BukkitRunnable() {
                    @Override
                    public void run() {
                        try {
                            URL downloadUrl = new URL(url + "/download");
                            HttpURLConnection httpConnection = (HttpURLConnection) downloadUrl.openConnection();
                            httpConnection.setRequestProperty("User-Agent", "SpigetResourceUpdater");
                            long completeFileSize = httpConnection.getContentLength();

                            java.io.BufferedInputStream in = new java.io.BufferedInputStream(httpConnection.getInputStream());
                            java.io.FileOutputStream fos = new java.io.FileOutputStream(new File(dataFolderPath.substring(0, dataFolderPath.lastIndexOf("/")) + "/" + locale.getFileName() + ".jar"));
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

                                    sendActionBar(initiator, locale.getUpdatingDownload().replace("%plugin%", pluginName).replace("%old_version%", currentVersion).replace("%new_version%", newVersion).replace("%download_bar%", bar).replace("%download_percent%", currentPercent + "%") + " &8[DOWNLOADING]");
                                }

                                bout.write(data, 0, x);
                            }

                            bout.close();
                            in.close();

                            new BukkitRunnable() {
                                @Override
                                public void run() {
                                    try {
                                        sendActionBarSync(initiator, locale.getUpdating().replace("%plugin%", pluginName).replace("%old_version%", currentVersion).replace("%new_version%", newVersion) + " &8[INITIALIZING]");

                                        Bukkit.getPluginManager().loadPlugin(new File(dataFolderPath.substring(0, dataFolderPath.lastIndexOf("/")) + "/" + locale.getFileName() + ".jar"));
                                        Bukkit.getPluginManager().enablePlugin(Bukkit.getPluginManager().getPlugin(pluginName));

                                        sendActionBarSync(initiator, locale.getUpdateComplete().replace("%plugin%", pluginName).replace("%old_version%", currentVersion).replace("%new_version%", newVersion));

                                        delete();

                                        AutoUpdaterAPI.getInstance().resourceUpdated();
                                    } catch(Exception ex) {
                                        AutoUpdaterAPI.getInstance().printError(ex, "Error occurred while initializing " + pluginName + ".");
                                        sendActionBarSync(initiator, locale.getUpdateFailed().replace("%plugin%", pluginName).replace("%old_version%", currentVersion).replace("%new_version%", newVersion));
                                        delete();
                                    }
                                }
                            }.runTask(AutoUpdaterAPI.getInstance());

                        } catch (Exception ex) {
                            AutoUpdaterAPI.getInstance().printError(ex, "Error occurred while updating " + pluginName + ".");
                            sendActionBar(initiator, locale.getUpdateFailed().replace("%plugin%", pluginName).replace("%old_version%", currentVersion).replace("%new_version%", newVersion));
                            delete();
                        }
                    }
                }.runTaskAsynchronously(AutoUpdaterAPI.getInstance());

            } catch (Exception ex) {
                AutoUpdaterAPI.getInstance().printError(ex, "Error occurred while updating " + pluginName + ".");
                sendActionBarSync(initiator, locale.getUpdateFailed().replace("%plugin%", pluginName).replace("%old_version%", currentVersion).replace("%new_version%", newVersion));
                delete();
            }
        } else {
            AutoUpdaterAPI.getInstance().printPluginError("Error occurred while updating " + pluginName + "!", "Plugin is up to date!");
            sendActionBarSync(initiator, locale.getUpdateFailed().replace("%plugin%", pluginName).replace("%old_version%", currentVersion).replace("%new_version%", newVersion) + " [PLUGIN IS UP TO DATE]");
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

    private String readFrom(String url) throws IOException {
        try (InputStream is = new URL(url).openStream()) {
            BufferedReader rd = new BufferedReader(new InputStreamReader(is, Charset.forName("UTF-8")));

            StringBuilder sb = new StringBuilder();
            int cp;
            while ((cp = rd.read()) != -1) {
                sb.append((char) cp);
            }
            return sb.toString();
        }
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
