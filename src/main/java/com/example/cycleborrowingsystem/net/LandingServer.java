package com.example.cycleborrowingsystem.net;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import javafx.application.Platform;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;

public class LandingServer {
    private static LandingServer instance;
    private HttpServer server;  // Not final to support resilient init
    private final String pairToken;
    private final int port;
    private final ConcurrentMap<String, LastLocation> lastLocations = new ConcurrentHashMap<>();
    private final ScheduledExecutorService janitor = Executors.newSingleThreadScheduledExecutor();
    private volatile BiConsumerOnLocation onLocation;
    private volatile boolean isStarted = false;

    public static LandingServer getInstance() {
        return instance;
    }

    public LandingServer(int port, BiConsumerOnLocation initialListener) throws IOException {
        this.port = port;
        this.onLocation = initialListener;
        try {
            this.server = HttpServer.create(new InetSocketAddress("0.0.0.0", port), 0);
        } catch (BindException be) {
            // Try to get token from existing server
            try {
                URL url = new URL("http://localhost:" + port + "/pair");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setConnectTimeout(2000);
                conn.setReadTimeout(2000);
                String html = new String(conn.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
                String tokenStart = "<p>Token: <b>";
                int idx = html.indexOf(tokenStart);
                if (idx >= 0) {
                    idx += tokenStart.length();
                    int end = html.indexOf("</b>", idx);
                    if (end > idx) {
                        this.pairToken = html.substring(idx, end);
                        this.server = null;
                        instance = this;
                        return;
                    }
                }
            } catch (Exception ignored) {}
            throw be;
        }
        this.server.setExecutor(Executors.newCachedThreadPool());
        this.pairToken = UUID.randomUUID().toString().substring(0, 8);
        server.createContext("/pair", new PairHandler());
        server.createContext("/location", new LocationHandler());
        server.createContext("/stop", new StopHandler());
        server.createContext("/latest", new LatestHandler());
        instance = this;
        janitor.scheduleAtFixedRate(this::cleanupStale, 5, 5, TimeUnit.MINUTES);
    }

    public void start() {
        if (server != null && !isStarted) {
            server.start();
            isStarted = true;
        }
    }

    public void stop() {
        if (server != null && isStarted) {
            try {
                server.stop(0);
                isStarted = false;
            } catch (Exception ignored) {}
        }
        janitor.shutdownNow();
    }

    public String getPairToken() {
        return pairToken;
    }
    
    public boolean isRunning() {
        return isStarted;
    }

    public LastLocation getLatestLocation() {
        if (isStarted) {
            return getLastLocation(pairToken);
        }
        // Try to get from existing server
        try {
            String url = String.format("http://localhost:%d/latest?token=%s", port, pairToken);
            HttpURLConnection conn = (HttpURLConnection) URI.create(url).toURL().openConnection();
            conn.setConnectTimeout(2000);
            conn.setReadTimeout(2000);
            if (conn.getResponseCode() == 200) {
                String json = new String(conn.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
                Map<String,String> map = parseSimpleJson(json);
                if ("ok".equals(map.get("status"))) {
                    double lat = Double.parseDouble(map.get("lat"));
                    double lon = Double.parseDouble(map.get("lon"));
                    long ts = Long.parseLong(map.get("ts"));
                    return new LastLocation(lat, lon, ts);
                }
            }
        } catch (Exception ignored) {}
        return null;
    }

    public String getLocalAddress() {
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface ni = interfaces.nextElement();
                if (!ni.isUp() || ni.isLoopback() || ni.isVirtual()) continue;
                Enumeration<InetAddress> addrs = ni.getInetAddresses();
                while (addrs.hasMoreElements()) {
                    InetAddress addr = addrs.nextElement();
                    if (addr instanceof Inet4Address && !addr.isLoopbackAddress()) {
                        return addr.getHostAddress();
                    }
                }
            }
            return InetAddress.getLocalHost().getHostAddress();
        } catch (Exception e) {
            return "localhost";
        }
    }

    public LastLocation getLastLocation(String token) {
        if (token == null) return null;
        return lastLocations.get(token);
    }

    public void clearLocation(String token) {
        if (token == null) return;
        lastLocations.remove(token);
    }

    public void setOnLocation(BiConsumerOnLocation listener) {
        this.onLocation = listener;
    }

    private void cleanupStale() {
        long cutoff = Instant.now().minusSeconds(60 * 60).toEpochMilli();
        for (Map.Entry<String, LastLocation> e : lastLocations.entrySet()) {
            if (e.getValue().timestamp < cutoff) lastLocations.remove(e.getKey(), e.getValue());
        }
    }

