package com.gamerking195.dev.autoupdaterapi.util;


import com.gamerking195.dev.autoupdaterapi.AutoUpdaterAPI;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.logging.Logger;

import java.io.*;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;

/*
 * Class is from MVdWUpdater in order to make this api work with the SpigotSiteAPI also made by Maxim Van de Wynckel "Maximvdw"
 *
 * MVdWUpdater https://www.spigotmc.org/resources/mvdwupdater.14803/
 * SpigotSiteAPI https://github.com/Maximvdw/SpigotSite-API
 */
public class UtilDownloader {
    private static final Class[] parameters = new Class[]{URL.class};

    public enum Library {
        HTMMLUNIT("http://repo.mvdw-software.be/content/groups/public/com/gargoylesoftware/HTMLUnit/2.15/HTMLUnit-2.15-OSGi.jar",
                         "HTMLUnit 2.15", "Used for HTTP connections with cloudflare protected sites", "htmlunit_2_15");

        private String url = "";
        private String name = "";
        private String description = "";
        private String fileName = "";

        Library(String url, String name, String description, String fileName) {
            setUrl(url);
            setName(name);
            setDescription(description);
            setFileName(fileName);
        }

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public String getFileName() {
            return fileName;
        }

        public void setFileName(String fileName) {
            this.fileName = fileName;
        }
    }

    /**
     * Download a library
     *
     * @param lib Library to download
     */
    public static void downloadLib(Library lib) {
        downloadLib(lib.getUrl(), lib.getName(), lib.getDescription(), lib.getFileName());
    }

    /**
     * Download a library
     *
     * @param url         URL to download it from
     * @param name        Name of the lib
     * @param description Description
     * @param fileName    filename to save it as
     */
    public static void downloadLib(String url, String name, String description, String fileName) {
        Logger SendConsole = AutoUpdaterAPI.getInstance().getLogger();

        String localPath = "./plugins/MVdWPlugin/lib/" + fileName + ".jar";
        if (!(new File(localPath).exists())) {
            SendConsole.info("Downloading " + name + " ...");
            SendConsole.info("Description: " + description);
            try {
                downloadFile(url, localPath);
            } catch (IOException e) {
                SendConsole.severe("An error occurred while downloading a required lib.");
                e.printStackTrace();
            }
        }

        SendConsole.info("Loading dependency " + name + " ...");

        try {
            addURL(new URL("jar:file:" + localPath + "!/"));
            SendConsole.info(name + " is now loaded!");
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public static void downloadFile(String url, String location) throws IOException {
        URL website = new URL(url);
        ReadableByteChannel rbc = Channels.newChannel(website.openStream());
        File yourFile = new File(location);
        yourFile.getParentFile().mkdirs();
        if (!yourFile.exists()) {
            yourFile.createNewFile();
        }
        FileOutputStream fos = new FileOutputStream(yourFile);
        fos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);
        fos.close();
    }

    public static void addURL(URL u) throws IOException {

        URLClassLoader sysloader = (URLClassLoader) ClassLoader.getSystemClassLoader();
        Class sysclass = URLClassLoader.class;

        try {
            Method method = sysclass.getDeclaredMethod("addURL", parameters);
            method.setAccessible(true);
            method.invoke(sysloader, new Object[]{u});
        } catch (Throwable t) {
            t.printStackTrace();
            throw new IOException("Error, could not add URL to system classloader");
        }

    }
}