package cc.flogi.dev.autoupdater.internal;

import cc.flogi.dev.autoupdater.api.SpigotPluginUpdater;
import cc.flogi.dev.autoupdater.api.UpdaterRunnable;
import cc.flogi.dev.autoupdater.api.exceptions.ResourceIsPremiumException;
import cc.flogi.dev.autoupdater.plugin.UpdaterPlugin;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
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
import java.util.*;

/**
 * @author Caden Kriese (flogic)
 *
 * Performs updates and handles information for public Spigot plugins.
 * Note that the plugin information is requested once when the class is instantiated.
 * This class is not designed to be kept around in runtime for extended periods of time.
 *
 * Created on 12/12/19
 */
public class PublicSpigotPluginUpdater extends PublicPluginUpdater implements SpigotPluginUpdater {
    private static final String SPIGET_BASE_URL = "https://api.spiget.org/v2/resources/";
    private static final JsonParser JSON_PARSER = new JsonParser();

    private final String url;
    private final int resourceId;

    private String spigetResponse;
    private String[] supportedVersions;
    private Double averageRating;

    protected PublicSpigotPluginUpdater(Plugin plugin, Player initiator, int resourceId, PluginUpdateLocale locale, boolean initialize, boolean replace) {
        this(plugin, initiator, resourceId, locale, initialize, replace, (successful, ex, updatedPlugin, pluginName) -> {});
    }

    protected PublicSpigotPluginUpdater(Plugin plugin, Player initiator, int resourceId, PluginUpdateLocale locale, boolean initialize, boolean replace, UpdaterRunnable endTask) {
        super(plugin, initiator, SPIGET_BASE_URL + resourceId + "/download", locale, replace, initialize);
        this.url = SPIGET_BASE_URL + resourceId;
        this.resourceId = resourceId;

        UtilThreading.async(() -> {
            try {
                spigetResponse = UtilIO.readFromURL(url);

                JsonObject json = JSON_PARSER.parse(spigetResponse).getAsJsonObject();
                if (json.get("premium").getAsBoolean())
                    error(new ResourceIsPremiumException("Error occurred while updating premium plugin."),
                            "Plugin is premium.", "PLUGIN IS PREMIUM");

            } catch (IOException ex) {
                error(ex, "Error occurred while retrieving Spigot plugin info.", "SPIGOT CONNECTION FAILED");
            }
        });
    }

    @Override public String getDownloadUrlString() {
        if (downloadUrlString != null)
            return downloadUrlString;

        JsonObject json = JSON_PARSER.parse(spigetResponse).getAsJsonObject();

        String urlString;
        if (json.get("external").getAsBoolean())
            urlString = "https://spigotmc.org/" + json.getAsJsonObject("file").get("url").getAsString();
        else
            urlString = SPIGET_BASE_URL + resourceId + "/download";

        downloadUrlString = urlString;
        return urlString;
    }

    @Override public String[] getSupportedVersions() {
        if (supportedVersions != null)
            return supportedVersions;

        try {
            JsonObject json = JSON_PARSER.parse(UtilIO.readFromURL(SPIGET_BASE_URL + "resources/" + resourceId)).getAsJsonObject();
            JsonArray supportedVersionsObj = json.getAsJsonArray("testedVersions");
            List<String> supportedVersionsList = new ArrayList<>();
            supportedVersionsObj.forEach(e -> supportedVersionsList.add(e.getAsString()));
            this.supportedVersions = supportedVersionsList.toArray(new String[]{});
            return supportedVersions;
        } catch (IOException ex) {
            error(ex, "Error occurred while retrieving download URL of Spigot plugin.", "SPIGOT CONNECTION FAILED");
            return null;
        }
    }

    @Override public Boolean currentVersionSupported() {
        return Arrays.stream(getSupportedVersions()).anyMatch(ver -> Bukkit.getVersion().contains(ver));
    }

    @Override public Double getAverageRating() {
        if (averageRating != null)
            return averageRating;

        JsonObject json = JSON_PARSER.parse(spigetResponse).getAsJsonObject();
        JsonObject supportedVersionsObj = json.getAsJsonObject("rating");
        averageRating = json.get("average").getAsDouble();
        return averageRating;
    }

    /**
     * @implNote Makes a web request, will halt current thread shortly (if run synchronously).
     */
    @Override public String getLatestVersion() {
        try {
            return UtilIO.readFromURL("https://api.spigotmc.org/legacy/update.php?resource=" + resourceId);
        } catch (IOException ex) {
            error(ex, "Error occurred while retrieving the latest update.", "SPIGOT CONNECTION FAILED");
            return null;
        }
    }

    //Mostly duplicate code, just sending spigot resourceId to UpdaterPlugin.
    @Override public void downloadResource() {
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

            if (initialize)
                initializePlugin();
            else {
                File cacheFile = new File(AutoUpdaterInternal.getCacheFolder(), destinationFile.getName());
                File metaFile = new File(AutoUpdaterInternal.getCacheFolder(), destinationFile.getName() + ".meta");

                AutoUpdaterInternal.getCacheFolder().mkdirs();
                Files.move(destinationFile.toPath(), cacheFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                HashMap<String, Object> updateMeta = new HashMap<>();
                updateMeta.put("replace", String.valueOf(replace));
                updateMeta.put("destination", destinationFile.getAbsolutePath());
                if (replace)
                    updateMeta.put("old-file", plugin.getClass()
                            .getProtectionDomain().getCodeSource()
                            .getLocation().toURI().getPath());

                UtilIO.writeToFile(metaFile, new Gson().toJson(updateMeta));

                double elapsedTimeSeconds = (double) (System.currentTimeMillis() - startingTime) / 1000;
                UtilUI.sendActionBar(initiator, locale.getCompletionMsg(),
                        "elapsed_time", String.format("%.2f", elapsedTimeSeconds),
                        "status", "INSTALLATION UPON RESTART");

                //TODO find a better way lol
                Plugin updated = Bukkit.getPluginManager().loadPlugin(cacheFile);

                UtilThreading.async(() -> UtilMetrics.sendUpdateInfo(UtilMetrics.getSpigotPlugin(updated, resourceId),
                        new UtilMetrics.PluginUpdate(new Date(),
                                cacheFile.length(),
                                elapsedTimeSeconds,
                                new UtilMetrics.PluginUpdateVersion(currentVersion, latestVersion))));

                UtilPlugin.unload(updated);
            }
        } catch (IOException | URISyntaxException | InvalidPluginException | InvalidDescriptionException ex) {
            error(ex, "Error occurred while updating " + pluginName + ".", "PLUGIN NOT FOUND");
        }
    }

    @Override public void initializePlugin() {
        //Copy plugin utility from src/main/resources
        String corePluginFile = "/autoupdater-plugin-" + AutoUpdaterInternal.PROPERTIES.VERSION + ".jar";
        File targetFile = new File(AutoUpdaterInternal.getDataFolder().getAbsolutePath() + corePluginFile);
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
                        startingTime, downloadUrlString, resourceId, endTask);
            } catch (FileNotFoundException | InvalidDescriptionException | InvalidPluginException ex) {
                error(ex, ex.getMessage(), "PLUGIN INIT FAILED");
            }
        });
    }
}
