# Smart Campus Sensor & Room Management API

This coursework submission implements a JAX-RS RESTful service for the `5COSC022W Client-Server Architectures` module. The API is designed to run as a Maven `war` project in NetBeans and deploy to Apache Tomcat.

The system models three core resources:

- `Room`
- `Sensor`
- `SensorReading`

All data is stored in memory using Java collections only, as required by the coursework brief.

## Technology Stack

- Java 17
- JAX-RS using Jersey 3.1.1
- Apache Tomcat 10.1+ (`jakarta.*` compatible)
- Maven WAR project
- JSON payloads via Jackson

## API Overview

Base URL after local deployment:

```text
http://localhost:8080/smart-campus-api/api/v1
```

Main endpoints:

- `GET /api/v1` - discovery endpoint
- `GET /api/v1/rooms` - list rooms
- `POST /api/v1/rooms` - create room
- `GET /api/v1/rooms/{roomId}` - get one room
- `DELETE /api/v1/rooms/{roomId}` - delete room if it has no sensors
- `GET /api/v1/sensors` - list sensors
- `GET /api/v1/sensors?type=CO2` - filter sensors by type
- `POST /api/v1/sensors` - create sensor linked to a room
- `GET /api/v1/sensors/{sensorId}` - get one sensor
- `DELETE /api/v1/sensors/{sensorId}` - remove a sensor and its readings
- `GET /api/v1/sensors/{sensorId}/readings` - get reading history
- `POST /api/v1/sensors/{sensorId}/readings` - append a reading and update `currentValue`

## NetBeans + Tomcat Setup

1. Install JDK 17 and make sure NetBeans is using it.
2. Install Apache Tomcat `10.1+`.
3. Open NetBeans and choose `File -> Open Project`, then open this folder.
4. NetBeans will detect it as a Maven web project because `pom.xml` uses `war` packaging.
5. In NetBeans, add your Tomcat server if it is not already configured:
   `Services -> Servers -> Add Server -> Apache Tomcat`.
6. Right-click the project and choose `Properties -> Run`.
7. Set the server to your Tomcat instance and keep the context path as `smart-campus-api` if you want the sample URLs below to work exactly as written.
8. Right-click the project and choose `Clean and Build`.
9. Right-click the project and choose `Run`.

If Tomcat starts successfully, open:

```text
http://localhost:8080/smart-campus-api/
```

The landing page links to the discovery endpoint and main collections.

## Build From Terminal

If Maven is installed on your machine:

```bash
mvn clean package
```

This produces a WAR file in `target/` that can be deployed to Tomcat.

## Sample curl Commands

Create a shell variable for convenience:

```bash
BASE=http://localhost:8080/smart-campus-api/api/v1
```

1. Check the discovery endpoint

```bash
curl "$BASE"
```

2. Create a room

```bash
curl -X POST "$BASE/rooms" \
  -H "Content-Type: application/json" \
  -d "{\"id\":\"LIB-301\",\"name\":\"Library Quiet Study\",\"capacity\":120}"
```

3. Create another room

```bash
curl -X POST "$BASE/rooms" \
  -H "Content-Type: application/json" \
  -d "{\"id\":\"ENG-101\",\"name\":\"Engineering Lab\",\"capacity\":40}"
```

4. Register a sensor in a valid room

```bash
curl -X POST "$BASE/sensors" \
  -H "Content-Type: application/json" \
  -d "{\"id\":\"CO2-001\",\"type\":\"CO2\",\"status\":\"ACTIVE\",\"currentValue\":415.8,\"roomId\":\"LIB-301\"}"
```

5. Filter sensors by type

```bash
curl "$BASE/sensors?type=CO2"
```

6. Add a reading to a sensor

```bash
curl -X POST "$BASE/sensors/CO2-001/readings" \
  -H "Content-Type: application/json" \
  -d "{\"value\":430.2}"
```

7. Get sensor reading history

```bash
curl "$BASE/sensors/CO2-001/readings"
```

