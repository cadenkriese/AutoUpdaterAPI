package cc.flogi.dev.autoupdater;

import be.maximvdw.spigotsite.api.SpigotSiteAPI;
import be.maximvdw.spigotsite.api.exceptions.ConnectionFailedException;
import be.maximvdw.spigotsite.api.resource.Resource;
import be.maximvdw.spigotsite.api.user.User;
import be.maximvdw.spigotsite.api.user.exceptions.InvalidCredentialsException;
import be.maximvdw.spigotsite.api.user.exceptions.TwoFactorAuthenticationException;
import be.maximvdw.spigotsite.user.SpigotUser;
import cc.flogi.dev.autoupdater.util.UtilReader;
import cc.flogi.dev.autoupdater.util.UtilSpigotCreds;
import cc.flogi.dev.autoupdater.util.UtilText;
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

import java.io.*;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Map;

/**
 * @author Caden Kriese (flogic)
 *
 * Performs the update task for premium Spigot plugins.
 *
 * Created on 6/6/17
 */

public class PremiumUpdater {
    private Player initiator;
    private Plugin plugin;

    private String pluginFolderPath;
    private String currentVersion;
    private String pluginName;
    private UpdateLocale locale;

    private User spigotUser;
    private Resource resource;
    private UpdaterRunnable endTask;
    private SpigotSiteAPI siteAPI;

    private boolean replace;
    private int resourceId;
    private int loginAttempts;
    private long startingTime;

    protected PremiumUpdater(Player initiator, Plugin plugin, int resourceId, UpdateLocale locale, boolean replace) {
        locale.updateVariables(plugin.getName(), plugin.getDescription().getVersion(), null);

        siteAPI = PremiumController.get().getSiteAPI();
        spigotUser = PremiumController.get().getCurrentUser();
        pluginFolderPath = plugin.getDataFolder().getParent();
        currentVersion = plugin.getDescription().getVersion();
        loginAttempts = 1;
        pluginName = locale.getPluginName();
        this.resourceId = resourceId;
        this.plugin = plugin;
        this.initiator = initiator;
        this.locale = locale;
        this.replace = replace;
        endTask = (successful, ex, updatedPlugin, pluginName) -> {
        };
    }

