package cc.flogi.dev.autoupdater.plugin;

import cc.flogi.dev.autoupdater.api.UpdaterRunnable;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.InvalidDescriptionException;
import org.bukkit.plugin.InvalidPluginException;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.Date;
import java.util.logging.Logger;

public final class UpdaterPlugin extends JavaPlugin {
    private static UpdaterPlugin instance;
    private static Logger logger;

    @Getter private boolean metrics = true;
    @Getter private boolean debug = false;

    public static UpdaterPlugin get() {
        return instance;
    }

    @Override
    public void onEnable() {
        instance = this;
        logger = getLogger();
        getLogger().info("AutoUpdaterAPI utility enabled.");
    }

    public void updatePlugin(Plugin plugin, Player initiator, boolean replace, boolean debug, boolean metrics, String pluginName,
                             String pluginFolderPath, PluginUpdateLocale locale, long startingTime, String downloadUrl,
                             Integer spigotResourceId, UpdaterRunnable endTask) {
        //Lot of messy variables due to transferring a whole class worth of information into one method call.
        this.metrics = metrics;
        this.debug = debug;
        final File restoreFile = new File(getDataFolder().getParent() + "/" + locale.getFileName());
        File cachedPlugin = null;

        UtilUI.sendActionBar(initiator, locale.getUpdatingMsg() + " &8[INITIALIZING]", 10);

        try {
            cachedPlugin = File.createTempFile("auapi-cached-plugin-", ".jar");

            if (replace) {
                File pluginFile = new File(plugin.getClass().getProtectionDomain().getCodeSource().getLocation().toURI().getPath());
                Files.copy(pluginFile.toPath(), cachedPlugin.toPath());
                UtilPlugin.unload(plugin);
                if (!pluginFile.delete())
                    printPluginError("Error occurred while updating " + pluginName + ".", "Could not delete old plugin jar.");
            }

            Plugin updated = initializePlugin(pluginName, pluginFolderPath, locale, endTask);

            if (!updated.isEnabled()) {
                UtilPlugin.unload(updated);
                throw new Exception("Plugin was not enabled.");
            }

            endTask.run(true, null, updated, pluginName);

            double elapsedTimeSeconds = (double) (System.currentTimeMillis() - startingTime) / 1000;
            UtilUI.sendActionBar(initiator, locale.getCompletionMsg(),
                    "elapsed_time", String.format("%.2f", elapsedTimeSeconds),
                    "status", "DOWNLOADED AND INSTALLED");

            //Update metrics
            new BukkitRunnable() {
                @Override public void run() {
                    long fileSize = new File(pluginFolderPath + "/" + locale.getFileName()).length();
                    String currentVersion = replace ? plugin.getDescription().getVersion() : null;

                    UtilMetrics.PluginUpdate updateMetrics = new UtilMetrics.PluginUpdate(new Date(), fileSize, elapsedTimeSeconds,
                            new UtilMetrics.PluginUpdateVersion(currentVersion, updated.getDescription().getVersion()));
                    UtilMetrics.Plugin pluginMetrics;

                    if (spigotResourceId == null)
                        pluginMetrics = new UtilMetrics.Plugin(updated.getName(),
                                updated.getDescription().getDescription(), downloadUrl);
                    else
                        pluginMetrics = UtilMetrics.getSpigotPlugin(updated, spigotResourceId);

                    UtilMetrics.sendUpdateInfo(pluginMetrics, updateMetrics);
                }
            }.runTaskAsynchronously(updated);
            //Run async with new plugin to avoid task being cancelled by self shutdown.
        } catch (Exception ex1) {
            getLogger().severe("A critical exception occurred while initializing the plugin '" + pluginName + "'");
            getLogger().severe("Restoring previous state...");

            new File(pluginFolderPath + "/" + locale.getFileName()).delete();

            if (debug)
                ex1.printStackTrace();

            if (replace) {
                try {
                    getLogger().severe("Attempting to re-initialize old version.");
                    Files.copy(cachedPlugin.toPath(), restoreFile.toPath());
                    Plugin oldVersion = initializePlugin(pluginName, pluginFolderPath, locale, endTask);
                    endTask.run(false, ex1, oldVersion, pluginName);
                } catch (Exception ex2) {
                    if (debug)
                        ex2.printStackTrace();
                }
            }
        } finally {
            cachedPlugin.delete();
            selfDestruct();
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
                    Path pluginFile = Files.find(getDataFolder().getParentFile().toPath(), 2,
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

    private Plugin initializePlugin(String pluginName, String pluginFolderPath, PluginUpdateLocale locale, UpdaterRunnable endTask)
            throws InvalidDescriptionException, InvalidPluginException {
        Plugin updated = getServer().getPluginManager().loadPlugin(new File(pluginFolderPath + "/" + locale.getFileName()));
        updated.onLoad();
        Bukkit.getPluginManager().enablePlugin(updated);
        return updated;
    }

    void printError(Exception ex) {
        printError(ex, null);
    }

    void printError(Exception ex, String extraInfo) {
        logger.warning("An error has occurred.");
        logger.warning("If you cannot figure out this error on your own please copy and paste " +
                "\neverything from here to END ERROR and post it at " + getDescription().getWebsite());
        logger.warning("\n============== BEGIN ERROR ==============");
        logger.warning("API VERSION: " + getDescription().getVersion());
        if (extraInfo != null)
            logger.warning("\nAPI MESSAGE: " + extraInfo);
        logger.warning("\nERROR MESSAGE: " + ex.getMessage());
        logger.warning("\nSTACKTRACE: ");
        ex.printStackTrace();
        logger.warning("\n============== END ERROR ==============");
    }

    void printPluginError(String header, String message) {
        logger.warning("============== BEGIN ERROR ==============");
        logger.warning(header);
        logger.warning("\nAPI VERSION: " + getDescription().getVersion());
        logger.warning("\nAPI MESSAGE: " + message);
        logger.warning("\n============== END ERROR ==============");
    }
}