8. Attempt to delete a room that still contains sensors

```bash
curl -X DELETE "$BASE/rooms/LIB-301"
```

9. Delete an empty room successfully

```bash
curl -X DELETE "$BASE/rooms/ENG-101"
```

## Example Error Scenarios

### 409 Conflict

Trying to delete a room that still has linked sensors returns `409 Conflict`.

### 422 Unprocessable Entity

Trying to create a sensor with a `roomId` that does not exist returns `422`.

### 403 Forbidden

Trying to add a reading to a sensor whose status is `MAINTENANCE` returns `403`.

### 500 Internal Server Error

Unexpected runtime failures are intercepted and converted into a safe JSON response instead of a stack trace or HTML server page.

## Project Structure

```text
src/main/java/com/smartcampus/config
src/main/java/com/smartcampus/resource
src/main/java/com/smartcampus/store
src/main/java/com/smartcampus/model
src/main/java/com/smartcampus/exception
src/main/java/com/smartcampus/filter
src/main/webapp/WEB-INF
```

## Coursework Report Answers

### Part 1: Service Architecture & Setup

#### 1. Project and Application Configuration

The default lifecycle for a JAX-RS resource class is request-scoped. In other words, the runtime typically creates a new resource instance for each incoming HTTP request unless the class is explicitly managed as a singleton. This design reduces accidental shared mutable state inside resource classes, which makes the resources themselves safer and easier to reason about.

Because resource instances are normally short-lived, my in-memory data structures cannot live inside the resource objects themselves. If they did, data would be lost between requests. For that reason, I placed the state in a separate singleton-style `CampusStore` component. Since multiple requests may arrive concurrently, write operations in that store are synchronized and the underlying collections use `ConcurrentHashMap` where appropriate. This prevents race conditions during operations that must update several linked structures together, such as creating a sensor and also attaching its ID to a room.

#### 2. Discovery Endpoint

Hypermedia is considered a hallmark of advanced RESTful design because it allows responses to describe what the client can do next, instead of forcing the client to rely entirely on hardcoded URL knowledge or external documentation. In practical terms, this means a client can discover collections, related resources, and transitions dynamically from server responses.

This benefits client developers because it reduces coupling between client code and URI structure. If the API evolves, clients can follow the links returned by the server rather than break when a route changes. Static documentation is still useful, but hypermedia makes the API more self-descriptive and easier to navigate programmatically.

### Part 2: Room Management

#### 1. Room Resource Implementation

Returning only room IDs reduces payload size and conserves bandwidth, which can matter when the collection is large or when clients only need identifiers for follow-up requests. However, it also forces clients to make more requests if they need room names, capacities, or sensor membership details.

Returning full room objects is more convenient for clients because it reduces round trips and offloads less assembly work onto the consumer. The trade-off is a larger response body and potentially more data transfer than necessary. For this coursework I return full room objects because the resource is small and it provides a better developer experience during testing and demonstration.

#### 2. Room Deletion and Safety Logic

The `DELETE` operation remains idempotent in this implementation. If a room has no sensors, the first successful `DELETE` removes it from the server state. If the same exact `DELETE` request is sent again, the room is already absent, so the second response becomes `404 Not Found`. Even though the status code is different, the server state after the first request and after any repeated identical request is the same: the room does not exist. That is why the operation is still considered idempotent.

### Part 3: Sensor Operations & Linking

#### 1. Sensor Resource and Integrity

The `@Consumes(MediaType.APPLICATION_JSON)` annotation tells JAX-RS that the method accepts JSON request bodies. If a client sends the payload using a different media type such as `text/plain` or `application/xml`, Jersey will not find a suitable method-body reader for that combination and the request is typically rejected with `415 Unsupported Media Type`.

This is useful because it protects the endpoint from silently accepting data in a format it was never written to parse. It also gives clients a clear protocol-level signal that the body format is wrong.

#### 2. Filtered Retrieval and Search

