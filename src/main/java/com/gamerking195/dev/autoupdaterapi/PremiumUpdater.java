package com.gamerking195.dev.autoupdaterapi;

import be.maximvdw.spigotsite.api.exceptions.ConnectionFailedException;
import be.maximvdw.spigotsite.api.resource.Resource;
import be.maximvdw.spigotsite.api.user.User;
import be.maximvdw.spigotsite.api.user.exceptions.InvalidCredentialsException;
import be.maximvdw.spigotsite.api.user.exceptions.TwoFactorAuthenticationException;
import be.maximvdw.spigotsite.user.SpigotUser;
import com.gamerking195.dev.autoupdaterapi.util.UtilPlugin;
import com.gamerking195.dev.autoupdaterapi.util.UtilReader;
import com.gamerking195.dev.autoupdaterapi.util.UtilSpigotCreds;
import com.gamerking195.dev.autoupdaterapi.util.UtilUI;
import com.gargoylesoftware.htmlunit.Page;
import com.gargoylesoftware.htmlunit.UnexpectedPage;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.WebResponse;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.gargoylesoftware.htmlunit.util.Cookie;
import net.wesjd.anvilgui.AnvilGUI;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
/**
 * @author Caden Kriese (flogic)
 *
 * Created on 6/6/17
 */
public class PremiumUpdater {
    private Player initiator;

    private Plugin plugin;

    private String dataFolderPath;
    private String currentVersion;
    private String pluginName;

    private User spigotUser;
    private Resource resource;

    private UpdateLocale locale;

    private boolean deleteUpdater;
    private boolean deleteOld;

    private int resourceId;
    private int loginAttempts;

    private long startingTime;

    private UpdaterRunnable endTask;

    /**
     * Instantiate PremiumUpdater
     *
     * @param initiator     The player that started this action (if there is none set to null).
     * @param plugin        The instance of the outdated plugin.
     * @param resourceId    The ID of the plugin on Spigot found in the url after the name.
     * @param locale        The locale file you want containing custom messages. Note most messages will be followed with a progress indicator like [DOWNLOADING].
     * @param deleteUpdater Should the updater delete itself after the update fails / succeeds.
     * @param deleteOld     Should the old version of the plugin be deleted & disabled.
     */
    public PremiumUpdater(Player initiator, Plugin plugin, int resourceId, UpdateLocale locale, boolean deleteUpdater, boolean deleteOld) {
        spigotUser = AutoUpdaterAPI.getInstance().getCurrentUser();
        dataFolderPath = AutoUpdaterAPI.getInstance().getDataFolder().getPath();
        currentVersion = plugin.getDescription().getVersion();
        loginAttempts = 1;
        this.resourceId = resourceId;
        this.plugin = plugin;
        this.initiator = initiator;
        this.locale = locale;
        this.deleteUpdater = deleteUpdater;
        this.deleteOld = deleteOld;
        endTask = (successful, ex, updatedPlugin, pluginName) -> {
        };

        if (locale.getPluginName() != null) {
            pluginName = locale.getPluginName().replace("%plugin%", plugin.getName()).replace("%old_version%", currentVersion);
            locale.setPluginName(locale.getPluginName().replace("%plugin%", plugin.getName()).replace("%old_version%", currentVersion));
        } else {
            pluginName = null;
        }
    }