    private void recordLocation(String token, double lat, double lon) {
        LastLocation r = new LastLocation(lat, lon, Instant.now().toEpochMilli());
        lastLocations.put(token, r);
        BiConsumerOnLocation cb = this.onLocation;
        if (cb != null) {
            Platform.runLater(() -> cb.on(lat, lon));
        }
    }

    public interface BiConsumerOnLocation {
        void on(double lat, double lon);
    }

    public static class LastLocation {
        public final double lat;
        public final double lon;
        public final long timestamp;
        public LastLocation(double lat, double lon, long ts) { this.lat = lat; this.lon = lon; this.timestamp = ts; }
    }

    class PairHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(405, -1);
                return;
            }
            String query = exchange.getRequestURI().getQuery();
            String token = "";
            if (query != null) {
                for (String q : query.split("&")) {
                    String[] kv = q.split("=", 2);
                    if (kv.length == 2 && "token".equals(kv[0])) {
                        token = URLDecoder.decode(kv[1], StandardCharsets.UTF_8);
                        break;
                    }
                }
            }
            if (token == null || token.isEmpty()) token = pairToken;
            String html = buildPairHtml(token);
            byte[] b = html.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "text/html; charset=utf-8");
            exchange.sendResponseHeaders(200, b.length);
            try (OutputStream os = exchange.getResponseBody()) { os.write(b); }
        }

        private String buildPairHtml(String token) {
            String esc = escapeHtml(token);
            return "<!doctype html>\n" +
                    "<html>\n" +
                    "<head>\n" +
                    "<meta charset='utf-8'/>\n" +
                    "<meta name='viewport' content='width=device-width,initial-scale=1'/>\n" +
                    "<title>CBS - Pair & Share</title>\n" +
                    "<style>body{font-family:Arial;padding:16px;max-width:560px;margin:auto}button{padding:12px;border-radius:10px;border:none;background:#2563eb;color:#fff} .secondary{background:#fff;color:#111;border:1px solid #ddd} input{width:100%;padding:10px;margin:8px 0;border-radius:8px;border:1px solid #ddd} pre{background:#f6f7fb;padding:12px;border-radius:6px;white-space:pre-wrap}</style>\n" +
                    "</head>\n" +
                    "<body>\n" +
                    "<h2>CBS — Share location</h2>\n" +
                    "<p>Token: <b>" + esc + "</b></p>\n" +
                    "<p id='status'>Tap <b>Start sharing</b> and allow location permission when prompted.</p>\n" +
                    "<div style='margin-top:10px'>\n" +
                    "<button id='start'>Start sharing</button>\n" +
                    "<button id='stop' class='secondary' style='display:none;margin-left:8px'>Stop sharing</button>\n" +
                    "<button id='one' class='secondary' style='margin-left:8px'>Send one location</button>\n" +
                    "</div>\n" +
                    "<div style='margin-top:12px'>\n" +
                    "<label class='hint'>Update interval (sec)</label>\n" +
                    "<input id='interval' value='15' />\n" +
                    "</div>\n" +
                    "<pre id='out'></pre>\n" +
                    "<script>\n" +
                    "const token = '" + esc + "';\n" +
                    "const out = document.getElementById('out');\n" +
                    "const status = document.getElementById('status');\n" +
                    "let watchId = null; let intervalId = null;\n" +
                    "function log(m){ out.innerText = new Date().toLocaleTimeString() + ' — ' + m + '\\n' + out.innerText; }\n" +
                    "async function post(lat, lon){ try{ const resp = await fetch('/location', { method:'POST', headers:{'Content-Type':'application/json'}, body: JSON.stringify({ token, lat, lon }) }); const txt = await resp.text(); log('sent: '+lat+','+lon+' -> '+txt);}catch(e){ log('send failed: '+e); }}\n" +
                    "function startWatch(){ if(!navigator.geolocation){ status.innerText='Geolocation not supported'; return; } status.innerText='Starting...'; watchId = navigator.geolocation.watchPosition(p => { post(p.coords.latitude, p.coords.longitude); }, e => { log('watch error: '+e.message); status.innerText='watch error: '+e.message; }, { enableHighAccuracy:true, maximumAge:1000, timeout:15000 }); }\n" +
                    "function startPolling(sec){ const ms = Math.max(5000, (sec||15)*1000); intervalId = setInterval(()=>{ navigator.geolocation.getCurrentPosition(p => { post(p.coords.latitude,p.coords.longitude); }, e => { log('poll error: '+e.message); }, { enableHighAccuracy:true, timeout:15000 }); }, ms); }\n" +
                    "function stopAll(){ if(watchId!=null){ navigator.geolocation.clearWatch(watchId); watchId=null; } if(intervalId!=null){ clearInterval(intervalId); intervalId=null; } fetch('/stop?token='+encodeURIComponent(token)).then(r=>r.text()).then(t=>log('stop: '+t)).catch(e=>log('stop failed: '+e)); status.innerText='Stopped'; }\n" +
                    "document.getElementById('start').addEventListener('click', async ()=>{ const intervalSec = parseInt(document.getElementById('interval').value||'15',10); try{ const p = await navigator.permissions.query({name:'geolocation'}); if(p.state==='denied'){ status.innerText='Permission was denied. Open site settings to allow location.'; return; } }catch(e){} startWatch(); startPolling(intervalSec); document.getElementById('start').style.display='none'; document.getElementById('stop').style.display='inline-block'; status.innerText='Sharing started'; });\n" +
                    "document.getElementById('stop').addEventListener('click', ()=>{ stopAll(); document.getElementById('start').style.display='inline-block'; document.getElementById('stop').style.display='none'; });\n" +
                    "document.getElementById('one').addEventListener('click', ()=>{ if(!navigator.geolocation){ log('no geo'); return; } navigator.geolocation.getCurrentPosition(p=>post(p.coords.latitude,p.coords.longitude), e=>log('one error:'+e.message), {timeout:15000}); });\n" +
                    "</script>\n" +
                    "</body>\n" +
                    "</html>";
        }
    }

    class LocationHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(405, -1);
                return;
            }
            InputStream is = exchange.getRequestBody();
            String body = new String(is.readAllBytes(), StandardCharsets.UTF_8);
            Map<String, String> map = parseSimpleJson(body);
            String token = map.get("token");
            String latS = map.get("lat");
            String lonS = map.get("lon");
            if (token == null || latS == null || lonS == null) {
                sendJson(exchange, 400, "{\"status\":\"error\",\"message\":\"invalid payload\"}");
                return;
            }
            try {
                double lat = Double.parseDouble(latS);
                double lon = Double.parseDouble(lonS);
                recordLocation(token, lat, lon);
                sendJson(exchange, 200, "{\"status\":\"ok\"}");
            } catch (NumberFormatException ex) {
                sendJson(exchange, 400, "{\"status\":\"error\",\"message\":\"invalid coordinates\"}");
            }
        }
    }

    class StopHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String query = exchange.getRequestURI().getQuery();
            String token = "";
            if (query != null) {
                for (String q : query.split("&")) {
                    String[] kv = q.split("=", 2);
                    if (kv.length == 2 && "token".equals(kv[0])) {
                        token = URLDecoder.decode(kv[1], StandardCharsets.UTF_8);
                        break;
                    }
                }
            }
            clearLocation(token);
            sendJson(exchange, 200, "{\"status\":\"stopped\"}");
        }
    }

    class LatestHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(405, -1);
                return;
            }
            String query = exchange.getRequestURI().getQuery();
            String token = "";
            if (query != null) {
                for (String q : query.split("&")) {
                    String[] kv = q.split("=", 2);
                    if (kv.length == 2 && "token".equals(kv[0])) {
                        token = URLDecoder.decode(kv[1], StandardCharsets.UTF_8);
                        break;
                    }
                }
            }
            LastLocation r = (token == null || token.isEmpty()) ? null : lastLocations.get(token);
            if (r == null) {
                sendJson(exchange, 404, "{\"status\":\"missing\"}");
            } else {
                String json = String.format(Locale.US, "{\"status\":\"ok\",\"lat\":%.6f,\"lon\":%.6f,\"ts\":%d}", r.lat, r.lon, r.timestamp);
                sendJson(exchange, 200, json);
            }
        }
    }

    private void sendJson(HttpExchange exchange, int code, String body) throws IOException {
        byte[] b = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "application/json; charset=utf-8");
        exchange.sendResponseHeaders(code, b.length);
        try (OutputStream os = exchange.getResponseBody()) { os.write(b); }
    }

    private Map<String, String> parseSimpleJson(String json) {
        Map<String, String> map = new HashMap<>();
        if (json == null) return map;
        json = json.trim();
        if (json.startsWith("{") && json.endsWith("}")) {
            json = json.substring(1, json.length() - 1);
            String[] parts = json.split(",");
            for (String p : parts) {
                String[] kv = p.split(":", 2);
                if (kv.length != 2) continue;
                String k = kv[0].trim().replaceAll("^\"|\"$", "");
                String v = kv[1].trim().replaceAll("^\"|\"$", "");
                map.put(k, v);
            }
        }
        return map;
    }

    private static String escapeHtml(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }
}
