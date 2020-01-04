package cc.flogi.dev.autoupdater;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

/**
 * @author Caden Kriese (flogic)
 *
 * Performs updates for public Spigot plugins.
 *
 * Created on 12/12/19
 */
public class PublicSpigotUpdater extends PublicUpdater {
    public static final String BASE_URL = "https://api.spiget.org/v2/resources/";

    private final String url;
    private final int resourceId;

    protected PublicSpigotUpdater(Plugin plugin, Player initiator, int resourceId, UpdateLocale locale, boolean replace) {
        this(plugin, initiator, resourceId, locale, replace, (successful, ex, updatedPlugin, pluginName) -> {});
    }

    protected PublicSpigotUpdater(Plugin plugin, Player initiator, int resourceId, UpdateLocale locale, boolean replace, UpdaterRunnable endTask) {
        super(plugin, initiator, BASE_URL + resourceId + "/download", locale, replace);
        url = BASE_URL + resourceId;
        this.resourceId = resourceId;
    }

    /**
     * Pings spiget to get the download URL of a resource.
     *
     * @return The download URL of a resource, or null if an error occurs.
     */
    @Override
    public String getDownloadUrlString() {
        if (downloadUrlString != null)
            return downloadUrlString;

        try {
            String spigetResponse = UtilReader.readFrom("https://api.spiget.org/v2/resources/" + resourceId);
            JSONParser parser = new JSONParser();
            JSONObject json = (JSONObject) parser.parse(spigetResponse);

            if ((Boolean) json.get("premium"))
                error(new Exception("Error occurred while updating premium plugin."), "Plugin is premium.",
                        "PLUGIN IS PREMIUM");

            if ((Boolean) json.get("external"))
                downloadUrlString = "https://spigotmc.org/" + ((JSONObject) json.get("file")).get("url");
            else
                downloadUrlString = BASE_URL + resourceId + "/download";

            return downloadUrlString;
        } catch (ParseException | IOException ex) {
            if (AutoUpdaterInternal.DEBUG)
                AutoUpdaterInternal.get().printError(ex);

            return null;
        }
    }

    /**
     * Pings spigot to retrieve the latest version of a plugin.
     *
     * @return The latest version of the plugin as a string.
     * @apiNote Makes a web request, will halt current thread shortly (if run synchronously).
     */
    @Override
    public String getLatestVersion() {
        try {
            return UtilReader.readFrom("https://api.spigotmc.org/legacy/update.php?resource=" + resourceId);
        } catch (Exception ex) {
            AutoUpdaterInternal.get().printError(ex, "Error occurred while retrieving the latest update.");
            UtilUI.sendActionBar(getInitiator(), getLocale().getUpdateFailed() + " &8[CHECK CONSOLE]");
            getEndTask().run(false, ex, null, getPluginName());
        }

        return "";
    }

    //Mostly duplicate code, just sending spigot resourceId to UpdaterPlugin.
    @Override
    public void initializePlugin() {
        //Copy plugin utility from src/main/resources
        String corePluginFile = "/autoupdater-plugin-" + AutoUpdaterInternal.PROPERTIES.VERSION + ".jar";
        File targetFile = new File(AutoUpdaterInternal.getDataFolder().getAbsolutePath() + corePluginFile);
        targetFile.getParentFile().mkdirs();
        try (InputStream is = getClass().getResourceAsStream(corePluginFile)) {
            Files.copy(is, targetFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
        } catch (Exception ex) {
            error(ex, ex.getMessage());
        }

        UtilThreading.sync(() -> {
            //Enable plugin and perform update task.
            try {
                UtilPlugin.loadPlugin(targetFile);
                if (Bukkit.getPluginManager().getPlugin("autoupdater-plugin") == null)
                    throw new FileNotFoundException("Unable to locate updater plugin.");

                UpdaterPlugin.get().updatePlugin(plugin, initiator, replace, pluginName,
                        pluginFolderPath, locale, startingTime, downloadUrlString, resourceId, endTask);
            } catch (Exception ex) {
                error(ex, ex.getMessage());
            }
        });
    }
}
