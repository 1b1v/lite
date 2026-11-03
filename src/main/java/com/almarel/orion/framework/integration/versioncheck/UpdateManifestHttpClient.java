package com.almarel.orion.framework.integration.versioncheck;

import com.google.gson.Gson;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.almarel.orion.framework.integration.versioncheck.data.VersionManifest;
import org.bukkit.plugin.java.JavaPlugin;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * Fetches manifest.json from the orion-network repository.
 * Pure networking + parsing — no caching, no scheduling, no persistence.
 * Mirrors the style of AssetHttpClient already used for schematic asset sync.
 */
@Singleton
public class UpdateManifestHttpClient {

    private static final String MANIFEST_URL =
            "https://raw.githubusercontent.com/1b1v/network/main/manifest.json";
    private static final Duration TIMEOUT = Duration.ofSeconds(10);

    private final Gson gson;
    private final HttpClient httpClient;

    @Inject
    public UpdateManifestHttpClient(JavaPlugin plugin, Gson gson) {
        this.gson = gson;
        this.httpClient = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.NORMAL)
                .connectTimeout(TIMEOUT)
                .build();
    }

    /**
     * Blocking call. Must be invoked from an async thread.
     */
    public VersionManifest fetchManifest() throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(MANIFEST_URL))
                .timeout(TIMEOUT)
                .header("User-Agent", "1b1v-UpdateCheck/1.0")
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new IllegalStateException("HTTP " + response.statusCode() + " while fetching manifest");
        }

        return gson.fromJson(response.body(), VersionManifest.class);
    }
}