package com.gamerking195.dev.autoupdaterapi;

import org.bukkit.plugin.Plugin;

/**
 * Created by Caden Kriese (flogic) on 10/12/17.
 * <p>
 * License is specified by the distributor which this
 * file was written for. Otherwise it can be found in the LICENSE file.
 * If there is no license file the code is then completely copyrighted
 * and you must contact me before using it IN ANY WAY.
 */
public interface UpdaterRunnable {
    /**
     * Runs the runnable.
     *
     * @param successful Was the update a success (true) or failure (false).
     * @param ex If the update was a failure, the exception that was created.
     * @param plugin The plugin that was updated, this will be null if the update failed.
     * @param pluginName The name of the plugin updated, this should never be null.
     */
    void run(boolean successful, Exception ex, Plugin plugin, String pluginName);
}
