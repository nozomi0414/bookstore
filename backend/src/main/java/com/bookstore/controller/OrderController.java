package com.bookstore.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import com.bookstore.entity.Order;
import com.bookstore.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RequestHeader;
import com.bookstore.dto.CreateOrderRequest;
import com.bookstore.dto.PayOrderResponse;

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

    @PostMapping("/{id}/pay")
    public PayOrderResponse pay(
            @RequestHeader("Authorization") String authorization,
            @PathVariable Long id) {
        return orderService.pay(authorization, id);
    }

    @PostMapping("/{id}/cancel")
    public Order cancel(
            @RequestHeader("Authorization") String authorization,
            @PathVariable Long id) {
        return orderService.cancel(authorization, id);
    }

    @PostMapping("/{id}/receive")
    public Order receive(
            @RequestHeader("Authorization") String authorization,
            @PathVariable Long id) {
        return orderService.receive(authorization, id);
    }

    @GetMapping 
    public List<Order> get(
        @RequestHeader("Authorization") String authorization) {
        return orderService.get(authorization);
    }
}
