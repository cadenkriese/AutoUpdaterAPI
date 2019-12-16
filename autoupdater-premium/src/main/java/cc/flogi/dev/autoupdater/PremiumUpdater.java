package cc.flogi.dev.autoupdater;

import be.maximvdw.spigotsite.SpigotSiteCore;
import be.maximvdw.spigotsite.api.SpigotSiteAPI;
import be.maximvdw.spigotsite.api.exceptions.ConnectionFailedException;
import be.maximvdw.spigotsite.api.resource.Resource;
import be.maximvdw.spigotsite.api.user.User;
import be.maximvdw.spigotsite.api.user.exceptions.InvalidCredentialsException;
import be.maximvdw.spigotsite.api.user.exceptions.TwoFactorAuthenticationException;
import be.maximvdw.spigotsite.user.SpigotUser;
import com.gargoylesoftware.htmlunit.*;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.gargoylesoftware.htmlunit.util.Cookie;
import net.md_5.bungee.api.ChatColor;
import net.wesjd.anvilgui.AnvilGUI;
import org.apache.commons.logging.LogFactory;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Caden Kriese (flogic)
 *
 * Performs the update task for premium Spigot plugins.
 *
 * Created on 6/6/17
 */

public class PremiumUpdater implements Updater {

    private static User currentUser;
    private static SpigotSiteAPI siteAPI;
    private static WebClient webClient;
    private Player initiator;
    private Plugin plugin;
    private String pluginFolderPath;
    private String currentVersion;
    private String newVersion;
    private String pluginName;
    private UpdateLocale locale;
    private Resource resource;
    private UpdaterRunnable endTask = (successful, ex, updatedPlugin, pluginName) -> {};
    private boolean replace;
    private int resourceId;
    private int loginAttempts;
    private long startingTime;

    protected PremiumUpdater(Player initiator, Plugin plugin, int resourceId, UpdateLocale locale, boolean replace) {
        locale.updateVariables(plugin.getName(), plugin.getDescription().getVersion(), null);

        pluginFolderPath = plugin.getDataFolder().getParent();
        currentVersion = plugin.getDescription().getVersion();
        loginAttempts = 1;
        pluginName = locale.getPluginName();
        this.resourceId = resourceId;
        this.plugin = plugin;
        this.initiator = initiator;
        this.locale = locale;
        this.replace = replace;
    }

