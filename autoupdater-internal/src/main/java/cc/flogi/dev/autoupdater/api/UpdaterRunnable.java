package cc.flogi.dev.autoupdater.api;

import org.bukkit.plugin.Plugin;

/**
 * @author Caden Kriese (flogic)
 *
 * Created on 10/12/17
 */
public interface UpdaterRunnable {
    /**
     * Runs the runnable.
     */
    void run();

    /**
     * @return Was the update successful. (or null if the update has not yet completed)
     */
    Boolean getSuccessful();

    /**
     * @return Any exception that occurred during the update.
     */
    Exception getException();

    /**
     * @return The updated instance of the plugin.
     */
    Plugin getPlugin();

    /**
     * @return The name of the updated plugin.
     */
    String getPluginName();

    /**
     * @return The runnable to be run on update completion or failure.
     */
    Runnable getRunnable();

    /**
     * Sets the runnable.
     *
     * @param runnable The runnable that will be run on completion.
     * @implSpec Should handle a null pluginName and plugin if ${@code successful == false}.
     */
    void setRunnable(Runnable runnable);
}
