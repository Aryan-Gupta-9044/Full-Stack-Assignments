package com.example.spb.repository;

import com.example.spb.model.BorrowRecord;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BorrowRecordRepository extends JpaRepository<BorrowRecord, Long> {

    long countByStudentIdAndReturnedFalse(Long studentId);
}