package cc.flogi.dev.autoupdater.internal;

import cc.flogi.dev.autoupdater.api.UpdateLocale;
import cc.flogi.dev.autoupdater.api.UpdaterRunnable;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.InvalidDescriptionException;
import org.bukkit.plugin.InvalidPluginException;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.*;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.Date;

public final class UpdaterPlugin extends JavaPlugin {
    private static UpdaterPlugin instance;

    public static UpdaterPlugin get() {
        return instance;
    }

    @Override
    public void onEnable() {
        instance = this;
        getLogger().info("AutoUpdaterAPI utility enabled.");
    }

    public void updatePlugin(Plugin plugin, Player initiator, boolean replace, String pluginName,
                             String pluginFolderPath, UpdateLocale locale, long startingTime, String downloadUrl,
                             Integer spigotResourceId, UpdaterRunnable endTask) {
        //Lot of messy variables due to transferring a whole class worth of information into one method call.
        final File restoreFile = new File(getDataFolder().getParent() + "/" + locale.getFileName() + ".jar");
        File cachedPlugin = null;

        UtilUI.sendActionBar(initiator, locale.getUpdating() + " &8[INITIALIZING]", 10);

        try {
            cachedPlugin = File.createTempFile("auapi-cached-plugin-", ".jar");

            if (replace) {
                File pluginFile = new File(plugin.getClass().getProtectionDomain().getCodeSource().getLocation().toURI().getPath());
                Files.copy(pluginFile.toPath(), cachedPlugin.toPath());
                UtilPlugin.unload(plugin);
                if (!pluginFile.delete())
                    AutoUpdaterInternal.get().printPluginError("Error occurred while updating " + pluginName + ".", "Could not delete old plugin jar.");
            }

            Plugin updated = initializePlugin(pluginName, pluginFolderPath, locale, endTask);

            if (!updated.isEnabled()) {
                UtilPlugin.unload(updated);
                throw new Exception("Plugin was not enabled.");
            }

            endTask.run(true, null, updated, pluginName);

            double elapsedTimeSeconds = (double) (System.currentTimeMillis() - startingTime) / 1000;
            UtilUI.sendActionBar(initiator, locale.getUpdateComplete().replace("%elapsed_time%", String.format("%.2f", elapsedTimeSeconds)));

            //Update metrics
            new BukkitRunnable() {
                @Override public void run() {
                    long fileSize = new File(pluginFolderPath + "/" + locale.getFileName() + ".jar").length();
                    String currentVersion = replace ? plugin.getDescription().getVersion() : null;

                    UtilMetrics.PluginUpdate updateMetrics = new UtilMetrics.PluginUpdate(new Date(), fileSize, elapsedTimeSeconds,
                            new UtilMetrics.PluginUpdateVersion(currentVersion, updated.getDescription().getVersion()));
                    UtilMetrics.Plugin pluginMetrics;

                    if (spigotResourceId == null)
                        pluginMetrics = new UtilMetrics.Plugin(updated.getName(),
                                updated.getDescription().getDescription(), downloadUrl);
                    else
                        pluginMetrics = getSpigotPlugin(updated, spigotResourceId);

                    UtilMetrics.sendUpdateInfo(pluginMetrics, updateMetrics);
                }
            }.runTaskAsynchronously(updated);
            //Run async with new plugin to avoid task being cancelled by self shutdown.
        } catch (Exception ex1) {
            getLogger().severe("A critical exception occurred while initializing the plugin '" + pluginName + "'");
            getLogger().severe("Restoring previous state...");

            new File(pluginFolderPath + "/" + locale.getFileName() + ".jar").delete();

            if (AutoUpdaterInternal.DEBUG)
                ex1.printStackTrace();

            if (replace) {
                try {
                    getLogger().severe("Attempting to re-initialize old version.");
                    Files.copy(cachedPlugin.toPath(), restoreFile.toPath());
                    Plugin oldVersion = initializePlugin(pluginName, pluginFolderPath, locale, endTask);
                    endTask.run(false, ex1, oldVersion, pluginName);
                } catch (Exception ex2) {
                    if (AutoUpdaterInternal.DEBUG)
                        ex2.printStackTrace();
                }
            }
        } finally {
            cachedPlugin.delete();
            selfDestruct();
        }
    }

