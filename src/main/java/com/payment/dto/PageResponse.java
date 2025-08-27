package com.payment.dto;


import lombok.*;
import java.util.List;


@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PageResponse<T> {
private List<T> items;
private long total;
private int limit;
private int offset;
}