    protected PremiumUpdater(Player initiator, Plugin plugin, int resourceId, UpdateLocale locale, boolean replace, UpdaterRunnable endTask) {
        locale.updateVariables(plugin.getName(), plugin.getDescription().getVersion(), null);

        siteAPI = PremiumController.get().getSiteAPI();
        spigotUser = PremiumController.get().getCurrentUser();
        pluginFolderPath = plugin.getDataFolder().getParent();
        currentVersion = plugin.getDescription().getVersion();
        loginAttempts = 1;
        pluginName = locale.getPluginName();
        this.resourceId = resourceId;
        this.plugin = plugin;
        this.initiator = initiator;
        this.locale = locale;
        this.replace = replace;
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
        } catch (Exception ex) {
            error(ex, "Error occurred while retrieving latest version of premium resource.");
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

        spigotUser = PremiumController.get().getCurrentUser();

        if (locale.getPluginName() != null)
            pluginName = locale.getPluginName();

        locale.setFileName(locale.getFileName());

        if (spigotUser == null) {
            authenticate(true);
            UtilUI.sendActionBar(initiator, locale.getUpdatingNoVar() + " &8[AUTHENTICATING SPIGOT ACCOUNT]");
            return;
        }

        try {
            for (Resource resource : siteAPI.getResourceManager().getPurchasedResources(spigotUser)) {
                if (resource.getResourceId() == resourceId) {
                    this.resource = resource;
                }
            }
        } catch (ConnectionFailedException ex) {
            error(ex, "Error occurred while connecting to spigot. (#1)");
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
                    WebClient webClient = PremiumController.get().getWebClient();

                    for (Map.Entry<String, String> entry : cookies.entrySet())
                        webClient.getCookieManager().addCookie(new Cookie("spigotmc.org", entry.getKey(), entry.getValue()));

                    webClient.getOptions().setThrowExceptionOnFailingStatusCode(false);
                    Page page = webClient.getPage(siteAPI.getResourceManager().getResourceById(resourceId, spigotUser).getDownloadURL());
                    webClient.waitForBackgroundJavaScript(10_000);
                    WebResponse response = page.getEnclosingWindow().getEnclosedPage().getWebResponse();

                    if (page instanceof HtmlPage) {
                        HtmlPage htmlPage = (HtmlPage) page;
                        printDebug2(htmlPage);
                        response = htmlPage.getEnclosingWindow().getEnclosedPage().getWebResponse();
                    }

                    String contentLength = response.getResponseHeaderValue("Content-Length");
                    int completeFileSize = 0;
                    int grabSize = 2048;
                    if (contentLength != null)
                        completeFileSize = Integer.parseInt(contentLength);

                    printDebug1(page, response, webClient);

                    BufferedInputStream in = new BufferedInputStream(response.getContentAsStream());
                    FileOutputStream fos = new FileOutputStream(new File(pluginFolderPath + "/" + locale.getFileName() + ".jar"));
                    BufferedOutputStream bout = new BufferedOutputStream(fos, grabSize);

                    byte[] data = new byte[grabSize];
                    long downloadedFileSize = 0;
                    int grabbed;
                    while ((grabbed = in.read(data, 0, grabSize)) >= 0) {
                        downloadedFileSize += grabbed;

                        //Don't send action bar for every grab we're not trying to crash any clients (or servers) here.
                        if (downloadedFileSize % (grabSize * 5) == 0 && completeFileSize > 0) {
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

                    printDebug3(downloadedFileSize, completeFileSize);

                    //Copy plugin utility from src/main/resources
                    String corePluginFile = "/autoupdater-plugin-" + AutoUpdaterAPI.PROPERTIES.VERSION + ".jar";
                    try (InputStream is = getClass().getResourceAsStream(corePluginFile)) {
                        File targetFile = new File(plugin.getDataFolder().getParent() + corePluginFile);
                        Files.copy(is, targetFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                        is.close();
                        new BukkitRunnable() {
                            @Override
                            public void run() {
                                //Enable plugin and perform update task.
                                try {
                                    Bukkit.getPluginManager().loadPlugin(targetFile);
                                    UpdaterPlugin updaterPlugin = (UpdaterPlugin) Bukkit.getPluginManager().getPlugin("autoupdater-plugin");
                                    if (updaterPlugin == null)
                                        throw new FileNotFoundException("Unable to locate updater plugin.");

                                    Bukkit.getPluginManager().enablePlugin(updaterPlugin);
                                    updaterPlugin.updatePlugin(plugin, initiator, replace, pluginName, pluginFolderPath, locale, startingTime, endTask);
                                } catch (Exception ex) {
                                    error(ex, ex.getMessage(), newVersion);
                                }
                            }
                        }.runTask(AutoUpdaterAPI.getPlugin());
                    } catch (Exception ex) {
                        error(ex, ex.getMessage(), newVersion);
                    }
                } catch (Exception ex) {
                    error(ex, "Error occurred while updating premium resource.", newVersion);
                }
            }
        }.runTaskAsynchronously(AutoUpdaterAPI.getPlugin());
    }

    public void authenticate(boolean recall) {
        new BukkitRunnable() {
            @Override
            public void run() {
                UtilUI.sendActionBar(initiator, locale.getUpdatingNoVar() + " &8[ATTEMPTING DECRYPT]");

                String username = UtilSpigotCreds.get().getUsername();
                String password = UtilSpigotCreds.get().getPassword();
                String twoFactor = UtilSpigotCreds.get().getTwoFactor();

                if (username == null || password == null) {
                    runGuis(recall);
                    return;
                }

                try {
                    UtilUI.sendActionBar(initiator, locale.getUpdatingNoVar() + " &8[ATTEMPTING AUTHENTICATION]");
                    spigotUser = siteAPI.getUserManager().authenticate(username, password);

                    if (spigotUser == null) {
                        UtilUI.sendActionBar(initiator, locale.getUpdatingNoVar() + "&c [INVALID CACHED CREDENTIALS]");
                        UtilSpigotCreds.get().clearFile();
                        runGuis(recall);
                        return;
                    }

                    PremiumController.get().setCurrentUser(spigotUser);
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

                            spigotUser = siteAPI.getUserManager().authenticate(username, password, twoFactor);

                            if (spigotUser == null) {
                                UtilUI.sendActionBar(initiator, locale.getUpdatingNoVar() + " &c[INVALID CACHED CREDENTIALS]");
                                UtilSpigotCreds.get().clearFile();
                                runGuis(recall);
                                return;
                            }

                            PremiumController.get().setCurrentUser(spigotUser);
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
                                UtilSpigotCreds.get().clearFile();
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
                        UtilSpigotCreds.get().clearFile();
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
                        if (siteAPI.getUserManager().getUserByName(usernameInput) != null) {
                            requestPassword(usernameInput, recall);
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
                        error(ex, "Error occurred while authenticating Spigot username.");
                    }

                    return null;
                });
            }
        }.runTask(AutoUpdaterAPI.getPlugin());
    }

