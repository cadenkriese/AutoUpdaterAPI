package cc.flogi.dev.autoupdater.premium;

import be.maximvdw.spigotsite.api.exceptions.ConnectionFailedException;
import be.maximvdw.spigotsite.api.resource.Resource;
import be.maximvdw.spigotsite.api.user.User;
import be.maximvdw.spigotsite.api.user.exceptions.InvalidCredentialsException;
import be.maximvdw.spigotsite.api.user.exceptions.TwoFactorAuthenticationException;
import be.maximvdw.spigotsite.user.SpigotUser;
import cc.flogi.dev.autoupdater.AutoUpdaterAPI;
import cc.flogi.dev.autoupdater.UpdateLocale;
import cc.flogi.dev.autoupdater.UpdaterRunnable;
import cc.flogi.dev.autoupdater.util.UtilPlugin;
import cc.flogi.dev.autoupdater.util.UtilReader;
import cc.flogi.dev.autoupdater.premium.util.UtilSpigotCreds;
import cc.flogi.dev.autoupdater.util.UtilUI;
import com.gargoylesoftware.htmlunit.Page;
import com.gargoylesoftware.htmlunit.UnexpectedPage;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.WebResponse;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.gargoylesoftware.htmlunit.util.Cookie;
import net.md_5.bungee.api.ChatColor;
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
     * @param deleteOld     Should the old version of the plugin be deleted disabled.
     */
    public PremiumUpdater(Player initiator, Plugin plugin, int resourceId, UpdateLocale locale, boolean deleteOld) {
        locale.updateVariables(plugin.getName(), plugin.getDescription().getVersion(), null);

        spigotUser = AutoUpdaterAPI.get().getCurrentUser();
        dataFolderPath = AutoUpdaterAPI.get().getPrivateDataFolder().getPath();
        currentVersion = plugin.getDescription().getVersion();
        loginAttempts = 1;
        pluginName = locale.getPluginName();
        this.resourceId = resourceId;
        this.plugin = plugin;
        this.initiator = initiator;
        this.locale = locale;
        this.deleteOld = deleteOld;
        endTask = (successful, ex, updatedPlugin, pluginName) -> {};
    }

    /**
     * Instantiate PremiumUpdater
     *
     * @param initiator     The player that started this action (if there is none set to null).
     * @param plugin        The instance of the outdated plugin.
     * @param resourceId    The ID of the plugin on Spigot found in the url after the name.
     * @param locale        The locale file you want containing custom messages. Note most messages will be followed with a progress indicator like [DOWNLOADING].
     * @param deleteOld     Should the old version of the plugin be deleted and disabled.
     * @param endTask       Runnable that will run once the update has completed.
     */
    public PremiumUpdater(Player initiator, Plugin plugin, int resourceId, UpdateLocale locale, boolean deleteOld, UpdaterRunnable endTask) {
        locale.updateVariables(plugin.getName(), plugin.getDescription().getVersion(), null);

        spigotUser = AutoUpdaterAPI.get().getCurrentUser();
        dataFolderPath = AutoUpdaterAPI.get().getPrivateDataFolder().getPath();
        currentVersion = plugin.getDescription().getVersion();
        loginAttempts = 1;
        pluginName = locale.getPluginName();
        this.resourceId = resourceId;
        this.plugin = plugin;
        this.initiator = initiator;
        this.locale = locale;
        this.deleteOld = deleteOld;
        this.endTask = endTask;
    }

    /**
     * Retrieves the latest version of the plugin from spigot.
     *
     * @return The latest version of the resource on spigot.
     */
    public String getLatestVersion() {
        try {
            return UtilReader.readFrom("https://api.spigotmc.org/legacy/update.php?resource=" + resourceId);
        } catch (Exception exception) {
            AutoUpdaterAPI.get().printError(exception);
            UtilUI.sendActionBar(initiator, locale.getUpdateFailed().replace("%new_version%", "&4NULL"));
        }

        return "";
    }

    /**
     * Updates the plugin.
     */
    public void update() {
        startingTime = System.currentTimeMillis();

        UtilUI.sendActionBar(initiator, locale.getUpdatingNoVar() + " &8[RETRIEVING PLUGIN INFO]");

        String newVersion = getLatestVersion();
        locale.updateVariables(plugin.getName(), currentVersion, newVersion);

        if (currentVersion.equals(newVersion)) {
            UtilUI.sendActionBar(initiator, "&c&lUPDATE FAILED &8[NO UPDATES AVAILABLE]");
            endTask.run(false, null, Bukkit.getPluginManager().getPlugin(pluginName), pluginName);
            return;
        }

        spigotUser = AutoUpdaterAPI.get().getCurrentUser();

        if (locale.getPluginName() != null)
            pluginName = locale.getPluginName();

        locale.setFileName(locale.getFileName());

        if (spigotUser == null) {
            authenticate(true);
            UtilUI.sendActionBar(initiator, locale.getUpdatingNoVar() + " &8[AUTHENTICATING SPIGOT ACCOUNT]");
            return;
        }

        try {
            for (Resource resource : AutoUpdaterAPI.get().getApi().getResourceManager().getPurchasedResources(spigotUser)) {
                if (resource.getResourceId() == resourceId) {
                    this.resource = resource;
                }
            }
        } catch (ConnectionFailedException ex) {
            UtilUI.sendActionBar(initiator, locale.getUpdateFailedNoVar());
            AutoUpdaterAPI.get().printError(ex, "Error occurred while connecting to spigot. (#1)");
            endTask.run(false, ex, null, pluginName);
        }

        if (resource == null) {
            AutoUpdaterAPI.get().printPluginError("Error occurred while updating " + pluginName + "!", "That plugin has not been bought by the current user!");
            UtilUI.sendActionBar(initiator, "&c&lUPDATE FAILED &8[YOU HAVE NOT BOUGHT THAT PLUGIN]");
            endTask.run(false, null, Bukkit.getPluginManager().getPlugin(pluginName), pluginName);
            return;
        }

        new BukkitRunnable() {
            @Override
            public void run() {
                try {
                    UtilUI.sendActionBar(initiator, locale.getUpdating() + " &8[ATTEMPTING DOWNLOAD]");

                    Map<String, String> cookies = ((SpigotUser) spigotUser).getCookies();

                    WebClient webClient = AutoUpdaterAPI.get().getWebClient();

                    for (Map.Entry<String, String> entry : cookies.entrySet())
                        webClient.getCookieManager().addCookie(new Cookie("spigotmc.org", entry.getKey(), entry.getValue()));

                    webClient.getOptions().setThrowExceptionOnFailingStatusCode(false);

                    Page page = webClient.getPage(AutoUpdaterAPI.get().getApi().getResourceManager().getResourceById(resourceId, spigotUser).getDownloadURL());

                    webClient.waitForBackgroundJavaScript(10_000);

                    WebResponse response = page.getEnclosingWindow().getEnclosedPage().getWebResponse();

                    if (page instanceof HtmlPage && AutoUpdaterAPI.DEBUG) {
                        AutoUpdaterAPI.get().getLogger().info("---- EARLY HTML OUTPUT ----");
                        AutoUpdaterAPI.get().getLogger().info("");
                        AutoUpdaterAPI.get().getLogger().info("PAGETYPE = HtmlPage");
                        HtmlPage htmlPage = (HtmlPage) page;

                        AutoUpdaterAPI.get().getLogger().info("PREVIOUS STATUS CODE = " + htmlPage.getWebResponse().getStatusCode());
                        AutoUpdaterAPI.get().getLogger().info("HISTORY = " + htmlPage.getEnclosingWindow().getHistory().toString());
                        AutoUpdaterAPI.get().getLogger().info("PREVIOUS STATUS CODE = " + htmlPage.getWebResponse().getStatusCode());
                        AutoUpdaterAPI.get().getLogger().info("STATUS CODE = " + htmlPage.getEnclosingWindow().getEnclosedPage().getWebResponse().getStatusCode());
                        htmlPage.getEnclosingWindow().getEnclosedPage().getWebResponse().getResponseHeaders().forEach(nvpair -> AutoUpdaterAPI.get().getLogger().info(nvpair.getName() + " | " + nvpair.getValue()));
                        AutoUpdaterAPI.get().getLogger().info("");
                        AutoUpdaterAPI.get().getLogger().info("---- EARLY HTML OUTPUT ----");

                        response = htmlPage.getEnclosingWindow().getEnclosedPage().getWebResponse();
                    }

                    String contentLength = response.getResponseHeaderValue("Content-Length");

                    int completeFileSize = 0;

                    if (contentLength != null)
                        completeFileSize = Integer.parseInt(contentLength);

                    if (AutoUpdaterAPI.DEBUG) {
                        AutoUpdaterAPI.get().getLogger().info("");
                        AutoUpdaterAPI.get().getLogger().info("");
                        AutoUpdaterAPI.get().getLogger().info("");
                        AutoUpdaterAPI.get().getLogger().info("");
                        AutoUpdaterAPI.get().getLogger().info("");
                        AutoUpdaterAPI.get().getLogger().info("");
                        AutoUpdaterAPI.get().getLogger().info("");
                        AutoUpdaterAPI.get().getLogger().info("");
                        AutoUpdaterAPI.get().getLogger().info("============== BEGIN PREMIUM PLUGIN DEBUG ==============");
                        if (pluginName != null)
                            AutoUpdaterAPI.get().getLogger().info("PLUGIN = " + pluginName);

                        if (page instanceof HtmlPage) {
                            AutoUpdaterAPI.get().getLogger().info("");
                            AutoUpdaterAPI.get().getLogger().info("");
                            AutoUpdaterAPI.get().getLogger().info("PAGETYPE = HtmlPage");
                            HtmlPage htmlPage = (HtmlPage) page;

                            AutoUpdaterAPI.get().getLogger().info("PREVIOUS STATUS CODE = " + htmlPage.getWebResponse().getStatusCode());
                            AutoUpdaterAPI.get().getLogger().info("HISTORY = " + htmlPage.getEnclosingWindow().getHistory().toString());
                            AutoUpdaterAPI.get().getLogger().info("PREVIOUS STATUS CODE = " + htmlPage.getWebResponse().getStatusCode());
                            AutoUpdaterAPI.get().getLogger().info("STATUS CODE = " + htmlPage.getEnclosingWindow().getEnclosedPage().getWebResponse().getStatusCode());
                            htmlPage.getEnclosingWindow().getEnclosedPage().getWebResponse().getResponseHeaders().forEach(nvpair -> AutoUpdaterAPI.get().getLogger().info(nvpair.getName() + " | " + nvpair.getValue()));
                        } else if (page instanceof UnexpectedPage) {
                            AutoUpdaterAPI.get().getLogger().info("");
                            AutoUpdaterAPI.get().getLogger().info("");
                            AutoUpdaterAPI.get().getLogger().info("PAGETYPE = UnexpectedPage");
                            UnexpectedPage unexpectedPage = (UnexpectedPage) page;
                            AutoUpdaterAPI.get().getLogger().info("PREVIOUS STATUS CODE = " + unexpectedPage.getWebResponse().getStatusCode());
                            AutoUpdaterAPI.get().getLogger().info("HISTORY = " + unexpectedPage.getEnclosingWindow().getHistory().toString());
                            AutoUpdaterAPI.get().getLogger().info("PREVIOUS STATUS CODE = " + unexpectedPage.getWebResponse().getStatusCode());
                            AutoUpdaterAPI.get().getLogger().info("STATUS CODE = " + unexpectedPage.getEnclosingWindow().getEnclosedPage().getWebResponse().getStatusCode());
                            AutoUpdaterAPI.get().getLogger().info("NAME | VALUE");
                            unexpectedPage.getEnclosingWindow().getEnclosedPage().getWebResponse().getResponseHeaders().forEach(nvpair -> AutoUpdaterAPI.get().getLogger().info(nvpair.getName() + " | " + nvpair.getValue()));
                        }

                        AutoUpdaterAPI.get().getLogger().info("");
                        AutoUpdaterAPI.get().getLogger().info("");
                        AutoUpdaterAPI.get().getLogger().info("PAGETYPE = Page");
                        AutoUpdaterAPI.get().getLogger().info("HISTORY = " + page.getEnclosingWindow().getHistory().toString());
                        AutoUpdaterAPI.get().getLogger().info("PREVIOUS STATUS CODE = " + page.getWebResponse().getStatusCode());
                        AutoUpdaterAPI.get().getLogger().info("STATUS CODE = " + response.getStatusCode());
                        AutoUpdaterAPI.get().getLogger().info("LOAD TIME = " + response.getLoadTime());
                        AutoUpdaterAPI.get().getLogger().info("CURRENT URL = " + webClient.getCurrentWindow().getEnclosedPage().getUrl());
                        AutoUpdaterAPI.get().getLogger().info("NAME | VALUE");
                        response.getResponseHeaders().forEach(nvpair -> AutoUpdaterAPI.get().getLogger().info(nvpair.getName() + " | " + nvpair.getValue()));
                    }

                    int grabSize = 2048;

                    BufferedInputStream in = new java.io.BufferedInputStream(response.getContentAsStream());
                    java.io.FileOutputStream fos = new java.io.FileOutputStream(new File(dataFolderPath.substring(0, dataFolderPath.lastIndexOf("/")) + "/" + locale.getFileName() + ".jar"));
                    java.io.BufferedOutputStream bout = new BufferedOutputStream(fos, grabSize);

                    byte[] data = new byte[grabSize];
                    long downloadedFileSize = 0;
                    int grabbed;
                    while ((grabbed = in.read(data, 0, grabSize)) >= 0) {
                        downloadedFileSize += grabbed;

                        //Don't send action bar for every byte of data we're not trying to crash any clients (or servers) here.
                        if (downloadedFileSize % (grabSize * 10) == 0 && completeFileSize > 0) {
                            String bar = UtilUI.progressBar(15, downloadedFileSize, completeFileSize, ':', ChatColor.RED, ChatColor.GREEN);
                            final String currentPercent = String.format("%.2f", (((double) downloadedFileSize) / ((double) completeFileSize)) * 100);

                            UtilUI.sendActionBar(initiator, locale.getUpdatingDownload()
                                                                    .replace("%download_bar%", bar)
                                                                    .replace("%download_percent%", currentPercent + "%")
                                                                    + " &8[DOWNLOADING RESOURCE]");
                        }

                        bout.write(data, 0, grabbed);
                    }

                    bout.close();
                    in.close();
                    fos.close();

                    if (AutoUpdaterAPI.DEBUG) {
                        AutoUpdaterAPI.get().getLogger().info("FINISHED WITH " + downloadedFileSize + "/" + completeFileSize + " (" + String.format("%.2f", (((double) downloadedFileSize) / ((double) completeFileSize)) * 100) + "%)");
                        AutoUpdaterAPI.get().getLogger().info("============== END PREMIUM PLUGIN DEBUG =======");
                    }

                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            try {
                                if (deleteOld) {
                                    File pluginFile = new File(plugin.getClass().getProtectionDomain().getCodeSource().getLocation().toURI().getPath());

                                    UtilPlugin.unload(plugin);

                                    if (!pluginFile.delete())
                                        AutoUpdaterAPI.get().printPluginError("Error occurred while updating " + pluginName + ".", "Could not delete old plugin jar.");
                                }

                                UtilUI.sendActionBar(initiator, locale.getUpdating() + " &8[INITIALIZING]");

                                List<Plugin> beforePlugins = new ArrayList<>(Arrays.asList(Bukkit.getPluginManager().getPlugins()));

                                Bukkit.getPluginManager().loadPlugin(new File(dataFolderPath.substring(0, dataFolderPath.lastIndexOf("/")) + "/" + locale.getFileName() + ".jar"));

                                if (pluginName == null) {
                                    List<Plugin> afterPlugins = new ArrayList<>(Arrays.asList(Bukkit.getPluginManager().getPlugins()));
                                    afterPlugins.removeAll(beforePlugins);
                                    pluginName = afterPlugins.get(0).getName();
                                }

                                Bukkit.getPluginManager().enablePlugin(Bukkit.getPluginManager().getPlugin(pluginName));

                                double elapsedTimeSeconds = (double) (System.currentTimeMillis() - startingTime) / 1000;
                                UtilUI.sendActionBar(initiator, locale.getUpdateComplete().replace("%elapsed_time%", String.format("%.2f", elapsedTimeSeconds)));

                                endTask.run(true, null, Bukkit.getPluginManager().getPlugin(pluginName), pluginName);
                            } catch (Exception ex) {
                                UtilUI.sendActionBar(initiator, locale.getUpdateFailedNoVar());
                                AutoUpdaterAPI.get().printError(ex, "Error occurred while updating premium resource.");
                                if (pluginName != null) {
                                    endTask.run(false, ex, Bukkit.getPluginManager().getPlugin(pluginName), pluginName);
                                }
                            }
                        }
                    }.runTask(AutoUpdaterAPI.getPlugin());
                } catch (Exception ex) {
                    if (AutoUpdaterAPI.DEBUG)
                        AutoUpdaterAPI.get().getLogger().info("============== END PREMIUM PLUGIN DEBUG =======");

                    UtilUI.sendActionBar(initiator, locale.getUpdateFailedNoVar());
                    AutoUpdaterAPI.get().printError(ex, "Error occurred while updating premium resource.");
                    if (pluginName != null)
                        endTask.run(false, ex, Bukkit.getPluginManager().getPlugin(pluginName), pluginName);
                }
            }
        }.runTaskAsynchronously(AutoUpdaterAPI.getPlugin());
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
                    spigotUser = AutoUpdaterAPI.get().getApi().getUserManager().authenticate(username, password);

                    if (spigotUser == null) {
                        UtilUI.sendActionBar(initiator, locale.getUpdatingNoVar() + "&c [INVALID CACHED CREDENTIALS]");
                        UtilSpigotCreds.getInstance().clearFile();
                        runGuis(recall);
                        return;
                    }

                    AutoUpdaterAPI.get().setCurrentUser(spigotUser);
                    UtilUI.sendActionBar(initiator, locale.getUpdatingNoVar() + " &8[AUTHENTICATION SUCCESSFUL]");
                    AutoUpdaterAPI.get().getLogger().info("Successfully logged in to Spigot as user '" + username + "'.");

                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            try {
                                if (recall)
                                    update();
                            } catch (Exception ex) {
                                AutoUpdaterAPI.get().printError(ex);
                            }
                        }
                    }.runTaskLater(AutoUpdaterAPI.getPlugin(), 40L);

                } catch (Exception ex) {
                    if (ex instanceof TwoFactorAuthenticationException) {
                        try {
                            UtilUI.sendActionBar(initiator, locale.getUpdatingNoVar() + " &8[RE-ATTEMPTING AUTHENTICATION]");
                            if (twoFactor == null) {
                                runGuis(recall);
                                return;
                            }

                            spigotUser = AutoUpdaterAPI.get().getApi().getUserManager().authenticate(username, password, twoFactor);

                            if (spigotUser == null) {
                                UtilUI.sendActionBar(initiator, locale.getUpdatingNoVar() + " &c[INVALID CACHED CREDENTIALS]");
                                UtilSpigotCreds.getInstance().clearFile();
                                runGuis(recall);
                                return;
                            }

                            AutoUpdaterAPI.get().setCurrentUser(spigotUser);
                            UtilUI.sendActionBar(initiator, locale.getUpdatingNoVar() + " &8[AUTHENTICATION SUCCESSFUL]");
                            AutoUpdaterAPI.get().getLogger().info("Successfully logged in to Spigot as user '" + username + "'.");

                            new BukkitRunnable() {
                                @Override
                                public void run() {
                                    try {
                                        if (recall)
                                            update();
                                    } catch (Exception ex) {
                                        AutoUpdaterAPI.get().printError(ex);
                                    }
                                }
                            }.runTask(AutoUpdaterAPI.getPlugin());
                        } catch (Exception otherException) {
                            if (otherException instanceof InvalidCredentialsException) {
                                UtilUI.sendActionBar(initiator, locale.getUpdatingNoVar() + " &c[INVALID CACHED CREDENTIALS]");
                                UtilSpigotCreds.getInstance().clearFile();
                                runGuis(recall);
                            } else if (otherException instanceof ConnectionFailedException) {
                                UtilUI.sendActionBar(initiator, locale.getUpdateFailedNoVar());
                                AutoUpdaterAPI.get().printError(ex, "Error occurred while connecting to spigot. (#6)");
                                endTask.run(false, otherException, null, pluginName);
                            } else if (otherException instanceof TwoFactorAuthenticationException) {
                                if (loginAttempts < 4) {
                                    UtilUI.sendActionBar(initiator, locale.getUpdatingNoVar() + " &8[RE-TRYING LOGIN IN 5s ATTEMPT #" + loginAttempts + "]");
                                    loginAttempts++;
                                    new BukkitRunnable() {
                                        @Override
                                        public void run() {
                                            authenticate(recall);
                                        }
                                    }.runTaskLater(AutoUpdaterAPI.getPlugin(), 100L);
                                } else {
                                    loginAttempts = 1;
                                    AutoUpdaterAPI.get().printError(otherException);
                                }
                            } else {
                                AutoUpdaterAPI.get().printError(otherException);
                            }
                        }
                    } else if (ex instanceof InvalidCredentialsException) {
                        UtilUI.sendActionBar(initiator, locale.getUpdatingNoVar() + " &c[INVALID CACHED CREDENTIALS]");
                        UtilSpigotCreds.getInstance().clearFile();
                        runGuis(recall);
                    } else if (ex instanceof ConnectionFailedException) {
                        UtilUI.sendActionBar(initiator, locale.getUpdatingNoVar() + " &8[RE-ATTEMPTING AUTHENTICATION]");
                        AutoUpdaterAPI.get().printError(ex, "Error occurred while connecting to spigot. (#2)");
                        endTask.run(false, ex, null, pluginName);
                    } else {
                        AutoUpdaterAPI.get().printError(ex);
                    }
                }
            }
        }.runTaskAsynchronously(AutoUpdaterAPI.getPlugin());
    }

    private void runGuis(boolean recall) {
        new BukkitRunnable() {
            @Override
            public void run() {
                UtilUI.sendActionBar(initiator, locale.getUpdatingNoVar() + " &8[RETRIEVING USERNAME]");
                new AnvilGUI(AutoUpdaterAPI.getPlugin(), initiator, "Spigot username", (Player player1, String usernameInput) -> {
                    try {
                        if (AutoUpdaterAPI.get().getApi().getUserManager().getUserByName(usernameInput) != null) {
                            UtilUI.sendActionBar(initiator, locale.getUpdatingNoVar() + " &8[RETRIEVING PASSWORD]");
                            new AnvilGUI(AutoUpdaterAPI.getPlugin(), initiator, "Spigot password", (Player player2, String passwordInput) -> {
                                try {
                                    spigotUser = AutoUpdaterAPI.get().getApi().getUserManager().authenticate(usernameInput, passwordInput);

                                    UtilUI.sendActionBar(initiator, locale.getUpdatingNoVar() + " &8[ENCRYPTING CREDENTIALS]");
                                    UtilSpigotCreds.getInstance().setUsername(usernameInput);
                                    UtilSpigotCreds.getInstance().setPassword(passwordInput);
                                    UtilSpigotCreds.getInstance().saveFile();

                                    new BukkitRunnable() {
                                        @Override
                                        public void run() {
                                            authenticate(recall);
                                        }
                                    }.runTaskLater(AutoUpdaterAPI.getPlugin(), 200L);
                                    player2.closeInventory();

                                } catch (TwoFactorAuthenticationException ex) {
                                    UtilUI.sendActionBar(initiator, locale.getUpdatingNoVar() + " &8[RETRIEVING TWO FACTOR SECRET]");
                                    new AnvilGUI(AutoUpdaterAPI.getPlugin(), initiator, "Spigot two factor secret", (Player player3, String twoFactorInput) -> {
                                        try {
                                            //Make extra string because the input seems to change for some reason.
                                            spigotUser = AutoUpdaterAPI.get().getApi().getUserManager().authenticate(usernameInput, passwordInput, twoFactorInput);

                                            UtilUI.sendActionBar(initiator, locale.getUpdatingNoVar() + " &8[ENCRYPTING CREDENTIALS]");

                                            UtilSpigotCreds.getInstance().setUsername(usernameInput);
                                            UtilSpigotCreds.getInstance().setPassword(passwordInput);
                                            UtilSpigotCreds.getInstance().setTwoFactor(twoFactorInput);
                                            UtilSpigotCreds.getInstance().saveFile();

                                            new BukkitRunnable() {
                                                @Override
                                                public void run() {
                                                    authenticate(recall);
                                                }
                                            }.runTaskLater(AutoUpdaterAPI.getPlugin(), 200L);
                                            player3.closeInventory();

                                            return "Retrieved credentials you may now close this GUI.";
                                        } catch (Exception exception) {
                                            UtilUI.sendActionBar(initiator, locale.getUpdateFailedNoVar());
                                            AutoUpdaterAPI.get().printError(exception, "Error occurred while authenticating Spigot user.");
                                            endTask.run(false, exception, null, pluginName);
                                            return "Authentication failed";
                                        }
                                    });
                                } catch (ConnectionFailedException ex) {
                                    UtilUI.sendActionBar(initiator, locale.getUpdateFailedNoVar());
                                    AutoUpdaterAPI.get().printError(ex, "Error occurred while connecting to Spigot. (#3)");
                                    endTask.run(false, ex, null, pluginName);
                                    return "Could not connect to Spigot";
                                } catch (InvalidCredentialsException ex) {
                                    UtilUI.sendActionBar(initiator, locale.getUpdateFailedNoVar());
                                    endTask.run(false, ex, null, pluginName);
                                    return "Invalid credentials";
                                }

                                return null;
                            });
                        } else if (usernameInput.contains("@") && usernameInput.contains(".")) {
                            initiator.closeInventory();

                            UtilUI.sendActionBar(initiator, "&cEmails are not supported!");

                            endTask.run(false, null, Bukkit.getPluginManager().getPlugin(pluginName), pluginName);
                            return "Emails are not supported!";
                        } else {
                            UtilUI.sendActionBar(initiator, locale.getUpdateFailedNoVar());
                            endTask.run(false, null, Bukkit.getPluginManager().getPlugin(pluginName), pluginName);
                            return "Invalid username!";
                        }
                    } catch (Exception ex) {
                        AutoUpdaterAPI.get().printError(ex, "Error occurred while authenticating Spigot username.");
                        UtilUI.sendActionBar(initiator, locale.getUpdateFailedNoVar());
                        endTask.run(false, ex, null, pluginName);
                    }

                    return null;
                });
            }
        }.runTask(AutoUpdaterAPI.getPlugin());
    }
}
