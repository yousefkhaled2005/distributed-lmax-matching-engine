package com.enterprise.matching;

import com.enterprise.matching.disruptor.RingBuffer;
import com.enterprise.matching.orderbook.*;
import com.enterprise.matching.gateway.TcpMatchingServer;
import java.util.List;

/**
 * Enterprise Ultra-Low Latency Order Matching Engine Bootstrap.
 * Designed & engineered by Yousef Khaled (2008-2014).
 * 
 * @author Yousef Khaled (Financial Systems Architecture - 2008)
 */
public class MatchingEngineApp {
    public static void main(String[] args) throws InterruptedException {
        System.out.println("==================================================================");
        System.out.println("  LMAX DISRUPTOR ELECTRONIC ORDER MATCHING ENGINE (2008-2014)     ");
        System.out.println("  Architect: Yousef Khaled <youssef460249@beng.bu.edu.eg>         ");
        System.out.println("  Standard: FIX 4.4 / Price-Time Priority L3 Limit Order Book    ");
        System.out.println("==================================================================\n");

        String symbol = "EGX30/USD";
        LimitOrderBook lob = new LimitOrderBook(symbol);
        RingBuffer<Order> ringBuffer = new RingBuffer<>(1024 * 64); // 64K slot lock-free ring

        System.out.printf("[INIT] Initialized Lock-Free Disruptor RingBuffer (%d slots)%n", ringBuffer.getBufferSize());
        System.out.printf("[INIT] Level-3 Order Book primed for symbol: %s%n%n", symbol);

        TcpMatchingServer server = new TcpMatchingServer(9800, lob);
        server.start();

        System.out.println("[BENCHMARK] Executing 50,000 order continuous crossing benchmark...");
        long startNs = System.nanoTime();

        // Populate resting bids and asks
        for (int i = 1; i <= 25000; i++) {
            long buyPrice = 10000 + (i % 50);
            Order buyOrder = new Order(i, symbol, Side.BUY, OrderType.LIMIT, buyPrice, 100, System.nanoTime());
            lob.match(buyOrder);
        }

        // Cross with aggressive asks
        for (int i = 25001; i <= 50000; i++) {
            long sellPrice = 10000 + (i % 50);
            Order sellOrder = new Order(i, symbol, Side.SELL, OrderType.LIMIT, sellPrice, 100, System.nanoTime());
            List<Trade> trades = lob.match(sellOrder);
        }

        long elapsedNs = System.nanoTime() - startNs;
        double elapsedMs = elapsedNs / 1_000_000.0;
        double ordersPerSec = (50000.0 / elapsedNs) * 1_000_000_000.0;
        double latencyPerOrderUs = (elapsedNs / 50000.0) / 1000.0;

        System.out.println("\n==================================================================");
        System.out.printf("  Total Orders Processed:     50,000 orders%n");
        System.out.printf("  Total Trades Executed:      %,d matched fills%n", lob.getTotalTradesExecuted());
        System.out.printf("  Total Volume Matched:       %,d contracts%n", lob.getTotalMatchedVolume());
        System.out.printf("  Elapsed Benchmark Time:     %.2f ms%n", elapsedMs);
        System.out.printf("  Throughput:                 %,.0f orders/sec%n", ordersPerSec);
        System.out.printf("  Average Matching Latency:   %.3f microseconds (µs)%n", latencyPerOrderUs);
        System.out.println("==================================================================\n");

        server.stop();
        System.out.println("[SHUTDOWN] Matching engine gateway stopped successfully.");
    }
}
