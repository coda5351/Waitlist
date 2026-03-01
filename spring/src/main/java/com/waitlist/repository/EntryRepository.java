package com.waitlist.repository;

import com.waitlist.model.Entry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EntryRepository extends JpaRepository<Entry, Long> {
    // only return non-deleted entries ordered by time
    List<Entry> findByActiveTrueOrderByTimestampAsc();

    // look up by the public code (only active entries)
    java.util.Optional<Entry> findByCodeAndActiveTrue(String code);

    // mark all entries inactive (soft delete) in a single operation
    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.data.jpa.repository.Query("update Entry e set e.active = false where e.active = true")
    void deactivateAll();
}