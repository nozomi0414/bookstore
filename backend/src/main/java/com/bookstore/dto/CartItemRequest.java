package com.bookstore.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CartItemRequest {
	private Long bookId;
	private Integer quantity;
}
