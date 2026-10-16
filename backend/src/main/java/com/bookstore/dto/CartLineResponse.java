package com.bookstore.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CartLineResponse {
	private Long bookId;
	private String title;
	private String author;
	private Integer price;
	private String coverImageUrl;
	private Integer stock;
	private int quantity;
}
