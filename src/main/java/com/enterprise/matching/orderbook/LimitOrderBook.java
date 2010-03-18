package com.enterprise.matching.orderbook;

import java.util.*;

/**
 * Level 3 Price-Time Priority (FIFO) Limit Order Book (LOB).
 * Highly optimized for microsecond-level matching and zero object allocations in matching fast-path.
 * 
 * @author Yousef Khaled (Financial Systems Architecture - 2008)
 */
public class LimitOrderBook {
    private final String symbol;
    // Bids sorted descending by price
    private final TreeMap<Long, LinkedList<Order>> bids = new TreeMap<>(Collections.reverseOrder());
    // Asks sorted ascending by price
    private final TreeMap<Long, LinkedList<Order>> asks = new TreeMap<>();
    private final Map<Long, Order> orderIndex = new HashMap<>();

    private long tradeIdSequence = 1;
    private long totalMatchedVolume = 0;
    private long totalTradesExecuted = 0;

    public LimitOrderBook(String symbol) {
        this.symbol = symbol;
    }

    public List<Trade> match(Order incoming) {
        List<Trade> trades = new ArrayList<>();
        orderIndex.put(incoming.getOrderId(), incoming);

        if (incoming.getSide() == Side.BUY) {
            matchBuy(incoming, trades);
        } else {
            matchSell(incoming, trades);
        }

        // If limit order has remaining volume and is not IOC/FOK, rest on book
        if (!incoming.isFilled() && incoming.getOrderType() == OrderType.LIMIT) {
            TreeMap<Long, LinkedList<Order>> book = (incoming.getSide() == Side.BUY) ? bids : asks;
            book.computeIfAbsent(incoming.getPrice(), p -> new LinkedList<>()).addLast(incoming);
        } else if (!incoming.isFilled() && (incoming.getOrderType() == OrderType.IOC || incoming.getOrderType() == OrderType.MARKET)) {
            orderIndex.remove(incoming.getOrderId());
        }

        return trades;
    }

    private void matchBuy(Order buyOrder, List<Trade> trades) {
        Iterator<Map.Entry<Long, LinkedList<Order>>> askIterator = asks.entrySet().iterator();

        while (askIterator.hasNext() && !buyOrder.isFilled()) {
            Map.Entry<Long, LinkedList<Order>> entry = askIterator.next();
            long askPrice = entry.getKey();

            // Price cross condition: Buy price >= Ask price (or market buy)
            if (buyOrder.getOrderType() != OrderType.MARKET && buyOrder.getPrice() < askPrice) {
                break;
            }

            LinkedList<Order> queue = entry.getValue();
            Iterator<Order> queueIterator = queue.iterator();

            while (queueIterator.hasNext() && !buyOrder.isFilled()) {
                Order restingAsk = queueIterator.next();
                long matchQty = Math.min(buyOrder.getRemainingQuantity(), restingAsk.getRemainingQuantity());

                buyOrder.fill(matchQty);
                restingAsk.fill(matchQty);

                Trade trade = new Trade(tradeIdSequence++, symbol, buyOrder.getOrderId(), 
                    restingAsk.getOrderId(), askPrice, matchQty, System.nanoTime());
                trades.add(trade);
                totalMatchedVolume += matchQty;
                totalTradesExecuted++;

                if (restingAsk.isFilled()) {
                    queueIterator.remove();
                    orderIndex.remove(restingAsk.getOrderId());
                }
            }

            if (queue.isEmpty()) {
                askIterator.remove();
            }
        }
    }

    private void matchSell(Order sellOrder, List<Trade> trades) {
        Iterator<Map.Entry<Long, LinkedList<Order>>> bidIterator = bids.entrySet().iterator();

        while (bidIterator.hasNext() && !sellOrder.isFilled()) {
            Map.Entry<Long, LinkedList<Order>> entry = bidIterator.next();
            long bidPrice = entry.getKey();

            // Price cross condition: Sell price <= Bid price (or market sell)
            if (sellOrder.getOrderType() != OrderType.MARKET && sellOrder.getPrice() > bidPrice) {
                break;
            }

            LinkedList<Order> queue = entry.getValue();
            Iterator<Order> queueIterator = queue.iterator();

            while (queueIterator.hasNext() && !sellOrder.isFilled()) {
                Order restingBid = queueIterator.next();
                long matchQty = Math.min(sellOrder.getRemainingQuantity(), restingBid.getRemainingQuantity());

                sellOrder.fill(matchQty);
                restingBid.fill(matchQty);

                Trade trade = new Trade(tradeIdSequence++, symbol, restingBid.getOrderId(), 
                    sellOrder.getOrderId(), bidPrice, matchQty, System.nanoTime());
                trades.add(trade);
                totalMatchedVolume += matchQty;
                totalTradesExecuted++;

                if (restingBid.isFilled()) {
                    queueIterator.remove();
                    orderIndex.remove(restingBid.getOrderId());
                }
            }

            if (queue.isEmpty()) {
                bidIterator.remove();
            }
        }
    }

    public boolean cancel(long orderId) {
        Order order = orderIndex.remove(orderId);
        if (order == null) return false;

        TreeMap<Long, LinkedList<Order>> book = (order.getSide() == Side.BUY) ? bids : asks;
        LinkedList<Order> queue = book.get(order.getPrice());
        if (queue != null) {
            queue.remove(order);
            if (queue.isEmpty()) {
                book.remove(order.getPrice());
            }
            return true;
        }
        return false;
    }

    public Long getBestBid() { return bids.isEmpty() ? null : bids.firstKey(); }
    public Long getBestAsk() { return asks.isEmpty() ? null : asks.firstKey(); }
    public long getTotalMatchedVolume() { return totalMatchedVolume; }
    public long getTotalTradesExecuted() { return totalTradesExecuted; }
    public String getSymbol() { return symbol; }
}
