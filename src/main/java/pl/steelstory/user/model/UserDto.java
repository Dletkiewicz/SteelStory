package pl.steelstory.user.model;

import java.util.UUID;

public record UserDto(UUID businessId, String username, String password) {
}
