package com.careerpick.repository;

import com.careerpick.entity.CommunityRoom;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface CommunityRoomRepository extends JpaRepository<CommunityRoom, Long> {
    Optional<CommunityRoom> findByCompanyName(String companyName);
}
