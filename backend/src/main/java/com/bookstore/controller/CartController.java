package com.bookstore.controller;

import java.util.List;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.bookstore.dto.CartItemRequest;
import com.bookstore.dto.CartLineResponse;
import com.bookstore.service.CartService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/cart")
@RequiredArgsConstructor
public class CartController {

	private final CartService cartService;

	@GetMapping
	public List<CartLineResponse> list(
			@RequestHeader(value = "Authorization", required = false) String authorization) {
		return cartService.list(authorization);
	}

	@PostMapping
	public List<CartLineResponse> add(
			@RequestHeader(value = "Authorization", required = false) String authorization,
			@RequestBody CartItemRequest request) {
		return cartService.add(authorization, request);
	}

	@PutMapping("/{bookId}")
	public List<CartLineResponse> setQuantity(
			@RequestHeader(value = "Authorization", required = false) String authorization,
			@PathVariable Long bookId,
			@RequestBody CartItemRequest request) {
		return cartService.setQuantity(authorization, bookId, request);
	}

	@DeleteMapping("/{bookId}")
	public List<CartLineResponse> remove(
			@RequestHeader(value = "Authorization", required = false) String authorization,
			@PathVariable Long bookId) {
		return cartService.remove(authorization, bookId);
	}

	@DeleteMapping
	public List<CartLineResponse> clear(
			@RequestHeader(value = "Authorization", required = false) String authorization) {
		return cartService.clear(authorization);
	}
}
