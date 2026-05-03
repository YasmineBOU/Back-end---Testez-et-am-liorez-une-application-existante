package com.openclassrooms.etudiant.mapper;

import com.openclassrooms.etudiant.dto.AddUserRequestDTO;
import com.openclassrooms.etudiant.dto.RegisterDTO;
import com.openclassrooms.etudiant.dto.UpdateRequestDTO;
import com.openclassrooms.etudiant.entities.User;
import com.openclassrooms.etudiant.entities.UserRoleEnum;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import static org.junit.jupiter.api.Assertions.*;

public class UserDtoMapperTest {

    private final UserDtoMapper mapper = Mappers.getMapper(UserDtoMapper.class);

    @Test
    void test_toEntity_fromRegisterDTO_mapsBasicFieldsAndIgnoresGeneratedOnes() {
        RegisterDTO dto = new RegisterDTO();
        dto.setFirstName("Alice");
        dto.setLastName("Smith");
        dto.setLogin("alice");
        dto.setPassword("secret");

        User u = mapper.toEntity(dto);

        assertNotNull(u);
        assertEquals("Alice", u.getFirstName());
        assertEquals("Smith", u.getLastName());
        assertEquals("alice", u.getLogin());
        assertEquals("secret", u.getPassword());

        // Ignored by mapper
        assertNull(u.getId());
        assertNull(u.getCreated_at());
        assertNull(u.getUpdated_at());
        assertEquals(UserRoleEnum.USER, u.getRole());
    }

    @Test
    void test_toEntity_fromAddUserRequestDTO_preservesBasicFields_and_roleIsIgnoredByMapper() {
        AddUserRequestDTO dto = new AddUserRequestDTO();
        dto.setFirstName("Bob");
        dto.setLastName("Jones");
        dto.setLogin("bobby");
        dto.setPassword("pwd");
        dto.setRole(UserRoleEnum.ADMIN);

        User u = mapper.toEntity(dto);

        assertEquals("Bob", u.getFirstName());
        assertEquals("Jones", u.getLastName());
        assertEquals("bobby", u.getLogin());
        assertEquals("pwd", u.getPassword());

        // For AddUserRequestDTO mapper maps role from DTO
        assertEquals(UserRoleEnum.ADMIN, u.getRole());
    }

    @Test
    void test_toEntity_fromUpdateRequestDTO_mapsOptionalFields() {
        UpdateRequestDTO dto = new UpdateRequestDTO();
        dto.setFirstName("Carol");
        dto.setLastName("White");
        dto.setLogin("carolw");
        dto.setPassword("p");
        dto.setRole(UserRoleEnum.ADMIN);

        User u = mapper.toEntity(dto);

        assertEquals("Carol", u.getFirstName());
        assertEquals("White", u.getLastName());
        assertEquals("carolw", u.getLogin());
        assertEquals("p", u.getPassword());
        // For UpdateRequestDTO mapper maps role from DTO
        assertEquals(UserRoleEnum.ADMIN, u.getRole());
    }
}
