package com.bookstore.service;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.bookstore.BookCategories;
import com.bookstore.entity.Book;
import com.bookstore.entity.User;
import com.bookstore.repository.BookRepository;
import com.bookstore.repository.UserRepository;

import io.jsonwebtoken.JwtException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class BookService {

	private final BookRepository bookRepository;
	private final JwtService jwtService;
	private final UserRepository userRepository;

	public List<Book> list() {
		return bookRepository.findAll();
	}

	public Book get(Long id) {
		return bookRepository.findById(id)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "找不到這本書"));
	}

	public Book create(String authorization, Book book) {
		requireAdmin(authorization, "只有管理員可以上架書籍");
		validate(book);
		book.setId(null);
		return bookRepository.save(book);
	}

	public Book update(String authorization, Long id, Book changes) {
		requireAdmin(authorization, "只有管理員可以編輯書籍");
		validate(changes);
		Book book = get(id);
		book.setTitle(changes.getTitle().trim());
		book.setAuthor(changes.getAuthor().trim());
		book.setCategory(changes.getCategory());
		book.setPrice(changes.getPrice());
		book.setStock(changes.getStock());
		book.setPublisher(blankToNull(changes.getPublisher()));
		book.setIsbn(blankToNull(changes.getIsbn()));
		book.setCoverImageUrl(blankToNull(changes.getCoverImageUrl()));
		book.setDescription(blankToNull(changes.getDescription()));
		return bookRepository.save(book);
	}

	private void validate(Book book) {
		if (book.getTitle() == null || book.getTitle().isBlank()) {
			throw new IllegalArgumentException("請填寫書名");
		}
		if (book.getAuthor() == null || book.getAuthor().isBlank()) {
			throw new IllegalArgumentException("請填寫作者");
		}
		if (book.getPrice() == null || book.getPrice() < 0) {
			throw new IllegalArgumentException("Price must be 0 or greater");
		}
		if (book.getStock() != null && book.getStock() < 0) {
			throw new IllegalArgumentException("庫存不可小於 0");
		}
		if (book.getCategory() == null || !BookCategories.NAMES.contains(book.getCategory())) {
			throw new IllegalArgumentException("分類不在允許的清單裡");
		}
	}

	private String blankToNull(String value) {
		if (value == null || value.isBlank()) {
			return null;
		}
		return value.trim();
	}

	private void requireAdmin(String authorization, String message) {
		User user = currentUser(authorization);
		if (!"ADMIN".equals(user.getRole())) {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN, message);
		}
	}

	private User currentUser(String authorization) {
		if (authorization == null || !authorization.startsWith("Bearer ")) {
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid authorization");
		}
		String token = authorization.substring("Bearer ".length()).trim();
		try {
			String email = jwtService.validateToken(token);
			return userRepository.findByEmail(email)
					.orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));
		} catch (JwtException | IllegalArgumentException ex) {
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid or expired token");
		}
	}
}
