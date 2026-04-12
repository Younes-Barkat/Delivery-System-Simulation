package com.smartdelivery.agents;

import jade.core.Agent;
import jade.core.behaviours.*;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.*;
import com.smartdelivery.model.Location;
import com.smartdelivery.gui.MapPanel;
import org.jxmapviewer.viewer.GeoPosition;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class DeliveryAgent extends Agent {

    private static final double SPEED = 30.0;
    private static final int STEP = 250;
    private static final double BASE_FEE= 20.0;
    private static final double RATE_PER_KM = 10.0;
    private static final double CIRCUITY = 1.4;
    private static final double LATE_MARGIN = 1.5;
    private static final double DISPLAY_SCALE = 20.0;
    private static final double WP_PER_KM = 35.0;
    private static final int BLOCK_PROB = 10;
    private static final int TRAFFIC_PROB = 25;
    private static final long BLOCK_EXTRA_MS = 3000;

    private Location pos;
    private boolean free        = true;
    private String color;
    private int  trust_level = 100;
    private final Random rng = new Random();

    @Override
    protected void setup() {
        pos   = WarehouseAgent.WAREHOUSE_LOCATION;
        color = getArguments() != null && getArguments().length > 0
                ? getArguments()[0].toString() : "BLUE";

        MapRegistry.updateAgent(getLocalName(), pos, "Idle", myColor());
        MapRegistry.log("[AGENT] " + getLocalName() + " ready at warehouse");

        DFAgentDescription dfd = new DFAgentDescription();
        dfd.setName(getAID());
        ServiceDescription sd = new ServiceDescription();
        sd.setType("delivery");
        sd.setName("Delivery-" + getLocalName());
        dfd.addServices(sd);
        try { DFService.register(this, dfd); }
        catch (Exception e) { e.printStackTrace(); }

        addBehaviour(new WaitForJob());
    }

    private java.awt.Color myColor() {
        return switch (color) {
            case "RED" -> new java.awt.Color(239, 68, 68);
            case "GREEN" -> new java.awt.Color(0, 200, 80);
            case "ORANGE"-> new java.awt.Color(160, 32, 240);
            case "CYAN" -> new java.awt.Color(34,  211, 238);
            case "YELLOW" -> new java.awt.Color(251, 191,36);
            case "PINK" -> new java.awt.Color(236, 72, 153);
            case "LIME" -> new java.awt.Color(132, 204,22);
            case "SKY" -> new java.awt.Color(56,  189, 248);
            case "CORAL" -> new java.awt.Color(251, 113,133);
            case "MINT"-> new java.awt.Color(52, 211,153);
            default  -> java.awt.Color.WHITE;
        };
    }

    // ETA in real milliseconds — the true budget for the trip
    private long calcEtaMs(double straightKm) {
        double waypoints = straightKm * CIRCUITY * WP_PER_KM;
        return (long)(waypoints * STEP);  // no inflation here — LATE_MARGIN is the grace
    }

    // ETA displayed to the user in sim-minutes
    private double calcEtaDisplayMin(double straightKm) {
        return calcEtaMs(straightKm) * DISPLAY_SCALE / 60000.0;
    }

    // convert real elapsed milliseconds to sim-minutes for display
    private static double msToDisplayMin(long ms) {
        return ms * DISPLAY_SCALE / 60000.0;
    }

    // higher trust = lower effective price = more competitive in auctions
    private double calcBidPrice(double straightKm) {
        return (BASE_FEE + straightKm * CIRCUITY * RATE_PER_KM) * (100.0 / trust_level);
    }

    private class WaitForJob extends CyclicBehaviour {
        @Override
        public void action() {
            ACLMessage cfp = myAgent.receive(MessageTemplate.MatchPerformative(ACLMessage.CFP));
            if (cfp != null) {
                if (free) {
                    String[] p   = cfp.getContent().split(":");
                    Location dst = new Location(Double.parseDouble(p[2]), Double.parseDouble(p[3]), "dst");
                    // dist = agent's current position → warehouse + warehouse → order
                    // (agent is idle at warehouse so pos ≈ warehouse, but this handles
                    //  future cases where agents could bid while not fully back)
                    double distToWarehouse = pos.distanceTo(WarehouseAgent.WAREHOUSE_LOCATION);
                    double distToOrder     = WarehouseAgent.WAREHOUSE_LOCATION.distanceTo(dst);
                    double dist            = distToWarehouse + distToOrder;
                    double bidPrice        = calcBidPrice(dist);
                    double etaDispMin      = calcEtaDisplayMin(dist);

                    ACLMessage bid = cfp.createReply();
                    bid.setPerformative(ACLMessage.PROPOSE);
                    bid.setContent("BID:" + getLocalName() + ":" + bidPrice
                            + ":" + dist + ":" + trust_level + ":" + etaDispMin);
                    bid.setConversationId(cfp.getConversationId());
                    send(bid);
                } else {
                    ACLMessage no = cfp.createReply();
                    no.setPerformative(ACLMessage.REFUSE);
                    no.setContent("BUSY");
                    send(no);
                }
            }

            MessageTemplate mt = MessageTemplate.or(
                    MessageTemplate.MatchPerformative(ACLMessage.ACCEPT_PROPOSAL),
                    MessageTemplate.MatchPerformative(ACLMessage.REJECT_PROPOSAL));
            ACLMessage reply = myAgent.receive(mt);
            if (reply != null) {
                if (reply.getPerformative() == ACLMessage.ACCEPT_PROPOSAL && free) {
                    free = false;
                    trust_level = Math.min(150, trust_level + 5);
                    MapRegistry.updateAgentTrust(getLocalName(), trust_level);
                    String[] p    = reply.getContent().split(":");
                    String   oid      = p[1];
                    Location dst      = new Location(Double.parseDouble(p[2]), Double.parseDouble(p[3]), p[4]);
                    double   dispMin  = Double.parseDouble(p[5]);
                    double   dist     = pos.distanceTo(WarehouseAgent.WAREHOUSE_LOCATION)
                            + WarehouseAgent.WAREHOUSE_LOCATION.distanceTo(dst);
                    long     etaMs    = calcEtaMs(dist);
                    long     started  = System.currentTimeMillis();
                    MapRegistry.log("[AGENT] " + getLocalName() + " trust=" + trust_level
                            + " won " + oid + " | ETA " + String.format("%.1f", dispMin) + " min");
                    addBehaviour(new GoDeliver(oid, dst, dispMin, etaMs, started));
                } else if (reply.getPerformative() == ACLMessage.REJECT_PROPOSAL) {
                    trust_level = Math.max(10, trust_level - 2);
                    MapRegistry.updateAgentTrust(getLocalName(), trust_level);
                }
            }
            if (free && cfp == null && reply == null) block();
        }
    }

    private class GoDeliver extends Behaviour {
        private final String oid;
        private final Location dst;
        private final double   dispMin;
        private final long  etaMs;
        private long  startedAt;
        private List<GeoPosition> path;
        private int  idx  = 0;
        private boolean fetched  = false;
        private boolean over  = false;
        private int  blockAt  = -1;
        private int  slowFrom = -1;
        private int  slowLen  = 0;

        GoDeliver(String oid, Location dst, double dispMin, long etaMs, long startedAt) {
            this.oid = oid;
            this.dst  = dst;
            this.dispMin= dispMin;
            this.etaMs= etaMs;
            this.startedAt = startedAt;
        }

        @Override
        public void action() {
            if (!fetched) {
                fetched = true;
                MapRegistry.updateAgent(getLocalName(), pos, "Routing...", myColor());
                path = MapPanel.fetchRoute(WarehouseAgent.WAREHOUSE_LOCATION, dst);
                if (path == null || path.size() < 2)
                    path = straightLine(WarehouseAgent.WAREHOUSE_LOCATION, dst, 30);
                MapRegistry.updateAgentRoute(getLocalName(), path);

                // FIX 1: Reset the clock AFTER the HTTP request to ignore API latency!
                this.startedAt = System.currentTimeMillis();

                MapRegistry.log("[MOVING] " + getLocalName() + " → " + oid
                        + " | ETA " + String.format("%.1f", dispMin) + " min");

                // roll obstacle dice once for the whole trip
                int n    = path.size();
                int roll = rng.nextInt(100);
                if (roll < BLOCK_PROB) {
                    blockAt = n/3 + rng.nextInt(Math.max(1, n/3));
                    MapRegistry.log("[OBSTACLE] " + getLocalName() + " road block ahead on " + oid);
                } else if (roll < BLOCK_PROB + TRAFFIC_PROB) {
                    slowFrom = n/3 + rng.nextInt(Math.max(1, n/3));
                    slowLen  = 10;
                    MapRegistry.log("[OBSTACLE] " + getLocalName() + " traffic jam ahead on " + oid);
                }
                return;
            }

            if (idx < path.size()) {
                GeoPosition wp = path.get(idx++);
                pos = new Location(wp.getLatitude(), wp.getLongitude(), getLocalName());

                if (blockAt >= 0 && idx - 1 == blockAt) {
                    MapRegistry.updateAgent(getLocalName(), pos, "Blocked! " + oid, myColor());
                    sleepMs(BLOCK_EXTRA_MS);
                    blockAt = -1;
                } else if (slowFrom >= 0 && idx - 1 >= slowFrom && idx - 1 < slowFrom + slowLen) {
                    MapRegistry.updateAgent(getLocalName(), pos, "Traffic " + oid, myColor());
                    sleepMs(STEP * 3);
                } else {
                    MapRegistry.updateAgent(getLocalName(), pos, "Delivering " + oid, myColor());
                    sleep();
                }
            } else {
                // arrived
                pos = dst;
                long elapsedMs = System.currentTimeMillis() - startedAt;
                long idealMs = (long) path.size() * STEP;
                long deadlineMs = idealMs + 1500;

                boolean onTime = elapsedMs <=deadlineMs;
                double elapsedDisp = msToDisplayMin(elapsedMs);

                MapRegistry.updateAgent(getLocalName(), pos, "Delivered", myColor());
                MapRegistry.updateAgentRoute(getLocalName(), null);

                if (!onTime) {
                    trust_level = Math.max(10, trust_level - 5);
                    MapRegistry.updateAgentTrust(getLocalName(), trust_level);
                    MapRegistry.log("[LATE] " + getLocalName()
                            + " took " + String.format("%.1f", elapsedDisp) + " min"
                            + " | promised " + String.format("%.1f", dispMin) + " min"
                            + " | trust=" + trust_level);
                } else {
                    MapRegistry.log("[ON TIME] " + getLocalName()
                            + " " + String.format("%.1f", elapsedDisp) + "/"
                            + String.format("%.1f", dispMin) + " min");
                }

                notifyWarehouse(oid, onTime, elapsedDisp);
                addBehaviour(new GoBack());
                over = true;
            }
        }
        @Override public boolean done(){ return over; }
    }

    private class GoBack extends Behaviour{
        private final Location startPos; // snapshot — safe from shared-field mutation
        private List<GeoPosition> path;
        private int idx = 0;
        private boolean fetched = false;
        private boolean over = false;

        GoBack() { this.startPos = pos; }

        @Override
        public void action() {
            if (!fetched) {
                fetched = true;
                MapRegistry.updateAgent(getLocalName(), pos, "Returning...", myColor());
                path = MapPanel.fetchRoute(startPos, WarehouseAgent.WAREHOUSE_LOCATION);
                if (path == null || path.size() < 2)
                    path = straightLine(startPos, WarehouseAgent.WAREHOUSE_LOCATION, 20);
                MapRegistry.updateAgentRoute(getLocalName(), path);
                return;
            }
            if (idx < path.size()) {
                GeoPosition wp = path.get(idx++);
                pos = new Location(wp.getLatitude(), wp.getLongitude(), getLocalName());
                MapRegistry.updateAgent(getLocalName(), pos, "Returning...", myColor());
                sleep();
            } else {
                pos  = WarehouseAgent.WAREHOUSE_LOCATION;
                free = true;
                MapRegistry.updateAgent(getLocalName(), pos, "Idle", myColor());
                MapRegistry.updateAgentRoute(getLocalName(), null);
                MapRegistry.log("[AGENT] " + getLocalName() + " back | trust=" + trust_level);
                over = true;
            }
        }
        @Override public boolean done() { return over; }
    }

    private void notifyWarehouse(String oid, boolean onTime, double elapsedMin) {
        DFAgentDescription t = new DFAgentDescription();
        ServiceDescription s = new ServiceDescription();
        s.setType("warehouse");
        t.addServices(s);
        try {
            DFAgentDescription[] found = DFService.search(this, t);
            if (found.length > 0) {
                ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
                msg.addReceiver(found[0].getName());
                msg.setContent("DELIVERED:" + oid + ":" + onTime
                        + ":" + String.format("%.2f", elapsedMin));
                send(msg);
            }
        } catch (Exception e) { e.printStackTrace(); }
    }

    private List<GeoPosition> straightLine(Location a, Location b, int n) {
        List<GeoPosition> pts = new ArrayList<>();
        for (int k = 1; k <= n; k++) {
            double t = (double) k / n;
            pts.add(new GeoPosition(
                    a.getLatitude()  + t * (b.getLatitude()  - a.getLatitude()),
                    a.getLongitude() + t * (b.getLongitude() - a.getLongitude())));
        }
        return pts;
    }

    private void sleep() {
        try { Thread.sleep(STEP); }
        catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }

    private void sleepMs(long ms) {
        try { Thread.sleep(ms); }
        catch (InterruptedException e){ Thread.currentThread().interrupt(); }
    }

    public int getTrustLevel() { return trust_level; }
    public Location getCurrentLocation() { return pos; }
    public String   getAgentColor() { return color; }

    @Override
    protected void takeDown() {
        try { DFService.deregister(this); } catch (Exception e) {}
    }
}