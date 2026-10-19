package orders;

import java.util.UUID;

public record LineItem(UUID productId, Integer quantity) {
}