    /**
     * Instantiate PremiumUpdater
     *
     * @param initiator     The player that started this action (if there is none set to null).
     * @param plugin        The instance of the outdated plugin.
     * @param resourceId    The ID of the plugin on Spigot found in the url after the name.
     * @param locale        The locale file you want containing custom messages. Note most messages will be followed with a progress indicator like [DOWNLOADING].
     * @param deleteUpdater Should the updater delete itself after the update fails / succeeds.
     * @param deleteOld     Should the old version of the plugin be deleted & disabled.
     * @param endTask       Runnable that will run once the update has completed.
     */
    public PremiumUpdater(Player initiator, Plugin plugin, int resourceId, UpdateLocale locale, boolean deleteUpdater, boolean deleteOld, UpdaterRunnable endTask) {
        spigotUser = AutoUpdaterAPI.getInstance().getCurrentUser();
        dataFolderPath = AutoUpdaterAPI.getInstance().getDataFolder().getPath();
        currentVersion = plugin.getDescription().getVersion();
        loginAttempts = 1;
        this.resourceId = resourceId;
        this.plugin = plugin;
        this.initiator = initiator;
        this.locale = locale;
        this.deleteUpdater = deleteUpdater;
        this.deleteOld = deleteOld;
        this.endTask = endTask;


        if (locale.getPluginName() != null) {
            pluginName = locale.getPluginName().replace("%plugin%", plugin.getName()).replace("%old_version%", currentVersion);
            locale.setPluginName(locale.getPluginName().replace("%plugin%", plugin.getName()).replace("%old_version%", currentVersion));
        } else {
            pluginName = null;
        }
    }

    public String getLatestVersion() {
        try {
            return UtilReader.readFrom("https://api.spigotmc.org/legacy/update.php?resource=" + resourceId);
        } catch (Exception exception) {
            AutoUpdaterAPI.getInstance().printError(exception);
            UtilUI.sendActionBarSync(initiator, locale.getUpdateFailed().replace("%plugin%", plugin.getName()).replace("%old_version%", currentVersion).replace("%new_version%", "&4NULL"));
        }

        return "";
    }

