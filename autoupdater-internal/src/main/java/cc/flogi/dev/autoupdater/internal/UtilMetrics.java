package cc.flogi.dev.autoupdater.internal;

import com.google.gson.*;
import com.google.gson.annotations.SerializedName;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.*;

/**
 * @author Caden Kriese (flogic)
 *
 * Created on 01/01/2020
 */
final class UtilMetrics {
    private static final String API_BASE_URL = "https://api.flogi.cc/updater-metrics/v1";
    private static final Gson GSON = new GsonBuilder()
            .setDateFormat("yyyy-MM-dd HH:mm:ss")
            .create();

    private static String token;
    private static LocalDateTime expirationDate;

    static void sendUpdateInfo(Plugin plugin, PluginUpdate update) {
        if (AutoUpdaterInternal.METRICS) {
            String type = plugin instanceof SpigotPlugin ? "spigot" : "public";

            if (plugin.getId() == null)
                plugin.setId(getPluginID(plugin));

            if (plugin.getId() != null) {
                plugin.setUpdates(null);
                sendRequest("POST", API_BASE_URL + "/plugins/" + plugin.getId() + "/updates", GSON.toJson(update));
                sendRequest("PUT", API_BASE_URL + "/plugins/" + plugin.getId(), GSON.toJson(plugin));
            } else {
                plugin.setUpdates(Collections.singletonList(update));
                sendRequest("POST", API_BASE_URL + "/plugins?type=" + type, GSON.toJson(plugin));
            }
        }
    }

    static UtilMetrics.SpigotPlugin getSpigotPlugin(org.bukkit.plugin.Plugin plugin, int resourceId) {
        final String SPIGET_BASE_URL = "https://api.spiget.org/v2/";
        final String SPIGOT_BASE_URL = "https://spigotmc.org/";

        try (InputStream is = new URL(SPIGET_BASE_URL + "resources/" + resourceId).openStream()) {
            BufferedReader rd = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();

            int cp;
            while ((cp = rd.read()) != -1) {
                sb.append((char) cp);
            }

            String spigetResponse = sb.toString();

            JsonParser parser = new JsonParser();
            JsonObject json = parser.parse(spigetResponse).getAsJsonObject();

            String resourceName = json.get("name").getAsString();
            long categoryId = json.getAsJsonObject("category").get("id").getAsLong();
            String categoryInfo = UtilMetrics.readFrom(SPIGET_BASE_URL + "categories/" + categoryId);
            String categoryName = parser.parse(categoryInfo).getAsJsonObject().get("name").getAsString();
            JsonArray supportedVersionsObj = json.getAsJsonArray("testedVersions");
            List<String> supportedVersions = new ArrayList<>();
            supportedVersionsObj.iterator().forEachRemaining(element -> supportedVersions.add(element.getAsString()));
            Date uploadDate = new Date(json.get("releaseDate").getAsLong() * 1000);
            Double averageRating = json.getAsJsonObject("rating").get("average").getAsDouble();
            String downloadUrl = SPIGOT_BASE_URL + json.getAsJsonObject("file").get("url").getAsString();
            boolean premium = json.get("premium").getAsBoolean();

            if (premium) {
                Double price = json.get("price").getAsDouble();
                String currency = json.get("currency").getAsString();
                return new UtilMetrics.SpigotPlugin(plugin.getName(), plugin.getDescription().getDescription(),
                        downloadUrl, resourceName, resourceId, categoryName, averageRating, uploadDate,
                        supportedVersions.toArray(new String[]{}), premium, price, currency);
            }

            return new UtilMetrics.SpigotPlugin(plugin.getName(), plugin.getDescription().getDescription(),
                    downloadUrl, resourceName, resourceId, categoryName, averageRating, uploadDate,
                    supportedVersions.toArray(new String[]{}));

        } catch (IOException ex) {
            if (AutoUpdaterInternal.DEBUG)
                ex.printStackTrace();
            return null;
        }
    }

