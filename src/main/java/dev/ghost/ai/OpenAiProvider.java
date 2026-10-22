package dev.ghost.ai;

import com.google.gson.*;
import dev.ghost.config.GhostConfig;

import java.io.IOException;
import java.io.InputStream;
import java.net.ConnectException;
import java.net.URI;
import java.net.UnknownHostException;
import java.net.http.*;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

public final class OpenAiProvider implements AiProvider {
    private static final URI ENDPOINT = URI.create("https://api.openai.com/v1/responses");
    private static final int MAX_RESPONSE_CHARS = 256_000;
    private static final String INSTRUCTIONS = "Você é Ghost, uma presença útil, levemente misteriosa, educada e objetiva dentro de Minecraft. Responda em português, salvo se o jogador usar outro idioma. Dê respostas curtas e claras, adequadas ao chat do jogo. Ajude com Minecraft Java 26.3 e assuntos gerais. Não invente fatos, ações realizadas, acesso a informações não fornecidas ou conhecimento em tempo real. Quando a informação puder variar por versão, modpack, servidor ou configuração, deixe isso claro. Você não controla o jogo e não deve alegar que executou ações.";
    private static final HttpClient CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(12)).followRedirects(HttpClient.Redirect.NEVER).build();
    private final Gson gson = new Gson();

    @Override public AiReply ask(String apiKey, GhostConfig config, String question, String context, String sessionHistory) throws AiException {
        JsonObject body = baseBody(config.model, question, context, sessionHistory, 320);
        if (config.webSearch && seemsTimeSensitive(question)) {
            JsonArray tools = new JsonArray();
            JsonObject tool = new JsonObject(); tool.addProperty("type", "web_search"); tools.add(tool);
            body.add("tools", tools);
        }
        JsonObject response = post(apiKey, body);
        return parseReply(response);
    }

    @Override public void testConnection(String apiKey, String model) throws AiException {
        JsonObject body = new JsonObject();
        body.addProperty("model", model);
        body.addProperty("instructions", "Responda somente: OK");
        body.addProperty("input", "OK?");
        body.addProperty("max_output_tokens", 24);
        body.addProperty("store", false);
        parseReply(post(apiKey, body));
    }

    private JsonObject baseBody(String model, String question, String context, String history, int maxOutput) {
        JsonObject body = new JsonObject();
        body.addProperty("model", model);
        body.addProperty("instructions", INSTRUCTIONS);
        body.addProperty("max_output_tokens", maxOutput);
        body.addProperty("store", false);
        JsonArray input = new JsonArray();
        if (history != null && !history.isBlank()) {
            JsonObject prior = new JsonObject(); prior.addProperty("role", "user");
            prior.addProperty("content", "Resumo das últimas conversas somente desta sessão:\n" + history);
            input.add(prior);
        }
        JsonObject current = new JsonObject(); current.addProperty("role", "user");
        current.addProperty("content", (context == null || context.isBlank() ? "" : "Contexto mínimo do jogo (fornecido pelo cliente):\n" + context + "\n\n") + question);
        input.add(current);
        body.add("input", input);
        return body;
    }

    private JsonObject post(String apiKey, JsonObject body) throws AiException {
        HttpRequest request = HttpRequest.newBuilder(ENDPOINT).timeout(Duration.ofSeconds(40))
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(gson.toJson(body))).build();
        try {
            HttpResponse<InputStream> response = CLIENT.send(request, HttpResponse.BodyHandlers.ofInputStream());
            String raw;
            try (InputStream bodyStream = response.body()) {
                byte[] bytes = bodyStream.readNBytes(MAX_RESPONSE_CHARS + 1);
                if (bytes.length > MAX_RESPONSE_CHARS) throw new AiException(AiException.Kind.FORMAT, "response_too_large");
                raw = new String(bytes, java.nio.charset.StandardCharsets.UTF_8);
            }
            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                try { return JsonParser.parseString(raw).getAsJsonObject(); }
                catch (RuntimeException ex) { throw new AiException(AiException.Kind.FORMAT, "A API retornou uma resposta em formato inválido."); }
            }
            String code = "";
            try { code = JsonParser.parseString(raw).getAsJsonObject().getAsJsonObject("error").get("code").getAsString(); }
            catch (RuntimeException ignored) {}
            if (response.statusCode() == 401) throw new AiException(AiException.Kind.INVALID_KEY, "Chave inválida ou sem autorização.");
            if (response.statusCode() == 404) throw new AiException(AiException.Kind.MODEL, "Modelo inválido ou indisponível para esta conta.");
            if (response.statusCode() == 400 && code.contains("model")) throw new AiException(AiException.Kind.MODEL, "Modelo inválido ou indisponível para esta conta.");
            if (response.statusCode() == 429 && (code.contains("quota") || code.contains("billing") || code.contains("insufficient")))
                throw new AiException(AiException.Kind.QUOTA, "Limite, saldo ou faturamento da API insuficiente.");
            if (response.statusCode() == 429) throw new AiException(AiException.Kind.RATE_LIMIT, "Limite de uso da API atingido. Aguarde e tente novamente.");
            throw new AiException(AiException.Kind.API, "A API retornou erro HTTP " + response.statusCode() + ".");
        } catch (AiException ex) { throw ex; }
        catch (HttpTimeoutException ex) { throw new AiException(AiException.Kind.TIMEOUT, "A conexão com a API expirou."); }
        catch (UnknownHostException | ConnectException ex) { throw new AiException(AiException.Kind.OFFLINE, "Sem conexão com a internet ou com a API."); }
        catch (InterruptedException ex) { Thread.currentThread().interrupt(); throw new AiException(AiException.Kind.CANCELLED, "Requisição cancelada."); }
        catch (IOException ex) { throw new AiException(AiException.Kind.OFFLINE, "Falha de rede ao conectar à API."); }
    }

    static AiReply parseReply(JsonObject response) throws AiException {
        try {
            JsonArray output = response.getAsJsonArray("output");
            if (output == null) throw new IllegalStateException();
            StringBuilder text = new StringBuilder();
            List<String> sources = new ArrayList<>();
            for (JsonElement itemElement : output) {
                JsonObject item = itemElement.getAsJsonObject();
                if (!"message".equals(item.has("type") ? item.get("type").getAsString() : "")) continue;
                JsonArray content = item.getAsJsonArray("content");
                if (content == null) continue;
                for (JsonElement contentElement : content) {
                    JsonObject part = contentElement.getAsJsonObject();
                    if ("output_text".equals(part.has("type") ? part.get("type").getAsString() : ""))
                        text.append(part.get("text").getAsString());
                    JsonArray annotations = part.getAsJsonArray("annotations");
                    if (annotations != null) for (JsonElement ann : annotations) {
                        JsonObject a = ann.getAsJsonObject();
                        if (a.has("url") && sources.size() < 3) sources.add(a.get("url").getAsString());
                    }
                }
            }
            String result = text.toString().trim();
            if (result.isBlank()) throw new IllegalStateException();
            if (result.length() > 12_000) result = result.substring(0, 12_000);
            return new AiReply(result, List.copyOf(sources));
        } catch (RuntimeException ex) {
            throw new AiException(AiException.Kind.FORMAT, "A API não retornou texto em um formato reconhecido.");
        }
    }

    private boolean seemsTimeSensitive(String question) {
        String q = question.toLowerCase();
        return q.matches(".*\\b(hoje|agora|atual|atualmente|not[ií]cia|pre[cç]o|vers[aã]o|lan[cç]amento|clima|tempo|quem [eé] o|resultado|202[5-9]|20[3-9][0-9])\\b.*");
    }
}
