package cc.flogi.dev.autoupdater;

import org.bukkit.Bukkit;

/**
 * @author Caden Kriese (flogic)
 *
 * Created on 12/12/19
 */
public final class UtilThreading {
    public static void async(Runnable runnable) {
        Bukkit.getScheduler().runTaskAsynchronously(InternalCore.getPlugin(), runnable);
    }

    public static void asyncDelayed(Runnable runnable, long delay) {
        Bukkit.getScheduler().runTaskLaterAsynchronously(InternalCore.getPlugin(), runnable, delay);
    }

    public static void asyncRepeating(Runnable runnable, long delay, long period) {
        Bukkit.getScheduler().runTaskTimerAsynchronously(InternalCore.getPlugin(), runnable, delay, period);
    }

    public static void sync(Runnable runnable) {
        Bukkit.getScheduler().runTask(InternalCore.getPlugin(), runnable);
    }

    public static void syncDelayed(Runnable runnable, long delay) {
        Bukkit.getScheduler().runTaskLater(InternalCore.getPlugin(), runnable, delay);
    }

    public static void syncRepeating(Runnable runnable, long delay, long period) {
        Bukkit.getScheduler().runTaskTimer(InternalCore.getPlugin(), runnable, delay, period);
    }
}
