package cc.flogi.dev.autoupdater.internal;

import cc.flogi.dev.autoupdater.api.PluginUpdater;
import cc.flogi.dev.autoupdater.api.UpdaterRunnable;
import cc.flogi.dev.autoupdater.api.exceptions.NoUpdateFoundException;
import cc.flogi.dev.autoupdater.plugin.UpdaterPlugin;
import lombok.Getter;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.plugin.InvalidDescriptionException;
import org.bukkit.plugin.InvalidPluginException;
import org.bukkit.plugin.Plugin;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Date;
import java.util.Map;

/**
 * @author Caden Kriese (flogic)
 *
 * Performs updates for public plugins.
 *
 * Created on 6/14/17
 */
@Getter
public class PublicPluginUpdater implements PluginUpdater {
    protected final Player initiator;
    protected final Plugin plugin;
    protected final UpdaterRunnable endTask;
    protected final PluginUpdateLocale locale;
    protected final String pluginFolderPath;
    protected final String currentVersion;
    protected final String pluginName;
    protected final boolean replace;
    protected final boolean initialize;

    protected long startingTime;
    protected String downloadUrlString;
    protected String latestVersion;

    protected PublicPluginUpdater(Plugin plugin, Player initiator, String downloadUrlString, PluginUpdateLocale locale, boolean initialize, boolean replace, String latestVersion) {
        this(plugin, initiator, downloadUrlString, locale, initialize, replace, (successful, ex, updatedPlugin, pluginName) -> {});
        this.latestVersion = latestVersion;
    }

    protected PublicPluginUpdater(Plugin plugin, Player initiator, String downloadUrlString, PluginUpdateLocale locale, boolean initialize, boolean replace, String latestVersion, UpdaterRunnable endTask) {
        this(plugin, initiator, downloadUrlString, locale, initialize, replace, endTask);
        this.latestVersion = latestVersion;
    }

    protected PublicPluginUpdater(Plugin plugin, Player initiator, String downloadUrlString, PluginUpdateLocale locale, boolean initialize, boolean replace) {
        this(plugin, initiator, downloadUrlString, locale, initialize, replace, (successful, ex, updatedPlugin, pluginName) -> {});
    }

    protected PublicPluginUpdater(Plugin plugin, Player initiator, String downloadUrlString, PluginUpdateLocale locale, boolean initialize, boolean replace, UpdaterRunnable endTask) {
        pluginFolderPath = plugin.getDataFolder().getParent();
        currentVersion = plugin.getDescription().getVersion();
        this.plugin = plugin;
        this.initiator = initiator;
        this.downloadUrlString = downloadUrlString;
        this.locale = locale;
        this.replace = replace;
        this.endTask = endTask;
        this.pluginName = locale.getBukkitPluginName();
        this.initialize = initialize;
    }

    @Override
    public void update() {
        startingTime = System.currentTimeMillis();

        UtilUI.sendActionBar(initiator, locale.getUpdatingMsgNoVar(), "status", "RETRIEVING PLUGIN INFO");
        UtilThreading.async(() -> {
            try {
                latestVersion = getLatestVersion();
                downloadUrlString = getDownloadUrlString();
                locale.updateVariables(plugin.getName(), currentVersion, latestVersion);

                if (latestVersion.equalsIgnoreCase(currentVersion)) {
                    error(new NoUpdateFoundException("Error occurred while updating plugin."), "Plugin is up to date!", "PLUGIN IS UP TO DATE");
                    return;
                }

                downloadResource();
            } catch (IllegalAccessException ex) {
                error(ex, "Error occurred while updating locale variables.", "INTERNAL ERROR");
            }
        });
    }

    @Override
    public void downloadResource() {
        UtilUI.sendActionBar(initiator, locale.getUpdatingMsg(), 20, "status", " RETRIEVING FILES");
        try {
            URL downloadUrl = new URL(downloadUrlString);
            HttpURLConnection httpConnection = (HttpURLConnection) downloadUrl.openConnection();
            if (downloadUrlString.contains("spiget.org"))
                httpConnection.setRequestProperty("User-Agent", UserAgent.SPIGET.toString());
            else
                httpConnection.setRequestProperty("User-Agent", UserAgent.CHROME.toString());

            long completeFileSize = httpConnection.getContentLength();
            int grabSize = 2048;

            final File destinationFile = new File(pluginFolderPath + "/" + locale.getFileName());

            BufferedInputStream in = new BufferedInputStream(httpConnection.getInputStream());
            FileOutputStream fos = new FileOutputStream(destinationFile);
            BufferedOutputStream bout = new BufferedOutputStream(fos, grabSize);

            byte[] data = new byte[grabSize];
            long downloadedFileSize = 0;
            int grab;
            while ((grab = in.read(data, 0, grabSize)) >= 0) {
                downloadedFileSize += grab;
                if (downloadedFileSize % (grabSize * 5) == 0) {
                    String bar = UtilUI.progressBar(15, downloadedFileSize, completeFileSize, ':', ChatColor.GREEN, ChatColor.RED);
                    final String currentPercent = String.format("%.2f", (((double) downloadedFileSize) / ((double) completeFileSize)) * 100);
                    UtilUI.sendActionBar(initiator, locale.getDownloadingMsg(),
                            "status", " DOWNLOADING",
                            "download_bar", bar,
                            "download_percent", currentPercent + "%");
                }
                bout.write(data, 0, grab);
            }

            bout.close();
            in.close();
            fos.close();

            initializePlugin();
        } catch (IOException ex) {
            error(ex, "Error occurred while updating " + pluginName + ".", "PLUGIN NOT FOUND");
        }
    }

