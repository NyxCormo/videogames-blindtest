package fr.insalan.blindtest.room;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.stereotype.Component;

@Component
public class RoomTimer implements DisposableBean {

    private static final Logger log = LoggerFactory.getLogger(RoomTimer.class);
    private static final Duration REVEAL_TIME = Duration.ofSeconds(6);
    private static final Duration PRESENCE_CHECK = Duration.ofSeconds(5);
    private static final Duration EMPTY_INFINITE_ROOM = Duration.ofMinutes(10);
    private static final Duration CLOSE_AFTER_END = Duration.ofMinutes(10);
    private static final Duration CLOSE_IF_NOT_STARTED = Duration.ofHours(3);

    private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
    private final RoomService roomService;
    private final RoomEvents roomEvents;
    private final InfinitePicker infinitePicker;

    public RoomTimer(RoomService roomService, RoomEvents roomEvents, InfinitePicker infinitePicker) {
        this.roomService = roomService;
        this.roomEvents = roomEvents;
        this.infinitePicker = infinitePicker;
        long period = PRESENCE_CHECK.toMillis();
        executor.scheduleAtFixedRate(this::checkPresence, period, period, TimeUnit.MILLISECONDS);
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

    public boolean reveal(Room room, int position) {
        if (!room.reveal(position)) {
            return false;
        }
        roomEvents.publish(room);
        if (room.getMode() == RoomMode.MULTI) {
            schedule(Instant.now().plus(REVEAL_TIME), () -> next(room, position));
        }
        return true;
    }

    public void presenceChanged(Room room) {
        roomEvents.publish(room);
        revealIfEveryoneDone(room);
    }

    public void revealIfEveryoneDone(Room room) {
        if (room.getMode() == RoomMode.MULTI && room.everyoneDone()) {
            reveal(room, room.getPosition());
        }
    }

    private void checkPresence() {
        try {
            Instant now = Instant.now();
            for (Room room : roomService.all()) {
                if (room.checkPresence(now)) {
                    presenceChanged(room);
                }
                if (room.isInfinite() && room.abandoned(now, EMPTY_INFINITE_ROOM)) {
                    stop(room);
                }
            }
        } catch (RuntimeException e) {
            log.warn("Vérification de la présence impossible : {}", e.getMessage());
        }
    }

    public boolean next(Room room, int position) {
        RoomTrack upcoming = room.isInfinite() ? infinitePicker.pick(room) : null;
        if (!room.next(position, Instant.now(), upcoming)) {
            return false;
        }
        roomEvents.publish(room);
        if (room.getPhase() == RoomPhase.PLAYING) {
            scheduleReveal(room);
        } else {
            scheduleClose(room);
        }
        return true;
    }

    private void scheduleReveal(Room room) {
        if (room.getMode() == RoomMode.PRESENTATION) {
            return;
        }
        int position = room.getPosition();
        schedule(room.getEndsAt(), () -> reveal(room, position));
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
