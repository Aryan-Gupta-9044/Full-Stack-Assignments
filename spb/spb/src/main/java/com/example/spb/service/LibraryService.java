package com.example.spb.service;

import com.example.spb.model.Book;
import com.example.spb.model.BorrowRecord;
import com.example.spb.repository.BookRepository;
import com.example.spb.repository.BorrowRecordRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class LibraryService {

    private final BookRepository bookRepository;
    private final BorrowRecordRepository borrowRecordRepository;

    public LibraryService(BookRepository bookRepository,
                          BorrowRecordRepository borrowRecordRepository) {
        this.bookRepository = bookRepository;
        this.borrowRecordRepository = borrowRecordRepository;
    }

    // Get all books
    public List<Book> getAllBooks() {
        return bookRepository.findAll();
    }

    // Add a new book
    public Book addBook(Book book) {
        book.setId(null);
        book.setAvailable(true);
        return bookRepository.save(book);
    }

    // Business rules + borrow operation
    public BorrowRecord borrowBook(Long bookId, Long studentId) {

        // Find the book
        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new RuntimeException("Book not found"));

        // Business Rule 1:
        // A book cannot be borrowed if it is already unavailable.
        if (!book.isAvailable()) {
            throw new RuntimeException("Book is already borrowed");
        }

        // Business Rule 2:
        // A student can have maximum 3 active borrowed books.
        long activeBorrowCount =
                borrowRecordRepository.countByStudentIdAndReturnedFalse(studentId);

        if (activeBorrowCount >= 3) {
            throw new RuntimeException(
                    "Student cannot borrow more than 3 books at a time"
            );
        }

        // Mark book unavailable
        book.setAvailable(false);
        bookRepository.save(book);

        // Create borrow record
        BorrowRecord record = new BorrowRecord(bookId, studentId);

        return borrowRecordRepository.save(record);
    }
}