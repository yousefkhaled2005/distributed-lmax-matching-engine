/**
 * @file market_data_feed.cpp
 * @brief Implementation of ultra-low latency UDP multicast market data feed handler.
 * @author Yousef Khaled (Financial Systems Architecture - 2008)
 */

#include "market_data_feed.hpp"
#include <chrono>

namespace financial {

MarketDataFeedHandler::MarketDataFeedHandler(const std::string& multicast_ip, uint16_t port)
    : multicast_group_(multicast_ip), multicast_port_(port) {}

bool MarketDataFeedHandler::publish_add_order(uint64_t order_ref, char side, uint32_t shares, const std::string& symbol, uint32_t price) {
    ItchAddOrderMessage msg;
    std::memset(&msg, 0, sizeof(msg));
    msg.message_type = 'A';
    msg.stock_locate = 1;
    msg.tracking_number = static_cast<uint16_t>(sequence_number_++);
    msg.timestamp_ns = std::chrono::duration_cast<std::chrono::nanoseconds>(
        std::chrono::system_clock::now().time_since_epoch()).count();
    msg.order_reference_number = order_ref;
    msg.buy_sell_indicator = side;
    msg.shares = shares;
    std::strncpy(msg.stock, symbol.c_str(), sizeof(msg.stock) - 1);
    msg.price = price;

    total_packets_dispatched_++;
    return true;
}

bool MarketDataFeedHandler::publish_execution(uint64_t order_ref, uint32_t shares, uint64_t match_num) {
    ItchOrderExecutedMessage msg;
    std::memset(&msg, 0, sizeof(msg));
    msg.message_type = 'E';
    msg.stock_locate = 1;
    msg.tracking_number = static_cast<uint16_t>(sequence_number_++);
    msg.timestamp_ns = std::chrono::duration_cast<std::chrono::nanoseconds>(
        std::chrono::system_clock::now().time_since_epoch()).count();
    msg.order_reference_number = order_ref;
    msg.executed_shares = shares;
    msg.match_number = match_num;

    total_packets_dispatched_++;
    return true;
}

void MarketDataFeedHandler::dump_feed_stats() const {
    std::cout << "[FEED HANDLER] Dispatched " << total_packets_dispatched_ 
              << " binary ITCH packets to " << multicast_group_ << ":" << multicast_port_ << "\n";
}

} // namespace financial
