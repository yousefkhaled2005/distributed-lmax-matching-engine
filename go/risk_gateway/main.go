package main

import (
	"encoding/json"
	"fmt"
	"log"
	"net/http"
	"sync"
	"sync/atomic"
	"time"
)

// Pre-trade risk controller engineered by Yousef Khaled (2008-2014)

type RiskRule struct {
	MaxOrderNotional   float64 `json:"max_order_notional"`
	MaxAccountPosition float64 `json:"max_account_position"`
	MaxOrdersPerSecond int64   `json:"max_orders_per_second"`
}

type OrderValidationRequest struct {
	AccountID string  `json:"account_id"`
	Symbol    string  `json:"symbol"`
	Side      string  `json:"side"`
	Quantity  int64   `json:"quantity"`
	Price     float64 `json:"price"`
}

type OrderValidationResponse struct {
	Approved     bool   `json:"approved"`
	RejectReason string `json:"reject_reason,omitempty"`
	LatencyNs    int64  `json:"latency_ns"`
}

type RiskGateway struct {
	mu           sync.RWMutex
	rules        RiskRule
	positions    map[string]float64
	orderCounts  map[string]*int64
	totalChecked uint64
	totalPasses  uint64
	totalRejects uint64
}

func NewRiskGateway() *RiskGateway {
	return &RiskGateway{
		rules: RiskRule{
			MaxOrderNotional:   500000.0, // $500,000 per single order
			MaxAccountPosition: 2000000.0, // $2,000,000 gross limit
			MaxOrdersPerSecond: 1000,
		},
		positions:   make(map[string]float64),
		orderCounts: make(map[string]*int64),
	}
}

func (rg *RiskGateway) ValidateOrder(req OrderValidationRequest) (bool, string) {
	atomic.AddUint64(&rg.totalChecked, 1)

	// Check 1: Max Notional
	notional := float64(req.Quantity) * req.Price
	if notional > rg.rules.MaxOrderNotional {
		atomic.AddUint64(&rg.totalRejects, 1)
		return false, fmt.Sprintf("Order notional $%.2f exceeds limit $%.2f", notional, rg.rules.MaxOrderNotional)
	}

	// Check 2: Account Position
	rg.mu.Lock()
	defer rg.mu.Unlock()

	currentPos := rg.positions[req.AccountID]
	if req.Side == "BUY" {
		currentPos += notional
	} else {
		currentPos -= notional
	}

	if currentPos > rg.rules.MaxAccountPosition || currentPos < -rg.rules.MaxAccountPosition {
		atomic.AddUint64(&rg.totalRejects, 1)
		return false, "Gross exposure limit exceeded for account"
	}

	rg.positions[req.AccountID] = currentPos
	atomic.AddUint64(&rg.totalPasses, 1)
	return true, ""
}

func main() {
	gateway := NewRiskGateway()

	http.HandleFunc("/api/v1/risk/validate", func(w http.ResponseWriter, r *http.Request) {
		start := time.Now()
		var req OrderValidationRequest
		if err := json.NewDecoder(r.Body).Decode(&req); err != nil {
			http.Error(w, err.Error(), http.StatusBadRequest)
			return
		}

		approved, reason := gateway.ValidateOrder(req)
		resp := OrderValidationResponse{
			Approved:     approved,
			RejectReason: reason,
			LatencyNs:    time.Since(start).Nanoseconds(),
		}

		w.Header().Set("Content-Type", "application/json")
		json.NewEncoder(w).Encode(resp)
	})

	fmt.Println("==================================================================")
	fmt.Println("  PRE-TRADE RISK CONTROLLER & ORDER GATEWAY IN GO (2008-2014)     ")
	fmt.Println("  Lead Architect: Yousef Khaled <youssef460249@beng.bu.edu.eg>    ")
	fmt.Println("  HTTP Risk Filter listening on :9801                            ")
	fmt.Println("==================================================================")
	log.Fatal(http.ListenAndServe(":9801", nil))
}
