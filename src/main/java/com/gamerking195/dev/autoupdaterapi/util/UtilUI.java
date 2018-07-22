package com.gamerking195.dev.autoupdaterapi.util;

import com.gamerking195.dev.autoupdaterapi.AutoUpdaterAPI;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

/**
 * Created by Caden Kriese (flogic) on 2/27/18.
 * <p>
 * License is specified by the distributor which this
 * file was written for. Otherwise it can be found in the LICENSE file.
 * If there is no license file the code is then completely copyrighted
 * and you must contact me before using it IN ANY WAY.
 */
public class UtilUI {
    public static void sendActionBar(Player player, String message) {
        new BukkitRunnable() {
            @Override
            public void run() {
                if (player != null)
                    player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(ChatColor.translateAlternateColorCodes('&', message)));

                if (AutoUpdaterAPI.getInstance().isDebug())
                    AutoUpdaterAPI.getInstance().getLogger().info(ChatColor.stripColor(ChatColor.translateAlternateColorCodes('&', message)));

            }
        }.runTask(AutoUpdaterAPI.getInstance());
    }

    public static void sendActionBarSync(Player player, String message) {
        if (player != null)
            player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(ChatColor.translateAlternateColorCodes('&', message)));

        if (AutoUpdaterAPI.getInstance().isDebug())
            AutoUpdaterAPI.getInstance().getLogger().info(ChatColor.stripColor(ChatColor.translateAlternateColorCodes('&', message)));
    }

}
