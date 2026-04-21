package com.openclassrooms.etudiant.repository;

import com.openclassrooms.etudiant.dto.UserBasicInfoDTO;
import com.openclassrooms.etudiant.dto.UserSummaryDTO;
import com.openclassrooms.etudiant.entities.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByLogin(String login);

    @Query("SELECT new com.openclassrooms.etudiant.dto.UserBasicInfoDTO(u.id, u.firstName, u.lastName, u.role) FROM User u")
    List<UserBasicInfoDTO> findAllUserBasicInfo();
    
    @Query("SELECT new com.openclassrooms.etudiant.dto.UserSummaryDTO(u.id, u.firstName, u.lastName, u.login, u.role, u.created_at, u.updated_at) FROM User u WHERE u.id = :id")
    UserSummaryDTO findUserById(Long id);
}
