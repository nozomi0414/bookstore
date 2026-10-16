package com.bookstore.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.bookstore.entity.CartItem;

public interface CartItemRepository extends JpaRepository<CartItem, Long> {
	List<CartItem> findByUserIdOrderByIdAsc(Long userId);

	Optional<CartItem> findByUserIdAndBookId(Long userId, Long bookId);
}
