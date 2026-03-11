package com.smartdelivery.model;

import java.time.Instant;
public class Order {
    public enum Status { PENDING, ASSIGNED, IN_TRANSIT, DELIVERED }
    private final String orderId;
    private final String customerId;
    private final Location destination;
    private Status status;
    private String assignedAgentId;
    private final Instant createdAt;
    private Instant deliveredAt;

    public Order(String orderId, String customerId, Location destination) {
        this.orderId    = orderId;
        this.customerId = customerId;
        this.destination = destination;
        this.status     = Status.PENDING;
        this.createdAt  = Instant.now();
    }
    public void assign(String agentId) {
        this.assignedAgentId = agentId;
        this.status = Status.ASSIGNED;
    }
    public void markDelivered() {
        this.status = Status.DELIVERED;
        this.deliveredAt = Instant.now();
    }
    public long getDeliveryTimeSeconds() {
        if (deliveredAt == null) return -1;
        return deliveredAt.getEpochSecond() - createdAt.getEpochSecond();
    }

    //getters
    public String getOrderId()       { return orderId; }
    public String getCustomerId()    { return customerId; }
    public Location getDestination() { return destination; }
    public Status getStatus()        { return status; }
    public String getAssignedAgent() { return assignedAgentId; }
}
