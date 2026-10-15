
package com.epicdrops;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class DropDataService
{
    private final Map<String, List<String>> cache =
            new ConcurrentHashMap<>();

    private final Map<String, String> errors =
            new ConcurrentHashMap<>();

    private final Map<String, Long> retryAfter =
            new ConcurrentHashMap<>();

    private final Set<String> loading =
            ConcurrentHashMap.newKeySet();

    private final ExecutorService executor =
            Executors.newSingleThreadExecutor(task ->
            {
                Thread thread = new Thread(
                        task, "npc-drop-wiki-sync");
                thread.setDaemon(true);
                return thread;
            });

    private final HttpClient httpClient =
            HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(10))
                    .build();

    public List<String> getDrops(int npcId, String npcName)
    {
        if (npcName == null || npcName.isBlank())
        {
            return Collections.emptyList();
        }

        String key = npcName.trim().toLowerCase(Locale.ROOT);

        if (!cache.containsKey(key)
                && !loading.contains(key)
                && System.currentTimeMillis()
                >= retryAfter.getOrDefault(key, 0L))
        {
            if (loading.add(key))
            {
                executor.submit(() -> loadDrops(key, npcName.trim()));
            }
        }

        return cache.getOrDefault(
                key, Collections.emptyList());
    }

    public String getStatus(String npcName)
    {
        if (npcName == null)
        {
            return "Unknown NPC";
        }

        String key = npcName.trim().toLowerCase(Locale.ROOT);

        if (loading.contains(key))
        {
            return "Syncing with OSRS Wiki...";
        }

        if (errors.containsKey(key))
        {
            return errors.get(key);
        }

        if (cache.containsKey(key))
        {
            return "No drops found in Wiki data";
        }

        return "Waiting for Wiki sync...";
    }

    private void loadDrops(String key, String npcName)
    {
        try
        {
            String wikiText = fetchWikiData(npcName);
            List<String> drops = parseDrops(wikiText);

            cache.put(key, List.copyOf(drops));
            errors.remove(key);
            retryAfter.remove(key);
        }
        catch (Exception exception)
        {
            errors.put(key, "Wiki sync failed - retrying later");
            retryAfter.put(
                    key, System.currentTimeMillis() + 60000L);

            System.err.println(
                    "[NPC Drop Inspector] Wiki error for "
                            + npcName + ": " + exception);
        }
        finally
        {
            loading.remove(key);
        }
    }

    public String fetchWikiData(String npcName) throws Exception
    {
        String page = URLEncoder.encode(
                npcName, StandardCharsets.UTF_8);

        String url =
                "https://oldschool.runescape.wiki/api.php"
                        + "?action=parse"
                        + "&page=" + page
                        + "&prop=wikitext"
                        + "&format=json"
                        + "&formatversion=2";

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("User-Agent",
                        "EpicDrops/1.0 "
                                + "(https://github.com/Glynsera/epic-drops)")
                .timeout(Duration.ofSeconds(15))
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(
                request,
                HttpResponse.BodyHandlers.ofString()
        );

        if (response.statusCode() != 200)
        {
            throw new IllegalStateException(
                    "HTTP " + response.statusCode());
        }

        // Compatible with older Gson versions
        JsonObject json = new JsonParser()
                .parse(response.body())
                .getAsJsonObject();

        if (json.has("error"))
        {
            throw new IllegalStateException(
                    json.get("error").toString());
        }

        return json.getAsJsonObject("parse")
                .get("wikitext").getAsString();
    }

    private List<String> parseDrops(String wikiText)
    {
        List<String> drops = new ArrayList<>();

        int position = 0;

        while (position < wikiText.length())
        {
            int start = wikiText.indexOf("{{", position);

            if (start < 0)
            {
                break;
            }

            int end = findTemplateEnd(wikiText, start);

            if (end < 0)
            {
                break;
            }

            String template = wikiText.substring(
                    start + 2, end - 2);

            List<String> parts = splitTopLevel(template);

            if (!parts.isEmpty())
            {
                String type = parts.get(0).trim()
                        .toLowerCase(Locale.ROOT);

                if (type.equals("dropsline")
                        || type.equals("dropslineclue"))
                {
                    String name = "";
                    String quantity = "";
                    String rarity = "";

                    for (int i = 1; i < parts.size(); i++)
                    {
                        String part = parts.get(i);
                        int equals = part.indexOf('=');

                        if (equals < 0)
                        {
                            continue;
                        }

                        String field = part.substring(0, equals)
                                .trim().toLowerCase(Locale.ROOT);

                        String value = part.substring(equals + 1)
                                .trim();

                        if (field.equals("name"))
                        {
                            name = clean(value);
                        }
                        else if (field.equals("quantity"))
                        {
                            quantity = clean(value);
                        }
                        else if (field.equals("rarity"))
                        {
                            rarity = clean(value);
                        }
                    }

                    if (!name.isBlank())
                    {
                        StringBuilder drop =
                                new StringBuilder(name);

                        if (!quantity.isBlank())
                        {
                            drop.append(" x").append(quantity);
                        }

                        if (!rarity.isBlank())
                        {
                            drop.append(" - ").append(rarity);
                        }

                        drops.add(drop.toString());
                    }
                }
            }

            position = end;
        }

        return drops;
    }

    private int findTemplateEnd(String text, int start)
    {
        int depth = 0;

        for (int i = start; i < text.length() - 1; i++)
        {
            if (text.startsWith("{{", i))
            {
                depth++;
                i++;
            }
            else if (text.startsWith("}}", i))
            {
                depth--;
                i++;

                if (depth == 0)
                {
                    return i + 1;
                }
            }
        }

        return -1;
    }

    private List<String> splitTopLevel(String text)
    {
        List<String> result = new ArrayList<>();
        int templateDepth = 0;
        int linkDepth = 0;
        int last = 0;

        for (int i = 0; i < text.length(); i++)
        {
            if (text.startsWith("{{", i))
            {
                templateDepth++;
                i++;
            }
            else if (text.startsWith("}}", i))
            {
                templateDepth--;
                i++;
            }
            else if (text.startsWith("[[", i))
            {
                linkDepth++;
                i++;
            }
            else if (text.startsWith("]]", i))
            {
                linkDepth--;
                i++;
            }
            else if (text.charAt(i) == '|'
                    && templateDepth == 0
                    && linkDepth == 0)
            {
                result.add(text.substring(last, i));
                last = i + 1;
            }
        }

        result.add(text.substring(last));
        return result;
    }

    private String clean(String value)
    {
        return value
                .replaceAll("\\[\\[([^\\]|]+)\\|([^\\]]+)\\]\\]", "$2")
                .replaceAll("\\[\\[([^\\]]+)\\]\\]", "$1")
                .replaceAll("<[^>]+>", "")
                .replaceAll("\\{\\{[^{}]*\\}\\}", "")
                .replace("&nbsp;", " ")
                .replace("<", "")
                .replace(">", "")
                .trim();
    }

    public void shutdown()
    {
        executor.shutdownNow();
    }
}