    protected PremiumUpdater(Player initiator, Plugin plugin, int resourceId, UpdateLocale locale, boolean replace, UpdaterRunnable endTask) {
        locale.updateVariables(plugin.getName(), plugin.getDescription().getVersion(), null);

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

    protected static void init(JavaPlugin javaPlugin) {
        Logger logger = InternalCore.getLogger();

        UtilSpigotCreds.init();
        UtilEncryption.init();

        UtilThreading.async(() -> {
            try {
                LogFactory.getFactory().setAttribute("org.apache.commons.logging.Log",
                        "org.apache.commons.logging.impl.NoOpLog");
                Logger.getLogger("org.apache.commons.httpclient").setLevel(Level.OFF);
            } catch (Exception ex) {
                InternalCore.get().printError(ex, "Unable to turn off HTMLUnit logging!.");
            }

            //Setup web client
            webClient = new WebClient(BrowserVersion.CHROME);
            webClient.getOptions().setJavaScriptEnabled(true);
            webClient.getOptions().setTimeout(15000);
            webClient.getOptions().setCssEnabled(false);
            webClient.getOptions().setRedirectEnabled(true);
            webClient.getOptions().setThrowExceptionOnFailingStatusCode(false);
            webClient.getOptions().setThrowExceptionOnScriptError(false);
            webClient.getOptions().setPrintContentOnFailingStatusCode(false);
            java.util.logging.Logger.getLogger("com.gargoylesoftware").setLevel(Level.OFF);

            logger.info("Initializing connection with Spigot...");

            //Spigot Site API
            siteAPI = new SpigotSiteCore();
            try {
                if (UtilSpigotCreds.getUsername() != null && UtilSpigotCreds.getPassword() != null) {
                    logger.info("Stored credentials detected, attempting login.");
                    new PremiumUpdater(null, javaPlugin, 1, new UpdateLocale(), false).authenticate(false);
                }
            } catch (Exception ex) {
                InternalCore.get().printError(ex, "Error occurred while initializing the spigot site API.");
            }

            logger.info("Connected to Spigot.");
        });
    }

    /**
     * Resets the current user.
     */
    protected static void resetUser() {
        currentUser = null;
        UtilSpigotCreds.reset();
    }

    @Override public String getLatestVersion() {
        try {
            return UtilReader.readFrom("https://api.spigotmc.org/legacy/update.php?resource=" + resourceId);
        } catch (Exception ex) {
            error(ex, "Error occurred while retrieving the latest version of a premium resource.");
        }
        return "";
    }

    @Override public void update() {
        startingTime = System.currentTimeMillis();

        UtilUI.sendActionBar(initiator, locale.getUpdatingNoVar() + " &8[RETRIEVING PLUGIN INFO]");

        newVersion = getLatestVersion();
        locale.updateVariables(plugin.getName(), currentVersion, newVersion);

        if (currentVersion.equalsIgnoreCase(newVersion)) {
            UtilUI.sendActionBar(initiator, "&c&lUPDATE FAILED &8[NO UPDATES AVAILABLE]");
            endTask.run(false, null, Bukkit.getPluginManager().getPlugin(pluginName), pluginName);
            return;
        }

        if (locale.getPluginName() != null)
            pluginName = locale.getPluginName();

        locale.setFileName(locale.getFileName());

        if (currentUser == null) {
            authenticate(true);
            UtilUI.sendActionBar(initiator, locale.getUpdatingNoVar() + " &8[AUTHENTICATING SPIGOT ACCOUNT]");
            return;
        }

        try {
            for (Resource resource : siteAPI.getResourceManager().getPurchasedResources(currentUser)) {
                if (resource.getResourceId() == resourceId) {
                    this.resource = resource;
                }
            }
        } catch (ConnectionFailedException ex) {
            error(ex, "Error occurred while connecting to spigot. (#1)");
        }

        if (resource == null) {
            InternalCore.get().printPluginError("Error occurred while updating " + pluginName + "!", "That plugin has not been bought by the current user!");
            UtilUI.sendActionBar(initiator, "&c&lUPDATE FAILED &8[YOU HAVE NOT BOUGHT THAT PLUGIN]");
            endTask.run(false, null, Bukkit.getPluginManager().getPlugin(pluginName), pluginName);
            return;
        }

        UtilUI.sendActionBar(initiator, locale.getUpdating() + " &8[ATTEMPTING DOWNLOAD]", 20);
        downloadResource();
    }

    @Override public void downloadResource() {
        UtilThreading.async(() -> {
            try {
                printDebug();
                Map<String, String> cookies = ((SpigotUser) currentUser).getCookies();

                cookies.forEach((key, value) -> webClient.getCookieManager().addCookie(
                        new Cookie("spigotmc.org", key, value)));

                webClient.waitForBackgroundJavaScript(10_000);
                webClient.getOptions().setThrowExceptionOnFailingStatusCode(false);
                Page page = webClient.getPage(siteAPI.getResourceManager().getResourceById(resourceId, currentUser).getDownloadURL());
                WebResponse response = page.getEnclosingWindow().getEnclosedPage().getWebResponse();

                if (page instanceof HtmlPage) {
                    HtmlPage htmlPage = (HtmlPage) page;
                    printDebug2(htmlPage);
                    if (htmlPage.asXml().contains("DDoS protection by Cloudflare")) {
                        UtilUI.sendActionBar(initiator, locale.getUpdating() + " &8[WAITING FOR CLOUDFLARE]", 20);
                        webClient.waitForBackgroundJavaScript(10_000);
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
                initializePlugin();
            } catch (Exception ex) {
                error(ex, "Error occurred while updating premium resource.", newVersion);
            }
        });
    }

    @Override public void initializePlugin() {
        //Copy plugin utility from src/main/resources
        String corePluginFile = "/autoupdater-plugin-" + InternalCore.PROPERTIES.VERSION + ".jar";
        try (InputStream is = getClass().getResourceAsStream(corePluginFile)) {
            File targetFile = new File(plugin.getDataFolder().getParent() + corePluginFile);
            Files.copy(is, targetFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            is.close();
            UtilThreading.sync(() -> {
                //Enable plugin and perform update task.
                try {
                    UpdaterPlugin updaterPlugin = (UpdaterPlugin) Bukkit.getPluginManager().loadPlugin(targetFile);
                    if (updaterPlugin == null)
                        throw new FileNotFoundException("Unable to locate updater plugin.");
                    Bukkit.getPluginManager().enablePlugin(updaterPlugin);
                    updaterPlugin.updatePlugin(plugin, initiator, replace, pluginName,
                            pluginFolderPath, locale, startingTime, endTask);
                } catch (Exception ex) {
                    error(ex, ex.getMessage(), newVersion);
                }
            });
        } catch (Exception ex) {
            error(ex, ex.getMessage(), newVersion);
        }
    }

    protected void authenticate(boolean recall) {
        UtilThreading.async(() -> {
            UtilUI.sendActionBar(initiator, locale.getUpdatingNoVar() + " &8[ATTEMPTING DECRYPT]", 10);

            String username = UtilSpigotCreds.getUsername();
            String password = UtilSpigotCreds.getPassword();
            String twoFactor = UtilSpigotCreds.getTwoFactor();

            if (username == null || password == null) {
                runGuis(recall);
                return;
            }

            try {
                //Spare the extra request
                if (twoFactor != null)
                    throw new TwoFactorAuthenticationException();

                UtilUI.sendActionBar(initiator, locale.getUpdatingNoVar() + " &8[ATTEMPTING AUTHENTICATION]", 15);
                currentUser = siteAPI.getUserManager().authenticate(username, password);

                if (currentUser == null) {
                    UtilUI.sendActionBar(initiator, locale.getUpdatingNoVar() + "&c [INVALID CACHED CREDENTIALS]");
                    UtilSpigotCreds.clearFile();
                    runGuis(recall);
                    return;
                }

                UtilUI.sendActionBar(initiator, locale.getUpdatingNoVar() + " &8[AUTHENTICATION SUCCESSFUL]");
                InternalCore.getLogger().info("Successfully logged in to Spigot as user '" + username + "'.");

                if (recall)
                    UtilThreading.sync(this::update);
            } catch (Exception ex) {
                if (ex instanceof TwoFactorAuthenticationException) {
                    try {
                        UtilUI.sendActionBar(initiator, locale.getUpdatingNoVar() + " &8[ATTEMPTING 2FA AUTHENTICATION]", 15);
                        if (twoFactor == null) {
                            requestTwoFactor(username, password, recall);
                            return;
                        }

                        currentUser = siteAPI.getUserManager().authenticate(username, password, twoFactor);

                        if (currentUser == null) {
                            UtilUI.sendActionBar(initiator, locale.getUpdatingNoVar() + " &c[INVALID CACHED CREDENTIALS]");
                            UtilSpigotCreds.clearFile();
                            runGuis(recall);
                            return;
                        }

                        UtilUI.sendActionBar(initiator, locale.getUpdatingNoVar() + " &8[AUTHENTICATION SUCCESSFUL]");
                        InternalCore.getLogger().info("Successfully logged in to Spigot as user '" + username + "'.");

                        if (recall)
                            UtilThreading.sync(this::update);
                    } catch (Exception otherException) {
                        if (otherException instanceof InvalidCredentialsException) {
                            UtilUI.sendActionBar(initiator, locale.getUpdatingNoVar() + " &c[INVALID CACHED CREDENTIALS]");
                            UtilSpigotCreds.clearFile();
                            runGuis(recall);
                        } else if (otherException instanceof ConnectionFailedException) {
                            UtilUI.sendActionBar(initiator, locale.getUpdateFailedNoVar());
                            InternalCore.get().printError(ex, "Error occurred while connecting to spigot. (#6)");
                            endTask.run(false, otherException, null, pluginName);
                        } else if (otherException instanceof TwoFactorAuthenticationException) {
                            if (currentUser != null) {
                                UtilThreading.sync(this::update);
                                return;
                            }

                            if (loginAttempts < 6) {
                                UtilUI.sendActionBar(initiator, locale.getUpdatingNoVar() + " &8[RE-TRYING LOGIN IN 5s ATTEMPT #" + loginAttempts + "/5]", 15);
                                loginAttempts++;
                                UtilThreading.syncDelayed(() -> authenticate(recall), 100);
                            } else {
                                loginAttempts = 1;
                                error(otherException, "All login attempts failed.");
                            }
                        } else {
                            InternalCore.get().printError(otherException);
                        }
                    }
                } else if (ex instanceof InvalidCredentialsException) {
                    UtilUI.sendActionBar(initiator, locale.getUpdatingNoVar() + " &c[INVALID CACHED CREDENTIALS]");
                    UtilSpigotCreds.clearFile();
                    runGuis(recall);
                } else if (ex instanceof ConnectionFailedException) {
                    UtilUI.sendActionBar(initiator, locale.getUpdatingNoVar() + " &8[RE-ATTEMPTING AUTHENTICATION]", 15);
                    InternalCore.get().printError(ex, "Error occurred while connecting to spigot. (#2)");
                    endTask.run(false, ex, null, pluginName);
                } else {
                    InternalCore.get().printError(ex);
                }
            }
        });
    }

    /*
     * LOGIN UI
     */

    private void runGuis(boolean recall) {
        UtilThreading.sync(() -> {
            UtilUI.sendActionBar(initiator, locale.getUpdatingNoVar() + " &8[RETRIEVING USERNAME]", 300);
            AtomicBoolean validInput = new AtomicBoolean(false);
            new AnvilGUI.Builder()
                    .text("Spigot username")
                    .plugin(InternalCore.getPlugin())
                    .onComplete((Player player, String usernameInput) -> {
                        try {
                            if (siteAPI.getUserManager().getUserByName(usernameInput) != null) {
                                validInput.set(true);
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
                    })
                    .onClose(player -> {
                        if (!validInput.get())
                            error(new InvalidCredentialsException(), "User closed GUI.");
                    })
                    .open(initiator);
        });
    }

    private void requestPassword(String usernameInput, boolean recall) {
        UtilUI.sendActionBar(initiator, locale.getUpdatingNoVar() + " &8[RETRIEVING PASSWORD]", 300);
        AtomicBoolean validInput = new AtomicBoolean(false);
        new AnvilGUI.Builder()
                .text("Spigot password")
                .plugin(InternalCore.getPlugin())
                .onComplete((player, passwordInput) -> {
                    try {
                        currentUser = siteAPI.getUserManager().authenticate(usernameInput, passwordInput);

                        UtilUI.sendActionBar(initiator, locale.getUpdatingNoVar() + " &8[ENCRYPTING CREDENTIALS]", 10);
                        UtilSpigotCreds.setUsername(usernameInput);
                        UtilSpigotCreds.setPassword(passwordInput);
                        UtilSpigotCreds.saveFile();

                        UtilThreading.syncDelayed(() -> authenticate(recall), 100);
                    } catch (TwoFactorAuthenticationException ex) {
                        validInput.set(true);
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
                })
                .onClose(player -> {
                    if (!validInput.get())
                        error(new InvalidCredentialsException(), "User closed GUI.");
                })
                .open(initiator);
    }

    private void requestTwoFactor(String usernameInput, String passwordInput, boolean recall) {
        UtilUI.sendActionBar(initiator, locale.getUpdatingNoVar() + " &8[RETRIEVING TWO FACTOR SECRET]", 300);
        AtomicBoolean validInput = new AtomicBoolean(false);
        new AnvilGUI.Builder().plugin(InternalCore.getPlugin())
                .text("Spigot two factor secret")
                .onComplete((Player player, String twoFactorInput) -> {
                    try {
                        currentUser = siteAPI.getUserManager().authenticate(usernameInput, passwordInput, twoFactorInput);
                        UtilUI.sendActionBar(initiator, locale.getUpdatingNoVar() + " &8[ENCRYPTING CREDENTIALS]", 10);

                        UtilSpigotCreds.setUsername(usernameInput);
                        UtilSpigotCreds.setPassword(passwordInput);
                        UtilSpigotCreds.setTwoFactor(twoFactorInput);
                        UtilSpigotCreds.saveFile();

                        validInput.set(true);
                        UtilThreading.syncDelayed(() -> authenticate(recall), 100);

                        return AnvilGUI.Response.text("Logging in, close GUI.");
                    } catch (Exception ex) {
                        error(ex, "Error occurred while authenticating Spigot user.");
                        return AnvilGUI.Response.text("Authentication failed");
                    }
                })
                .onClose(player -> {
                    if (!validInput.get())
                        error(new InvalidCredentialsException(), "User closed GUI.");
                })
                .open(initiator);
    }

    /*
     * ERROR HANDLING
     */

    private void error(Exception ex, String message) {
        InternalCore.get().printError(ex, message);
        UtilUI.sendActionBar(initiator, locale.getUpdateFailedNoVar());
        endTask.run(false, ex, null, pluginName);
    }

    private void error(Exception ex, String message, String newVersion) {
        InternalCore.get().printError(ex, message);
        UtilUI.sendActionBar(initiator, UtilText.format(locale.getUpdateFailed(),
                "old_version", currentVersion,
                "new_version", newVersion));
        endTask.run(false, ex, Bukkit.getPluginManager().getPlugin(pluginName), pluginName);
    }

    /*
     * DEBUG MESSAGES
     */

    private void printDebug() {
        if (InternalCore.DEBUG) {
            InternalCore.getLogger().info("\n\n\n\n\n\n============== BEGIN PREMIUM PLUGIN DEBUG ==============");
            InternalCore.getLogger().info("AUTHENTICATED: " + currentUser.isAuthenticated());
            InternalCore.getLogger().info("COOKIES: ");
            ((SpigotUser) currentUser).getCookies().forEach((k, v) -> InternalCore.getLogger().info("\t" + k + " | " + v));
        }
    }

    private void printDebug1(Page page, WebResponse response, WebClient webClient) {
        if (InternalCore.DEBUG) {
            if (pluginName != null)
                InternalCore.getLogger().info("PLUGIN = " + pluginName);

            if (page instanceof HtmlPage) {
                InternalCore.getLogger().info("\n\nPAGETYPE = HtmlPage");
                HtmlPage htmlPage = (HtmlPage) page;

                InternalCore.getLogger().info("PREVIOUS STATUS CODE = " + htmlPage.getWebResponse().getStatusCode());
                InternalCore.getLogger().info("HISTORY: ");
                for (int i = 0; i < page.getEnclosingWindow().getHistory().getLength(); i++) {
                    InternalCore.getLogger().info(htmlPage.getEnclosingWindow().getHistory().getUrl(i).toString());
                }
                InternalCore.getLogger().info("STATUS CODE = " + htmlPage.getEnclosingWindow().getEnclosedPage().getWebResponse().getStatusCode());
                htmlPage.getEnclosingWindow().getEnclosedPage().getWebResponse().getResponseHeaders().forEach(nvpair -> InternalCore.getLogger().info(nvpair.getName() + " | " + nvpair.getValue()));
            } else if (page instanceof UnexpectedPage) {
                InternalCore.getLogger().info("\n\nPAGETYPE = UnexpectedPage");
                UnexpectedPage unexpectedPage = (UnexpectedPage) page;
                InternalCore.getLogger().info("PREVIOUS STATUS CODE = " + unexpectedPage.getWebResponse().getStatusCode());
                InternalCore.getLogger().info("HISTORY: ");
                for (int i = 0; i < page.getEnclosingWindow().getHistory().getLength(); i++) {
                    InternalCore.getLogger().info(page.getEnclosingWindow().getHistory().getUrl(i).toString());
                }
                InternalCore.getLogger().info("STATUS CODE = " + unexpectedPage.getEnclosingWindow().getEnclosedPage().getWebResponse().getStatusCode());
                InternalCore.getLogger().info("NAME | VALUE");
                unexpectedPage.getEnclosingWindow().getEnclosedPage().getWebResponse().getResponseHeaders().forEach(nvpair -> InternalCore.getLogger().info(nvpair.getName() + " | " + nvpair.getValue()));
            }

            InternalCore.getLogger().info("\n\nPAGETYPE = Page");
            InternalCore.getLogger().info("HISTORY: ");
            for (int i = 0; i < page.getEnclosingWindow().getHistory().getLength(); i++) {
                InternalCore.getLogger().info(page.getEnclosingWindow().getHistory().getUrl(i).toString());
            }
            InternalCore.getLogger().info("PREVIOUS STATUS CODE = " + page.getWebResponse().getStatusCode());
            InternalCore.getLogger().info("STATUS CODE = " + response.getStatusCode());
            InternalCore.getLogger().info("CONTENT TYPE = " + response.getContentType());
            InternalCore.getLogger().info("STATUS MESSAGE = " + response.getStatusMessage());
            InternalCore.getLogger().info("LOAD TIME = " + response.getLoadTime());
            InternalCore.getLogger().info("CURRENT URL = " + webClient.getCurrentWindow().getEnclosedPage().getUrl());
            InternalCore.getLogger().info("NAME | VALUE");
            response.getResponseHeaders().forEach(nvpair -> InternalCore.getLogger().info(nvpair.getName() + " | " + nvpair.getValue()));
        }
    }

    private void printDebug2(HtmlPage htmlPage) {
        if (InternalCore.DEBUG) {
            InternalCore.getLogger().info("---- EARLY HTML OUTPUT ----");
            InternalCore.getLogger().info("\nPAGETYPE = HtmlPage");

            InternalCore.getLogger().info("PREVIOUS STATUS CODE = " + htmlPage.getWebResponse().getStatusCode());
            InternalCore.getLogger().info("HISTORY: ");
            for (int i = 0; i < htmlPage.getEnclosingWindow().getHistory().getLength(); i++) {
                InternalCore.getLogger().info(htmlPage.getEnclosingWindow().getHistory().getUrl(i).toString());
            }
            InternalCore.getLogger().info("PREVIOUS STATUS CODE = " + htmlPage.getWebResponse().getStatusCode());
            InternalCore.getLogger().info("STATUS CODE = " + htmlPage.getEnclosingWindow().getEnclosedPage().getWebResponse().getStatusCode());
            htmlPage.getEnclosingWindow().getEnclosedPage().getWebResponse().getResponseHeaders().forEach(nvpair -> InternalCore.getLogger().info(nvpair.getName() + " | " + nvpair.getValue()));
            InternalCore.getLogger().info("\n---- EARLY HTML OUTPUT ----");
        }
    }

    private void printDebug3(long downloadedFileSize, int completeFileSize) {
        if (InternalCore.DEBUG) {
            InternalCore.getLogger().info("FINISHED WITH " + downloadedFileSize + "/" + completeFileSize + " (" + String.format("%.2f", (((double) downloadedFileSize) / ((double) completeFileSize)) * 100) + "%)");
            InternalCore.getLogger().info("============== END PREMIUM PLUGIN DEBUG =======");
        }
    }
}
