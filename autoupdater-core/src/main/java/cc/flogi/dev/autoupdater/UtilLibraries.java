package cc.flogi.dev.autoupdater;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
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
public class UtilLibraries {
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

    protected static void downloadPremiumSupport() throws IOException {
        File dataFolder = new File(InternalCore.getDataFolder().getAbsolutePath() + "/libs");
        if (!dataFolder.exists() && !dataFolder.mkdirs())
            throw new IOException("Failed to create directories");

        ProjectProperties properties = InternalCore.PROPERTIES;

        //Cleanup unused versions.
        for (File file : dataFolder.listFiles()) {
            if (file.getName().contains("autoupdater-premium")) {
                if (file.getName().contains(properties.VERSION)) {
                    addURL(file);
                } else
                    file.delete();
            }
        }

        String urlString = properties.ARTIFACTORY_URL + "autoupdater-premium/" + properties.VERSION + "/autoupdater-premium-3.0.1-SNAPSHOT.jar";
        String downloadPath = InternalCore.getDataFolder().getAbsolutePath() + "/libs/autoupdater-premium-" + properties.VERSION + ".jar";
        URL url = new URL(urlString);

        ReadableByteChannel readableByteChannel = Channels.newChannel(url.openStream());
        FileOutputStream fileOutputStream = new FileOutputStream(downloadPath);
        FileChannel fileChannel = fileOutputStream.getChannel();
        fileOutputStream.getChannel().transferFrom(readableByteChannel, 0, Long.MAX_VALUE);

        readableByteChannel.close();
        fileOutputStream.close();
        fileChannel.close();

        addURL(new File(downloadPath));
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
