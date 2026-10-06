package com.bookstore.dto;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class CreateOrderRequest {
    private List<OrderLineRequest> orderLines;
}
