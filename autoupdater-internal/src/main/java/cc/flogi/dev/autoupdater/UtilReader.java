package cc.flogi.dev.autoupdater;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;

/**
 * @author Caden Kriese (flogic)
 *
 * Created on 8/12/17
 */
final class UtilReader {
    /**
     * Reads content from a URL.
     *
     * @param url The URL to read from.
     *
     * @return The content from the page.
     * @throws IOException from stream.
     */
    public static String readFrom(String url) throws IOException {
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
}
