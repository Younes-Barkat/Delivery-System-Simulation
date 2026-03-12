package com.smartdelivery.agents;

import jade.core.Agent;
import jade.core.behaviours.*;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.*;
import com.smartdelivery.model.*;
import java.util.*;

public class WarehouseAgent extends Agent {

    public static final Location WAREHOUSE_LOCATION =
            new Location(35.7069, 4.5428, "Central Warehouse-M'sila");

    private final Queue<Order> pendingOrders = new LinkedList<>();
    private final Map<String, Order> allOrders = new HashMap<>();
    private int deliveredCount = 0;

    @Override
    protected void setup() {
        System.out.println("[WAREHOUSE] Agent started: " + getLocalName());
        DFAgentDescription dfd = new DFAgentDescription();
        dfd.setName(getAID());
        ServiceDescription sd = new ServiceDescription();
        sd.setType("warehouse");
        sd.setName("SmartDelivery-Warehouse");
        dfd.addServices(sd);
        try { DFService.register(this, dfd); }
        catch (Exception e) { e.printStackTrace(); }
        addBehaviour(new ReceiveOrderBehaviour());
        addBehaviour(new TickerBehaviour(this, 3000) {
            @Override
            protected void onTick() {
                if (!pendingOrders.isEmpty()) {
                    dispatchNextOrder();
                }
            }
        });
        addBehaviour(new ReceiveDeliveryConfirmation());
    }

    private class ReceiveOrderBehaviour extends CyclicBehaviour {
        @Override
        public void action() {
            MessageTemplate mt = MessageTemplate.MatchPerformative(
                    ACLMessage.REQUEST);
            ACLMessage msg = myAgent.receive(mt);
            if (msg != null) {
                String content = msg.getContent();
                if (content.startsWith("ORDER:")) {
                    String[] parts = content.split(":");
                    String customerId = parts[1];
                    double lat = Double.parseDouble(parts[2]);
                    double lon = Double.parseDouble(parts[3]);
                    Location dest = new Location(lat, lon, "Customer-" + customerId);
                    String orderId = "ORD-" + System.currentTimeMillis();
                    Order order = new Order(orderId, customerId, dest);
                    pendingOrders.offer(order);
                    allOrders.put(orderId, order);
                    System.out.println("[WAREHOUSE] New order: " + orderId +
                            " for " + customerId);
                }
            } else { block(); }
        }
    }

    private void dispatchNextOrder() {
        Order order = pendingOrders.poll();
        if (order == null) return;

        DFAgentDescription template = new DFAgentDescription();
        ServiceDescription sd = new ServiceDescription();
        sd.setType("delivery");
        template.addServices(sd);
        try {
            DFAgentDescription[] results = DFService.search(this, template);
            if (results.length == 0) {
                System.out.println("[WAREHOUSE] No delivery agents available!");
                pendingOrders.offer(order); //requeue
                return;
            }

            ACLMessage cfp = new ACLMessage(ACLMessage.CFP);
            for (DFAgentDescription r : results) {
                cfp.addReceiver(r.getName());
            }
            cfp.setContent("JOB:" + order.getOrderId() + ":" +
                    order.getDestination().getLatitude() + ":" +
                    order.getDestination().getLongitude());
            cfp.setConversationId("job-" + order.getOrderId());
            send(cfp);
            System.out.println("[WAREHOUSE] CFP sent to " +
                    results.length + " agents for " + order.getOrderId());

            addBehaviour(new CollectProposalsBehaviour(order, results.length));
        } catch (Exception e) { e.printStackTrace(); }
    }

    private class CollectProposalsBehaviour extends Behaviour {
        private final Order order;
        private final int expectedReplies;
        private int repliesReceived = 0;
        private double bestTime = Double.MAX_VALUE;
        private jade.core.AID bestAgent = null;
        private final List<jade.core.AID> otherAgents = new ArrayList<>();

        CollectProposalsBehaviour(Order order, int expected) {
            this.order = order;
            this.expectedReplies = expected;
        }

        @Override
        public void action() {
            MessageTemplate mt = MessageTemplate.and(
                    MessageTemplate.MatchPerformative(ACLMessage.PROPOSE),
                    MessageTemplate.MatchConversationId("job-" + order.getOrderId()));
            ACLMessage msg = myAgent.receive(mt);
            if (msg != null) {
                repliesReceived++;
                String[] parts = msg.getContent().split(":");
                double time = Double.parseDouble(parts[2]);
                if (time < bestTime) {
                    if (bestAgent != null) otherAgents.add(bestAgent);
                    bestTime = time;
                    bestAgent = msg.getSender();
                } else {
                    otherAgents.add(msg.getSender());
                }
            } else { block(500); }
        }

        @Override
        public boolean done() { return repliesReceived >= expectedReplies; }

        @Override
        public int onEnd() {
            if (bestAgent != null) {
                ACLMessage accept = new ACLMessage(ACLMessage.ACCEPT_PROPOSAL);
                accept.addReceiver(bestAgent);
                accept.setContent("ASSIGNED:" + order.getOrderId() + ":" +
                        order.getDestination().getLatitude() + ":" +
                        order.getDestination().getLongitude() + ":" +
                        order.getCustomerId());
                myAgent.send(accept);
                order.assign(bestAgent.getLocalName());
                System.out.println("[WAREHOUSE] Assigned " + order.getOrderId() +
                        " to " + bestAgent.getLocalName());
                //reject
                for (jade.core.AID other : otherAgents) {
                    ACLMessage reject = new ACLMessage(ACLMessage.REJECT_PROPOSAL);
                    reject.addReceiver(other);
                    reject.setContent("REJECTED:" + order.getOrderId());
                    myAgent.send(reject);
                }
            }
            return 0;
        }
    }

    private class ReceiveDeliveryConfirmation extends CyclicBehaviour {
        @Override
        public void action() {
            MessageTemplate mt = MessageTemplate.and(
                    MessageTemplate.MatchPerformative(ACLMessage.INFORM),
                    MessageTemplate.MatchContent("DELIVERED:"));
            MessageTemplate mt2 = MessageTemplate.MatchPerformative(ACLMessage.INFORM);
            ACLMessage msg = myAgent.receive(mt2);
            if (msg != null && msg.getContent().startsWith("DELIVERED:")) {
                String[] p = msg.getContent().split(":");
                String orderId = p[1];
                Order order = allOrders.get(orderId);
                if (order != null) {
                    order.markDelivered();
                    deliveredCount++;
                    System.out.println("[WAREHOUSE] Delivery confirmed: " + orderId +
                            " | Total delivered: " + deliveredCount);
                }
            } else { block(); }
        }
    }

    @Override
    protected void takeDown() {
        try { DFService.deregister(this); } catch (Exception e) {}
        System.out.println("[WAREHOUSE] Agent terminated. Total deliveries: " +
                deliveredCount);
    }
}
