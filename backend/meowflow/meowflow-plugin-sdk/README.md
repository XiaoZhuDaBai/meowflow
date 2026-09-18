# meowflow-plugin-sdk

Third-party Java client for MeowFlow plugin triggers and lifecycle APIs.

## Usage

```java
PluginClient client = new PluginClient(
        "http://localhost:8080/workflow/api/plugin",
        "my-plugin",
        "my-secret");

client.register(Map.of(
        "workflowId", 123L,
        "secret", "my-secret",
        "status", "active"));

Map<String, Object> result = client.trigger(Map.of(
        "event", "user.created",
        "userId", 42));

client.updateStatus("disabled");
client.delete();
```

`baseUrl` is the gateway route ending at `/api/plugin`. If you call the workflow
service directly, use `http://localhost:8081/api/plugin`.

## Client Methods

- `register(Map<String, Object> manifest)`
- `list()`
- `get()`
- `updateStatus(String status)`
- `delete()`
- `trigger(Map<String, Object> event)`
- `trigger(Map<String, Object> event, String eventId)`

The trigger method creates a timestamp and HMAC-SHA256 signature, and sends a
unique event ID to support server-side deduplication.
