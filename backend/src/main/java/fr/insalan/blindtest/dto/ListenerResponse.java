package fr.insalan.blindtest.dto;

import fr.insalan.blindtest.model.Listener;

public record ListenerResponse(
    Integer id,
    String name
) {
    public static ListenerResponse from(Listener listener) {
        return new ListenerResponse(listener.getId(), listener.getName());
    }
}
