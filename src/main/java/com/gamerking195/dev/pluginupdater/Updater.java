package com.gamerking195.dev.pluginupdater;

import com.gamerking195.dev.pluginupdater.util.UtilPlugin;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.*;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.SimpleCommandMap;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.RegisteredListener;
import org.bukkit.plugin.java.JavaPlugin;

import java.awt.*;
import java.io.*;
import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.Charset;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;

public class Updater {
    private Player initiater;

    private JavaPlugin plugin;

    private String dataFolderPath;
    private String currentVersion;
    private String url;
    private String pluginName;

    private UpdateLocale locale;

    private boolean delete;

    public Updater(Player initiater, JavaPlugin plugin, int spigotId, UpdateLocale locale, boolean delete) {
        dataFolderPath = Main.getInstance().getDataFolder().getPath();
        currentVersion = plugin.getDescription().getVersion();
        pluginName = plugin.getName();
        url = "https://api.spiget.org/v2/resources/"+spigotId;
        this.plugin = plugin;
        this.initiater = initiater;
        this.locale = locale;
        this.delete = delete;
    }

    public Updater(JavaPlugin plugin, int spigotId, String fileName, boolean delete) {
        dataFolderPath = Main.getInstance().getDataFolder().getPath();
        currentVersion = plugin.getDescription().getVersion();
        pluginName = plugin.getName();
        url = "https://api.spiget.org/v2/resources/"+spigotId;
        this.plugin = plugin;

        locale = new UpdateLocale();
        locale.fileName = fileName;
        this.delete = delete;
    }

    public String getLatestVersion() {
        Gson gson = new Gson();

        try {
            String latestVersion = readFrom(url+"/versions/latest");
            Type type = new TypeToken<JsonObject>() {
            }.getType();
            JsonObject object = gson.fromJson(latestVersion, type);

            return object.get("name").getAsString();
        } catch (Exception exception) {
            Main.getInstance().printError(exception);
            sendActionBar(initiater, locale.updateFailed.replace("%plugin%", pluginName).replace("%old_version%", currentVersion).replace("%new_version%", getLatestVersion()));
        }

        return "";
    }

    public void update() {
        if (!getLatestVersion().equalsIgnoreCase(currentVersion)) {

            String newVersion = getLatestVersion();

            sendActionBar(initiater, locale.updating.replace("%plugin%", pluginName).replace("%old_version%", currentVersion).replace("%new_version%", newVersion)+" &8[RETREIVING FILES]");
            try {
                    if (!new File(plugin.getClass().getProtectionDomain().getCodeSource().getLocation().toURI().getPath()).delete())
                        Main.getInstance().printPluginError("Error occured while updating " + pluginName + ".", "Could not delete old plugin jar.");

                    UtilPlugin.unload(plugin);

                //Download new plugin
                URL url = new URL(this.url+"/download");
                HttpURLConnection httpConnection = (HttpURLConnection) url.openConnection();
                httpConnection.setRequestProperty("User-Agent", "SpigetResourceUpdater");
                long completeFileSize = httpConnection.getContentLength();

                java.io.BufferedInputStream in = new java.io.BufferedInputStream(httpConnection.getInputStream());
                java.io.FileOutputStream fos = new java.io.FileOutputStream(new File(dataFolderPath.substring(0, dataFolderPath.lastIndexOf("/")) + "/"+locale.fileName+".jar"));
                java.io.BufferedOutputStream bout = new BufferedOutputStream(fos, 1024);

                byte[] data = new byte[1024];
                long downloadedFileSize = 0;
                int x = 0;
                while ((x = in.read(data, 0, 1024)) >= 0) {
                    downloadedFileSize += x;

                    final int currentProgress = (int) ((((double) downloadedFileSize) / ((double) completeFileSize)) * 15);

                    final String currentPercent = String.format("%.2f", (((double) downloadedFileSize) / ((double) completeFileSize)) * 100);

                    String bar = "&a:::::::::::::::";

                    bar = bar.substring(0, currentProgress + 2) + "&c" + bar.substring(currentProgress + 2);

                    sendActionBar(initiater, locale.updatingDownload.replace("%plugin%", pluginName).replace("%old_version%", currentVersion).replace("%new_version%", newVersion).replace("%download_bar%", bar).replace("%download_percent%", currentPercent+"%")+" &8[DOWNLOADING]");

                    bout.write(data, 0, x);
                }

                bout.close();
                in.close();

                sendActionBar(initiater, locale.updating.replace("%plugin%", pluginName).replace("%old_version%", currentVersion).replace("%new_version%", newVersion) + " &8[INITIALIZING]");

                Bukkit.getPluginManager().loadPlugin(new File(dataFolderPath.substring(0, dataFolderPath.lastIndexOf("/")) + "/"+locale.fileName+".jar"));
                Bukkit.getPluginManager().enablePlugin(Bukkit.getPluginManager().getPlugin(pluginName));

                sendActionBar(initiater, locale.updateComplete.replace("%plugin%", pluginName).replace("%old_version%", currentVersion).replace("%new_version%", newVersion));

                if (delete) {
                    if (!new File(Main.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath()).delete())
                        Main.getInstance().printPluginError("Error occured while updating "+pluginName+".", "Could not delete updater jar.");

                    UtilPlugin.unload(Main.getInstance());
                }

            } catch (Exception e) {
                Main.getInstance().printError(e, "Error occured while updating "+pluginName+".");
                sendActionBar(initiater, locale.updateFailed.replace("%plugin%", pluginName).replace("%old_version%", currentVersion).replace("%new_version%", newVersion));
            }
        }
    }

    /*
     * UTILITIES
     */

    //Fairly long method to call repetadly ¯\_(ツ)_/¯
    private void sendActionBar(Player player, String message) {
        if (player != null)
            player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(ChatColor.translateAlternateColorCodes('&', message)));
    }

    private String readFrom(String url) throws IOException
    {
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
}
