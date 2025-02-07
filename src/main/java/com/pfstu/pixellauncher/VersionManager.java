package com.pfstu.pixellauncher;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class VersionManager {
    public static final String VERSION_MANIFEST_URL =
            "https://launchermeta.mojang.com/mc/game/version_manifest.json";

    public List<String> fetchVersions() throws IOException {
        HttpClient client = HttpClientBuilder.create().build();
        HttpGet request = new HttpGet(VERSION_MANIFEST_URL);
        HttpResponse response = client.execute(request);
        String json = EntityUtils.toString(response.getEntity());

        JsonObject manifest = JsonParser.parseString(json).getAsJsonObject();
        JsonArray versions = manifest.getAsJsonArray("versions");

        List<String> versionList = new ArrayList<>();
        for (JsonElement element : versions) {
            String version = element.getAsJsonObject().get("id").getAsString();
            versionList.add(version);
        }
        return versionList;
    }
}