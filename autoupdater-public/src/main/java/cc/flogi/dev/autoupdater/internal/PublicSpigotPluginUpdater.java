package cc.flogi.dev.autoupdater.internal;

import cc.flogi.dev.autoupdater.api.SpigotPluginUpdater;
import cc.flogi.dev.autoupdater.api.UpdaterRunnable;
import cc.flogi.dev.autoupdater.api.exceptions.ResourceIsPremiumException;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.InvalidDescriptionException;
import org.bukkit.plugin.InvalidPluginException;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

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

    protected PublicSpigotPluginUpdater(Plugin plugin, Player initiator, int resourceId, PluginUpdateLocale locale, boolean replace) {
        this(plugin, initiator, resourceId, locale, replace, (successful, ex, updatedPlugin, pluginName) -> {});
    }

    protected PublicSpigotPluginUpdater(Plugin plugin, Player initiator, int resourceId, PluginUpdateLocale locale, boolean replace, UpdaterRunnable endTask) {
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
                error(ex, "Error occurred while retrieving Spigot plugin info.");
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
            error(ex, "Error occurred while retrieving download URL of Spigot plugin.");
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
     * @implNote  Makes a web request, will halt current thread shortly (if run synchronously).
     */
    @Override public String getLatestVersion() {
        try {
            return UtilIO.readFromURL("https://api.spigotmc.org/legacy/update.php?resource=" + resourceId);
        } catch (IOException ex) {
            error(ex, "Error occurred while retrieving the latest update.");
            return null;
        }
    }

    //Mostly duplicate code, just sending spigot resourceId to UpdaterPlugin.
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
                UtilPlugin.loadPlugin(targetFile);
                if (Bukkit.getPluginManager().getPlugin("autoupdater-plugin") == null)
                    throw new FileNotFoundException("Unable to locate updater plugin.");

                UpdaterPlugin.get().updatePlugin(plugin, initiator, replace, pluginName,
                        pluginFolderPath, locale, startingTime, downloadUrlString, resourceId, endTask);
            } catch (FileNotFoundException | InvalidDescriptionException | InvalidPluginException ex) {
                error(ex, ex.getMessage());
            }
        });
    }
}
