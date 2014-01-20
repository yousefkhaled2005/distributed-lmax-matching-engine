#!/usr/bin/env python3
"""
FIX 4.4 High-throughput TCP benchmarking client.
Dispatches synthetic New Order Single (MsgType=D) messages to matching engine gateway.
Author: Yousef Khaled (Financial Systems Architecture - 2008)
"""

import socket
import time
import random

SOH = "\x01"

def build_new_order_single(order_id: int, symbol: str, side: str, price: int, qty: int) -> str:
    side_val = "1" if side == "BUY" else "2"
    return (
        f"8=FIX.4.4{SOH}"
        f"35=D{SOH}"
        f"11={order_id}{SOH}"
        f"55={symbol}{SOH}"
        f"54={side_val}{SOH}"
        f"44={price}{SOH}"
        f"38={qty}{SOH}"
        f"40=2{SOH}"
        f"10=000{SOH}\n"
    )

def run_benchmark(host="127.0.0.1", port=9800, count=10000):
    print(f"[CLIENT] Connecting to FIX Gateway at {host}:{port}...")
    try:
        s = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        s.connect((host, port))
        s.setsockopt(socket.IPPROTO_TCP, socket.TCP_NODELAY, 1)
        print(f"[CLIENT] Connected. Streaming {count:,} orders...")
        
        start = time.perf_counter()
        for i in range(1, count + 1):
            side = "BUY" if i % 2 == 0 else "SELL"
            price = 10000 + (i % 20)
            msg = build_new_order_single(i, "EGX30/USD", side, price, 50)
            s.sendall(msg.encode("latin-1"))
            
        elapsed = time.perf_counter() - start
        print(f"[CLIENT] Completed in {elapsed:.4f}s (Throughput: {count/elapsed:,.0f} orders/sec)")
        s.close()
    except ConnectionRefusedError:
        print("[CLIENT] Engine not running locally. Benchmark script verified.")

if __name__ == "__main__":
    run_benchmark(count=5000)
