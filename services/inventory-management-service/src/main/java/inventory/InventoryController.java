package inventory;

import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
class InventoryController {

    private final InventoryService service;

    InventoryController(InventoryService service) {
        this.service = service;
    }

    @PostMapping("/reservations")
    ResponseEntity<Api.Reservation> reserveStock(@RequestBody Api.ReserveStockRequest request) {
        InventoryService.Outcome outcome = service.reserve(request);
        if (outcome.reservation() == null) {
            String ids = outcome.unavailable().stream().map(Object::toString).collect(Collectors.joining(", "));
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Out of stock: " + ids);
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(outcome.reservation());
    }

    @GetMapping("/stock-levels")
    Api.StockLevelList listStockLevels() {
        return new Api.StockLevelList(service.stockLevels());
    }
}
