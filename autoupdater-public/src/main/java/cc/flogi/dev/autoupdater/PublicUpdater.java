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

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

/**
 * @author Caden Kriese (flogic)
 *
 * Performs the update task for public Spigot plugins.
 *
 * Created on 6/14/17
 */
@Getter public class PublicUpdater {
    private Player initiator;
    private Plugin plugin;

    private String pluginFolderPath;
    private String currentVersion;
    private String url;
    private String resourceId;
    private String pluginName;
    private UpdateLocale locale;

    private boolean replace;
    private long startingTime;

    private UpdaterRunnable endTask = (successful, ex, updatedPlugin, pluginName) -> {};

    protected PublicUpdater(Plugin plugin, Player initiator, int resourceId, UpdateLocale locale, boolean replace) {
        locale.updateVariables(plugin.getName(), plugin.getDescription().getVersion(), null);

        pluginFolderPath = plugin.getDataFolder().getParent();
        currentVersion = plugin.getDescription().getVersion();
        url = "https://api.spiget.org/v2/resources/" + resourceId;
        this.plugin = plugin;
        this.initiator = initiator;
        this.locale = locale;
        this.replace = replace;
        this.resourceId = String.valueOf(resourceId);
        this.pluginName = locale.getPluginName();
    }

    protected PublicUpdater(Plugin plugin, Player initiator, int resourceId, UpdateLocale locale, boolean replace, UpdaterRunnable endTask) {
        locale.updateVariables(plugin.getName(), plugin.getDescription().getVersion(), null);

        pluginFolderPath = plugin.getDataFolder().getParent();
        currentVersion = plugin.getDescription().getVersion();
        url = "https://api.spiget.org/v2/resources/" + resourceId;
        this.plugin = plugin;
        this.initiator = initiator;
        this.locale = locale;
        this.replace = replace;
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
            UtilUI.sendActionBar(initiator, locale.getUpdateFailedNoVar());
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
                                @SuppressWarnings("ConstantConditions") @Override
                                public void run() {
                                    try {
                                        URL downloadUrl = new URL(url + "/download");
                                        HttpURLConnection httpConnection = (HttpURLConnection) downloadUrl.openConnection();
                                        httpConnection.setRequestProperty("User-Agent", "SpigetResourceUpdater");
                                        long completeFileSize = httpConnection.getContentLength();
                                        int grabSize = 2048;

                                        BufferedInputStream in = new BufferedInputStream(httpConnection.getInputStream());
                                        FileOutputStream fos = new FileOutputStream(new File(pluginFolderPath + "/" + locale.getFileName() + ".jar"));
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

                                        //Copy plugin utility from src/main/resources
                                        //TODO copy to premium branch
                                        String corePluginFile = "/autoupdater-plugin-" + AutoUpdaterAPI.PROPERTIES.VERSION + ".jar";
                                        try (InputStream is = getClass().getResourceAsStream(corePluginFile+".disabled")) {
                                            File targetFile = new File(plugin.getDataFolder().getParent() + corePluginFile);
                                            Files.copy(is, targetFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                                            is.close();
                                            new BukkitRunnable() {
                                                @Override
                                                public void run() {
                                                    //Enable plugin and perform update task.
                                                    try {
                                                        Bukkit.getPluginManager().loadPlugin(targetFile);
                                                        UpdaterPlugin updaterPlugin = (UpdaterPlugin) Bukkit.getPluginManager().getPlugin("autoupdater-plugin");
                                                        if (updaterPlugin == null)
                                                            throw new FileNotFoundException("Unable to locate updater plugin.");
                                                        updaterPlugin.updatePlugin(plugin, initiator, replace, pluginName, pluginFolderPath, locale, startingTime, endTask);
                                                    } catch (Exception ex) {
                                                        error(ex, ex.getMessage(), newVersion);
                                                    }
                                                }
                                            }.runTask(AutoUpdaterAPI.getPlugin());
                                        } catch (Exception ex) {
                                            error(ex, ex.getMessage(), newVersion);
                                        }
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
        AutoUpdaterAPI.get().printError(ex, message);
        UtilUI.sendActionBar(initiator, UtilText.format(locale.getUpdateFailed(),
                "old_version", currentVersion,
                "new_version", newVersion));
        endTask.run(false, ex, Bukkit.getPluginManager().getPlugin(pluginName), pluginName);
    }
}