    private static UUID getPluginID(Plugin plugin) {
        StringBuilder params = new StringBuilder();
        Set<Map.Entry<String, JsonElement>> jsonPlugin;

        //Remove categories that are subject to change.
        if (plugin instanceof SpigotPlugin) {
            SpigotPlugin spigotPlugin = new SpigotPlugin((SpigotPlugin) plugin);
            spigotPlugin.setDescription(null);
            spigotPlugin.setDownloadUrl(null);
            spigotPlugin.setUpdates(null);
            spigotPlugin.setAverageRating(null);
            spigotPlugin.setCurrency(null);
            spigotPlugin.setPrice(null);
            spigotPlugin.setSupportedVersions(null);
            spigotPlugin.setCategory(null);

            jsonPlugin = GSON.toJsonTree(spigotPlugin).getAsJsonObject().entrySet();
        } else {
            Plugin publicPlugin = new Plugin(plugin);
            publicPlugin.setDescription(null);
            publicPlugin.setDownloadUrl(null);
            publicPlugin.setUpdates(null);

            jsonPlugin = GSON.toJsonTree(publicPlugin).getAsJsonObject().entrySet();
        }

        try {
            int i = 0;
            for (Map.Entry<String, JsonElement> entry : jsonPlugin) {
                if (i == 0)
                    params.append("?");
                else
                    params.append("&");
                params.append(URLEncoder.encode(entry.getKey(), StandardCharsets.UTF_8.name()))
                        .append("=")
                        .append(URLEncoder.encode(entry.getValue().getAsString(), StandardCharsets.UTF_8.name()));
                i++;
            }

            String response = readFrom(API_BASE_URL + "/plugins" + params.toString());
            if (!response.contains("error")) {
                JsonArray array = new JsonParser().parse(response).getAsJsonArray();
                if (array.size() == 0)
                    return null;
                JsonObject elem = array.get(0).getAsJsonObject();
                if (elem.get("id") == null)
                    return null;
                return UUID.fromString(elem.get("id").getAsString());
            }
        } catch (IOException ex) {
            if (AutoUpdaterInternal.DEBUG && !(ex instanceof FileNotFoundException))
                ex.printStackTrace();
        }

        return null;
    }

    private static void sendRequest(String requestMethod, String urlString, String body) {
        if (AutoUpdaterInternal.DEBUG)
            AutoUpdaterInternal.getLogger().info("SENDING " + requestMethod + " TO " + urlString + " \n\tBODY:\n" + body);

        try {
            auth();

            StringBuilder requestData = new StringBuilder();

            URL url = new URL(urlString);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod(requestMethod.toUpperCase());
            conn.setDoOutput(true);

            conn.setRequestProperty("Authorization", "Bearer " + token);
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("Content-Length", Integer.toString(body.length()));

            conn.setFixedLengthStreamingMode(body.length());
            conn.connect();

            try (DataOutputStream writer = new DataOutputStream(conn.getOutputStream())) {
                writer.write(body.getBytes(StandardCharsets.UTF_8));
                writer.flush();

                int responseCode = conn.getResponseCode();

                try (InputStream is = conn.getInputStream()) {
                    String responseBody = buildStringFromInputStream(is);

                    if (responseCode == 401 && responseBody.contains("token has expired")) {
                        token = null;
                        sendRequest(requestMethod, urlString, body);
                    } else if (responseCode != 200 && AutoUpdaterInternal.DEBUG)
                        AutoUpdaterInternal.getLogger().info("Metrics request failed code = " + responseCode);
                }
            } finally {
                conn.disconnect();
            }
        } catch (IOException ex) {
            if (AutoUpdaterInternal.DEBUG)
                ex.printStackTrace();
        }
    }

