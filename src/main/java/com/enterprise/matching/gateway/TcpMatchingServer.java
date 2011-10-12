package com.enterprise.matching.gateway;

import com.enterprise.matching.orderbook.*;
import com.enterprise.matching.fix.*;
import java.io.*;
import java.net.*;
import java.util.concurrent.*;

/**
 * High-concurrency TCP socket gateway accepting inbound FIX orders.
 * 
 * @author Yousef Khaled (Financial Systems Architecture - 2008)
 */
public class TcpMatchingServer {
    private final int port;
    private final LimitOrderBook orderBook;
    private final ExecutorService clientPool = Executors.newFixedThreadPool(16);
    private volatile boolean running = true;

    public TcpMatchingServer(int port, LimitOrderBook orderBook) {
        this.port = port;
        this.orderBook = orderBook;
    }

    public void start() {
        clientPool.submit(() -> {
            try (ServerSocket serverSocket = new ServerSocket(port)) {
                serverSocket.setReuseAddress(true);
                System.out.printf("[GATEWAY] Electronic Trading Gateway listening on TCP port %d%n", port);

                while (running) {
                    Socket clientSocket = serverSocket.accept();
                    clientSocket.setTcpNoDelay(true); // Disable Nagle's algorithm for sub-millisecond response
                    clientPool.submit(() -> handleClient(clientSocket));
                }
            } catch (IOException e) {
                if (running) e.printStackTrace();
            }
        });
    }

    private void handleClient(Socket socket) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()))) {

            String line;
            while ((line = reader.readLine()) != null) {
                FixMessage msg = FixParser.parse(line);
                if ("D".equals(msg.getMsgType())) { // New Order Single
                    long orderId = msg.getLong(11);
                    String symbol = msg.getField(55);
                    Side side = "1".equals(msg.getField(54)) ? Side.BUY : Side.SELL;
                    long price = msg.getLong(44);
                    long qty = msg.getLong(38);

                    Order order = new Order(orderId, symbol, side, OrderType.LIMIT, price, qty, System.nanoTime());
                    var trades = orderBook.match(order);

                    // Send Execution Report
                    char status = order.isFilled() ? '2' : (order.getRemainingQuantity() < qty ? '1' : '0');
                    String report = FixParser.buildExecutionReport(orderId, System.currentTimeMillis(), 
                        symbol, '0', status, price, qty, qty - order.getRemainingQuantity());
                    writer.write(report);
                    writer.newLine();
                    writer.flush();
                }
            }
        } catch (IOException ignored) {}
    }

    public void stop() {
        running = false;
        clientPool.shutdownNow();
    }
}
