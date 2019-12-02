package cc.flogi.dev.autoupdater;

import cc.flogi.dev.autoupdater.util.UtilPlugin;
import cc.flogi.dev.autoupdater.util.UtilUI;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.plugin.InvalidDescriptionException;
import org.bukkit.plugin.InvalidPluginException;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.File;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public final class UpdaterPlugin extends JavaPlugin {
    private static UpdaterPlugin instance;

    @Override
    public void onEnable() {
        instance = this;
        getLogger().info("AutoUpdaterAPI utility enabled.");
    }

    public void updatePlugin(Plugin plugin, Player initiator, boolean deleteOld, String pluginName, String dataFolderPath, UpdateLocale locale, long startingTime, UpdaterRunnable endTask) throws URISyntaxException, InvalidDescriptionException, InvalidPluginException {
        if (deleteOld) {
            File pluginFile = new File(plugin.getClass().getProtectionDomain().getCodeSource().getLocation().toURI().getPath());
            UtilPlugin.unload(plugin);
            if (!pluginFile.delete())
                AutoUpdaterAPI.get().printPluginError("Error occurred while updating " + pluginName + ".", "Could not delete old plugin jar.");
        }

        sendActionBar(initiator, locale.getUpdating() + " &8[INITIALIZING]");

        List<Plugin> beforePlugins = new ArrayList<>(Arrays.asList(Bukkit.getPluginManager().getPlugins()));
        Plugin updated = Bukkit.getPluginManager().loadPlugin(new File(dataFolderPath.substring(0, dataFolderPath.lastIndexOf("/")) + "/" + locale.getFileName() + ".jar"));

        if (pluginName == null) {
            List<Plugin> afterPlugins = new ArrayList<>(Arrays.asList(Bukkit.getPluginManager().getPlugins()));
            afterPlugins.removeAll(beforePlugins);
            pluginName = afterPlugins.get(0).getName();
        }

        Bukkit.getPluginManager().enablePlugin(updated);
        endTask.run(true, null, updated, pluginName);
        double elapsedTimeSeconds = (double) (System.currentTimeMillis() - startingTime) / 1000;
        UtilUI.sendActionBar(initiator, locale.getUpdateComplete().replace("%elapsed_time%", String.format("%.2f", elapsedTimeSeconds)));
    }

    /**
     * Sends an action bar message to the player.
     *
     * @param player  The player to receive the message.
     * @param message The message to be sent (with color codes to be replaced).
     */
    public static void sendActionBar(Player player, String message) {
        if (!Bukkit.isPrimaryThread()) {
            new BukkitRunnable() {
                @Override public void run() {
                    player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(ChatColor.translateAlternateColorCodes('&', message)));
                }
            }.runTask(instance);
            return;
        }

        player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(ChatColor.translateAlternateColorCodes('&', message)));
    }

    public static UpdaterPlugin get() {
        return instance;
    }
}
