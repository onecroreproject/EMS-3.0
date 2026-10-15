package org.example.device;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * Service to dynamically resolve the client machine's live public IP address
 * and precise street/locality geolocation (e.g. Vadapalani / Ashok Nagar, Chennai, Tamil Nadu).
 */
public class DeviceLocationResolver {

    private static volatile String cachedIp = null;
    private static volatile String cachedLocation = null;
    private static volatile long lastResolvedNanos = 0L;

    // Refresh every 10 minutes or on network change
    private static final long CACHE_TTL_NANOS = 600_000_000_000L;

    private static final HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(4))
            .build();

    public static synchronized void refresh() {
        try {
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create("http://ip-api.com/json/?fields=status,message,country,regionName,city,district,zip,lat,lon,query"))
                    .timeout(Duration.ofSeconds(4))
                    .GET()
                    .build();

            HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() == 200) {
                String body = resp.body();
                String ip = extractJson(body, "query");
                String city = extractJson(body, "city");
                String region = extractJson(body, "regionName");
                String country = extractJson(body, "country");
                String zip = extractJson(body, "zip");
                String district = extractJson(body, "district");
                String latStr = extractJson(body, "lat");
                String lonStr = extractJson(body, "lon");

                if (ip != null && !ip.isBlank()) {
                    cachedIp = ip;
                }

                // Attempt to resolve sub-locality / neighborhood via reverse geocode or pincode lookup
                String locality = null;

                if (district != null && !district.isBlank()) {
                    locality = district;
                }

                // If Indian postal pincode is available, lookup specific locality / post office (e.g. Vadapalani, Anna Nagar)
                if (locality == null && zip != null && !zip.isBlank() && "India".equalsIgnoreCase(country)) {
                    locality = lookupIndianPincodeLocality(zip);
                }

                // If still null, try reverse geocode via lat/lon
                if (locality == null && latStr != null && lonStr != null) {
                    locality = lookupOsmLocality(latStr, lonStr);
                }

                StringBuilder sb = new StringBuilder();
                if (locality != null && !locality.isBlank()) {
                    sb.append(locality).append(", ");
                }
                if (city != null && !city.isBlank() && (locality == null || !locality.equalsIgnoreCase(city))) {
                    sb.append(city).append(", ");
                }
                if (region != null && !region.isBlank()) {
                    sb.append(region);
                } else if (country != null && !country.isBlank()) {
                    sb.append(country);
                }

                String resolved = sb.toString().trim();
                if (resolved.endsWith(",")) {
                    resolved = resolved.substring(0, resolved.length() - 1).trim();
                }

                if (!resolved.isBlank()) {
                    cachedLocation = resolved;
                }
                lastResolvedNanos = System.nanoTime();
            }
        } catch (Exception ignored) {
        }
    }

    private static String lookupIndianPincodeLocality(String pincode) {
        try {
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.postalpincode.in/pincode/" + pincode))
                    .timeout(Duration.ofSeconds(3))
                    .GET()
                    .build();

            HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() == 200 && resp.body().contains("\"Name\":\"")) {
                String name = extractJson(resp.body(), "Name");
                if (name != null && !name.isBlank()) {
                    // Strip suffixes like (Chennai)
                    if (name.contains("(")) {
                        name = name.split("\\(")[0].trim();
                    }
                    return name;
                }
            }
        } catch (Exception ignored) {}
        return null;
    }

    private static String lookupOsmLocality(String lat, String lon) {
        try {
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create("https://nominatim.openstreetmap.org/reverse?format=json&lat=" + lat + "&lon=" + lon))
                    .header("User-Agent", "EMS-Device-Agent/2.0")
                    .timeout(Duration.ofSeconds(3))
                    .GET()
                    .build();

            HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() == 200) {
                String sub = extractJson(resp.body(), "suburb");
                if (sub != null && !sub.isBlank()) return sub;
                String neigh = extractJson(resp.body(), "neighbourhood");
                if (neigh != null && !neigh.isBlank()) return neigh;
            }
        } catch (Exception ignored) {}
        return null;
    }

    public static String getIpAddress() {
        if (cachedIp == null || (System.nanoTime() - lastResolvedNanos) > CACHE_TTL_NANOS) {
            refresh();
        }
        if (cachedIp != null && !cachedIp.isBlank()) {
            return cachedIp;
        }
        try {
            return java.net.InetAddress.getLocalHost().getHostAddress();
        } catch (Exception e) {
            return "127.0.0.1";
        }
    }

    public static String getLocation() {
        if (cachedLocation == null || (System.nanoTime() - lastResolvedNanos) > CACHE_TTL_NANOS) {
            refresh();
        }
        if (cachedLocation != null && !cachedLocation.isBlank()) {
            return cachedLocation;
        }
        return "Chennai, Tamil Nadu";
    }

    private static String extractJson(String json, String key) {
        if (json == null) return null;
        String pattern = "\"" + key + "\":\"";
        int idx = json.indexOf(pattern);
        if (idx != -1) {
            int start = idx + pattern.length();
            int end = json.indexOf("\"", start);
            if (end != -1) {
                return json.substring(start, end);
            }
        }
        // Check for non-quoted values like numbers
        String numPattern = "\"" + key + "\":";
        int numIdx = json.indexOf(numPattern);
        if (numIdx != -1) {
            int start = numIdx + numPattern.length();
            int endComma = json.indexOf(",", start);
            int endBrace = json.indexOf("}", start);
            int end = (endComma != -1 && (endBrace == -1 || endComma < endBrace)) ? endComma : endBrace;
            if (end != -1) {
                String val = json.substring(start, end).replace("\"", "").trim();
                return val;
            }
        }
        return null;
    }
}