    private void requestPassword(String usernameInput, boolean recall) {
        UtilUI.sendActionBar(initiator, locale.getUpdatingNoVar() + " &8[RETRIEVING PASSWORD]");
        new AnvilGUI(AutoUpdaterAPI.getPlugin(), initiator, "Spigot password", (Player player, String passwordInput) -> {
            try {
                spigotUser = siteAPI.getUserManager().authenticate(usernameInput, passwordInput);

                UtilUI.sendActionBar(initiator, locale.getUpdatingNoVar() + " &8[ENCRYPTING CREDENTIALS]");
                UtilSpigotCreds.get().setUsername(usernameInput);
                UtilSpigotCreds.get().setPassword(passwordInput);
                UtilSpigotCreds.get().saveFile();

                new BukkitRunnable() {
                    @Override
                    public void run() {
                        authenticate(recall);
                    }
                }.runTaskLater(AutoUpdaterAPI.getPlugin(), 200L);

                //TODO possibly uncomment, potentially caused console spam.
                //player.closeInventory();

            } catch (TwoFactorAuthenticationException ex) {
                requestTwoFactor(usernameInput, passwordInput, recall);
            } catch (ConnectionFailedException ex) {
                error(ex, "Error occurred while connecting to Spigot. (#3)");
                return "Could not connect to Spigot.";
            } catch (InvalidCredentialsException ex) {
                UtilUI.sendActionBar(initiator, locale.getUpdateFailedNoVar());
                endTask.run(false, ex, null, pluginName);
                return "Invalid credentials!";
            }

            return null;
        });
    }

    private void requestTwoFactor(String usernameInput, String passwordInput, boolean recall) {
        UtilUI.sendActionBar(initiator, locale.getUpdatingNoVar() + " &8[RETRIEVING TWO FACTOR SECRET]");
        new AnvilGUI(AutoUpdaterAPI.getPlugin(), initiator, "Spigot two factor secret", (Player player, String twoFactorInput) -> {
            try {
                //Make extra string because the input seems to change for some reason.
                spigotUser = siteAPI.getUserManager().authenticate(usernameInput, passwordInput, twoFactorInput);

                UtilUI.sendActionBar(initiator, locale.getUpdatingNoVar() + " &8[ENCRYPTING CREDENTIALS]");

                UtilSpigotCreds.get().setUsername(usernameInput);
                UtilSpigotCreds.get().setPassword(passwordInput);
                UtilSpigotCreds.get().setTwoFactor(twoFactorInput);
                UtilSpigotCreds.get().saveFile();

                new BukkitRunnable() {
                    @Override
                    public void run() {
                        authenticate(recall);
                    }
                }.runTaskLater(AutoUpdaterAPI.getPlugin(), 200L);
                //TODO possibly uncomment, potentially caused console spam.
                //player.closeInventory();

                return "Retrieved credentials you may now close this GUI.";
            } catch (Exception exception) {
                UtilUI.sendActionBar(initiator, locale.getUpdateFailedNoVar());
                AutoUpdaterAPI.get().printError(exception, "Error occurred while authenticating Spigot user.");
                endTask.run(false, exception, null, pluginName);
                return "Authentication failed";
            }
        });
    }

    private void error(Exception ex, String message) {
        AutoUpdaterAPI.get().printError(ex, message);
        UtilUI.sendActionBar(initiator, locale.getUpdateFailedNoVar());
        endTask.run(false, ex, null, pluginName);
    }

    private void error(Exception ex, String message, String newVersion) {
        AutoUpdaterAPI.get().printError(ex, message);
        UtilUI.sendActionBar(initiator, UtilText.format(locale.getUpdateFailed(),
                "old_version", currentVersion,
                "new_version", newVersion));
        endTask.run(false, ex, Bukkit.getPluginManager().getPlugin(pluginName), pluginName);
    }

    /*
     * DEBUG MESSAGES
     */

