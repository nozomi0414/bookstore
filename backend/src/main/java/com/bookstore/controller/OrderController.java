package com.bookstore.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import com.bookstore.entity.Order;
import com.bookstore.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RequestHeader;
import com.bookstore.dto.CreateOrderRequest;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    public Order create(
        @RequestHeader("Authorization") String authorization,
        @RequestBody CreateOrderRequest request) {
        return orderService.create(authorization, request);
    }

    @GetMapping 
    public List<Order> get(
        @RequestHeader("Authorization") String authorization) {
        return orderService.get(authorization);
    }
}
