package com.enterprise.matching.orderbook;

/**
 * Cache-aligned primitive-optimized order representation.
 * Avoids Java object boxing overhead.
 * 
 * @author Yousef Khaled (Financial Systems Architecture - 2008)
 */
public class Order {
    private final long orderId;
    private final String symbol;
    private final Side side;
    private final OrderType orderType;
    private final long price; // Fixed point (e.g. 10000 = $1.0000)
    private long remainingQuantity;
    private final long originalQuantity;
    private final long timestampNs;

    public Order(long orderId, String symbol, Side side, OrderType orderType, long price, long quantity, long timestampNs) {
        this.orderId = orderId;
        this.symbol = symbol;
        this.side = side;
        this.orderType = orderType;
        this.price = price;
        this.remainingQuantity = quantity;
        this.originalQuantity = quantity;
        this.timestampNs = timestampNs;
    }

    public long getOrderId() { return orderId; }
    public String getSymbol() { return symbol; }
    public Side getSide() { return side; }
    public OrderType getOrderType() { return orderType; }
    public long getPrice() { return price; }
    public long getRemainingQuantity() { return remainingQuantity; }
    public long getOriginalQuantity() { return originalQuantity; }
    public long getTimestampNs() { return timestampNs; }

    public void fill(long qty) {
        if (qty > remainingQuantity) {
            throw new IllegalArgumentException("Fill quantity exceeds remaining quantity");
        }
        this.remainingQuantity -= qty;
    }

    public boolean isFilled() {
        return remainingQuantity == 0;
    }

    @Override
    public String toString() {
        return String.format("Order[#%d %s %s %d@%d rem=%d]", 
            orderId, symbol, side, originalQuantity, price, remainingQuantity);
    }
}
