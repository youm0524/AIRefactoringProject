package com.careerpick.repository;

import com.careerpick.entity.CommunityRoomMember;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface CommunityRoomMemberRepository extends JpaRepository<CommunityRoomMember, Long> {
    @Modifying
    @Query(value = """
        INSERT INTO community_room_members (user_id, room_id, joined_at)
        VALUES (:userId, :roomId, UTC_TIMESTAMP(6))
        ON DUPLICATE KEY UPDATE joined_at = joined_at
        """, nativeQuery = true)
    void join(@Param("userId") long userId, @Param("roomId") long roomId);

    boolean existsByUserIdAndRoomId(long userId, long roomId);
    List<CommunityRoomMember> findByUserId(long userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT member FROM CommunityRoomMember member WHERE member.user.id = :userId AND member.room.id = :roomId")
    Optional<CommunityRoomMember> findForUpdate(@Param("userId") long userId, @Param("roomId") long roomId);
}
