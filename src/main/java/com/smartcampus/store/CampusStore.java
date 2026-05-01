package com.smartcampus.store;

import com.smartcampus.exception.LinkedResourceNotFoundException;
import com.smartcampus.exception.ResourceNotFoundException;
import com.smartcampus.exception.RoomNotEmptyException;
import com.smartcampus.exception.SensorUnavailableException;
import com.smartcampus.model.Room;
import com.smartcampus.model.Sensor;
import com.smartcampus.model.SensorReading;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.ClientErrorException;
import javax.ws.rs.core.Response;

public final class CampusStore {

    private static final CampusStore INSTANCE = new CampusStore();

    private final Map<String, Room> rooms = new ConcurrentHashMap<>();
    private final Map<String, Sensor> sensors = new ConcurrentHashMap<>();
    private final Map<String, List<SensorReading>> readingsBySensor = new ConcurrentHashMap<>();

    private CampusStore() {
    }

    public static CampusStore getInstance() {
        return INSTANCE;
    }

    public synchronized List<Room> listRooms() {
        return rooms.values().stream()
                .map(this::copyRoom)
                .sorted(Comparator.comparing(Room::getId))
                .toList();
    }

    public synchronized Room createRoom(Room room) {
        if (room == null) {
            throw new BadRequestException("Room payload is required.");
        }

        String id = requireNonBlank(room.getId(), "Room id");
        String name = requireNonBlank(room.getName(), "Room name");

        if (room.getCapacity() < 0) {
            throw new BadRequestException("Room capacity cannot be negative.");
        }

        if (rooms.containsKey(id)) {
            throw new ClientErrorException("Room with id '" + id + "' already exists.", Response.Status.CONFLICT);
        }

        Room storedRoom = new Room(id, name, room.getCapacity(), new ArrayList<>());
        rooms.put(id, storedRoom);
        return copyRoom(storedRoom);
    }

    public synchronized Room getRoom(String roomId) {
        Room room = rooms.get(normalizeId(roomId));
        if (room == null) {
            throw new ResourceNotFoundException("Room '" + roomId + "' was not found.");
        }
        return copyRoom(room);
    }

    public synchronized void deleteRoom(String roomId) {
        String normalizedRoomId = normalizeId(roomId);
        Room room = rooms.get(normalizedRoomId);
        if (room == null) {
            throw new ResourceNotFoundException("Room '" + roomId + "' was not found.");
        }

        if (!room.getSensorIds().isEmpty()) {
            throw new RoomNotEmptyException(
                    "Room '" + normalizedRoomId + "' cannot be deleted because sensors are still assigned to it.");
        }

        rooms.remove(normalizedRoomId);
    }

    public synchronized List<Sensor> listSensors(String type) {
        return sensors.values().stream()
                .filter(sensor -> type == null || type.isBlank()
                        || sensor.getType().equalsIgnoreCase(type.trim()))
                .map(this::copySensor)
                .sorted(Comparator.comparing(Sensor::getId))
                .toList();
    }

    public synchronized Sensor createSensor(Sensor sensor) {
        if (sensor == null) {
            throw new BadRequestException("Sensor payload is required.");
        }

        String id = requireNonBlank(sensor.getId(), "Sensor id");
        String type = requireNonBlank(sensor.getType(), "Sensor type");
        String roomId = requireNonBlank(sensor.getRoomId(), "Sensor roomId");
        String status = normalizeStatus(sensor.getStatus());

        if (!Double.isFinite(sensor.getCurrentValue())) {
            throw new BadRequestException("Sensor currentValue must be a valid number.");
        }

        Room room = rooms.get(roomId);
        if (room == null) {
            throw new LinkedResourceNotFoundException(
                    "Cannot create sensor '" + id + "' because room '" + roomId + "' does not exist.");
        }

        if (sensors.containsKey(id)) {
            throw new ClientErrorException("Sensor with id '" + id + "' already exists.", Response.Status.CONFLICT);
        }

        Sensor storedSensor = new Sensor(id, type, status, sensor.getCurrentValue(), roomId);
        sensors.put(id, storedSensor);
        readingsBySensor.putIfAbsent(id, new ArrayList<>());

        if (!room.getSensorIds().contains(id)) {
            room.getSensorIds().add(id);
        }

        return copySensor(storedSensor);
    }

