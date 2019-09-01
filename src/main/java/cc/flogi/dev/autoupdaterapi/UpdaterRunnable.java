package cc.flogi.dev.autoupdaterapi;

import org.bukkit.plugin.Plugin;

/**
 * @author Caden Kriese (flogic)
 *
 * Created on 10/12/17
 */
public interface UpdaterRunnable {
    /**
     * Runs the runnable.
     *
     * @param successful Was the update a success (true) or failure (false).
     * @param ex         If the update was a failure, the exception that was created.
     * @param plugin     The plugin that was updated, this will be null if the update failed.
     * @param pluginName The name of the plugin updated, this should never be null.
     */
    void run(boolean successful, Exception ex, Plugin plugin, String pluginName);
}
