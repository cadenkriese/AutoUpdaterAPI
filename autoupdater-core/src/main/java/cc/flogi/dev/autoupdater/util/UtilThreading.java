package cc.flogi.dev.autoupdater.util;

import cc.flogi.dev.autoupdater.AutoUpdaterAPI;
import org.bukkit.Bukkit;

/**
 * @author Caden Kriese (flogic)
 *
 * Created on 12/12/19
 */
public class UtilThreading {
    public static void async(Runnable runnable) {
        Bukkit.getScheduler().runTaskAsynchronously(AutoUpdaterAPI.getPlugin(), runnable);
    }

    public static void asyncDelayed(Runnable runnable, long delay) {
        Bukkit.getScheduler().runTaskLaterAsynchronously(AutoUpdaterAPI.getPlugin(), runnable, delay);
    }

    public static void asyncRepeating(Runnable runnable, long delay, long period) {
        Bukkit.getScheduler().runTaskTimerAsynchronously(AutoUpdaterAPI.getPlugin(), runnable, delay, period);
    }

    public static void sync(Runnable runnable) {
        Bukkit.getScheduler().runTask(AutoUpdaterAPI.getPlugin(), runnable);
    }

    public static void syncDelayed(Runnable runnable, long delay) {
        Bukkit.getScheduler().runTaskLater(AutoUpdaterAPI.getPlugin(), runnable, delay);
    }

    public static void syncRepeating(Runnable runnable, long delay, long period) {
        Bukkit.getScheduler().runTaskTimer(AutoUpdaterAPI.getPlugin(), runnable, delay, period);
    }
}
