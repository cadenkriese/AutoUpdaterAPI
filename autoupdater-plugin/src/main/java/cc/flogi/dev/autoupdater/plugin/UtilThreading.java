package cc.flogi.dev.autoupdater.plugin;

import org.bukkit.Bukkit;

/**
 * @author Caden Kriese (flogic)
 *
 * Created on 12/12/19
 */
final class UtilThreading {
    static void async(Runnable runnable) {
        Bukkit.getScheduler().runTaskAsynchronously(UpdaterPlugin.get(), runnable);
    }

    static void asyncDelayed(Runnable runnable, long delay) {
        Bukkit.getScheduler().runTaskLaterAsynchronously(UpdaterPlugin.get(), runnable, delay);
    }

    static void asyncRepeating(Runnable runnable, long delay, long period) {
        Bukkit.getScheduler().runTaskTimerAsynchronously(UpdaterPlugin.get(), runnable, delay, period);
    }

    static void sync(Runnable runnable) {
        Bukkit.getScheduler().runTask(UpdaterPlugin.get(), runnable);
    }

    static void syncDelayed(Runnable runnable, long delay) {
        Bukkit.getScheduler().runTaskLater(UpdaterPlugin.get(), runnable, delay);
    }

    static void syncRepeating(Runnable runnable, long delay, long period) {
        Bukkit.getScheduler().runTaskTimer(UpdaterPlugin.get(), runnable, delay, period);
    }
}