    @Override
    public void initializePlugin() {
        //Copy plugin utility from src/main/resources
        if (!initialize) {
            try {
                File cacheFile = UtilPlugin.cachePlugin(new File(pluginFolderPath + "/" + locale.getFileName()),
                        replace, plugin);

                double elapsedTimeSeconds = (double) (System.currentTimeMillis() - startingTime) / 1000;
                UtilUI.sendActionBar(initiator, locale.getCompletionMsg(),
                        "elapsed_time", String.format("%.2f", elapsedTimeSeconds),
                        "status", "INSTALLATION UPON RESTART");

                if (AutoUpdaterInternal.METRICS) {

                    UtilMetrics.Plugin metricsPlugin = getMetricsPlugin(cacheFile);
                    String oldVersion = replace ? currentVersion : null;

                    UtilThreading.async(() -> UtilMetrics.sendUpdateInfo(metricsPlugin,
                            new UtilMetrics.PluginUpdate(new Date(),
                                    cacheFile.length(),
                                    elapsedTimeSeconds,
                                    new UtilMetrics.PluginUpdateVersion(oldVersion, latestVersion))));
                }
            } catch (InvalidDescriptionException | IOException | URISyntaxException ex) {
                error(ex, "Error occurred while caching plugin.", "CACHING FAILED");
            }

            return;
        }

        String corePluginFile = "/autoupdater-plugin-" + AutoUpdaterInternal.PROPERTIES.VERSION + ".jar";
        File targetFile = new File(AutoUpdaterInternal.getDataFolder().getParent() + corePluginFile);
        targetFile.getParentFile().mkdirs();
        try (InputStream is = getClass().getResourceAsStream(corePluginFile)) {
            Files.copy(is, targetFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException ex) {
            error(ex, ex.getMessage());
            return;
        }

        UtilThreading.sync(() -> {
            //Enable plugin and perform update task.
            try {
                if (UtilPlugin.loadAndEnable(targetFile) == null)
                    throw new FileNotFoundException("Unable to locate updater plugin.");

                UpdaterPlugin.get().updatePlugin(plugin, initiator, replace, AutoUpdaterInternal.DEBUG, AutoUpdaterInternal.METRICS,
                        pluginName, pluginFolderPath, new cc.flogi.dev.autoupdater.plugin.PluginUpdateLocale(locale),
                        startingTime, downloadUrlString, getSpigotResourceID(downloadUrlString), endTask);
            } catch (FileNotFoundException | InvalidPluginException | InvalidDescriptionException ex) {
                error(ex, ex.getMessage());
            }
        });
    }

    protected Integer getSpigotResourceID(String downloadUrlString) {
        if (downloadUrlString.contains("spigotmc.org")) {
            try {
                return Integer.valueOf(downloadUrlString.substring(
                        downloadUrlString.lastIndexOf('.'),
                        downloadUrlString.lastIndexOf('/')));
            } catch (Exception ignored) {}
        }

        return null;
    }

    protected UtilMetrics.Plugin getMetricsPlugin(File pluginFile) throws IOException, InvalidDescriptionException {
        Map<?, ?> pluginInfo = UtilPlugin.getPluginInfo(pluginFile);

        String description = "";
        Object descriptionObj = pluginInfo.get("description");
        if (descriptionObj != null)
            description = descriptionObj.toString();

        return new UtilMetrics.Plugin(pluginInfo.get("name").toString(), description, getDownloadUrlString());
    }

    protected void error(Exception ex, String errorMessage, String shortErrorMessage) {
        if (AutoUpdaterInternal.DEBUG)
            AutoUpdaterInternal.get().printError(ex, errorMessage);

        UtilUI.sendActionBar(initiator, locale.getFailureMsg(), 10, "status", shortErrorMessage);
        endTask.run(false, ex, null, pluginName);
    }

    protected void error(Exception ex, String message) {
        if (AutoUpdaterInternal.DEBUG)
            AutoUpdaterInternal.get().printError(ex, message);

        UtilUI.sendActionBar(initiator, locale.getFailureMsg(), "status", " CHECK CONSOLE");
        endTask.run(false, ex, null, pluginName);
    }
}
