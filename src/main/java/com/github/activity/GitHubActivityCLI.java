package com.github.activity;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(name = "PrintUsername", description = "Prints the provided username to the output.")
public class GitHubActivityCLI implements Runnable {

  @Option(names = {"-u", "--username"}, description = "The username to be printed", required = true)
  private String username;

  private static final String GITHUB_API_URL = "https://api.github.com/users/%s/events";
  private static final ObjectMapper mapper = new ObjectMapper();

  public static void main(String[] args) {
    int exitCode = new CommandLine(new GitHubActivityCLI()).execute(args);
    System.exit(exitCode);
  }

  @Override
  public void run() {
    HttpClient client = HttpClient.newHttpClient();
    String url = String.format(GITHUB_API_URL, username);

    try {
      HttpRequest request = HttpRequest.newBuilder()
          .uri(new URI(url))
          .header("Accept", "application/vnd.github+json")
          .GET()
          .build();

      HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

      if (response.statusCode() == 404) {
        System.out.println("User not found. Please check the username.");
        return;
      }

      if (response.statusCode() == 200) {
        JsonNode events = mapper.readTree(response.body());
        displayActivity(events);
      } else {
        System.out.println("Error: " + response.statusCode());
      }
    } catch (Exception e) {
      System.err.println("Error fetching GitHub activity: " + e.getMessage());
    }
  }

  private void displayActivity(JsonNode events) {
    Map<String, Function<JsonNode, String>> eventHandlers = new HashMap<>();
    eventHandlers.put("PushEvent", this::handlePushEvent);
    eventHandlers.put("IssuesEvent", this::handleIssuesEvent);
    eventHandlers.put("WatchEvent", event -> "Starred " + event.get("repo").get("name").asText());
    eventHandlers.put("ForkEvent", event -> "Forked " + event.get("repo").get("name").asText());
    eventHandlers.put("CreateEvent", this::handleCreateEvent);

    for (JsonNode event : events) {
      String type = event.get("type").asText();
      String action = eventHandlers.getOrDefault(type, e -> defaultHandler(e, type)).apply(event);
      System.out.println(action);
    }
  }

  private String handlePushEvent(JsonNode event) {
    int commitCount = event.get("payload").get("commits").size();
    return "Pushed " + commitCount + " commit(s) to " + event.get("repo").get("name").asText();
  }

  private String handleIssuesEvent(JsonNode event) {
    String action = event.get("payload").get("action").asText();
    return action.substring(0, 1).toUpperCase() + action.substring(1)
        + " an issue in " + event.get("repo").get("name").asText();
  }

  private String handleCreateEvent(JsonNode event) {
    String refType = event.get("payload").get("ref_type").asText();
    return "Created " + refType + " in " + event.get("repo").get("name").asText();
  }

  private String defaultHandler(JsonNode event, String type) {
    return type.replace("Event", "") + " in " + event.get("repo").get("name").asText();
  }
}
