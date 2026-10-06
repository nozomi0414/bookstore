package com.bookstore.dto;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

@Getter
@Setter
@NoArgsConstructor
public class OrderLineRequest {

    private Long bookId;

    private int quantity;
}
