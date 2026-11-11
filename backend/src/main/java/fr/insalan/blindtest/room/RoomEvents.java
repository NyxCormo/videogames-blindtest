package fr.insalan.blindtest.room;

import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import fr.insalan.blindtest.dto.room.RoomResponse;

@Component
public class RoomEvents {

    private static final long TIMEOUT_MS = Duration.ofMinutes(30).toMillis();

    private final Map<String, List<SseEmitter>> emitters = new ConcurrentHashMap<>();

    public SseEmitter subscribe(Room room) {
        SseEmitter emitter = new SseEmitter(TIMEOUT_MS);
        List<SseEmitter> roomEmitters = emitters.computeIfAbsent(room.getCode(), code -> new CopyOnWriteArrayList<>());
        roomEmitters.add(emitter);
        emitter.onCompletion(() -> roomEmitters.remove(emitter));
        send(roomEmitters, emitter, room);
        return emitter;
    }

    public void publish(Room room) {
        List<SseEmitter> roomEmitters = emitters.getOrDefault(room.getCode(), List.of());
        for (SseEmitter emitter : roomEmitters) {
            send(roomEmitters, emitter, room);
        }
    }

    @EventListener(ContextClosedEvent.class)
    public void closeAll() {
        for (List<SseEmitter> roomEmitters : emitters.values()) {
            roomEmitters.forEach(SseEmitter::complete);
        }
    }

    private void send(List<SseEmitter> roomEmitters, SseEmitter emitter, Room room) {
        try {
            emitter.send(SseEmitter.event().name("room").data(RoomResponse.from(room)));
        } catch (IOException e) {
            roomEmitters.remove(emitter);
        }
    }
}
