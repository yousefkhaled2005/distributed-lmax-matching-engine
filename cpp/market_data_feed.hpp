/**
 * @file market_data_feed.hpp
 * @brief Ultra-low latency UDP multicast market data feed handler (NASDAQ ITCH/OUCH 5.0 compatible).
 * @author Yousef Khaled (Financial Systems Architecture - 2008)
 */

#ifndef MARKET_DATA_FEED_HPP
#define MARKET_DATA_FEED_HPP

#include <cstdint>
#include <string>
#include <vector>
#include <cstring>
#include <iostream>

#pragma pack(push, 1)
struct ItchAddOrderMessage {
    char message_type;      // 'A' = Add Order
    uint16_t stock_locate;
    uint16_t tracking_number;
    uint64_t timestamp_ns;
    uint64_t order_reference_number;
    char buy_sell_indicator; // 'B' / 'S'
    uint32_t shares;
    char stock[8];
    uint32_t price;         // 4 decimal fixed point
};

struct ItchOrderExecutedMessage {
    char message_type;      // 'E' = Order Executed
    uint16_t stock_locate;
    uint16_t tracking_number;
    uint64_t timestamp_ns;
    uint64_t order_reference_number;
    uint32_t executed_shares;
    uint64_t match_number;
};
#pragma pack(pop)

namespace financial {

class MarketDataFeedHandler {
public:
    MarketDataFeedHandler(const std::string& multicast_ip, uint16_t port);
    ~MarketDataFeedHandler() = default;

    bool publish_add_order(uint64_t order_ref, char side, uint32_t shares, const std::string& symbol, uint32_t price);
    bool publish_execution(uint64_t order_ref, uint32_t shares, uint64_t match_num);
    void dump_feed_stats() const;

private:
    std::string multicast_group_;
    uint16_t multicast_port_;
    uint64_t total_packets_dispatched_ = 0;
    uint64_t sequence_number_ = 1;
};

} // namespace financial

#endif // MARKET_DATA_FEED_HPP
