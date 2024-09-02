package com.github.activity;

public enum EventType {
  PUSH_EVENT("PushEvent"),
  ISSUES_EVENT("IssuesEvent"),
  WATCH_EVENT("WatchEvent"),
  FORK_EVENT("ForkEvent"),
  CREATE_EVENT("CreateEvent"),
  UNKNOWN("Unknown");

  private final String typeName;

  EventType(String typeName) {
    this.typeName = typeName;
  }

  public String getDisplayName() {
    return typeName.replace("Event", "");
  }

  public static EventType fromString(String type) {
    for (EventType eventType : EventType.values()) {
      if (eventType.typeName.equalsIgnoreCase(type)) {
        return eventType;
      }
    }
    return UNKNOWN;
  }
}
