package com.bookstore.service;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.bookstore.entity.Book;
import com.bookstore.repository.BookRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class BookService {

	private final BookRepository bookRepository;

	public List<Book> list() {
		return bookRepository.findAll();
	}

	public Book get(Long id) {
		return bookRepository.findById(id)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
	}

	public Book create(Book book) {
		if (book.getPrice() == null || book.getPrice() < 0) {
			throw new IllegalArgumentException("Price must be 0 or greater");
		}
		return bookRepository.save(book);
	}
}
