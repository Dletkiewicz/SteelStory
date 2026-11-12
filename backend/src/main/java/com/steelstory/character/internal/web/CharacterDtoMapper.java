package com.steelstory.character.internal.web;

import com.steelstory.character.internal.application.CreateCharacterCommand;
import com.steelstory.character.internal.domain.CharacterAttributes;
import com.steelstory.character.internal.domain.CharacterClass;
import com.steelstory.character.internal.domain.CharacterName;
import com.steelstory.character.internal.domain.InvalidCharacterInputException;
import com.steelstory.character.internal.domain.PlayerCharacter;
import com.steelstory.character.internal.domain.StartingAllocation;
import com.steelstory.character.internal.web.dto.CharacterAttributesResponseDto;
import com.steelstory.character.internal.web.dto.CharacterResponseDto;
import com.steelstory.character.internal.web.dto.CreateCharacterRequestDto;
import com.steelstory.character.internal.web.dto.StartingAllocationRequestDto;
import java.math.BigDecimal;

final class CharacterDtoMapper {
    private CharacterDtoMapper() {
    }

    static CreateCharacterCommand toCommand(CreateCharacterRequestDto request) {
        StartingAllocationRequestDto allocation = request.startingAllocation();
        if (allocation == null) {
            throw new InvalidCharacterInputException("Starting allocation is required.");
        }

        CharacterAttributes points = new CharacterAttributes(
                integer(allocation.strength()),
                integer(allocation.dexterity()),
                integer(allocation.intelligence()),
                integer(allocation.constitution()),
                integer(allocation.luck()));
        return new CreateCharacterCommand(
                new CharacterName(request.name()), CharacterClass.parse(request.characterClass()),
                new StartingAllocation(points));
    }

    static CharacterResponseDto toResponse(PlayerCharacter character) {
        return new CharacterResponseDto(character.id().value(), character.name().value(),
                character.characterClass().name(), character.level(), character.experience(),
                character.unspentTrainingPoints(), attributes(character.startingAttributes()),
                attributes(character.trainedAttributes()), character.createdAt(), character.updatedAt());
    }

    private static CharacterAttributesResponseDto attributes(CharacterAttributes attributes) {
        return new CharacterAttributesResponseDto(attributes.strength(), attributes.dexterity(),
                attributes.intelligence(), attributes.constitution(), attributes.luck());
    }

    private static int integer(BigDecimal value) {
        if (value == null) {
            throw new InvalidCharacterInputException("Every allocation field is required.");
        }

        try {
            return value.intValueExact();
        } catch (ArithmeticException exception) {
            throw new InvalidCharacterInputException("Allocation fields must be representable integers.");
        }
    }
}
