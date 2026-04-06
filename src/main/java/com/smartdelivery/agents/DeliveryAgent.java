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

public class DeliveryAgent extends Agent {

    private static final double SPEED = 30.0;
    private static final int STEP = 250;
    private static final double BASE_FEE = 20.0;
    private static final double RATE_PER_KM = 10.0;

    private Location pos;
    private boolean free = true;
    private String color;
    private int trust_level = 100;

    @Override
    protected void setup() {
        pos = WarehouseAgent.WAREHOUSE_LOCATION;
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
            case "ORANGE" -> new java.awt.Color(160, 32, 240);
            case "CYAN" -> new java.awt.Color(34, 211, 238);
            case "YELLOW" -> new java.awt.Color(251, 191, 36);
            case "PINK" -> new java.awt.Color(236, 72, 153);
            case "LIME" -> new java.awt.Color(132, 204, 22);
            case "SKY" -> new java.awt.Color(56, 189, 248);
            case "CORAL" -> new java.awt.Color(251, 113, 133);
            case "MINT" -> new java.awt.Color(52, 211, 153);
            default -> java.awt.Color.WHITE;
        };
    }

    private double calcBidPrice(double distKm) {
        return (BASE_FEE + distKm*RATE_PER_KM) * (100.0/trust_level);
    }

    private double calcEtaMinutes(double distKm) {
        return (distKm/SPEED) * 60.0;
    }

    private class WaitForJob extends CyclicBehaviour {
        @Override
        public void action() {
            ACLMessage cfp = myAgent.receive(MessageTemplate.MatchPerformative(ACLMessage.CFP));
            if (cfp != null) {
                if (free) {
                    String[] p = cfp.getContent().split(":");
                    Location dst = new Location(Double.parseDouble(p[2]), Double.parseDouble(p[3]), "dst");
                    double dist = WarehouseAgent.WAREHOUSE_LOCATION.distanceTo(dst);
                    double bidPrice = calcBidPrice(dist);
                    double etaMin = calcEtaMinutes(dist);

                    ACLMessage bid = cfp.createReply();
                    bid.setPerformative(ACLMessage.PROPOSE);
                    bid.setContent("BID:" + getLocalName() + ":" + bidPrice + ":" + dist + ":" + trust_level + ":" + etaMin);
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
                    trust_level = Math.min(150, trust_level+5);
                    MapRegistry.updateAgentTrust(getLocalName(), trust_level);
                    String[] p = reply.getContent().split(":");
                    String oid = p[1];
                    Location dst = new Location(Double.parseDouble(p[2]), Double.parseDouble(p[3]), p[4]);
                    double promisedEta = Double.parseDouble(p[5]);
                    long startedAt = System.currentTimeMillis();
                    MapRegistry.log("[AGENT] " + getLocalName() + " trust=" + trust_level
                            + " won " + oid + " | ETA " + String.format("%.1f", promisedEta) + "min");
                    addBehaviour(new GoDeliver(oid, dst, promisedEta, startedAt));
                } else if (reply.getPerformative() == ACLMessage.REJECT_PROPOSAL) {
                    trust_level = Math.max(10, trust_level-2);
                    MapRegistry.updateAgentTrust(getLocalName(), trust_level);
                }
            }
            if (free && cfp == null && reply == null) block();
        }
    }

    private class GoDeliver extends Behaviour {
        private final String oid;
        private final Location dst;
        private final double promisedEtaMin;
        private final long startedAt;
        private List<GeoPosition> path;
        private int idx = 0;
        private boolean fetched = false;
        private boolean over = false;

        GoDeliver(String oid, Location dst, double promisedEtaMin, long startedAt) {
            this.oid = oid;
            this.dst = dst;
            this.promisedEtaMin = promisedEtaMin;
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
                MapRegistry.log("[MOVING] " + getLocalName() + " heading out for " + oid);
                return;
            }
            if (idx < path.size()) {
                GeoPosition wp = path.get(idx++);
                pos = new Location(wp.getLatitude(), wp.getLongitude(), getLocalName());
                MapRegistry.updateAgent(getLocalName(), pos, "Delivering " + oid, myColor());
                sleep();
            } else {
                pos = dst;
                MapRegistry.updateAgent(getLocalName(), pos, "Delivered", myColor());
                MapRegistry.updateAgentRoute(getLocalName(), null);
                double elapsedMin = (System.currentTimeMillis()-startedAt)/60000.0;
                if (elapsedMin > promisedEtaMin * 1.15) {
                    trust_level = Math.max(10, trust_level-5);
                    MapRegistry.updateAgentTrust(getLocalName(), trust_level);
                    MapRegistry.log("[LATE] " + getLocalName()
                            + " took " + String.format("%.1f", elapsedMin) + "min"
                            + " promised " + String.format("%.1f", promisedEtaMin) + "min"
                            + " → trust=" + trust_level);
                }
                notifyWarehouse(oid);
                addBehaviour(new GoBack());
                over = true;
            }
        }
        @Override public boolean done() { return over; }
    }

    private class GoBack extends Behaviour {
        private final Location startPos;
        private List<GeoPosition> path;
        private int idx = 0;
        private boolean fetched = false;
        private boolean over = false;

        GoBack() {
            this.startPos = pos;
        }

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
                pos = WarehouseAgent.WAREHOUSE_LOCATION;
                free = true;
                MapRegistry.updateAgent(getLocalName(), pos, "Idle", myColor());
                MapRegistry.updateAgentRoute(getLocalName(), null);
                MapRegistry.log("[AGENT] " + getLocalName() + " back | trust=" + trust_level);
                over = true;
            }
        }
        @Override public boolean done() { return over; }
    }

    private void notifyWarehouse(String oid) {
        DFAgentDescription t = new DFAgentDescription();
        ServiceDescription s = new ServiceDescription();
        s.setType("warehouse");
        t.addServices(s);
        try {
            DFAgentDescription[] found = DFService.search(this, t);
            if (found.length > 0) {
                ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
                msg.addReceiver(found[0].getName());
                msg.setContent("DELIVERED:" + oid);
                send(msg);
            }
        } catch (Exception e) { e.printStackTrace(); }
    }

    private List<GeoPosition> straightLine(Location a, Location b, int n) {
        List<GeoPosition> pts = new ArrayList<>();
        for (int k = 1; k <= n; k++) {
            double t = (double) k/n;
            pts.add(new GeoPosition(
                    a.getLatitude() + t*(b.getLatitude()-a.getLatitude()),
                    a.getLongitude() + t*(b.getLongitude()-a.getLongitude())));
        }
        return pts;
    }

    private void sleep() {
        try { Thread.sleep(STEP); }
        catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }

    public int getTrustLevel() { return trust_level; }
    public Location getCurrentLocation() { return pos; }
    public String getAgentColor() { return color; }

    @Override
    protected void takeDown() {
        try { DFService.deregister(this); } catch (Exception e) {}
    }
}