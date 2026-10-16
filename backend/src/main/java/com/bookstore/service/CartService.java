package com.bookstore.service;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.bookstore.dto.CartItemRequest;
import com.bookstore.dto.CartLineResponse;
import com.bookstore.entity.Book;
import com.bookstore.entity.CartItem;
import com.bookstore.entity.User;
import com.bookstore.repository.BookRepository;
import com.bookstore.repository.CartItemRepository;
import com.bookstore.repository.UserRepository;

import io.jsonwebtoken.JwtException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CartService {

	private final CartItemRepository cartItemRepository;
	private final BookRepository bookRepository;
	private final UserRepository userRepository;
	private final JwtService jwtService;

	@Transactional(readOnly = true)
	public List<CartLineResponse> list(String authorization) {
		return toResponse(currentUser(authorization));
	}

	@Transactional
	public List<CartLineResponse> add(String authorization, CartItemRequest request) {
		User user = currentUser(authorization);
		if (request.getBookId() == null) {
			throw new IllegalArgumentException("Book id is required");
		}
		int addQty = request.getQuantity() == null ? 1 : request.getQuantity();
		if (addQty < 1) {
			throw new IllegalArgumentException("Quantity must be at least 1");
		}
		Book book = bookRepository.findById(request.getBookId())
				.orElseThrow(() -> new IllegalArgumentException("Book not found"));
		CartItem item = cartItemRepository.findByUserIdAndBookId(user.getId(), book.getId()).orElseGet(() -> {
			CartItem created = new CartItem();
			created.setUser(user);
			created.setBook(book);
			created.setQuantity(0);
			return created;
		});
		int next = item.getQuantity() + addQty;
		if (book.getStock() != null) {
			next = Math.min(next, book.getStock());
		}
		if (next < 1) {
			throw new IllegalArgumentException(stockMessage(book));
		}
		item.setQuantity(next);
		cartItemRepository.save(item);
		return toResponse(user);
	}

	@Transactional
	public List<CartLineResponse> setQuantity(String authorization, Long bookId, CartItemRequest request) {
		User user = currentUser(authorization);
		int quantity = request.getQuantity() == null ? 0 : request.getQuantity();
		CartItem item = cartItemRepository.findByUserIdAndBookId(user.getId(), bookId).orElse(null);
		if (item == null) {
			return toResponse(user);
		}
		if (quantity < 1) {
			cartItemRepository.delete(item);
			return toResponse(user);
		}
		Book book = item.getBook();
		int next = quantity;
		if (book.getStock() != null) {
			next = Math.min(next, book.getStock());
		}
		if (next < 1) {
			cartItemRepository.delete(item);
			return toResponse(user);
		}
		item.setQuantity(next);
		cartItemRepository.save(item);
		return toResponse(user);
	}

	@Transactional
	public List<CartLineResponse> remove(String authorization, Long bookId) {
		User user = currentUser(authorization);
		cartItemRepository.findByUserIdAndBookId(user.getId(), bookId).ifPresent(cartItemRepository::delete);
		return toResponse(user);
	}

	@Transactional
	public List<CartLineResponse> clear(String authorization) {
		User user = currentUser(authorization);
		cartItemRepository.deleteAll(cartItemRepository.findByUserIdOrderByIdAsc(user.getId()));
		return List.of();
	}

	private List<CartLineResponse> toResponse(User user) {
		return cartItemRepository.findByUserIdOrderByIdAsc(user.getId()).stream().map(item -> {
			Book book = item.getBook();
			CartLineResponse line = new CartLineResponse();
			line.setBookId(book.getId());
			line.setTitle(book.getTitle());
			line.setAuthor(book.getAuthor());
			line.setPrice(book.getPrice());
			line.setCoverImageUrl(book.getCoverImageUrl());
			line.setStock(book.getStock());
			line.setQuantity(item.getQuantity());
			return line;
		}).toList();
	}

	private String stockMessage(Book book) {
		String title = book.getTitle() == null || book.getTitle().isBlank() ? "這本書" : book.getTitle();
		int stock = book.getStock() == null ? 0 : book.getStock();
		return title + "庫存不足，目前剩下 " + stock + " 本";
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
