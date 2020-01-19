package cc.flogi.dev.autoupdater.plugin;

import java.io.*;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.stream.Stream;

/**
 * @author Caden Kriese (flogic)
 *
 * Created on 8/12/17
 */
@SuppressWarnings("DuplicatedCode") final class UtilIO {
    /**
     * Reads content from a URL.
     *
     * @param url The URL to read from.
     * @return The content from the page.
     * @throws IOException from stream.
     */
    static String readFromURL(String url) throws IOException {
        try (InputStream is = new URL(url).openStream()) {
            BufferedReader rd = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();

            int cp;
            while ((cp = rd.read()) != -1) {
                sb.append((char) cp);
            }
            return sb.toString();
        }
    }

    /**
     * Writes string to specified file.
     *
     * @param destination The file to be written to.
     * @param content     The content to be written to the file.
     * @throws IOException Upon error finding or writing to the file.
     */
    static void writeToFile(File destination, String content) throws IOException {
        BufferedWriter writer = new BufferedWriter(new FileWriter(destination));
        writer.append(content);

        writer.close();
    }

    static String readFromFile(File source) throws IOException {
        StringBuilder contentBuilder = new StringBuilder();
        try (Stream<String> stream = Files.lines(source.toPath(), StandardCharsets.UTF_8)) {
            stream.forEach(s -> contentBuilder.append(s).append("\n"));
        }
        return contentBuilder.toString();
    }
}
