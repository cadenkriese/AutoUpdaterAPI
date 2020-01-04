package cc.flogi.dev.autoupdater;

import org.bukkit.Bukkit;

/**
 * @author Caden Kriese (flogic)
 *
 * Created on 12/12/19
 */
final class UtilThreading {
    static void async(Runnable runnable) {
        Bukkit.getScheduler().runTaskAsynchronously(AutoUpdaterInternal.getPlugin(), runnable);
    }

    static void asyncDelayed(Runnable runnable, long delay) {
        Bukkit.getScheduler().runTaskLaterAsynchronously(AutoUpdaterInternal.getPlugin(), runnable, delay);
    }

    static void asyncRepeating(Runnable runnable, long delay, long period) {
        Bukkit.getScheduler().runTaskTimerAsynchronously(AutoUpdaterInternal.getPlugin(), runnable, delay, period);
    }

    static void sync(Runnable runnable) {
        Bukkit.getScheduler().runTask(AutoUpdaterInternal.getPlugin(), runnable);
    }

    static void syncDelayed(Runnable runnable, long delay) {
        Bukkit.getScheduler().runTaskLater(AutoUpdaterInternal.getPlugin(), runnable, delay);
    }

    static void syncRepeating(Runnable runnable, long delay, long period) {
        Bukkit.getScheduler().runTaskTimer(AutoUpdaterInternal.getPlugin(), runnable, delay, period);
    }
}
