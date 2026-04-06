package com.smartdelivery.agents;

import com.smartdelivery.gui.MapPanel;
import com.smartdelivery.gui.SimulationWindow;
import com.smartdelivery.model.Location;
import com.smartdelivery.model.Order;
import org.jxmapviewer.viewer.GeoPosition;
import java.awt.Color;
import java.util.List;

public class MapRegistry {
    private static MapPanel map;
    private static SimulationWindow win;

    public static void setMapPanel(MapPanel m) { map = m; }
    public static void setWindow(SimulationWindow w) { win = w; }

    public static void updateAgent(String id, Location loc, String status, Color col) {
        if (map != null) map.updateAgentPosition(id, loc, status, col);
    }
    public static void updateAgentRoute(String id, List<GeoPosition> route) {
        if (map != null) map.updateAgentRoute(id, route);
    }
    public static void addDeliveryPin(String oid, Location loc) {
        if (map != null) map.addDeliveryPin(oid, loc);
    }
    public static void removeDeliveryPin(String oid) {
        if (map != null) map.removeDeliveryPin(oid);
    }
    public static void updateDeliveryPinColor(String oid, Color col) {
        if (map != null) map.updateDeliveryPinColor(oid, col);
    }
    public static void addOrder(Order o) {
        if (win != null) win.addOrder(o);
    }
    public static void updateOrderStatus(Order o) {
        if (win != null) win.updateOrderStatus(o);
    }
    public static void setOrderPrice(String orderId, double price) {
        if (win != null) win.setOrderPrice(orderId, price);
    }
    public static void updateAgentTrust(String agentName, double trust) {
        if (win != null) win.updateAgentTrust(agentName, trust);
    }
    public static void log(String msg) {
        if (win != null) win.log(msg);
    }
    public static void incrementDelivered() {
        if (win != null) win.incrementDelivered();
    }
    public static void recordDelivery(Order o) {
        if (win != null) win.recordDelivery(o);
    }
}