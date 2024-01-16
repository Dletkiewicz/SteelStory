package pl.steelstory.model.user;

import java.util.UUID;

public record UserDto(UUID businessId, String username, String password) {
}