    private void printDebug1(Page page, WebResponse response, WebClient webClient) {
        if (AutoUpdaterAPI.DEBUG) {
            AutoUpdaterAPI.get().getLogger().info("\n\n\n\n\n\n============== BEGIN PREMIUM PLUGIN DEBUG ==============");
            if (pluginName != null)
                AutoUpdaterAPI.get().getLogger().info("PLUGIN = " + pluginName);

            if (page instanceof HtmlPage) {
                AutoUpdaterAPI.get().getLogger().info("\n\nPAGETYPE = HtmlPage");
                HtmlPage htmlPage = (HtmlPage) page;

                AutoUpdaterAPI.get().getLogger().info("PREVIOUS STATUS CODE = " + htmlPage.getWebResponse().getStatusCode());
                AutoUpdaterAPI.get().getLogger().info("HISTORY = " + htmlPage.getEnclosingWindow().getHistory().toString());
                AutoUpdaterAPI.get().getLogger().info("PREVIOUS STATUS CODE = " + htmlPage.getWebResponse().getStatusCode());
                AutoUpdaterAPI.get().getLogger().info("STATUS CODE = " + htmlPage.getEnclosingWindow().getEnclosedPage().getWebResponse().getStatusCode());
                htmlPage.getEnclosingWindow().getEnclosedPage().getWebResponse().getResponseHeaders().forEach(nvpair -> AutoUpdaterAPI.get().getLogger().info(nvpair.getName() + " | " + nvpair.getValue()));
            } else if (page instanceof UnexpectedPage) {
                AutoUpdaterAPI.get().getLogger().info("\n\nPAGETYPE = UnexpectedPage");
                UnexpectedPage unexpectedPage = (UnexpectedPage) page;
                AutoUpdaterAPI.get().getLogger().info("PREVIOUS STATUS CODE = " + unexpectedPage.getWebResponse().getStatusCode());
                AutoUpdaterAPI.get().getLogger().info("HISTORY = " + unexpectedPage.getEnclosingWindow().getHistory().toString());
                AutoUpdaterAPI.get().getLogger().info("PREVIOUS STATUS CODE = " + unexpectedPage.getWebResponse().getStatusCode());
                AutoUpdaterAPI.get().getLogger().info("STATUS CODE = " + unexpectedPage.getEnclosingWindow().getEnclosedPage().getWebResponse().getStatusCode());
                AutoUpdaterAPI.get().getLogger().info("NAME | VALUE");
                unexpectedPage.getEnclosingWindow().getEnclosedPage().getWebResponse().getResponseHeaders().forEach(nvpair -> AutoUpdaterAPI.get().getLogger().info(nvpair.getName() + " | " + nvpair.getValue()));
            }

            AutoUpdaterAPI.get().getLogger().info("\n\nPAGETYPE = Page");
            AutoUpdaterAPI.get().getLogger().info("HISTORY = " + page.getEnclosingWindow().getHistory().toString());
            AutoUpdaterAPI.get().getLogger().info("PREVIOUS STATUS CODE = " + page.getWebResponse().getStatusCode());
            AutoUpdaterAPI.get().getLogger().info("STATUS CODE = " + response.getStatusCode());
            AutoUpdaterAPI.get().getLogger().info("LOAD TIME = " + response.getLoadTime());
            AutoUpdaterAPI.get().getLogger().info("CURRENT URL = " + webClient.getCurrentWindow().getEnclosedPage().getUrl());
            AutoUpdaterAPI.get().getLogger().info("NAME | VALUE");
            response.getResponseHeaders().forEach(nvpair -> AutoUpdaterAPI.get().getLogger().info(nvpair.getName() + " | " + nvpair.getValue()));
        }
    }

    private void printDebug2(HtmlPage htmlPage) {
        if (AutoUpdaterAPI.DEBUG) {
            AutoUpdaterAPI.get().getLogger().info("---- EARLY HTML OUTPUT ----");
            AutoUpdaterAPI.get().getLogger().info("\nPAGETYPE = HtmlPage");

            AutoUpdaterAPI.get().getLogger().info("PREVIOUS STATUS CODE = " + htmlPage.getWebResponse().getStatusCode());
            AutoUpdaterAPI.get().getLogger().info("HISTORY = " + htmlPage.getEnclosingWindow().getHistory().toString());
            AutoUpdaterAPI.get().getLogger().info("PREVIOUS STATUS CODE = " + htmlPage.getWebResponse().getStatusCode());
            AutoUpdaterAPI.get().getLogger().info("STATUS CODE = " + htmlPage.getEnclosingWindow().getEnclosedPage().getWebResponse().getStatusCode());
            htmlPage.getEnclosingWindow().getEnclosedPage().getWebResponse().getResponseHeaders().forEach(nvpair -> AutoUpdaterAPI.get().getLogger().info(nvpair.getName() + " | " + nvpair.getValue()));
            AutoUpdaterAPI.get().getLogger().info("\n---- EARLY HTML OUTPUT ----");
        }
    }

    private void printDebug3(long downloadedFileSize, int completeFileSize) {
        if (AutoUpdaterAPI.DEBUG) {
            AutoUpdaterAPI.get().getLogger().info("FINISHED WITH " + downloadedFileSize + "/" + completeFileSize + " (" + String.format("%.2f", (((double) downloadedFileSize) / ((double) completeFileSize)) * 100) + "%)");
            AutoUpdaterAPI.get().getLogger().info("============== END PREMIUM PLUGIN DEBUG =======");
        }
    }
}
