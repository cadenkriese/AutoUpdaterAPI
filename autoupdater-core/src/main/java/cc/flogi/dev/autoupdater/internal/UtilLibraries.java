package cc.flogi.dev.autoupdater.internal;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;

/**
 * @author Caden Kriese (flogic)
 *
 * Created on 12/14/19
 */
final class UtilLibraries {
    private static final Method ADD_URL_METHOD;

    static {
        Method addUrlMethod = null;

        try {
            addUrlMethod = URLClassLoader.class.getDeclaredMethod("addURL", URL.class);
            addUrlMethod.setAccessible(true);
        } catch (NoSuchMethodException ex) {
            ex.printStackTrace();
        }

        ADD_URL_METHOD = addUrlMethod;
    }

    static void downloadPremiumSupport(File location) throws IOException {
        if (!location.exists() && !location.mkdirs())
            throw new IOException("Failed to create directories");

        ProjectProperties properties = AutoUpdaterInternal.PROPERTIES;

        //Cleanup unused versions.
        for (File file : location.listFiles()) {
            if (file.getName().contains("autoupdater-premium")) {
                //Ensure we're adding the correct version and not a corrupt download.
                if (file.getName().contains(properties.VERSION) && file.length() > 10_000_000) {
                    addURL(file);
                    return;
                } else if (!file.delete())
                    throw new IOException("Failed to delete old resource file.");
            }
        }

        File downloadLocation = new File(location.getAbsolutePath() + "/autoupdater-premium-" + properties.VERSION + ".jar");
        downloadLibrary(downloadLocation);
        addURL(downloadLocation);
    }

    static void downloadLibrary(File location) throws IOException {
        ProjectProperties properties = AutoUpdaterInternal.PROPERTIES;

        String urlString = properties.ARTIFACTORY_URL + "autoupdater-premium/" + properties.VERSION + "/autoupdater-premium-" + properties.VERSION + ".jar";
        URL url = new URL(urlString);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestProperty("User-Agent", UserAgent.CHROME.toString());

        ReadableByteChannel readableByteChannel = Channels.newChannel(connection.getInputStream());
        FileOutputStream fileOutputStream = new FileOutputStream(location.getAbsolutePath());
        FileChannel fileChannel = fileOutputStream.getChannel();
        fileOutputStream.getChannel().transferFrom(readableByteChannel, 0, Long.MAX_VALUE);

        readableByteChannel.close();
        fileOutputStream.close();
        fileChannel.close();
    }

    private static void addURL(File file) throws MalformedURLException {
        URL url = new URL("jar:file:" + file.getAbsolutePath() + "!/");

        ClassLoader classLoader = AutoUpdaterAPI.class.getClassLoader();
        if (classLoader instanceof URLClassLoader) {
            try {
                ADD_URL_METHOD.invoke(classLoader, url);
            } catch (IllegalAccessException | InvocationTargetException ex) {
                throw new RuntimeException("Unable to invoke URLClassLoader#addURL", ex);
            }
        } else
            throw new RuntimeException("Unknown classloader: " + classLoader.getClass());
    }
}
