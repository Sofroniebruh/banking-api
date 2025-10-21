package com.example.banking_api.users;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {
    @Query("""
    SELECT CASE WHEN EXISTS (SELECT 1 FROM User u WHERE u.id = :userId) 
           THEN TRUE ELSE FALSE END
    """)
    boolean userExists(@Param("userId") UUID userId);

    Optional<User> findUserById(UUID id);

    Optional<User> findUserByEmail(String email);
}
