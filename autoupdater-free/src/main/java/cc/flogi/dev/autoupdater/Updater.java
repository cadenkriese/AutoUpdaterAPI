package cc.flogi.dev.autoupdater;

import cc.flogi.dev.autoupdater.util.UtilReader;
import cc.flogi.dev.autoupdater.util.UtilText;
import cc.flogi.dev.autoupdater.util.UtilUI;
import lombok.Getter;
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

/**
 * @author Caden Kriese (flogic)
 *
 * Created on 6/14/17
 */
@Getter public class Updater {
    private Player initiator;
    private Plugin plugin;

    private String dataFolderPath;
    private String currentVersion;
    private String url;
    private String resourceId;
    private String pluginName;
    private UpdateLocale locale;

    private boolean deleteOld;
    private long startingTime;

    private UpdaterRunnable endTask = (successful, ex, updatedPlugin, pluginName) -> {
    };

    protected Updater(Plugin plugin, Player initiator, int resourceId, UpdateLocale locale, boolean deleteOld) {
        locale.updateVariables(plugin.getName(), plugin.getDescription().getVersion(), null);

        dataFolderPath = plugin.getDataFolder().getParent();
        currentVersion = plugin.getDescription().getVersion();
        url = "https://api.spiget.org/v2/resources/" + resourceId;
        this.plugin = plugin;
        this.initiator = initiator;
        this.locale = locale;
        this.deleteOld = deleteOld;
        this.resourceId = String.valueOf(resourceId);
        this.pluginName = locale.getPluginName();
    }

    protected Updater(Plugin plugin, Player initiator, int resourceId, UpdateLocale locale, boolean deleteOld, UpdaterRunnable endTask) {
        locale.updateVariables(plugin.getName(), plugin.getDescription().getVersion(), null);

        dataFolderPath = plugin.getDataFolder().getParent();
        currentVersion = plugin.getDescription().getVersion();
        url = "https://api.spiget.org/v2/resources/" + resourceId;
        this.plugin = plugin;
        this.initiator = initiator;
        this.locale = locale;
        this.deleteOld = deleteOld;
        this.resourceId = String.valueOf(resourceId);
        this.endTask = endTask;
        this.pluginName = locale.getPluginName();
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
            UtilUI.sendActionBar(initiator, locale.getUpdateFailed().replace("%new_version%", "&4NULL"));
        }

        return "";
    }

    /**
     * Begins the update process, this will disable and delete the current instance of the plugin being updated.
     */
    public void update() {
        startingTime = System.currentTimeMillis();
        new BukkitRunnable() {
            @Override public void run() {
                String newVersion = getLatestVersion();
                locale.updateVariables(plugin.getName(), currentVersion, newVersion);
                new BukkitRunnable() {
                    @Override public void run() {
                        if (newVersion.equalsIgnoreCase(currentVersion)) {
                            AutoUpdaterAPI.get().printPluginError("Error occurred while updating " + pluginName + "!", "Plugin is up to date!");
                            UtilUI.sendActionBar(initiator, locale.getUpdateFailed() + " [PLUGIN IS UP TO DATE]");
                            endTask.run(false, null, Bukkit.getPluginManager().getPlugin(pluginName), pluginName);
                            return;
                        }

                        UtilUI.sendActionBar(initiator, locale.getUpdating() + " &8[RETRIEVING FILES]");
                        try {
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
                                                UtilUI.sendActionBar(initiator, UtilText.format(locale.getUpdatingDownload() + " &8[DOWNLOADING]",
                                                        "%download_bar%", bar,
                                                        "%download_percent%", currentPercent + "%"));
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
                                                    //TODO Copy compiled plugin from src/main/resources and enable it.
                                                    UpdaterPlugin updaterPlugin = (UpdaterPlugin) Bukkit.getPluginManager().getPlugin("updater-plugin");
                                                    updaterPlugin.updatePlugin(plugin, initiator, deleteOld, pluginName, dataFolderPath, locale, startingTime, endTask);
                                                } catch (Exception ex) {
                                                    error(ex, "Error occurred while initializing " + pluginName + ".", newVersion);
                                                }
                                            }
                                        }.runTask(AutoUpdaterAPI.getPlugin());
                                    } catch (Exception ex) {
                                        error(ex, "Error occurred while updating " + pluginName + ".", newVersion);
                                    }
                                }
                            }.runTaskAsynchronously(AutoUpdaterAPI.getPlugin());
                        } catch (Exception ex) {
                            error(ex, "Error occurred while updating " + pluginName + ".", newVersion);
                        }
                    }
                }.runTask(AutoUpdaterAPI.getPlugin());
            }
        }.runTaskAsynchronously(AutoUpdaterAPI.getPlugin());
    }

    private void error(Exception ex, String message, String newVersion) {
        AutoUpdaterAPI.get().printError(ex, "Error occurred while updating " + pluginName + ".");
        UtilUI.sendActionBar(initiator, locale.getUpdateFailed());
        endTask.run(false, ex, Bukkit.getPluginManager().getPlugin(pluginName), pluginName);
    }
}
