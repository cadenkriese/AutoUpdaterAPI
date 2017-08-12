package com.gamerking195.dev.autoupdaterapi.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.Charset;

/**
 * Created by GamerKing195 on 8/12/17.
 * <p>
 * License is specified by the distributor which this
 * file was written for. Otherwise it can be found in the LICENSE file.
 */
public class UtilReader {
    public static String readFrom(String url) throws IOException {
        try (InputStream is = new URL(url).openStream()) {
            BufferedReader rd = new BufferedReader(new InputStreamReader(is, Charset.forName("UTF-8")));

            StringBuilder sb = new StringBuilder();
            int cp;
            while ((cp = rd.read()) != -1) {
                sb.append((char) cp);
            }
            return sb.toString();
        }
    }
}
