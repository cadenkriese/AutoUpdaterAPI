package cc.flogi.dev.autoupdater;

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
     * @param exception  If the update was a failure, the exception that was created.
     * @param plugin     The plugin that was updated, this will be null if the update failed.
     * @param pluginName The name of the plugin updated, this should never be null.
     *
     * @implSpec Should handle a null pluginName and plugin if ${@code successful == false}.
     */
    void run(boolean successful, Exception exception, Plugin plugin, String pluginName);
}
