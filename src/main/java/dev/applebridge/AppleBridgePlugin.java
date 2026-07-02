package dev.applebridge;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.SecureRandom;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class AppleBridgePlugin extends JavaPlugin {

    private static final Pattern COMMAND_PATTERN = Pattern.compile("\"command\"\\s*:\\s*\"((?:\\\\.|[^\\\\\"])*)\"");
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss");

    private final SecureRandom secureRandom = new SecureRandom();
    private final Deque<String> recentLogs = new ArrayDeque<>();
    private HttpServer httpServer;
    private ExecutorService httpExecutor;
    private Handler logHandler;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        ensureSecret();
        startLogCapture();
        startApiServer();
    }

    @Override
    public void onDisable() {
        stopApiServer();
        stopLogCapture();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!command.getName().equalsIgnoreCase("applebridge")) {
            return false;
        }

        if (args.length == 0) {
            sender.sendMessage("§cUsage: /applebridge <reload|status>");
            return true;
        }

        String subCommand = args[0].toLowerCase(Locale.ROOT);
        if (subCommand.equals("reload")) {
            reloadConfig();
            ensureSecret();
            restartApiServer();
            sender.sendMessage("§aAppleBridge config reloaded.");
            return true;
        }

        if (subCommand.equals("status")) {
            boolean enabled = getConfig().getBoolean("enabled", true);
            int port = getConfig().getInt("port", 8080);
            boolean running = httpServer != null;
            int logBufferSize = getConfig().getInt("log-buffer-size", 200);
            sender.sendMessage("§eAppleBridge status:");
            sender.sendMessage("§7enabled: §f" + enabled);
            sender.sendMessage("§7running: §f" + running);
            sender.sendMessage("§7port: §f" + port);
            sender.sendMessage("§7log-buffer-size: §f" + logBufferSize);
            return true;
        }

        sender.sendMessage("§cUnknown subcommand. Usage: /applebridge <reload|status>");
        return true;
    }

    private void restartApiServer() {
        stopApiServer();
        startApiServer();
    }

    private void startApiServer() {
        if (!getConfig().getBoolean("enabled", true)) {
            getLogger().info("AppleBridge API disabled in config.");
            return;
        }

        int port = getConfig().getInt("port", 8080);

        try {
            httpServer = HttpServer.create(new InetSocketAddress(port), 0);
            httpExecutor = Executors.newCachedThreadPool();
            httpServer.createContext("/execute", new ExecuteHandler());
            httpServer.createContext("/logs", new LogsHandler());
            httpServer.setExecutor(httpExecutor);
            httpServer.start();
            getLogger().info("AppleBridge API started on port " + port);
        } catch (IOException exception) {
            httpServer = null;
            if (httpExecutor != null) {
                httpExecutor.shutdownNow();
                httpExecutor = null;
            }
            getLogger().severe("Failed to start AppleBridge API: " + exception.getMessage());
        }
    }

    private void stopApiServer() {
        if (httpServer != null) {
            httpServer.stop(0);
            httpServer = null;
        }

        if (httpExecutor != null) {
            httpExecutor.shutdownNow();
            httpExecutor = null;
        }
    }

    private void ensureSecret() {
        String secret = getConfig().getString("secret", "CHANGE_ME");
        if (secret == null || secret.isBlank() || Objects.equals(secret, "CHANGE_ME")) {
            String generatedSecret = generateSecret();
            getConfig().set("secret", generatedSecret);
            saveConfig();
            getLogger().info("Generated a new secret for AppleBridge API.");
        }
    }

    private String generateSecret() {
        String alphabet = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        StringBuilder builder = new StringBuilder();

        for (int index = 0; index < 32; index++) {
            builder.append(alphabet.charAt(secureRandom.nextInt(alphabet.length())));
        }

        return builder.toString();
    }

    private void startLogCapture() {
        if (logHandler != null) {
            return;
        }

        int bufferSize = Math.max(10, getConfig().getInt("log-buffer-size", 200));
        Logger serverLogger = Bukkit.getLogger();

        logHandler = new Handler() {
            @Override
            public void publish(LogRecord record) {
                if (record == null || !isLoggable(record)) {
                    return;
                }

                appendLogLine(formatLogRecord(record), bufferSize);
            }

            @Override
            public void flush() {
            }

            @Override
            public void close() {
            }
        };

        logHandler.setLevel(Level.INFO);
        serverLogger.addHandler(logHandler);
    }

    private void stopLogCapture() {
        if (logHandler == null) {
            return;
        }

        Bukkit.getLogger().removeHandler(logHandler);
        logHandler = null;
    }

    private void appendLogLine(String line, int maxEntries) {
        synchronized (recentLogs) {
            recentLogs.addLast(line);

            while (recentLogs.size() > maxEntries) {
                recentLogs.removeFirst();
            }
        }
    }

    private List<String> getRecentLogs(int limit) {
        List<String> snapshot;

        synchronized (recentLogs) {
            snapshot = new ArrayList<>(recentLogs);
        }

        int fromIndex = Math.max(0, snapshot.size() - limit);
        return snapshot.subList(fromIndex, snapshot.size());
    }

    private List<String> readRecentLogs(int limit) {
        Path logFile = Path.of("logs", "latest.log");

        if (Files.exists(logFile)) {
            try {
                List<String> fileLines = Files.readAllLines(logFile, StandardCharsets.UTF_8);
                int fromIndex = Math.max(0, fileLines.size() - limit);
                return fileLines.subList(fromIndex, fileLines.size());
            } catch (IOException exception) {
                getLogger().warning("Failed to read logs/latest.log: " + exception.getMessage());
            }
        }

        return getRecentLogs(limit);
    }

    private String formatLogRecord(LogRecord record) {
        String time = LocalTime.now().format(TIME_FORMATTER);
        String level = record.getLevel().getName();
        String loggerName = record.getLoggerName() == null ? "Server" : record.getLoggerName();
        String message = record.getMessage() == null ? "" : record.getMessage();
        return "[" + time + "] [" + level + "] [" + loggerName + "] " + message;
    }

    private boolean isAuthorized(Headers headers) {
        String authorization = headers.getFirst("Authorization");
        String secret = getConfig().getString("secret", "");
        return authorization != null && !secret.isBlank() && authorization.equals(secret);
    }

    private String readBody(InputStream inputStream) throws IOException {
        return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
    }

    private String extractCommand(String json) {
        Matcher matcher = COMMAND_PATTERN.matcher(json);
        if (!matcher.find()) {
            return null;
        }

        return unescapeJsonString(matcher.group(1)).trim();
    }

    private String unescapeJsonString(String value) {
        return value
                .replace("\\\"", "\"")
                .replace("\\\\", "\\")
                .replace("\\n", "\n")
                .replace("\\r", "\r")
                .replace("\\t", "\t");
    }

    private boolean dispatchServerCommand(String command) throws ExecutionException, InterruptedException {
        Future<Boolean> future = Bukkit.getScheduler().callSyncMethod(
                AppleBridgePlugin.this,
                () -> Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command)
        );
        return future.get();
    }

    private void writeResponse(HttpExchange exchange, int statusCode, String response) throws IOException {
        byte[] bytes = response.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "text/plain; charset=utf-8");
        exchange.sendResponseHeaders(statusCode, bytes.length);

        try (OutputStream outputStream = exchange.getResponseBody()) {
            outputStream.write(bytes);
        }
    }

    private int resolveLogLimit(URI uri) {
        String query = uri.getQuery();
        if (query == null || query.isBlank()) {
            return 50;
        }

        for (String part : query.split("&")) {
            String[] keyValue = part.split("=", 2);
            if (keyValue.length == 2 && keyValue[0].equalsIgnoreCase("limit")) {
                try {
                    int requested = Integer.parseInt(keyValue[1]);
                    return Math.max(1, Math.min(200, requested));
                } catch (NumberFormatException ignored) {
                    return 50;
                }
            }
        }

        return 50;
    }

    private final class ExecuteHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            try {
                if (!exchange.getRequestMethod().equalsIgnoreCase("POST")) {
                    writeResponse(exchange, 405, "Method Not Allowed");
                    return;
                }

                if (!isAuthorized(exchange.getRequestHeaders())) {
                    writeResponse(exchange, 403, "Forbidden");
                    return;
                }

                String body = readBody(exchange.getRequestBody());
                String command = extractCommand(body);
                if (command == null || command.isBlank()) {
                    writeResponse(exchange, 400, "Invalid JSON");
                    return;
                }

                boolean executed = dispatchServerCommand(command);
                if (!executed) {
                    writeResponse(exchange, 500, "Command Failed");
                    return;
                }

                writeResponse(exchange, 200, "OK");
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                getLogger().warning("HTTP request was interrupted: " + exception.getMessage());
                writeResponse(exchange, 500, "Interrupted");
            } catch (ExecutionException exception) {
                getLogger().warning("Command execution failed: " + exception.getMessage());
                writeResponse(exchange, 500, "Execution Error");
            } catch (Exception exception) {
                getLogger().warning("Failed to handle HTTP request: " + exception.getMessage());
                writeResponse(exchange, 500, "Internal Server Error");
            } finally {
                exchange.close();
            }
        }
    }

    private final class LogsHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            try {
                if (!exchange.getRequestMethod().equalsIgnoreCase("GET")) {
                    writeResponse(exchange, 405, "Method Not Allowed");
                    return;
                }

                if (!isAuthorized(exchange.getRequestHeaders())) {
                    writeResponse(exchange, 403, "Forbidden");
                    return;
                }

                int limit = resolveLogLimit(exchange.getRequestURI());
                List<String> logs = readRecentLogs(limit);
                String response = String.join("\n", logs);
                writeResponse(exchange, 200, response.isBlank() ? "No logs yet" : response);
            } catch (Exception exception) {
                getLogger().warning("Failed to return logs through HTTP API: " + exception.getMessage());
                writeResponse(exchange, 500, "Internal Server Error");
            } finally {
                exchange.close();
            }
        }
    }
}
