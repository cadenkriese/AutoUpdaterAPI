package cc.flogi.dev.autoupdater;

import cc.flogi.dev.autoupdater.util.UserAgent;
import cc.flogi.dev.autoupdater.util.UtilText;
import cc.flogi.dev.autoupdater.util.UtilThreading;
import cc.flogi.dev.autoupdater.util.UtilUI;
import lombok.Getter;
import lombok.Setter;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

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
    private final Player initiator;
    private final Plugin plugin;

    private final UpdateLocale locale;
    private final String pluginFolderPath;
    private final String currentVersion;
    private final String pluginName;
    private final String downloadUrlString;

    private final boolean replace;
    private long startingTime;

    @Setter private String newVersion;

    private UpdaterRunnable endTask = (successful, ex, updatedPlugin, pluginName) -> {};

    protected PublicUpdater(Plugin plugin, Player initiator, String downloadUrlString, UpdateLocale locale, boolean replace) {
        locale.updateVariables(plugin.getName(), plugin.getDescription().getVersion(), null);

        pluginFolderPath = plugin.getDataFolder().getParent();
        currentVersion = plugin.getDescription().getVersion();
        this.plugin = plugin;
        this.initiator = initiator;
        this.downloadUrlString = downloadUrlString;
        this.locale = locale;
        this.replace = replace;
        this.pluginName = locale.getPluginName();
    }

    protected PublicUpdater(Plugin plugin, Player initiator, String downloadUrlString, UpdateLocale locale, boolean replace, UpdaterRunnable endTask) {
        locale.updateVariables(plugin.getName(), plugin.getDescription().getVersion(), null);

        pluginFolderPath = plugin.getDataFolder().getParent();
        currentVersion = plugin.getDescription().getVersion();
        this.plugin = plugin;
        this.initiator = initiator;
        this.downloadUrlString = downloadUrlString;
        this.locale = locale;
        this.replace = replace;
        this.endTask = endTask;
        this.pluginName = locale.getPluginName();
    }

    /**
     * Begins the update process, this will disable and delete the current instance of the plugin being updated.
     */
    public void update() {
        startingTime = System.currentTimeMillis();
        UtilThreading.async(() -> {
            locale.updateVariables(plugin.getName(), currentVersion, newVersion);
            UtilThreading.sync(() -> {
                if (newVersion.equalsIgnoreCase(currentVersion)) {
                    AutoUpdaterAPI.get().printPluginError("Error occurred while updating " + pluginName + "!", "Plugin is up to date!");
                    UtilUI.sendActionBar(initiator, locale.getUpdateFailed() + " [PLUGIN IS UP TO DATE]");
                    endTask.run(false, null, Bukkit.getPluginManager().getPlugin(pluginName), pluginName);
                    return;
                }

                UtilUI.sendActionBar(initiator, locale.getUpdating() + " &8[RETRIEVING FILES]", 20);
                try {
                    UtilThreading.async(() -> {
                        try {
                            URL downloadUrl = new URL(downloadUrlString);
                            HttpURLConnection httpConnection = (HttpURLConnection) downloadUrl.openConnection();
                            if (downloadUrlString.contains("spiget.org"))
                                httpConnection.setRequestProperty("User-Agent", UserAgent.SPIGET.toString());
                            else
                                httpConnection.setRequestProperty("User-Agent", UserAgent.CHROME.toString());

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
                            String corePluginFile = "/autoupdater-plugin-" + AutoUpdaterAPI.PROPERTIES.VERSION + ".jar";
                            try (InputStream is = getClass().getResourceAsStream(corePluginFile)) {
                                File targetFile = new File(plugin.getDataFolder().getParent() + corePluginFile);
                                Files.copy(is, targetFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                                is.close();
                                UtilThreading.sync(() -> {
                                    //Enable plugin and perform update task.
                                    try {
                                        UpdaterPlugin updaterPlugin = (UpdaterPlugin) Bukkit.getPluginManager().loadPlugin(targetFile);
                                        if (updaterPlugin == null)
                                            throw new FileNotFoundException("Unable to locate updater plugin.");

                                        Bukkit.getPluginManager().enablePlugin(updaterPlugin);
                                        updaterPlugin.updatePlugin(plugin, initiator, replace, pluginName, pluginFolderPath, locale, startingTime, endTask);
                                    } catch (Exception ex) {
                                        error(ex, ex.getMessage(), newVersion);
                                    }
                                });
                            } catch (Exception ex) {
                                error(ex, ex.getMessage(), newVersion);
                            }
                        } catch (Exception ex) {
                            error(ex, "Error occurred while updating " + pluginName + ".", newVersion);
                        }
                    });
                } catch (Exception ex) {
                    error(ex, "Error occurred while updating " + pluginName + ".", newVersion);
                }
            });
        });
    }

    private void error(Exception ex, String message, String newVersion) {
        AutoUpdaterAPI.get().printError(ex, message);
        UtilUI.sendActionBar(initiator, UtilText.format(locale.getUpdateFailed(),
                "old_version", currentVersion,
                "new_version", newVersion));
        endTask.run(false, ex, Bukkit.getPluginManager().getPlugin(pluginName), pluginName);
    }
}
