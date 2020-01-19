package cc.flogi.dev.autoupdater.internal;

import cc.flogi.dev.autoupdater.api.SpigotPluginUpdater;
import cc.flogi.dev.autoupdater.api.UpdaterRunnable;
import cc.flogi.dev.autoupdater.api.exceptions.ResourceIsPremiumException;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.io.IOException;
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
    private final Integer spigotResourceID;

    private String spigetResponse;
    private String[] supportedVersions;
    private Double averageRating;

    PublicSpigotPluginUpdater(Plugin plugin, Player initiator, int spigotResourceID, PluginUpdateLocale locale, boolean initialize, boolean replace) {
        this(plugin, initiator, spigotResourceID, locale, initialize, replace, (successful, ex, updatedPlugin, pluginName) -> {});
    }

    PublicSpigotPluginUpdater(Plugin plugin, Player initiator, int spigotResourceID, PluginUpdateLocale locale, boolean initialize, boolean replace, UpdaterRunnable endTask) {
        super(plugin, initiator, SPIGET_BASE_URL + spigotResourceID + "/download", locale, replace, initialize);
        this.url = SPIGET_BASE_URL + spigotResourceID;
        this.spigotResourceID = spigotResourceID;

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
            urlString = SPIGET_BASE_URL + spigotResourceID + "/download";

        downloadUrlString = urlString;
        return urlString;
    }

    @Override public String[] getSupportedVersions() {
        if (supportedVersions != null)
            return supportedVersions;

        try {
            JsonObject json = JSON_PARSER.parse(UtilIO.readFromURL(SPIGET_BASE_URL + "resources/" + spigotResourceID)).getAsJsonObject();
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
            return UtilIO.readFromURL("https://api.spigotmc.org/legacy/update.php?resource=" + spigotResourceID);
        } catch (IOException ex) {
            error(ex, "Error occurred while retrieving the latest update.", "SPIGOT CONNECTION FAILED");
            return null;
        }
    }

    @Override protected UtilMetrics.Plugin getMetricsPlugin(Plugin plugin) {
        return UtilMetrics.getSpigotPlugin(plugin, spigotResourceID);
    }

    @Override protected Integer getSpigotResourceID(String downloadUrlString) {
        return this.spigotResourceID;
    }
}