    private static void auth() {
        if (token == null || LocalDateTime.now().isAfter(expirationDate)) {
            try {
                JsonParser parser = new JsonParser();
                JsonObject obj = parser.parse(readFrom(API_BASE_URL + "/auth")).getAsJsonObject();
                token = obj.get("token").getAsString();
                expirationDate = LocalDateTime.now().plusDays(7);
            } catch (IOException ex) {
                if (AutoUpdaterInternal.DEBUG)
                    ex.printStackTrace();
            }
        }
    }

    /**
     * Reads content from a URL.
     *
     * Note has to be a duplicate of ${@link UtilIO#readFromURL(String)} to maintain package-private scope.
     *
     * @param url The URL to read from.
     * @return The content from the page.
     * @throws IOException from stream.
     */
    static String readFrom(String url) throws IOException {
        try (InputStream is = new URL(url).openStream()) {
            return buildStringFromInputStream(is);
        }
    }

    private static String buildStringFromInputStream(InputStream inputStream) throws IOException {
        BufferedReader rd = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
        StringBuilder sb = new StringBuilder();

        int cp;
        while ((cp = rd.read()) != -1) {
            sb.append((char) cp);
        }
        return sb.toString();
    }

    @AllArgsConstructor @Getter @Setter
    static final class PluginUpdateVersion {
        @SerializedName("old")
        private String oldVersion;
        @SerializedName("new")
        private String newVersion;
    }

    @AllArgsConstructor @Getter @Setter
    static final class PluginUpdate {
        private Date timestamp;
        private Long size;
        @SerializedName("update_duration")
        private Double updateDuration;
        private PluginUpdateVersion version;

        PluginUpdate(Date timestamp, PluginUpdateVersion version) {
            this.timestamp = timestamp;
            this.version = version;
        }
    }

    @AllArgsConstructor @Getter @Setter
    static class Plugin {
        private UUID id;
        private String name;
        private String description;
        @SerializedName("download_url")
        private String downloadUrl;
        private List<PluginUpdate> updates;

        private Plugin() {}

        Plugin(Plugin plugin) {
            this(plugin.getName(), plugin.getDescription(), plugin.getDownloadUrl());
            updates = plugin.getUpdates();
            id = plugin.getId();
        }

        Plugin(String name, String description, String downloadUrl) {
            this(null, name, description, downloadUrl, null);
        }
    }

    @Getter @Setter
    static final class SpigotPlugin extends Plugin {
        @SerializedName("spigot_name")
        private String spigotName;
        @SerializedName("resource_id")
        private int resourceId;
        private String category;
        @SerializedName("average_rating")
        private Double averageRating;
        @SerializedName("upload_date")
        private Date uploadDate;
        @SerializedName("supported_versions")
        private String[] supportedVersions;
        private Boolean premium;
        private Double price;
        private String currency;

        SpigotPlugin(SpigotPlugin plugin) {
            this(plugin.getName(), plugin.getDescription(), plugin.getDownloadUrl(), plugin.getSpigotName(),
                    plugin.getResourceId(), plugin.getCategory(), plugin.getAverageRating(), plugin.getUploadDate(),
                    plugin.getSupportedVersions(), plugin.getPremium(), plugin.getPrice(), plugin.getCurrency());
        }

        SpigotPlugin(String name, String description, String downloadUrl, String spigotName, int resourceId,
                     String category, Double averageRating, Date uploadDate, String[] supportedVersions) {
            this(name, description, downloadUrl, spigotName, resourceId, category, averageRating, uploadDate,
                    supportedVersions, false, null, null);
        }

        SpigotPlugin(String name, String description, String downloadUrl, String spigotName, int resourceId,
                     String category, Double averageRating, Date uploadDate, String[] supportedVersions, boolean premium, Double price, String currency) {
            super(name, description, downloadUrl);

            this.spigotName = spigotName;
            this.resourceId = resourceId;
            this.category = category;
            this.averageRating = averageRating;
            this.uploadDate = uploadDate;
            this.supportedVersions = supportedVersions;
            this.premium = premium;
            this.price = price;
            this.currency = currency;
        }
    }
}
