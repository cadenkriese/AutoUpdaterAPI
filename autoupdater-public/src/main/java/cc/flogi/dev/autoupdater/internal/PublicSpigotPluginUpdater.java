package cc.flogi.dev.autoupdater.internal;

import cc.flogi.dev.autoupdater.api.SpigotPluginUpdater;
import cc.flogi.dev.autoupdater.api.UpdateLocale;
import cc.flogi.dev.autoupdater.api.UpdaterRunnable;
import cc.flogi.dev.autoupdater.api.exceptions.ResourceIsPremiumException;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.InvalidDescriptionException;
import org.bukkit.plugin.InvalidPluginException;
import org.bukkit.plugin.Plugin;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;

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
    private static final JSONParser JSON_PARSER = new JSONParser();

    private final String url;
    private final int resourceId;

    private String spigetResponse;
    private String[] supportedVersions;
    private Double averageRating;

    protected PublicSpigotPluginUpdater(Plugin plugin, Player initiator, int resourceId, UpdateLocale locale, boolean replace) {
        this(plugin, initiator, resourceId, locale, replace, (successful, ex, updatedPlugin, pluginName) -> {});
    }

    protected PublicSpigotPluginUpdater(Plugin plugin, Player initiator, int resourceId, UpdateLocale locale, boolean replace, UpdaterRunnable endTask) {
        super(plugin, initiator, SPIGET_BASE_URL + resourceId + "/download", locale, replace);
        this.url = SPIGET_BASE_URL + resourceId;
        this.resourceId = resourceId;

        UtilThreading.async(() -> {
            try {
                spigetResponse = UtilReader.readFrom(url);

                JSONObject json = (JSONObject) JSON_PARSER.parse(spigetResponse);
                if ((Boolean) json.get("premium"))
                    error(new ResourceIsPremiumException("Error occurred while updating premium plugin."),
                            "Plugin is premium.", "PLUGIN IS PREMIUM");

            } catch (IOException | ParseException ex) {
                error(ex, "Error occurred while retrieving Spigot plugin info.");
            }
        });
    }

    @Override public String getDownloadUrlString() {
        if (downloadUrlString != null)
            return downloadUrlString;

        try {
            JSONObject json = (JSONObject) JSON_PARSER.parse(spigetResponse);

            String urlString;
            if ((Boolean) json.get("external"))
                urlString = "https://spigotmc.org/" + ((JSONObject) json.get("file")).get("url");
            else
                urlString = SPIGET_BASE_URL + resourceId + "/download";

            downloadUrlString = urlString;
            return urlString;
        } catch (ParseException ex) {
            error(ex, "Error occurred while retrieving download URL of Spigot plugin.");
            return null;
        }
    }

    @Override public String[] getSupportedVersions() {
        if (supportedVersions != null)
            return supportedVersions;

        try {
            JSONObject json = (JSONObject) JSON_PARSER.parse(spigetResponse);
            JSONArray supportedVersionsObj = (JSONArray) json.get("testedVersions");
            supportedVersions = (String[]) supportedVersionsObj.toArray(new String[]{});
            return supportedVersions;
        } catch (ParseException ex) {
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

        try {
            JSONObject json = (JSONObject) JSON_PARSER.parse(spigetResponse);
            JSONObject supportedVersionsObj = (JSONObject) json.get("rating");
            averageRating = (Double) json.get("average");
            return averageRating;
        } catch (ParseException ex) {
            error(ex, "Error occurred while retrieving download URL of Spigot plugin.");
            return null;
        }
    }

    /**
     * @implNote  Makes a web request, will halt current thread shortly (if run synchronously).
     */
    @Override public String getLatestVersion() {
        try {
            return UtilReader.readFrom("https://api.spigotmc.org/legacy/update.php?resource=" + resourceId);
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
