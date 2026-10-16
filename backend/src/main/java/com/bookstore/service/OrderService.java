package com.bookstore.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.bookstore.entity.Order;
import com.bookstore.dto.CreateOrderRequest;
import com.bookstore.dto.PayOrderResponse;
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
    private final OrderMailService orderMailService;

    public enum OrderStatus {
        UNPAID,
        CREATED,
        PAID,
        SHIPPED,
        DELIVERED,
        COMPLETED,
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
            ensureStock(book, quantity);
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
        order.setStatus(OrderStatus.UNPAID.name());
        order.setCreatedAt(LocalDateTime.now());

        return orderRepository.save(order);
    }

    @Transactional
    public Order cancel(String authorization, Long orderId) {
        Order order = ownedOrder(authorization, orderId);
        boolean unpaid = OrderStatus.UNPAID.name().equals(order.getStatus());
        boolean legacyCreated = OrderStatus.CREATED.name().equals(order.getStatus());
        if (!unpaid && !legacyCreated) {
            throw new IllegalArgumentException("這筆訂單無法取消");
        }
        if (legacyCreated) {
            for (OrderItem item : order.getOrderItems()) {
                Book book = bookRepository.findByIdForUpdate(item.getBook().getId())
                        .orElseThrow(() -> new IllegalArgumentException("Book not found"));
                restoreStock(book, item.getQuantity());
            }
        }
        order.setStatus(OrderStatus.CANCELLED.name());
        return orderRepository.save(order);
    }

    @Transactional
    public Order receive(String authorization, Long orderId) {
        Order order = ownedOrder(authorization, orderId);
        if (!OrderStatus.DELIVERED.name().equals(order.getStatus())) {
            throw new IllegalArgumentException("這筆訂單還沒抵達，不能確認收貨");
        }
        order.setStatus(OrderStatus.COMPLETED.name());
        return orderRepository.save(order);
    }

    @Transactional
    public PayOrderResponse pay(String authorization, Long orderId) {
        Order order = ownedOrder(authorization, orderId);
        boolean unpaid = OrderStatus.UNPAID.name().equals(order.getStatus());
        boolean legacyCreated = OrderStatus.CREATED.name().equals(order.getStatus());
        if (!unpaid && !legacyCreated) {
            throw new IllegalArgumentException("這筆訂單不能付款");
        }
        if (unpaid) {
            for (OrderItem item : order.getOrderItems()) {
                Book book = bookRepository.findByIdForUpdate(item.getBook().getId())
                        .orElseThrow(() -> new IllegalArgumentException("Book not found"));
                takeStock(book, item.getQuantity());
            }
        }
        order.setStatus(OrderStatus.DELIVERED.name());
        order = orderRepository.save(order);
        boolean mailSent = orderMailService.notifyEstablished(order);
        PayOrderResponse response = new PayOrderResponse();
        response.setOrder(order);
        response.setMailSent(mailSent);
        return response;
    }

    private void ensureStock(Book book, int quantity) {
        if (book.getStock() == null || book.getStock() >= quantity) {
            return;
        }
        throw new IllegalArgumentException(stockMessage(book));
    }

    private void restoreStock(Book book, int quantity) {
        if (book.getStock() == null) {
            return;
        }
        book.setStock(book.getStock() + quantity);
    }

    private void takeStock(Book book, int quantity) {
        if (book.getStock() == null) {
            return;
        }
        if (book.getStock() < quantity) {
            throw new IllegalArgumentException(stockMessage(book));
        }
        book.setStock(book.getStock() - quantity);
    }

    private String stockMessage(Book book) {
        String title = book.getTitle() == null || book.getTitle().isBlank() ? "這本書" : book.getTitle();
        return title + "庫存不足，目前剩下 " + book.getStock() + " 本";
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

    private Order ownedOrder(String authorization, Long orderId) {
        User user = currentUser(authorization);
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "找不到這筆訂單"));
        if (order.getUser() == null || !user.getId().equals(order.getUser().getId())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "找不到這筆訂單");
        }
        return order;
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
