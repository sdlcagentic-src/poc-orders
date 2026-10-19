package orders;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/orders")
class OrderController {

    private final OrderService service;

    OrderController(OrderService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    OrderView createOrder(@RequestBody CreateOrderRequest request,
                          @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization) {
        return service.create(request, authorization);
    }

    @GetMapping("/{orderId}")
    OrderView getOrder(@PathVariable String orderId) {
        return service.get(orderId);
    }

    @PostMapping("/{orderId}/cancellation")
    OrderView cancelOrder(@PathVariable String orderId) {
        return service.cancel(orderId);
    }
}
