package cc.flogi.dev.autoupdater.plugin;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.plugin.Plugin;

/**
 * @author Caden Kriese (flogic)
 *
 * Duplicate class for purposes of maintaining entirely seperate classes
 * from the internal module.
 *
 * Created on 1/19/2020
 */
@AllArgsConstructor @Getter @Setter @Builder
public class UpdaterRunnable {
    private Boolean successful;
    private Exception exception;
    private Plugin plugin;
    private String pluginName;
    private Runnable runnable;
    /**
     * Runs the runnable.
     *
     * @implSpec Should handle a null pluginName and plugin if ${@code successful == false}.
     */
    public void run() { runnable.run(); }
}
