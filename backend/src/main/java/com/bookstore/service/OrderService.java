package com.bookstore.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import com.bookstore.entity.Order;
import com.bookstore.dto.CreateOrderRequest;
import com.bookstore.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import com.bookstore.repository.UserRepository;
import com.bookstore.repository.BookRepository;
import com.bookstore.entity.User;
import com.bookstore.entity.OrderItem;
import com.bookstore.entity.Book;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import io.jsonwebtoken.JwtException;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final JwtService jwtService;
    private final UserRepository userRepository;
    private final BookRepository bookRepository;

    public enum OrderStatus {
        CREATED,
        PAID,
        SHIPPED,
        DELIVERED,
        CANCELLED
    }

    public Order create(String authorization, CreateOrderRequest request) {
        User user = currentUser(authorization);

        if (request.getOrderLines() == null || request.getOrderLines().isEmpty()) {
            throw new IllegalArgumentException("Order lines are required");
        }

        Order order = new Order();

        List<OrderItem> orderItems = request.getOrderLines().stream()
            .map(orderLine -> {
                if (orderLine.getBookId() == null) {
                    throw new IllegalArgumentException("Book id is required");
                }
                if (orderLine.getQuantity() < 1) {
                    throw new IllegalArgumentException("Quantity must be at least 1");
                }
                Book book = bookRepository.findById(orderLine.getBookId())
                    .orElseThrow(() -> new IllegalArgumentException("Book not found"));
                if (book.getPrice() == null) {
                    throw new IllegalArgumentException("Book price is missing");
                }
                OrderItem orderItem = new OrderItem();
                orderItem.setBook(book);
                orderItem.setQuantity(orderLine.getQuantity());
                orderItem.setUnitPrice(book.getPrice());
                orderItem.setTitle(book.getTitle());
                orderItem.setOrder(order);
                return orderItem;
            }).collect(Collectors.toList());
        
        int totalPrice = orderItems.stream()
        .mapToInt(item -> item.getUnitPrice() * item.getQuantity())
        .sum();

        order.setUser(user);
        order.setOrderItems(orderItems);
        order.setTotalPrice(totalPrice);
        order.setStatus(OrderStatus.CREATED.name());
        order.setCreatedAt(LocalDateTime.now());

        return orderRepository.save(order);
    }

    public List<Order> get(String authorization) {
        User user = currentUser(authorization);
        return orderRepository.findByUserId(user.getId());
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
