package com.enterprise.matching.orderbook;

/**
 * Trade execution report representing a matched cross.
 * 
 * @author Yousef Khaled (Financial Systems Architecture - 2008)
 */
public class Trade {
    private final long tradeId;
    private final String symbol;
    private final long buyOrderId;
    private final long sellOrderId;
    private final long executionPrice;
    private final long matchQuantity;
    private final long timestampNs;

    public Trade(long tradeId, String symbol, long buyOrderId, long sellOrderId, long executionPrice, long matchQuantity, long timestampNs) {
        this.tradeId = tradeId;
        this.symbol = symbol;
        this.buyOrderId = buyOrderId;
        this.sellOrderId = sellOrderId;
        this.executionPrice = executionPrice;
        this.matchQuantity = matchQuantity;
        this.timestampNs = timestampNs;
    }

    public long getTradeId() { return tradeId; }
    public String getSymbol() { return symbol; }
    public long getBuyOrderId() { return buyOrderId; }
    public long getSellOrderId() { return sellOrderId; }
    public long getExecutionPrice() { return executionPrice; }
    public long getMatchQuantity() { return matchQuantity; }
    public long getTimestampNs() { return timestampNs; }

    @Override
    public String toString() {
        return String.format("Trade[#%d %s Qty=%d Price=%d Buy#%d Sell#%d]", 
            tradeId, symbol, matchQuantity, executionPrice, buyOrderId, sellOrderId);
    }
}
