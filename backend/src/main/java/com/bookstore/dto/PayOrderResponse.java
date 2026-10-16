package com.bookstore.dto;

import com.bookstore.entity.Order;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PayOrderResponse {
	private Order order;
	private boolean mailSent;
}
