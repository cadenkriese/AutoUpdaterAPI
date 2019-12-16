package cc.flogi.dev.autoupdater;

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
 * Performs updates for public plugins.
 *
 * Created on 6/14/17
 */
@Getter public class PublicUpdater implements Updater {
    private final Player initiator;
    private final Plugin plugin;
    private final UpdaterRunnable endTask;
    private final UpdateLocale locale;
    private final String pluginFolderPath;
    private final String currentVersion;
    private final String pluginName;
    private final String downloadUrlString;
    private final boolean replace;

    private long startingTime;
    @Setter private String newVersion;

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
        endTask = (successful, ex, updatedPlugin, pluginName) -> {};
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

    @Override public String getLatestVersion() {
        return newVersion;
    }

    @Override public void update() {
        startingTime = System.currentTimeMillis();

        locale.updateVariables(plugin.getName(), currentVersion, newVersion);

        if (newVersion.equalsIgnoreCase(currentVersion)) {
            error(new Exception("Error occurred while updating plugin."), "Plugin is up to date!", "PLUGIN IS UP TO DATE");
            return;
        }

        downloadResource();
    }

    @Override public void downloadResource() {
        UtilUI.sendActionBar(initiator, locale.getUpdating() + " &8[RETRIEVING FILES]", 20);
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

                initializePlugin();
            } catch (Exception ex) {
                error(ex, "Error occurred while updating " + pluginName + ".", newVersion);
            }
        });
    }

    @Override public void initializePlugin() {
        //Copy plugin utility from src/main/resources
        String corePluginFile = "/autoupdater-plugin-" + InternalCore.PROPERTIES.VERSION + ".jar";
        File targetFile = new File(InternalCore.getDataFolder().getAbsolutePath() + corePluginFile);
        targetFile.getParentFile().mkdirs();
        try (InputStream is = getClass().getResourceAsStream(corePluginFile)) {
            Files.copy(is, targetFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
        } catch (Exception ex) {
            error(ex, ex.getMessage(), newVersion);
        }

        UtilThreading.sync(() -> {
            //Enable plugin and perform update task.
            try {
                UtilPlugin.loadPlugin(targetFile);
                if (Bukkit.getPluginManager().getPlugin("autoupdater-plugin") == null)
                    throw new FileNotFoundException("Unable to locate updater plugin.");

                UpdaterPlugin.get().updatePlugin(plugin, initiator, replace, pluginName,
                        pluginFolderPath, locale, startingTime, endTask);
            } catch (Exception ex) {
                error(ex, ex.getMessage(), newVersion);
            }
        });
    }

    private void error(Exception ex, String errorMessage, String shortErrorMessage) {
        InternalCore.get().printError(ex, errorMessage);
        UtilUI.sendActionBar(initiator, locale.getUpdateFailedNoVar() + " &8[" + shortErrorMessage + "&8]", 10);
        endTask.run(false, ex, null, pluginName);
    }
}
