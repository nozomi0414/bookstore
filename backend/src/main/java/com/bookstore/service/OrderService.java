package com.bookstore.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.bookstore.entity.Order;
import com.bookstore.dto.CreateOrderRequest;
import com.bookstore.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
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

    @Transactional
    public Order create(String authorization, CreateOrderRequest request) {
        User user = currentUser(authorization);

        if (request.getOrderLines() == null || request.getOrderLines().isEmpty()) {
            throw new IllegalArgumentException("Order lines are required");
        }

        String recipientName = requireText(request.getRecipientName(), "請填寫收件人", 40, "收件人最多 40 個字");
        String recipientPhone = requirePhone(request.getRecipientPhone());
        String shippingAddress = requireText(request.getShippingAddress(), "請填寫地址", 200, "地址最多 200 個字");

        Map<Long, Integer> quantities = new LinkedHashMap<>();
        for (var orderLine : request.getOrderLines()) {
            if (orderLine.getBookId() == null) {
                throw new IllegalArgumentException("Book id is required");
            }
            if (orderLine.getQuantity() < 1) {
                throw new IllegalArgumentException("Quantity must be at least 1");
            }
            quantities.merge(orderLine.getBookId(), orderLine.getQuantity(), Integer::sum);
        }

        List<Long> bookIds = new ArrayList<>(quantities.keySet());
        bookIds.sort(Long::compareTo);

        Order order = new Order();
        List<OrderItem> orderItems = new ArrayList<>();
        for (Long bookId : bookIds) {
            Book book = bookRepository.findByIdForUpdate(bookId)
                    .orElseThrow(() -> new IllegalArgumentException("Book not found"));
            int quantity = quantities.get(bookId);
            takeStock(book, quantity);
            if (book.getPrice() == null) {
                throw new IllegalArgumentException("Book price is missing");
            }
            OrderItem orderItem = new OrderItem();
            orderItem.setBook(book);
            orderItem.setQuantity(quantity);
            orderItem.setUnitPrice(book.getPrice());
            orderItem.setTitle(book.getTitle());
            orderItem.setOrder(order);
            orderItems.add(orderItem);
        }

        int totalPrice = orderItems.stream()
                .mapToInt(item -> item.getUnitPrice() * item.getQuantity())
                .sum();

        order.setUser(user);
        order.setOrderItems(orderItems);
        order.setTotalPrice(totalPrice);
        order.setRecipientName(recipientName);
        order.setRecipientPhone(recipientPhone);
        order.setShippingAddress(shippingAddress);
        order.setStatus(OrderStatus.CREATED.name());
        order.setCreatedAt(LocalDateTime.now());

        return orderRepository.save(order);
    }

    private void takeStock(Book book, int quantity) {
        if (book.getStock() == null) {
            return;
        }
        if (book.getStock() < quantity) {
            String title = book.getTitle() == null || book.getTitle().isBlank() ? "這本書" : book.getTitle();
            throw new IllegalArgumentException(title + "庫存不足，目前剩下 " + book.getStock() + " 本");
        }
        book.setStock(book.getStock() - quantity);
    }

    private String requireText(String value, String missingMessage, int maxLength, String tooLongMessage) {
        String text = value == null ? "" : value.trim();
        if (text.isEmpty()) {
            throw new IllegalArgumentException(missingMessage);
        }
        if (text.length() > maxLength) {
            throw new IllegalArgumentException(tooLongMessage);
        }
        return text;
    }

    private String requirePhone(String value) {
        String phone = value == null ? "" : value.trim();
        String digits = phone.replaceAll("\\D", "");
        if (phone.isEmpty() || !phone.matches("[0-9+\\-()\\s]+") || digits.length() < 8 || digits.length() > 15) {
            throw new IllegalArgumentException("請填寫有效的電話");
        }
        return phone;
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
