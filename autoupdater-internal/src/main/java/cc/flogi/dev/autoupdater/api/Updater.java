package cc.flogi.dev.autoupdater.api;

/**
 * @author Caden Kriese (flogic)
 *
 * Created on 12/14/19
 */
public interface Updater {
    /**
     * Retrieves the latest version of the plugin.
     *
     * @return The latest version of the plugin.
     */
    String getLatestVersion();

    /**
     * Retrieves the download URL String of the plugin.
     *
     * @return The download URL String of the plugin.
     */
    String getDownloadUrlString();

    /**
     * Performs the update task on the plugin.
     *
     * @apiNote This method automatically calls {@link Updater#downloadResource()},
     * which calls {@link Updater#initializePlugin()}, this should be the only call you need to make.
     * @implSpec Should involve calling {@link Updater#downloadResource()}.
     */
    void update();

    /**
     * Downloads the resource to the plugins directory and initializes it.
     * Uses the specified name from the relevant {@link UpdateLocale}.
     *
     * @apiNote You probably want to call the {@link Updater#update()} method, this method is only here for special cases.
     * @implSpec Should involve calling {@link Updater#initializePlugin()}.
     */
    void downloadResource();

    /**
     * Uses the bundled plugin utility to initialize and enable the downloaded plugin.
     *
     * @apiNote You probably want to call the {@link Updater#update()} method, this method is only here for special cases.
     */
    void initializePlugin();
}
