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
            error(ex, "Error occurred while retrieving the latest version of a premium resource.");
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

        if (currentVersion.equalsIgnoreCase(newVersion)) {
            UtilUI.sendActionBar(initiator, "&c&lUPDATE FAILED &8[NO UPDATES AVAILABLE]");
            endTask.run(false, null, Bukkit.getPluginManager().getPlugin(pluginName), pluginName);
            return;
        }

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

        UtilUI.sendActionBar(initiator, locale.getUpdating() + " &8[ATTEMPTING DOWNLOAD]", 20);
        new BukkitRunnable() {
            @Override
            public void run() {
                try {
                    printDebug();

                    Map<String, String> cookies = ((SpigotUser) spigotUser).getCookies();
                    WebClient webClient = PremiumController.get().getWebClient();

                    cookies.forEach((key, value) -> webClient.getCookieManager().addCookie(
                            new Cookie("spigotmc.org", key, value)));

                    webClient.waitForBackgroundJavaScript(10_000);
                    webClient.getOptions().setThrowExceptionOnFailingStatusCode(false);
                    Page page = webClient.getPage(siteAPI.getResourceManager().getResourceById(resourceId, spigotUser).getDownloadURL());
                    WebResponse response = page.getEnclosingWindow().getEnclosedPage().getWebResponse();

                    if (page instanceof HtmlPage) {
                        HtmlPage htmlPage = (HtmlPage) page;
                        printDebug2(htmlPage);
                        if (htmlPage.asXml().contains("DDoS protection by Cloudflare")) {
                            UtilUI.sendActionBar(initiator, locale.getUpdating() + " &8[WAITING FOR CLOUDFLARE]", 20);
                            AutoUpdaterAPI.getLogger().info("Arrived at DDoS protection screen.");
                            webClient.waitForBackgroundJavaScript(8_000);
                        }
                        response = htmlPage.getEnclosingWindow().getEnclosedPage().getWebResponse();
                    }

                    String contentLength = response.getResponseHeaderValue("content-length");
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
                UtilUI.sendActionBar(initiator, locale.getUpdatingNoVar() + " &8[ATTEMPTING DECRYPT]", 10);

                String username = UtilSpigotCreds.get().getUsername();
                String password = UtilSpigotCreds.get().getPassword();
                String twoFactor = UtilSpigotCreds.get().getTwoFactor();

                if (username == null || password == null) {
                    runGuis(recall);
                    return;
                }

                try {
                    UtilUI.sendActionBar(initiator, locale.getUpdatingNoVar() + " &8[ATTEMPTING AUTHENTICATION]", 15);
                    spigotUser = siteAPI.getUserManager().authenticate(username, password);

                    if (spigotUser == null) {
                        UtilUI.sendActionBar(initiator, locale.getUpdatingNoVar() + "&c [INVALID CACHED CREDENTIALS]");
                        UtilSpigotCreds.get().clearFile();
                        runGuis(recall);
                        return;
                    }

                    PremiumController.get().setCurrentUser(spigotUser);
                    UtilUI.sendActionBar(initiator, locale.getUpdatingNoVar() + " &8[AUTHENTICATION SUCCESSFUL]");
                    AutoUpdaterAPI.getLogger().info("Successfully logged in to Spigot as user '" + username + "'.");

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
                            UtilUI.sendActionBar(initiator, locale.getUpdatingNoVar() + " &8[RE-ATTEMPTING AUTHENTICATION]", 15);
                            if (twoFactor == null) {
                                requestTwoFactor(username, password, recall);
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
                            AutoUpdaterAPI.getLogger().info("Successfully logged in to Spigot as user '" + username + "'.");

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
                                    UtilUI.sendActionBar(initiator, locale.getUpdatingNoVar() + " &8[RE-TRYING LOGIN IN 5s ATTEMPT #" + loginAttempts + "/3]", 15);
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
                        UtilUI.sendActionBar(initiator, locale.getUpdatingNoVar() + " &8[RE-ATTEMPTING AUTHENTICATION]", 15);
                        AutoUpdaterAPI.get().printError(ex, "Error occurred while connecting to spigot. (#2)");
                        endTask.run(false, ex, null, pluginName);
                    } else {
                        AutoUpdaterAPI.get().printError(ex);
                    }
                }
            }
        }.runTaskAsynchronously(AutoUpdaterAPI.getPlugin());
    }

    /*
     * LOGIN UI
     */

    private void runGuis(boolean recall) {
        new BukkitRunnable() {
            @Override
            public void run() {
                UtilUI.sendActionBar(initiator, locale.getUpdatingNoVar() + " &8[RETRIEVING USERNAME]", 120);
                new AnvilGUI.Builder()
                        .text("Spigot username")
                        .plugin(AutoUpdaterAPI.getPlugin())
                        .onComplete((Player player, String usernameInput) -> {
                            try {
                                if (siteAPI.getUserManager().getUserByName(usernameInput) != null) {
                                    requestPassword(usernameInput, recall);
                                } else if (usernameInput.contains("@") && usernameInput.contains(".")) {
                                    initiator.closeInventory();
                                    UtilUI.sendActionBar(initiator, "&cEmails aren't supported!", 10);
                                    endTask.run(false, null, Bukkit.getPluginManager().getPlugin(pluginName), pluginName);
                                    return AnvilGUI.Response.text("Emails aren't supported!");
                                } else {
                                    UtilUI.sendActionBar(initiator, locale.getUpdateFailedNoVar());
                                    endTask.run(false, null, Bukkit.getPluginManager().getPlugin(pluginName), pluginName);
                                    return AnvilGUI.Response.text("Invalid username!");
                                }
                            } catch (Exception ex) {
                                error(ex, "Error occurred while authenticating Spigot username.");
                            }

                            return AnvilGUI.Response.text("Success!");
                        }).open(initiator);
            }
        }.runTask(AutoUpdaterAPI.getPlugin());
    }

    private void requestPassword(String usernameInput, boolean recall) {
        UtilUI.sendActionBar(initiator, locale.getUpdatingNoVar() + " &8[RETRIEVING PASSWORD]", 120);
        new AnvilGUI.Builder()
                .text("Spigot password")
                .plugin(AutoUpdaterAPI.getPlugin())
                .onComplete(((player, passwordInput) -> {
                    try {
                        spigotUser = siteAPI.getUserManager().authenticate(usernameInput, passwordInput);

                        UtilUI.sendActionBar(initiator, locale.getUpdatingNoVar() + " &8[ENCRYPTING CREDENTIALS]", 10);
                        UtilSpigotCreds.get().setUsername(usernameInput);
                        UtilSpigotCreds.get().setPassword(passwordInput);
                        UtilSpigotCreds.get().saveFile();

                        new BukkitRunnable() {
                            @Override
                            public void run() {
                                authenticate(recall);
                            }
                        }.runTaskLater(AutoUpdaterAPI.getPlugin(), 100L);
                    } catch (TwoFactorAuthenticationException ex) {
                        requestTwoFactor(usernameInput, passwordInput, recall);
                    } catch (ConnectionFailedException ex) {
                        error(ex, "Error occurred while connecting to Spigot. (#3)");
                        return AnvilGUI.Response.text("Could not connect to Spigot.");
                    } catch (InvalidCredentialsException ex) {
                        UtilUI.sendActionBar(initiator, locale.getUpdateFailedNoVar() + "&8[INVALID CREDENTIALS]", 10);
                        endTask.run(false, ex, null, pluginName);
                        return AnvilGUI.Response.text("Invalid credentials!");
                    }

                    return AnvilGUI.Response.text("Success!");
                })).open(initiator);
    }

    private void requestTwoFactor(String usernameInput, String passwordInput, boolean recall) {
        UtilUI.sendActionBar(initiator, locale.getUpdatingNoVar() + " &8[RETRIEVING TWO FACTOR SECRET]", 120);
        new AnvilGUI.Builder().plugin(AutoUpdaterAPI.getPlugin())
                .text("Spigot two factor secret")
                .onComplete((Player player, String twoFactorInput) -> {
                    try {
                        spigotUser = siteAPI.getUserManager().authenticate(usernameInput, passwordInput, twoFactorInput);
                        UtilUI.sendActionBar(initiator, locale.getUpdatingNoVar() + " &8[ENCRYPTING CREDENTIALS]", 10);

                        UtilSpigotCreds.get().setUsername(usernameInput);
                        UtilSpigotCreds.get().setPassword(passwordInput);
                        UtilSpigotCreds.get().setTwoFactor(twoFactorInput);
                        UtilSpigotCreds.get().saveFile();

                        new BukkitRunnable() {
                            @Override
                            public void run() {
                                authenticate(recall);
                            }
                        }.runTaskLater(AutoUpdaterAPI.getPlugin(), 100L);

                        return AnvilGUI.Response.text("Logging in, close GUI.");
                    } catch (Exception ex) {
                        error(ex, "Error occurred while authenticating Spigot user.");
                        return AnvilGUI.Response.text("Authentication failed");
                    }
                }).open(initiator);
    }

    /*
     * ERROR HANDLING
     */

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

    private void printDebug() {
        if (AutoUpdaterAPI.DEBUG) {
            AutoUpdaterAPI.getLogger().info("\n\n\n\n\n\n============== BEGIN PREMIUM PLUGIN DEBUG ==============");
            AutoUpdaterAPI.getLogger().info("AUTHENTICATED: " + spigotUser.isAuthenticated());
            AutoUpdaterAPI.getLogger().info("COOKIES: ");
            ((SpigotUser) spigotUser).getCookies().forEach((k, v) -> AutoUpdaterAPI.getLogger().info("\t" + k + " | " + v));
        }
    }

    private void printDebug1(Page page, WebResponse response, WebClient webClient) {
        if (AutoUpdaterAPI.DEBUG) {
            if (pluginName != null)
                AutoUpdaterAPI.getLogger().info("PLUGIN = " + pluginName);

            if (page instanceof HtmlPage) {
                AutoUpdaterAPI.getLogger().info("\n\nPAGETYPE = HtmlPage");
                HtmlPage htmlPage = (HtmlPage) page;

                AutoUpdaterAPI.getLogger().info("PREVIOUS STATUS CODE = " + htmlPage.getWebResponse().getStatusCode());
                AutoUpdaterAPI.getLogger().info("HISTORY: ");
                for (int i = 0; i < page.getEnclosingWindow().getHistory().getLength(); i++) {
                    AutoUpdaterAPI.getLogger().info(htmlPage.getEnclosingWindow().getHistory().getUrl(i).toString());
                }
                AutoUpdaterAPI.getLogger().info("STATUS CODE = " + htmlPage.getEnclosingWindow().getEnclosedPage().getWebResponse().getStatusCode());
                htmlPage.getEnclosingWindow().getEnclosedPage().getWebResponse().getResponseHeaders().forEach(nvpair -> AutoUpdaterAPI.getLogger().info(nvpair.getName() + " | " + nvpair.getValue()));
            } else if (page instanceof UnexpectedPage) {
                AutoUpdaterAPI.getLogger().info("\n\nPAGETYPE = UnexpectedPage");
                UnexpectedPage unexpectedPage = (UnexpectedPage) page;
                AutoUpdaterAPI.getLogger().info("PREVIOUS STATUS CODE = " + unexpectedPage.getWebResponse().getStatusCode());
                AutoUpdaterAPI.getLogger().info("HISTORY: ");
                for (int i = 0; i < page.getEnclosingWindow().getHistory().getLength(); i++) {
                    AutoUpdaterAPI.getLogger().info(page.getEnclosingWindow().getHistory().getUrl(i).toString());
                }
                AutoUpdaterAPI.getLogger().info("STATUS CODE = " + unexpectedPage.getEnclosingWindow().getEnclosedPage().getWebResponse().getStatusCode());
                AutoUpdaterAPI.getLogger().info("NAME | VALUE");
                unexpectedPage.getEnclosingWindow().getEnclosedPage().getWebResponse().getResponseHeaders().forEach(nvpair -> AutoUpdaterAPI.getLogger().info(nvpair.getName() + " | " + nvpair.getValue()));
            }

            AutoUpdaterAPI.getLogger().info("\n\nPAGETYPE = Page");
            AutoUpdaterAPI.getLogger().info("HISTORY: ");
            for (int i = 0; i < page.getEnclosingWindow().getHistory().getLength(); i++) {
                AutoUpdaterAPI.getLogger().info(page.getEnclosingWindow().getHistory().getUrl(i).toString());
            }
            AutoUpdaterAPI.getLogger().info("PREVIOUS STATUS CODE = " + page.getWebResponse().getStatusCode());
            AutoUpdaterAPI.getLogger().info("STATUS CODE = " + response.getStatusCode());
            AutoUpdaterAPI.getLogger().info("CONTENT TYPE = " + response.getContentType());
            AutoUpdaterAPI.getLogger().info("STATUS MESSAGE = " + response.getStatusMessage());
            AutoUpdaterAPI.getLogger().info("LOAD TIME = " + response.getLoadTime());
            AutoUpdaterAPI.getLogger().info("CURRENT URL = " + webClient.getCurrentWindow().getEnclosedPage().getUrl());
            AutoUpdaterAPI.getLogger().info("NAME | VALUE");
            response.getResponseHeaders().forEach(nvpair -> AutoUpdaterAPI.getLogger().info(nvpair.getName() + " | " + nvpair.getValue()));
        }
    }

    private void printDebug2(HtmlPage htmlPage) {
        if (AutoUpdaterAPI.DEBUG) {
            AutoUpdaterAPI.getLogger().info("---- EARLY HTML OUTPUT ----");
            AutoUpdaterAPI.getLogger().info("\nPAGETYPE = HtmlPage");

            AutoUpdaterAPI.getLogger().info("PREVIOUS STATUS CODE = " + htmlPage.getWebResponse().getStatusCode());
            AutoUpdaterAPI.getLogger().info("HISTORY: ");
            for (int i = 0; i < htmlPage.getEnclosingWindow().getHistory().getLength(); i++) {
                AutoUpdaterAPI.getLogger().info(htmlPage.getEnclosingWindow().getHistory().getUrl(i).toString());
            }
            AutoUpdaterAPI.getLogger().info("PREVIOUS STATUS CODE = " + htmlPage.getWebResponse().getStatusCode());
            AutoUpdaterAPI.getLogger().info("STATUS CODE = " + htmlPage.getEnclosingWindow().getEnclosedPage().getWebResponse().getStatusCode());
            htmlPage.getEnclosingWindow().getEnclosedPage().getWebResponse().getResponseHeaders().forEach(nvpair -> AutoUpdaterAPI.getLogger().info(nvpair.getName() + " | " + nvpair.getValue()));
            AutoUpdaterAPI.getLogger().info("\n---- EARLY HTML OUTPUT ----");
        }
    }

    private void printDebug3(long downloadedFileSize, int completeFileSize) {
        if (AutoUpdaterAPI.DEBUG) {
            AutoUpdaterAPI.getLogger().info("FINISHED WITH " + downloadedFileSize + "/" + completeFileSize + " (" + String.format("%.2f", (((double) downloadedFileSize) / ((double) completeFileSize)) * 100) + "%)");
            AutoUpdaterAPI.getLogger().info("============== END PREMIUM PLUGIN DEBUG =======");
        }
    }
}
