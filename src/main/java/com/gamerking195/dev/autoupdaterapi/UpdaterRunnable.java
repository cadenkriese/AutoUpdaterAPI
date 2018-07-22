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
    void run(boolean successful, Exception ex, Plugin plugin);
}
