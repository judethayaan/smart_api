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
- Java EE (JAX-RS using javax.ws.rs)
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

Coursework Report Answers
Part 1: Service Architecture & Setup
1. Project and Application Configuration

In JAX-RS, resource classes typically follow a request-scoped lifecycle. This means a fresh instance of the resource is created for each incoming HTTP request unless it is explicitly defined as a singleton. Such an approach minimizes the risk of unintended shared mutable state, making resource classes more reliable and easier to maintain.

Since these resource objects are short-lived, storing persistent data within them is not practical. Any data stored there would be lost after each request completes. To handle this, I used a separate singleton-like component called CampusStore to manage application state. Given that multiple requests can occur simultaneously, thread safety is ensured by synchronizing write operations and using thread-safe structures like ConcurrentHashMap. This helps avoid inconsistencies when multiple related updates occur together, such as adding a sensor and linking it to a room.

2. Discovery Endpoint

Hypermedia plays an important role in RESTful APIs by allowing responses to guide clients on possible next actions. Instead of relying solely on predefined URLs or external documentation, clients can dynamically navigate the API using links provided in responses.

This approach reduces dependency between the client and the server’s URI structure. Even if endpoints change, clients can continue functioning by following provided links. While documentation remains useful, hypermedia enhances the API’s self-descriptiveness and usability.

Part 2: Room Management
1. Room Resource Implementation

Providing only room IDs in responses helps reduce payload size and saves bandwidth, especially when dealing with large datasets or when clients only need identifiers. However, this approach requires additional requests if clients need detailed information like room names or capacities.

On the other hand, returning complete room objects simplifies client-side operations by minimizing the number of requests needed. The downside is increased response size and possibly unnecessary data transfer. For this project, full room details are returned because the dataset is small and it improves usability during development and testing.

2. Room Deletion and Safety Logic

The DELETE operation is designed to be idempotent. When a room without sensors is deleted, it is removed from the system. If the same request is repeated, the room will no longer exist, resulting in a 404 Not Found response. Despite the change in status code, the system state remains unchanged after repeated requests, which satisfies the definition of idempotency.

Part 3: Sensor Operations & Linking
1. Sensor Resource and Integrity

The @Consumes(MediaType.APPLICATION_JSON) annotation specifies that the endpoint accepts JSON input. If a client sends data in a different format, such as XML or plain text, the framework will not process it and will usually return a 415 Unsupported Media Type response.

This ensures that the API only processes data in expected formats, preventing errors and providing clear feedback to clients when incorrect formats are used.

2. Filtered Retrieval and Search

Using @QueryParam for filtering is more appropriate because it maintains the concept of accessing the same resource with additional constraints. For example, /sensors and /sensors?type=CO2 both refer to the sensors collection, with the latter applying a filter.

In contrast, using path-based filtering like /sensors/type/CO2 suggests a different hierarchical resource. Query parameters are also more flexible and can easily support multiple conditions, such as ?type=CO2&status=ACTIVE, making them more scalable for future enhancements.

Part 4: Deep Nesting with Sub-Resources
1. The Sub-Resource Locator Pattern

The Sub-Resource Locator pattern improves code structure by delegating specific responsibilities to smaller, focused classes. In this implementation, SensorResource handles sensor-related operations, while SensorReadingResource manages readings associated with a particular sensor. This separation prevents resource classes from becoming overly complex.

As the system grows, this pattern supports better organization by allowing each sub-resource to handle its own logic, validation, and responses. This leads to improved maintainability and easier testing.

2. Historical Data Management

When a new sensor reading is added, it is stored in the sensor’s history, and the sensor’s currentValue is updated immediately. This ensures consistency between the latest sensor value and its recorded history. Clients can either access the current value directly or retrieve historical data for more detailed analysis.

Part 5: Advanced Error Handling, Exception Mapping and Logging
1. Resource Conflict (409)

A custom RoomNotEmptyException is thrown when attempting to delete a room that still contains sensors. This is mapped to a 409 Conflict response with a structured JSON message. The request itself is valid, but it conflicts with the current state of the system.

2. Dependency Validation (422)

The 422 Unprocessable Entity status code is more appropriate than 404 Not Found in this context because the endpoint exists and the request is properly structured. The issue lies within the request data, where it references a non-existent resource. This code clearly indicates that the server understands the request but cannot process it due to invalid input.

3. State Constraint (403)

If a sensor is under maintenance, the API prevents new readings from being added by returning a 403 Forbidden response via the SensorUnavailableException. This reflects a business rule where the request is valid, but the operation is not allowed due to the current state of the resource.

4. Global Safety Net (500)

Exposing internal error details such as stack traces can create security risks by revealing sensitive implementation information. Attackers could use this information to identify vulnerabilities or weaknesses in the system.

To prevent this, a global ExceptionMapper<Throwable> is used to handle unexpected errors and return a generic 500 Internal Server Error response, keeping internal details hidden.

5. API Request and Response Logging Filters

Logging is handled more effectively using JAX-RS filters, as they allow cross-cutting concerns to be managed in a centralized manner. Embedding logging code within each resource method would lead to duplication and increased maintenance effort.

By implementing ContainerRequestFilter and ContainerResponseFilter, the system logs request methods, URIs, and response statuses consistently across all endpoints, improving monitoring without cluttering business logic.

Notes
The application uses only in-memory data storage and does not rely on a database.
All responses, including errors, are returned in JSON format.
Since the project uses jakarta.* packages, it should be deployed on Tomcat 10.1 or later instead of Tomcat 9.
