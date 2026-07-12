# Subprocess JSON Communication Contract

This document defines the JSON schema for standard input/output (stdin/stdout) 
communication between the Python `api-core` and the Java `event-service`.

## 1. Envelope Shapes

To ensure consistent parsing across both languages, every message must adhere 
to strict envelope structures.

### Request Envelope
Every request sent to a subprocess must contain a `requestType` string to route
the message, and a `payload` object containing the specific arguments.

The `requestType` string must be one of the following:
- `GET_USER_BADGES`
- `GET_USER_FRIENDS`
- `AWARD_BADGE` 
- `GET_USER_EVENTS`
- `GET_RECOMMENDED_EVENTS`
- `RECORD_ATTENDANCE`

For example:
```json
{ 
  "requestType": "GET_USER_BADGES", 
  "payload": { 
    "userId": "86aa54b8-2d08-498b-aee9-b2c26a97717e"
  }
}
```

### Response Envelope
Every response from a subprocess must contain a `"ok"` in the `status` 
field, and a `payload` object containing the specific response.

For example:
```json
{ 
  "status": "ok",
  "payload": { 
    "badges": ["First Event", "Social5"]
  }
}
```

### Error Envelope
If an error occurs during processing, the response will have a status of 
`error`, and a `error` field containing the error message.

For example:
```json
{ 
  "status": "error", 
  "error": "User 86aa54b8-2d08-498b-aee9-b2c26a97717e not found"
}
```

