package com.openclassrooms.etudiant.mapper;

import com.openclassrooms.etudiant.dto.AddUserRequestDTO;
import com.openclassrooms.etudiant.dto.UpdateRequestDTO;
import com.openclassrooms.etudiant.dto.RegisterDTO;
import com.openclassrooms.etudiant.entities.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface UserDtoMapper {
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "created_at", ignore = true)
    @Mapping(target = "updated_at", ignore = true)
    @Mapping(target = "authorities", ignore = true)
    @Mapping(target = "role", ignore = true)
    User toEntity(RegisterDTO registerDTO);

    // Mapping for 'AddUserRequestDTO', similar to 'RegisterDTO'
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "created_at", ignore = true)
    @Mapping(target = "updated_at", ignore = true)
    @Mapping(target = "authorities", ignore = true)
    User toEntity(AddUserRequestDTO addUserRequestDTO);

    // Mapping for 'AddUserRequestDTO', similar to 'RegisterDTO'
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "created_at", ignore = true)
    @Mapping(target = "updated_at", ignore = true)
    @Mapping(target = "authorities", ignore = true)
    User toEntity(UpdateRequestDTO updateRequestDTO);
}
