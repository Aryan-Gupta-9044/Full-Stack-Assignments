package com.example.spb.controller;

import com.example.spb.model.Book;
import com.example.spb.model.BorrowRecord;
import com.example.spb.service.LibraryService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
public class LibraryController {

    private final LibraryService libraryService;

    public LibraryController(LibraryService libraryService) {
        this.libraryService = libraryService;
    }

    @GetMapping("/")
    public String home() {
        return "Library Management System is running!";
    }

    @GetMapping("/books")
    public List<Book> getAllBooks() {
        return libraryService.getAllBooks();
    }

    @PostMapping("/books")
    public Book addBook(@RequestBody Book book) {
        return libraryService.addBook(book);
    }

    @PostMapping("/borrow")
    public BorrowRecord borrowBook(
            @RequestParam Long bookId,
            @RequestParam Long studentId) {

        return libraryService.borrowBook(bookId, studentId);
    }

    // Handle business rule violations
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<String> handleRuntimeException(RuntimeException ex) {
        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(ex.getMessage());
    }
}