package fr.insalan.blindtest.room;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.stereotype.Component;

@Component
public class RoomTimer implements DisposableBean {

    private static final Duration REVEAL_TIME = Duration.ofSeconds(6);
    private static final Duration CLOSE_AFTER_END = Duration.ofMinutes(10);
    private static final Duration CLOSE_IF_NOT_STARTED = Duration.ofHours(3);

    private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
    private final RoomService roomService;
    private final RoomEvents roomEvents;

    public RoomTimer(RoomService roomService, RoomEvents roomEvents) {
        this.roomService = roomService;
        this.roomEvents = roomEvents;
    }

    public void closeIfNeverStarted(Room room) {
        schedule(Instant.now().plus(CLOSE_IF_NOT_STARTED), () -> {
            if (room.getPhase() == RoomPhase.WAITING) {
                close(room);
            }
        });
    }

    public void start(Room room, List<RoomTrack> tracks) {
        room.start(tracks, Instant.now());
        roomEvents.publish(room);
        scheduleReveal(room);
    }

    public boolean stop(Room room) {
        if (!room.stop()) {
            return false;
        }
        roomEvents.publish(room);
        scheduleClose(room);
        return true;
    }

    public void reveal(Room room, int position) {
        if (room.reveal(position)) {
            roomEvents.publish(room);
            schedule(Instant.now().plus(REVEAL_TIME), () -> next(room, position));
        }
    }

    private void scheduleReveal(Room room) {
        int position = room.getPosition();
        schedule(room.getEndsAt(), () -> reveal(room, position));
    }

    private void next(Room room, int position) {
        if (room.next(position, Instant.now())) {
            roomEvents.publish(room);
            if (room.getPhase() == RoomPhase.PLAYING) {
                scheduleReveal(room);
            } else {
                scheduleClose(room);
            }
        }
    }

    private void scheduleClose(Room room) {
        schedule(Instant.now().plus(CLOSE_AFTER_END), () -> close(room));
    }

    private void close(Room room) {
        roomService.remove(room.getCode());
        roomEvents.close(room.getCode());
    }

    private void schedule(Instant at, Runnable task) {
        executor.schedule(task, Duration.between(Instant.now(), at).toMillis(), TimeUnit.MILLISECONDS);
    }

    @Override
    public void destroy() {
        executor.shutdownNow();
    }
}
