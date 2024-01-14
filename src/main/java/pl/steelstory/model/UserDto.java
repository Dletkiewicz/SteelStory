package pl.steelstory.model;

import java.util.UUID;

public record UserDto(UUID businessId, String username, String password) {
}
