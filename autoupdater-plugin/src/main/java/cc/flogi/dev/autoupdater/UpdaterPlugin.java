package cc.flogi.dev.autoupdater;

import cc.flogi.dev.autoupdater.util.UtilPlugin;
import cc.flogi.dev.autoupdater.util.UtilUI;
import com.google.common.io.Files;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.InvalidDescriptionException;
import org.bukkit.plugin.InvalidPluginException;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@SuppressWarnings("UnstableApiUsage") public final class UpdaterPlugin extends JavaPlugin {
    private static UpdaterPlugin instance;

    public static UpdaterPlugin get() {
        return instance;
    }

    @Override
    public void onEnable() {
        instance = this;
        getLogger().info("AutoUpdaterAPI utility enabled.");
    }

    public void updatePlugin(Plugin plugin, Player initiator, boolean replace, String pluginName, String pluginFolderPath, UpdateLocale locale, long startingTime, UpdaterRunnable endTask)
            throws URISyntaxException, InvalidDescriptionException, InvalidPluginException, IOException {
        final File restoreFile = new File(getDataFolder().getParent() + locale.getFileName() + ".jar");
        File cachedPlugin = null;

        UtilUI.sendActionBar(initiator, locale.getUpdating() + " &8[INITIALIZING]", 6);

        try {
            cachedPlugin = File.createTempFile("auapi-cached-plugin", ".jar");

            if (replace) {
                File pluginFile = new File(plugin.getClass().getProtectionDomain().getCodeSource().getLocation().toURI().getPath());
                Files.copy(pluginFile, cachedPlugin);
                UtilPlugin.unload(plugin);
                if (!pluginFile.delete())
                    AutoUpdaterAPI.get().printPluginError("Error occurred while updating " + pluginName + ".", "Could not delete old plugin jar.");
            }

            initializePlugin(pluginName, pluginFolderPath, locale, endTask);

            double elapsedTimeSeconds = (double) (System.currentTimeMillis() - startingTime) / 1000;
            UtilUI.sendActionBar(initiator, locale.getUpdateComplete().replace("%elapsed_time%", String.format("%.2f", elapsedTimeSeconds)));
        } catch (Exception ex) {
            if (replace) {
                getLogger().severe("A critical exception occurred while updating the plugin '" + pluginName + "'");
                getLogger().severe("Attempting to re-initialize old version.");

                if (cachedPlugin != null && !cachedPlugin.renameTo(restoreFile)) {
                    throw new IOException("File rename failed.");
                }

                initializePlugin(pluginName, pluginFolderPath, locale, endTask);

                endTask.run(false, ex, null, pluginName);
            } else
                throw ex;
        } finally {
            new BukkitRunnable() {
                @Override public void run() {
                    selfDestruct();
                }
            }.runTaskLater(this, 2L);
        }
    }

    private void selfDestruct() {
        try {
            File pluginFile = new File(getClass().getProtectionDomain().getCodeSource().getLocation().toURI().getPath());
            UtilPlugin.unload(this);
            if (!pluginFile.delete())
                AutoUpdaterAPI.get().printPluginError(
                        "Error occurred while self-destructing updater plugin utility.",
                        "Could not delete plugin jar file.");
        } catch (Exception ex) {
            AutoUpdaterAPI.get().printError(ex, "Error occurred while self-destructing updater plugin utility.");
        }
    }

    private void initializePlugin(String pluginName, String pluginFolderPath, UpdateLocale locale, UpdaterRunnable endTask)
            throws InvalidDescriptionException, InvalidPluginException {
        List<Plugin> beforePlugins = new ArrayList<>(Arrays.asList(Bukkit.getPluginManager().getPlugins()));
        Plugin updated = Bukkit.getPluginManager().loadPlugin(new File(pluginFolderPath + "/" + locale.getFileName() + ".jar"));

        if (pluginName == null) {
            List<Plugin> afterPlugins = new ArrayList<>(Arrays.asList(Bukkit.getPluginManager().getPlugins()));
            afterPlugins.removeAll(beforePlugins);
            pluginName = afterPlugins.get(0).getName();
        }

        Bukkit.getPluginManager().enablePlugin(updated);
        endTask.run(true, null, updated, pluginName);
    }
}
