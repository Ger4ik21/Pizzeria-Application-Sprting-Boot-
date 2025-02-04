package com.modsen.pizzeria.service.impl;

import com.modsen.pizzeria.aspect.annotation.OrderOwnerOrAdminAccess;
import com.modsen.pizzeria.config.SecurityUser;
import com.modsen.pizzeria.domain.Order;
import com.modsen.pizzeria.domain.OrderStatus;
import com.modsen.pizzeria.dto.response.OrderResponse;
import com.modsen.pizzeria.dto.request.CreateOrderRequest;
import com.modsen.pizzeria.dto.request.UpdateOrderRequest;
import com.modsen.pizzeria.error.ErrorMessages;
import com.modsen.pizzeria.exception.InvalidOrderStatusException;
import com.modsen.pizzeria.exception.ResourceNotFoundException;
import com.modsen.pizzeria.mapper.OrderMapper;
import com.modsen.pizzeria.repository.OrderRepository;
import com.modsen.pizzeria.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final OrderMapper orderMapper;

    @Override
    public OrderResponse createOrder(CreateOrderRequest createOrderRequest) {
        Order order = orderMapper.toOrder(createOrderRequest);
        order.setStatus(OrderStatus.PENDING);
        SecurityUser user = (SecurityUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        order.setUser(user.getUser());
        order.getOrderItems().forEach(item -> item.setOrder(order));
        return orderMapper.toOrderResponse(orderRepository.save(order));
    }

    @Override
    @OrderOwnerOrAdminAccess
    public OrderResponse updateOrder(Long id, UpdateOrderRequest updateOrderRequest) {
        Order existingOrder = findOrderByIdOrThrow(id);
        validateStatusChange(existingOrder.getStatus(), updateOrderRequest.status());
        Order newOrder = orderMapper.toOrder(updateOrderRequest);
        existingOrder.setStatus(newOrder.getStatus());
        existingOrder.setOrderItems(newOrder.getOrderItems());
        existingOrder.getOrderItems().forEach(item -> item.setOrder(existingOrder));

        Order updatedOrder = orderRepository.save(existingOrder);
        return orderMapper.toOrderResponse(updatedOrder);
    }

    @Override
    public OrderResponse updateStatus(Long id, OrderStatus newOrderStatus) {
        Order order = findOrderByIdOrThrow(id);
        validateStatusChange(order.getStatus(), newOrderStatus);

        order.setStatus(newOrderStatus);
        return orderMapper.toOrderResponse(orderRepository.save(order));
    }


    @Override
    @OrderOwnerOrAdminAccess
    public void deleteOrder(Long id) {
        Order order = findOrderByIdOrThrow(id);
        orderRepository.deleteById(id);
    }

    @Override
    @OrderOwnerOrAdminAccess
    public OrderResponse getOrderById(Long id) {
        Order order = findOrderByIdOrThrow(id);
        return orderMapper.toOrderResponse(order);
    }

    @Override
    public List<OrderResponse> getAllOrders() {
        return orderRepository.findAll().stream()
                .map(orderMapper::toOrderResponse)
                .toList();
    }


    private Order findOrderByIdOrThrow(Long id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(String.format(ErrorMessages.RESOURCE_NOT_FOUND_MESSAGE, "Order", id)));
    }

    private void validateStatusChange(OrderStatus orderStatus, OrderStatus newOrderStatus) {
        if (orderStatus == OrderStatus.COMPLETED || orderStatus == OrderStatus.CANCELLED) {
            throw new InvalidOrderStatusException(ErrorMessages.COMPLETED_OR_CANCELLED_STATUS_MESSAGE);
        }
        if (orderStatus == OrderStatus.PENDING && newOrderStatus != OrderStatus.PROCESSING) {
            throw new InvalidOrderStatusException(ErrorMessages.FROM_PENDING_TO_PROCESSING_STATUS_MESSAGE);
        }
        if (orderStatus == OrderStatus.PROCESSING && newOrderStatus == OrderStatus.PENDING) {
            throw new InvalidOrderStatusException(ErrorMessages.FROM_PROCESSING_TO_COMPLETED_OR_CANCELLED_STATUS_MESSAGE);
        }
    }
}