    public synchronized Sensor getSensor(String sensorId) {
        Sensor sensor = sensors.get(normalizeId(sensorId));
        if (sensor == null) {
            throw new ResourceNotFoundException("Sensor '" + sensorId + "' was not found.");
        }
        return copySensor(sensor);
    }

    public synchronized void deleteSensor(String sensorId) {
        String normalizedSensorId = normalizeId(sensorId);
        Sensor sensor = sensors.remove(normalizedSensorId);
        if (sensor == null) {
            throw new ResourceNotFoundException("Sensor '" + sensorId + "' was not found.");
        }

        Room room = rooms.get(sensor.getRoomId());
        if (room != null) {
            room.getSensorIds().remove(normalizedSensorId);
        }

        readingsBySensor.remove(normalizedSensorId);
    }

    public synchronized List<SensorReading> listReadings(String sensorId) {
        Sensor sensor = sensors.get(normalizeId(sensorId));
        if (sensor == null) {
            throw new ResourceNotFoundException("Sensor '" + sensorId + "' was not found.");
        }

        List<SensorReading> readings = readingsBySensor.getOrDefault(sensor.getId(), new ArrayList<>());
        return readings.stream()
                .map(this::copyReading)
                .sorted(Comparator.comparingLong(SensorReading::getTimestamp))
                .toList();
    }

    public synchronized SensorReading addReading(String sensorId, SensorReading reading) {
        if (reading == null) {
            throw new BadRequestException("Sensor reading payload is required.");
        }

        String normalizedSensorId = normalizeId(sensorId);
        Sensor sensor = sensors.get(normalizedSensorId);
        if (sensor == null) {
            throw new ResourceNotFoundException("Sensor '" + sensorId + "' was not found.");
        }

        if ("MAINTENANCE".equalsIgnoreCase(sensor.getStatus())) {
            throw new SensorUnavailableException(
                    "Sensor '" + normalizedSensorId + "' is in MAINTENANCE mode and cannot accept readings.");
        }

        if (!Double.isFinite(reading.getValue())) {
            throw new BadRequestException("Reading value must be a valid number.");
        }

        String readingId = reading.getId() == null || reading.getId().isBlank()
                ? UUID.randomUUID().toString()
                : reading.getId().trim();

        long timestamp = reading.getTimestamp() <= 0 ? System.currentTimeMillis() : reading.getTimestamp();

        List<SensorReading> readings = readingsBySensor.computeIfAbsent(normalizedSensorId, key -> new ArrayList<>());
        boolean duplicateReading = readings.stream().anyMatch(item -> item.getId().equals(readingId));
        if (duplicateReading) {
            throw new ClientErrorException(
                    "Reading with id '" + readingId + "' already exists for sensor '" + normalizedSensorId + "'.",
                    Response.Status.CONFLICT
            );
        }

        SensorReading storedReading = new SensorReading(readingId, timestamp, reading.getValue());
        readings.add(storedReading);
        sensor.setCurrentValue(storedReading.getValue());

        return copyReading(storedReading);
    }

    private String requireNonBlank(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new BadRequestException(fieldName + " is required.");
        }
        return value.trim();
    }

    private String normalizeStatus(String status) {
        String resolvedStatus = status == null || status.isBlank() ? "ACTIVE" : status.trim().toUpperCase();
        if (!resolvedStatus.equals("ACTIVE")
                && !resolvedStatus.equals("MAINTENANCE")
                && !resolvedStatus.equals("OFFLINE")) {
            throw new BadRequestException("Sensor status must be ACTIVE, MAINTENANCE, or OFFLINE.");
        }
        return resolvedStatus;
    }

    private String normalizeId(String value) {
        if (value == null || value.isBlank()) {
            throw new BadRequestException("Resource id is required.");
        }
        return value.trim();
    }

    private Room copyRoom(Room room) {
        return new Room(room.getId(), room.getName(), room.getCapacity(), room.getSensorIds());
    }

    private Sensor copySensor(Sensor sensor) {
        return new Sensor(
                sensor.getId(),
                sensor.getType(),
                sensor.getStatus(),
                sensor.getCurrentValue(),
                sensor.getRoomId()
        );
    }

    private SensorReading copyReading(SensorReading reading) {
        return new SensorReading(reading.getId(), reading.getTimestamp(), reading.getValue());
    }
}