    private UtilMetrics.SpigotPlugin getSpigotPlugin(Plugin plugin, int resourceId) {
        final String SPIGET_BASE_URL = "https://api.spiget.org/v2/";
        final String SPIGOT_BASE_URL = "https://spigotmc.org/";

        try (InputStream is = new URL(SPIGET_BASE_URL + "resources/" + resourceId).openStream()) {
            BufferedReader rd = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();

            int cp;
            while ((cp = rd.read()) != -1) {
                sb.append((char) cp);
            }

            String spigetResponse = sb.toString();

            JSONParser parser = new JSONParser();
            JSONObject json = (JSONObject) parser.parse(spigetResponse);

            String resourceName = (String) json.get("name");
            Long categoryId = (Long) ((JSONObject) json.get("category")).get("id");
            String categoryInfo = UtilMetrics.readFrom(SPIGET_BASE_URL + "categories/" + categoryId);
            String categoryName = (String) ((JSONObject) parser.parse(categoryInfo)).get("name");
            JSONArray supportedVersionsObj = (JSONArray) json.get("testedVersions");
            String[] supportedVersions = (String[]) supportedVersionsObj.toArray(new String[]{});
            Date uploadDate = new Date(((long) json.get("releaseDate")) * 1000);
            Double averageRating = (Double) ((JSONObject) json.get("rating")).get("average");
            String downloadUrl = SPIGOT_BASE_URL + ((JSONObject) json.get("file")).get("url");
            Boolean premium = (Boolean) json.get("premium");

            if (premium) {
                Double price = (Double) json.get("price");
                String currency = (String) json.get("currency");
                return new UtilMetrics.SpigotPlugin(plugin.getName(), plugin.getDescription().getDescription(),
                        downloadUrl, resourceName, resourceId, categoryName, averageRating, uploadDate, supportedVersions,
                        premium, price, currency);
            }

            return new UtilMetrics.SpigotPlugin(plugin.getName(), plugin.getDescription().getDescription(),
                    downloadUrl, resourceName, resourceId, categoryName, averageRating, uploadDate, supportedVersions);
        } catch (IOException | ParseException ex) {
            if (AutoUpdaterInternal.DEBUG)
                ex.printStackTrace();
            return null;
        }
    }

    private void selfDestruct() {
        try {
            File pluginFile = new File(UpdaterPlugin.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath());
            Files.delete(pluginFile.toPath());

            UtilPlugin.unload(instance);
        } catch (Exception ex) {
            if (ex instanceof NoSuchFileException) {
                try {
                    Path pluginFile = Files.find(AutoUpdaterInternal.getDataFolder().getParentFile().toPath(), 2,
                            ((path, basicFileAttributes) -> path.getFileName().toString().contains("autoupdater-plugin"))).findFirst()
                            .orElseThrow(IOException::new);
                    Files.delete(pluginFile);

                    UtilPlugin.unload(instance);
                    return;
                } catch (IOException ex2) {
                    ex2.printStackTrace();
                }
            }

            ex.printStackTrace();
        }
    }

    private Plugin initializePlugin(String pluginName, String pluginFolderPath, UpdateLocale locale, UpdaterRunnable endTask)
            throws InvalidDescriptionException, InvalidPluginException {
        Plugin updated = getServer().getPluginManager().loadPlugin(new File(pluginFolderPath + "/" + locale.getFileName() + ".jar"));
        updated.onLoad();
        Bukkit.getPluginManager().enablePlugin(updated);
        return updated;
    }
}