    /**
     * Updates the plugin.
     */
    public void update() {
        startingTime = System.currentTimeMillis();

        UtilUI.sendActionBarSync(initiator, locale.getUpdatingNoVar() + " &8[RETRIEVING PLUGIN INFO]");

        String newVersion = getLatestVersion();

        if (currentVersion.equals(newVersion)) {
            UtilUI.sendActionBarSync(initiator, "&c&lUPDATE FAILED &8[NO UPDATES AVAILABLE]");
            endTask.run(false, null, Bukkit.getPluginManager().getPlugin(pluginName), pluginName);
            delete();
            return;
        }

        spigotUser = AutoUpdaterAPI.getInstance().getCurrentUser();

        if (locale.getPluginName() != null)
            pluginName = locale.getPluginName().replace("%plugin%", plugin.getName()).replace("%old_version%", currentVersion).replace("%new_version%", newVersion);

        locale.setFileName(locale.getFileName().replace("%plugin%", plugin.getName()).replace("%old_version%", currentVersion).replace("%new_version%", newVersion).replace(" ", "_"));

        if (spigotUser == null) {
            authenticate(true);
            UtilUI.sendActionBarSync(initiator, locale.getUpdatingNoVar() + " &8[AUTHENTICATING SPIGOT ACCOUNT]");
            return;
        }

        try {
            for (Resource resource : AutoUpdaterAPI.getInstance().getApi().getResourceManager().getPurchasedResources(spigotUser)) {
                if (resource.getResourceId() == resourceId) {
                    this.resource = resource;
                }
            }
        } catch (ConnectionFailedException ex) {
            UtilUI.sendActionBarSync(initiator, locale.getUpdateFailedNoVar());
            AutoUpdaterAPI.getInstance().printError(ex, "Error occurred while connecting to spigot. (#1)");
            endTask.run(false, ex, null, pluginName);
            delete();
        }

        if (resource == null) {
            AutoUpdaterAPI.getInstance().printPluginError("Error occurred while updating " + pluginName + "!", "That plugin has not been bought by the current user!");
            UtilUI.sendActionBarSync(initiator, "&c&lUPDATE FAILED &8[YOU HAVE NOT BOUGHT THAT PLUGIN]");
            endTask.run(false, null, Bukkit.getPluginManager().getPlugin(pluginName), pluginName);
            delete();
            return;
        }

        new BukkitRunnable() {
            @Override
            public void run() {
                try {
                    UtilUI.sendActionBar(initiator, locale.getUpdating().replace("%plugin%", plugin.getName()).replace("%old_version%", currentVersion).replace("%new_version%", newVersion) + " &8[ATTEMPTING DOWNLOAD]");

                    Map<String, String> cookies = ((SpigotUser) spigotUser).getCookies();

                    WebClient webClient = AutoUpdaterAPI.getInstance().getWebClient();

                    for (Map.Entry<String, String> entry : cookies.entrySet())
                        webClient.getCookieManager().addCookie(new Cookie("spigotmc.org", entry.getKey(), entry.getValue()));

                    webClient.getOptions().setThrowExceptionOnFailingStatusCode(false);

                    Page page = webClient.getPage(AutoUpdaterAPI.getInstance().getApi().getResourceManager().getResourceById(resourceId, spigotUser).getDownloadURL());

                    webClient.waitForBackgroundJavaScript(10_000);

                    WebResponse response = page.getEnclosingWindow().getEnclosedPage().getWebResponse();

                    if (page instanceof HtmlPage && AutoUpdaterAPI.getInstance().isDebug()) {
                        AutoUpdaterAPI.getInstance().getLogger().info("---- EARLY HTML OUTPUT ----");
                        AutoUpdaterAPI.getInstance().getLogger().info("");
                        AutoUpdaterAPI.getInstance().getLogger().info("PAGETYPE = HtmlPage");
                        HtmlPage htmlPage = (HtmlPage) page;

                        AutoUpdaterAPI.getInstance().getLogger().info("PREVIOUS STATUS CODE = " + htmlPage.getWebResponse().getStatusCode());
                        AutoUpdaterAPI.getInstance().getLogger().info("HISTORY = " + htmlPage.getEnclosingWindow().getHistory().toString());
                        AutoUpdaterAPI.getInstance().getLogger().info("PREVIOUS STATUS CODE = " + htmlPage.getWebResponse().getStatusCode());
                        AutoUpdaterAPI.getInstance().getLogger().info("STATUS CODE = " + htmlPage.getEnclosingWindow().getEnclosedPage().getWebResponse().getStatusCode());
                        htmlPage.getEnclosingWindow().getEnclosedPage().getWebResponse().getResponseHeaders().forEach(nvpair -> AutoUpdaterAPI.getInstance().getLogger().info(nvpair.getName() + " | " + nvpair.getValue()));
                        AutoUpdaterAPI.getInstance().getLogger().info("");
                        AutoUpdaterAPI.getInstance().getLogger().info("---- EARLY HTML OUTPUT ----");

                        response = htmlPage.getEnclosingWindow().getEnclosedPage().getWebResponse();
                    }

                    String contentLength = response.getResponseHeaderValue("Content-Length");

                    int completeFileSize = 0;

                    if (contentLength != null)
                        completeFileSize = Integer.parseInt(contentLength);

                    if (AutoUpdaterAPI.getInstance().isDebug()) {
                        AutoUpdaterAPI.getInstance().getLogger().info("");
                        AutoUpdaterAPI.getInstance().getLogger().info("");
                        AutoUpdaterAPI.getInstance().getLogger().info("");
                        AutoUpdaterAPI.getInstance().getLogger().info("");
                        AutoUpdaterAPI.getInstance().getLogger().info("");
                        AutoUpdaterAPI.getInstance().getLogger().info("");
                        AutoUpdaterAPI.getInstance().getLogger().info("");
                        AutoUpdaterAPI.getInstance().getLogger().info("");
                        AutoUpdaterAPI.getInstance().getLogger().info("============== BEGIN PREMIUM PLUGIN DEBUG ==============");
                        if (pluginName != null)
                            AutoUpdaterAPI.getInstance().getLogger().info("PLUGIN = " + pluginName);

                        if (page instanceof HtmlPage) {
                            AutoUpdaterAPI.getInstance().getLogger().info("");
                            AutoUpdaterAPI.getInstance().getLogger().info("");
                            AutoUpdaterAPI.getInstance().getLogger().info("PAGETYPE = HtmlPage");
                            HtmlPage htmlPage = (HtmlPage) page;

                            AutoUpdaterAPI.getInstance().getLogger().info("PREVIOUS STATUS CODE = " + htmlPage.getWebResponse().getStatusCode());
                            AutoUpdaterAPI.getInstance().getLogger().info("HISTORY = " + htmlPage.getEnclosingWindow().getHistory().toString());
                            AutoUpdaterAPI.getInstance().getLogger().info("PREVIOUS STATUS CODE = " + htmlPage.getWebResponse().getStatusCode());
                            AutoUpdaterAPI.getInstance().getLogger().info("STATUS CODE = " + htmlPage.getEnclosingWindow().getEnclosedPage().getWebResponse().getStatusCode());
                            htmlPage.getEnclosingWindow().getEnclosedPage().getWebResponse().getResponseHeaders().forEach(nvpair -> AutoUpdaterAPI.getInstance().getLogger().info(nvpair.getName() + " | " + nvpair.getValue()));
                        } else if (page instanceof UnexpectedPage) {
                            AutoUpdaterAPI.getInstance().getLogger().info("");
                            AutoUpdaterAPI.getInstance().getLogger().info("");
                            AutoUpdaterAPI.getInstance().getLogger().info("PAGETYPE = UnexpectedPage");
                            UnexpectedPage unexpectedPage = (UnexpectedPage) page;
                            AutoUpdaterAPI.getInstance().getLogger().info("PREVIOUS STATUS CODE = " + unexpectedPage.getWebResponse().getStatusCode());
                            AutoUpdaterAPI.getInstance().getLogger().info("HISTORY = " + unexpectedPage.getEnclosingWindow().getHistory().toString());
                            AutoUpdaterAPI.getInstance().getLogger().info("PREVIOUS STATUS CODE = " + unexpectedPage.getWebResponse().getStatusCode());
                            AutoUpdaterAPI.getInstance().getLogger().info("STATUS CODE = " + unexpectedPage.getEnclosingWindow().getEnclosedPage().getWebResponse().getStatusCode());
                            AutoUpdaterAPI.getInstance().getLogger().info("NAME | VALUE");
                            unexpectedPage.getEnclosingWindow().getEnclosedPage().getWebResponse().getResponseHeaders().forEach(nvpair -> AutoUpdaterAPI.getInstance().getLogger().info(nvpair.getName() + " | " + nvpair.getValue()));
                        }

                        AutoUpdaterAPI.getInstance().getLogger().info("");
                        AutoUpdaterAPI.getInstance().getLogger().info("");
                        AutoUpdaterAPI.getInstance().getLogger().info("PAGETYPE = Page");
                        AutoUpdaterAPI.getInstance().getLogger().info("HISTORY = " + page.getEnclosingWindow().getHistory().toString());
                        AutoUpdaterAPI.getInstance().getLogger().info("PREVIOUS STATUS CODE = " + page.getWebResponse().getStatusCode());
                        AutoUpdaterAPI.getInstance().getLogger().info("STATUS CODE = " + response.getStatusCode());
                        AutoUpdaterAPI.getInstance().getLogger().info("LOAD TIME = " + response.getLoadTime());
                        AutoUpdaterAPI.getInstance().getLogger().info("CURRENT URL = " + webClient.getCurrentWindow().getEnclosedPage().getUrl());
                        AutoUpdaterAPI.getInstance().getLogger().info("NAME | VALUE");
                        response.getResponseHeaders().forEach(nvpair -> AutoUpdaterAPI.getInstance().getLogger().info(nvpair.getName() + " | " + nvpair.getValue()));
                    }

                    int grabSize = 2048;

                    BufferedInputStream in = new java.io.BufferedInputStream(response.getContentAsStream());
                    java.io.FileOutputStream fos = new java.io.FileOutputStream(new File(dataFolderPath.substring(0, dataFolderPath.lastIndexOf("/")) + "/" + locale.getFileName() + ".jar"));
                    java.io.BufferedOutputStream bout = new BufferedOutputStream(fos, grabSize);

                    byte[] data = new byte[grabSize];
                    long downloadedFileSize = 0;
                    int grabbed;
                    while ((grabbed = in.read(data, 0, grabSize)) >= 0) {

                        //if (AutoUpdaterAPI.getInstance().isDebug())
                        //AutoUpdaterAPI.getInstance().getLogger().info(System.currentTimeMillis()+" - GRABBED "+grabbed+"/"+grabSize+" ("+String.format("%.2f", (((double) grabbed) / ((double) grabSize)) * 100)+"%)");

                        downloadedFileSize += grabbed;

                        //Don't send action bar for every byte of data we're not trying to crash any clients (or servers) here.
                        if (downloadedFileSize % (grabSize * 10) == 0 && completeFileSize > 0) {
                            final int currentProgress = (int) ((((double) downloadedFileSize) / ((double) completeFileSize)) * 15);

                            final String currentPercent = String.format("%.2f", (((double) downloadedFileSize) / ((double) completeFileSize)) * 100);

                            String bar = "&a:::::::::::::::";

                            bar = bar.substring(0, currentProgress + 2) + "&c" + bar.substring(currentProgress + 2);

                            UtilUI.sendActionBar(initiator, locale.getUpdatingDownload().replace("%plugin%", plugin.getName()).replace("%old_version%", currentVersion).replace("%new_version%", newVersion).replace("%download_bar%", bar).replace("%download_percent%", currentPercent + "%") + " &8[DOWNLOADING RESOURCE]");
                        }

                        bout.write(data, 0, grabbed);
                    }

                    bout.close();
                    in.close();
                    fos.close();

                    if (AutoUpdaterAPI.getInstance().isDebug()) {
                        AutoUpdaterAPI.getInstance().getLogger().info("FINISHED WITH " + downloadedFileSize + "/" + completeFileSize + " (" + String.format("%.2f", (((double) downloadedFileSize) / ((double) completeFileSize)) * 100) + "%)");
                        AutoUpdaterAPI.getInstance().getLogger().info("============== END PREMIUM PLUGIN DEBUG =======");
                    }

                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            try {
                                if (deleteOld) {
                                    File pluginFile = new File(plugin.getClass().getProtectionDomain().getCodeSource().getLocation().toURI().getPath());

                                    UtilPlugin.unload(plugin);

                                    if (!pluginFile.delete())
                                        AutoUpdaterAPI.getInstance().printPluginError("Error occurred while updating " + pluginName + ".", "Could not delete old plugin jar.");
                                }

                                UtilUI.sendActionBar(initiator, locale.getUpdating().replace("%plugin%", plugin.getName()).replace("%old_version%", currentVersion).replace("%new_version%", newVersion) + " &8[INITIALIZING]");

                                List<Plugin> beforePlugins = new ArrayList<>(Arrays.asList(Bukkit.getPluginManager().getPlugins()));

                                Bukkit.getPluginManager().loadPlugin(new File(dataFolderPath.substring(0, dataFolderPath.lastIndexOf("/")) + "/" + locale.getFileName() + ".jar"));

                                if (pluginName == null) {
                                    List<Plugin> afterPlugins = new ArrayList<>(Arrays.asList(Bukkit.getPluginManager().getPlugins()));
                                    afterPlugins.removeAll(beforePlugins);
                                    pluginName = afterPlugins.get(0).getName();
                                }

                                Bukkit.getPluginManager().enablePlugin(Bukkit.getPluginManager().getPlugin(pluginName));

                                double elapsedTimeSeconds = (double) (System.currentTimeMillis() - startingTime) / 1000;
                                UtilUI.sendActionBar(initiator, locale.getUpdateComplete().replace("%plugin%", plugin.getName()).replace("%old_version%", currentVersion).replace("%new_version%", newVersion).replace("%elapsed_time%", String.format("%.2f", elapsedTimeSeconds)));

                                endTask.run(true, null, Bukkit.getPluginManager().getPlugin(pluginName), pluginName);
                                delete();
                            } catch (Exception ex) {
                                UtilUI.sendActionBar(initiator, locale.getUpdateFailedNoVar());
                                AutoUpdaterAPI.getInstance().printError(ex, "Error occurred while updating premium resource.");
                                if (pluginName != null) {
                                    endTask.run(false, ex, Bukkit.getPluginManager().getPlugin(pluginName), pluginName);
                                }
                                delete();
                            }
                        }
                    }.runTask(AutoUpdaterAPI.getInstance());
                } catch (Exception ex) {
                    if (AutoUpdaterAPI.getInstance().isDebug())
                        AutoUpdaterAPI.getInstance().getLogger().info("============== END PREMIUM PLUGIN DEBUG =======");

                    UtilUI.sendActionBar(initiator, locale.getUpdateFailedNoVar());
                    AutoUpdaterAPI.getInstance().printError(ex, "Error occurred while updating premium resource.");
                    if (pluginName != null)
                        endTask.run(false, ex, Bukkit.getPluginManager().getPlugin(pluginName), pluginName);
                    delete();
                }
            }
        }.runTaskAsynchronously(AutoUpdaterAPI.getInstance());
    }

    public void authenticate(boolean recall) {
        new BukkitRunnable() {
            @Override
            public void run() {
                UtilUI.sendActionBar(initiator, locale.getUpdatingNoVar() + " &8[ATTEMPTING DECRYPT]");

                String username = UtilSpigotCreds.getInstance().getUsername();
                String password = UtilSpigotCreds.getInstance().getPassword();
                String twoFactor = UtilSpigotCreds.getInstance().getTwoFactor();

                if (username == null || password == null) {
                    runGuis(recall);
                    return;
                }

                try {
                    UtilUI.sendActionBar(initiator, locale.getUpdatingNoVar() + " &8[ATTEMPTING AUTHENTICATION]");
                    spigotUser = AutoUpdaterAPI.getInstance().getApi().getUserManager().authenticate(username, password);

                    if (spigotUser == null) {
                        UtilUI.sendActionBar(initiator, locale.getUpdatingNoVar() + "&c [INVALID CACHED CREDENTIALS]");
                        UtilSpigotCreds.getInstance().clearFile();
                        runGuis(recall);
                        return;
                    }

                    AutoUpdaterAPI.getInstance().setCurrentUser(spigotUser);
                    UtilUI.sendActionBar(initiator, locale.getUpdatingNoVar() + " &8[AUTHENTICATION SUCCESSFUL]");
                    AutoUpdaterAPI.getInstance().getLogger().info("Successfully logged in to Spigot as user '" + username + "'.");

                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            try {
                                if (recall)
                                    update();
                            } catch (Exception ex) {
                                AutoUpdaterAPI.getInstance().printError(ex);
                            }
                        }
                    }.runTaskLater(AutoUpdaterAPI.getInstance(), 40L);

                } catch (Exception ex) {
                    if (ex instanceof TwoFactorAuthenticationException) {
                        try {
                            UtilUI.sendActionBar(initiator, locale.getUpdatingNoVar() + " &8[RE-ATTEMPTING AUTHENTICATION]");
                            if (twoFactor == null) {
                                runGuis(recall);
                                return;
                            }

                            spigotUser = AutoUpdaterAPI.getInstance().getApi().getUserManager().authenticate(username, password, twoFactor);

                            if (spigotUser == null) {
                                UtilUI.sendActionBar(initiator, locale.getUpdatingNoVar() + " &c[INVALID CACHED CREDENTIALS]");
                                UtilSpigotCreds.getInstance().clearFile();
                                runGuis(recall);
                                return;
                            }

                            AutoUpdaterAPI.getInstance().setCurrentUser(spigotUser);
                            UtilUI.sendActionBar(initiator, locale.getUpdatingNoVar() + " &8[AUTHENTICATION SUCCESSFUL]");
                            AutoUpdaterAPI.getInstance().getLogger().info("Successfully logged in to Spigot as user '" + username + "'.");

                            new BukkitRunnable() {
                                @Override
                                public void run() {
                                    try {
                                        if (recall)
                                            update();
                                    } catch (Exception ex) {
                                        AutoUpdaterAPI.getInstance().printError(ex);
                                    }
                                }
                            }.runTask(AutoUpdaterAPI.getInstance());
                        } catch (Exception otherException) {
                            if (otherException instanceof InvalidCredentialsException) {
                                UtilUI.sendActionBar(initiator, locale.getUpdatingNoVar() + " &c[INVALID CACHED CREDENTIALS]");
                                UtilSpigotCreds.getInstance().clearFile();
                                runGuis(recall);
                            } else if (otherException instanceof ConnectionFailedException) {
                                UtilUI.sendActionBar(initiator, locale.getUpdateFailedNoVar());
                                AutoUpdaterAPI.getInstance().printError(ex, "Error occurred while connecting to spigot. (#6)");
                                endTask.run(false, otherException, null, pluginName);
                                delete();
                            } else if (otherException instanceof TwoFactorAuthenticationException) {
                                if (loginAttempts < 4) {
                                    UtilUI.sendActionBar(initiator, locale.getUpdatingNoVar() + " &8[RE-TRYING LOGIN IN 5s ATTEMPT #" + loginAttempts + "]");
                                    loginAttempts++;
                                    new BukkitRunnable() {
                                        @Override
                                        public void run() {
                                            authenticate(recall);
                                        }
                                    }.runTaskLater(AutoUpdaterAPI.getInstance(), 100L);
                                } else {
                                    loginAttempts = 1;
                                    AutoUpdaterAPI.getInstance().printError(otherException);
                                }
                            } else {
                                AutoUpdaterAPI.getInstance().printError(otherException);
                            }
                        }
                    } else if (ex instanceof InvalidCredentialsException) {
                        UtilUI.sendActionBar(initiator, locale.getUpdatingNoVar() + " &c[INVALID CACHED CREDENTIALS]");
                        UtilSpigotCreds.getInstance().clearFile();
                        runGuis(recall);
                    } else if (ex instanceof ConnectionFailedException) {
                        UtilUI.sendActionBar(initiator, locale.getUpdatingNoVar() + " &8[RE-ATTEMPTING AUTHENTICATION]");
                        AutoUpdaterAPI.getInstance().printError(ex, "Error occurred while connecting to spigot. (#2)");
                        endTask.run(false, ex, null, pluginName);
                        delete();
                    } else {
                        AutoUpdaterAPI.getInstance().printError(ex);
                    }
                }
            }
        }.runTaskAsynchronously(AutoUpdaterAPI.getInstance());
    }

    private void runGuis(boolean recall) {
        new BukkitRunnable() {
            @Override
            public void run() {
                UtilUI.sendActionBarSync(initiator, locale.getUpdatingNoVar() + " &8[RETRIEVING USERNAME]");
                new AnvilGUI(AutoUpdaterAPI.getInstance(), initiator, "Spigot username", (Player player1, String usernameInput) -> {
                    try {
                        if (AutoUpdaterAPI.getInstance().getApi().getUserManager().getUserByName(usernameInput) != null) {
                            UtilUI.sendActionBarSync(initiator, locale.getUpdatingNoVar() + " &8[RETRIEVING PASSWORD]");
                            new AnvilGUI(AutoUpdaterAPI.getInstance(), initiator, "Spigot password", (Player player2, String passwordInput) -> {
                                try {
                                    spigotUser = AutoUpdaterAPI.getInstance().getApi().getUserManager().authenticate(usernameInput, passwordInput);

                                    UtilUI.sendActionBarSync(initiator, locale.getUpdatingNoVar() + " &8[ENCRYPTING CREDENTIALS]");
                                    UtilSpigotCreds.getInstance().setUsername(usernameInput);
                                    UtilSpigotCreds.getInstance().setPassword(passwordInput);
                                    UtilSpigotCreds.getInstance().saveFile();

                                    new BukkitRunnable() {
                                        @Override
                                        public void run() {
                                            authenticate(recall);
                                        }
                                    }.runTaskLater(AutoUpdaterAPI.getInstance(), 200L);
                                    player2.closeInventory();

                                } catch (TwoFactorAuthenticationException ex) {
                                    UtilUI.sendActionBarSync(initiator, locale.getUpdatingNoVar() + " &8[RETRIEVING TWO FACTOR SECRET]");
                                    new AnvilGUI(AutoUpdaterAPI.getInstance(), initiator, "Spigot two factor secret", (Player player3, String twoFactorInput) -> {
                                        try {
                                            //Make extra string because the input seems to change for some reason.
                                            final String twoFactorSecret = twoFactorInput;
                                            spigotUser = AutoUpdaterAPI.getInstance().getApi().getUserManager().authenticate(usernameInput, passwordInput, twoFactorSecret);

                                            UtilUI.sendActionBarSync(initiator, locale.getUpdatingNoVar() + " &8[ENCRYPTING CREDENTIALS]");

                                            UtilSpigotCreds.getInstance().setUsername(usernameInput);
                                            UtilSpigotCreds.getInstance().setPassword(passwordInput);
                                            UtilSpigotCreds.getInstance().setTwoFactor(twoFactorSecret);
                                            UtilSpigotCreds.getInstance().saveFile();

                                            new BukkitRunnable() {
                                                @Override
                                                public void run() {
                                                    authenticate(recall);
                                                }
                                            }.runTaskLater(AutoUpdaterAPI.getInstance(), 200L);
                                            player3.closeInventory();

                                            return "Retrieved credentials you may now close this GUI.";
                                        } catch (Exception exception) {
                                            UtilUI.sendActionBarSync(initiator, locale.getUpdateFailedNoVar());
                                            AutoUpdaterAPI.getInstance().printError(exception, "Error occurred while authenticating Spigot user.");
                                            endTask.run(false, exception, null, pluginName);
                                            delete();
                                            return "Authentication failed";
                                        }
                                    });
                                } catch (ConnectionFailedException ex) {
                                    UtilUI.sendActionBarSync(initiator, locale.getUpdateFailedNoVar());
                                    AutoUpdaterAPI.getInstance().printError(ex, "Error occurred while connecting to Spigot. (#3)");
                                    endTask.run(false, ex, null, pluginName);
                                    delete();
                                    return "Could not connect to Spigot";
                                } catch (InvalidCredentialsException ex) {
                                    UtilUI.sendActionBarSync(initiator, locale.getUpdateFailedNoVar());
                                    endTask.run(false, ex, null, pluginName);
                                    delete();
                                    return "Invalid credentials";
                                }

                                return null;
                            });
                        } else if (usernameInput.contains("@") && usernameInput.contains(".")) {
                            initiator.closeInventory();

                            UtilUI.sendActionBarSync(initiator, "&cEmails are not supported!");

                            endTask.run(false, null, Bukkit.getPluginManager().getPlugin(pluginName), pluginName);
                            delete();
                            return "Emails are not supported!";
                        } else {
                            UtilUI.sendActionBarSync(initiator, locale.getUpdateFailedNoVar());
                            endTask.run(false, null, Bukkit.getPluginManager().getPlugin(pluginName), pluginName);
                            delete();
                            return "Invalid username!";
                        }
                    } catch (Exception ex) {
                        AutoUpdaterAPI.getInstance().printError(ex, "Error occurred while authenticating Spigot username.");
                        UtilUI.sendActionBarSync(initiator, locale.getUpdateFailedNoVar());
                        endTask.run(false, ex, null, pluginName);
                        delete();
                    }

                    return null;
                });
            }
        }.runTask(AutoUpdaterAPI.getInstance());
    }

    /*
     * Utilities
     */

    private void delete() {
        new BukkitRunnable() {
            @Override
            public void run() {
                if (deleteUpdater) {
                    try {
                        if (!new File(AutoUpdaterAPI.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath()).delete()) {
                            AutoUpdaterAPI.getInstance().printPluginError("Error occurred while updating " + pluginName + ".", "Could not delete updater jar.");
                        }

                        UtilPlugin.unload(AutoUpdaterAPI.getInstance());
                    } catch (Exception ex) {
                        AutoUpdaterAPI.getInstance().printError(ex);
                    }
                }
            }
        }.runTask(AutoUpdaterAPI.getInstance());
    }
}
