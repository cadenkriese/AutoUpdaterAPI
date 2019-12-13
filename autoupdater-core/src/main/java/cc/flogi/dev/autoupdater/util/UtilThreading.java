package cc.flogi.dev.autoupdater.util;

import cc.flogi.dev.autoupdater.AutoUpdaterAPI;
import org.bukkit.scheduler.BukkitRunnable;

/**
 * @author Caden Kriese (flogic)
 *
 * Created on 12/12/19
 */
public class UtilThreading {
    public static void async(Runnable runnable) {
        new BukkitRunnable() {
            @Override public void run() {
                runnable.run();
            }
        }.runTaskAsynchronously(AutoUpdaterAPI.getPlugin());
    }

    public static void asyncDelayed(Runnable runnable, long delay) {
        new BukkitRunnable() {
            @Override public void run() {
                runnable.run();
            }
        }.runTaskLaterAsynchronously(AutoUpdaterAPI.getPlugin(), delay);
    }

    public static void asyncRepeating(Runnable runnable, long delay, long period) {
        new BukkitRunnable() {
            @Override public void run() {
                runnable.run();
            }
        }.runTaskTimerAsynchronously(AutoUpdaterAPI.getPlugin(), delay, period);
    }

    public static void sync(Runnable runnable) {
        new BukkitRunnable() {
            @Override public void run() {
                runnable.run();
            }
        }.runTask(AutoUpdaterAPI.getPlugin());
    }

    public static void syncDelayed(Runnable runnable, long delay) {
        new BukkitRunnable() {
            @Override public void run() {
                runnable.run();
            }
        }.runTaskLater(AutoUpdaterAPI.getPlugin(), delay);
    }

    public static void syncRepeating(Runnable runnable, long delay, long period) {
        new BukkitRunnable() {
            @Override public void run() {
                runnable.run();
            }
        }.runTaskTimer(AutoUpdaterAPI.getPlugin(), delay, period);
    }
}
