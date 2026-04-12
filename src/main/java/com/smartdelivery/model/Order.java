package com.smartdelivery.model;

import java.time.Instant;

public class Order{
    public enum Status{PENDING,ASSIGNED,IN_TRANSIT,DELIVERED}
    private final String id;
    private final String zone;      // neighborhood name used as customer label
    private final Location dest;
    private Status status;
    private String agent;
    private final Instant createdAt;
    private Instant assignedAt;
    private Instant  deliveredAt;

    public Order(String id, String zone, Location dest) {
        this.id= id;
        this.zone= zone;
        this.dest= dest;
        this.status= Status.PENDING;
        this.createdAt= Instant.now();
    }

    public void assign(String agentId){
        this.agent= agentId;
        this.status= Status.ASSIGNED;
        this.assignedAt= Instant.now();
    }

    public void markInTransit(){
        this.status=Status.IN_TRANSIT;
    }
    private boolean onTime      = true;
    private double  actualMin   = -1;

    public void markDelivered(boolean onTime, double actualMin) {
        this.status     = Status.DELIVERED;
        this.deliveredAt = Instant.now();
        this.onTime     = onTime;
        this.actualMin  = actualMin;
    }

    // keep the old no-arg version so nothing else breaks
    public void markDelivered() {
        markDelivered(true, -1);
    }

    public boolean isOnTime()    { return onTime; }
    public double  getActualMin(){ return actualMin; }
    public long getWaitTimeSeconds() {
        return assignedAt==null ?-1:assignedAt.getEpochSecond()-createdAt.getEpochSecond();
    }
    public long getTotalTimeSeconds(){
        return deliveredAt==null ?-1:deliveredAt.getEpochSecond()-createdAt.getEpochSecond();
    }
    public long getAgeSeconds(){
        return Instant.now().getEpochSecond()-createdAt.getEpochSecond();
    }
    public String getOrderId(){
        return id;
    }
    public String getCustomerId(){
        return zone;
    }
    public Location getDestination() {
        return dest;
    }
    public Status getStatus(){
        return status;
    }
    public String getAssignedAgent() {
        return agent;
    }
    public Instant getCreatedAt() {
        return createdAt;
    }
}