Using `@QueryParam` for filtering is generally superior because the client is still requesting the same collection resource, just with additional criteria applied. `/sensors` and `/sensors?type=CO2` both represent the sensors collection; the second request simply narrows the result set. This matches the semantics of filtering and searching.

By contrast, encoding the filter in the path, such as `/sensors/type/CO2`, makes the URL look like a different hierarchical resource rather than a modified view of the same collection. Query parameters are also easier to combine, for example `?type=CO2&status=ACTIVE`, which scales much better for future API evolution.

### Part 4: Deep Nesting with Sub-Resources

#### 1. The Sub-Resource Locator Pattern

The Sub-Resource Locator pattern improves architecture by delegating nested concerns to smaller classes that focus on a specific context. In this project, `SensorResource` is responsible for sensors, while `SensorReadingResource` handles the reading history beneath a specific sensor. That separation keeps responsibilities clear and avoids an oversized resource class full of unrelated path methods.

This pattern becomes more valuable as the API grows. Instead of putting every nested endpoint into one large controller, each sub-resource can own its validation, business rules, and response handling. That improves maintainability, readability, and testability.

#### 2. Historical Data Management

When a new reading is posted successfully, the reading is appended to the sensor's history and the parent sensor's `currentValue` is updated immediately. This keeps the collection of historical readings consistent with the top-level sensor representation. A client can therefore inspect the sensor directly for the latest value or fetch the reading history for deeper detail.

### Part 5: Advanced Error Handling, Exception Mapping and Logging

#### 1. Resource Conflict (409)

The API throws a custom `RoomNotEmptyException` when a client tries to delete a room that still has sensors assigned to it. The exception mapper converts that condition into `409 Conflict` with a structured JSON body. This is appropriate because the request is syntactically valid, but it conflicts with the current state of the resource graph.

#### 2. Dependency Validation (422)

HTTP `422 Unprocessable Entity` is often more semantically accurate than `404 Not Found` in this case because the target endpoint itself exists and the overall request syntax is valid. The problem is inside the submitted representation: the payload references another resource that is missing. The server understood the body, but it cannot process it successfully because the room dependency is invalid.

Using `404` would usually imply that the requested endpoint URL itself does not exist. Here, the client reached the correct endpoint but provided an invalid linked identifier inside the JSON body, so `422` communicates the problem more precisely.

#### 3. State Constraint (403)

If a sensor is in `MAINTENANCE`, the API rejects posted readings with `403 Forbidden` through the `SensorUnavailableException` mapper. This models a business rule in which the client is recognized, the endpoint exists, and the payload is valid, but the current state of the sensor does not permit the action.

#### 4. Global Safety Net (500)

Exposing Java stack traces to external users is dangerous because they can reveal implementation details that should remain private. An attacker could learn package names, class names, library versions, internal file paths, server structure, framework choices, and the exact area of the code that failed. That information can be used to plan more targeted attacks, fingerprint vulnerable dependencies, or discover weak points in the application.

For that reason, the application uses a catch-all `ExceptionMapper<Throwable>` to replace unexpected failures with a generic `500 Internal Server Error` JSON message.

#### 5. API Request and Response Logging Filters

JAX-RS filters are a better place for cross-cutting concerns like logging because they centralize behavior that applies to every endpoint. If logging code were manually inserted into each resource method, it would create repetition, increase maintenance effort, and make it easy to forget logging in new endpoints.

By using `ContainerRequestFilter` and `ContainerResponseFilter`, the project logs method, URI, and final status consistently for every request in one place. This improves observability without cluttering the business logic of the resource classes.

## Notes

- The project intentionally uses in-memory collections only and no database.
- The API returns JSON for normal responses and error responses.
- Because the project uses `jakarta.*` packages, it should be deployed to Tomcat 10.1 or newer rather than Tomcat 9.

## References

- Coursework brief: `5COSC022W Coursework Specification.v3(1).pdf`
- Supporting reference: `JAX-RS_Reference_Documentation.pdf`
