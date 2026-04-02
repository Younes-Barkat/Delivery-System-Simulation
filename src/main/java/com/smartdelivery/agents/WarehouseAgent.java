package com.smartdelivery.agents;

import jade.core.Agent;
import jade.core.behaviours.*;
import java.util.*;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.*;
import com.smartdelivery.model.*;

public class WarehouseAgent extends Agent {

    public static final Location WAREHOUSE_LOCATION = new Location(35.7069, 4.5428, "Central Warehouse - M'sila");

    private static final double LAT_MIN = 35.685;
    private static final double LAT_MAX = 35.740;
    private static final double LON_MIN = 4.505;
    private static final double LON_MAX = 4.580;
    private static final int SPAWN_DELAY = 6000;
    private static final int TICK = 2500;
    private final Queue<Order>  pending =new LinkedList<>();
    private final Map<String, Order> allOrders = new HashMap<>();
    private final Random  rng = new Random();
    private int maxOrders = 6;
    private int live =0;
    private int totalDone = 0;
    private int seq= 1;

    @Override
    protected void setup() {
        Object[] args = getArguments();
        if(args != null && args.length >0){
            try { maxOrders = Integer.parseInt(args[0].toString());}
            catch (NumberFormatException ignored) {}
        }
        System.out.println("[WAREHOUSE] online | maxOrders=" + maxOrders);
        DFAgentDescription dfd = new DFAgentDescription();
        dfd.setName(getAID());
        ServiceDescription sd = new ServiceDescription();
        sd.setType("warehouse");
        sd.setName("SmartDelivery-Warehouse");
        dfd.addServices(sd);
        try { DFService.register(this, dfd);
        }
        catch(Exception e) {
            e.printStackTrace();
        }
        addBehaviour(new ReceiveDeliveryConfirmation());
        addBehaviour(new TickerBehaviour(this,TICK) {
            @Override protected void onTick(){
                if (!pending.isEmpty()) dispatchNextOrder();
            }
        });

        addBehaviour(new WakerBehaviour(this, 3000) {
            @Override
            protected void onWake() {
                for(int i=0;i< maxOrders;i++)
                    spawnOrder();
            }
        });

        addBehaviour(new TickerBehaviour(this,1500){
            @Override protected void onTick() {
                while (live < maxOrders)
                    spawnOrder();
            }
        });
    }

    private void spawnOrder() {
        if(live>=maxOrders)
            return;
        double lat = LAT_MIN + rng.nextDouble() * (LAT_MAX -LAT_MIN);
        double lon = LON_MIN + rng.nextDouble() * (LON_MAX -LON_MIN);
        String coords = String.format("%.4f, %.4f",lat,lon);
        Location dest = new Location(lat, lon,coords);
        String id = "ORD-" + (seq++);
        Order order= new Order(id, coords, dest);
        pending.offer(order);
        allOrders.put(id, order);
        live++;
        MapRegistry.addDeliveryPin(id, dest);
        MapRegistry.addOrder(order);
        MapRegistry.log("[NEW ORDER] " + id + " → (" + coords + ")");
        System.out.println("[WAREHOUSE] spawned " + id + " live=" + live + "/"+ maxOrders);
    }

    private void dispatchNextOrder() {
        Order order = pending.poll();
        if (order == null) return;
        DFAgentDescription template = new DFAgentDescription();
        ServiceDescription sd = new ServiceDescription();
        sd.setType("delivery");
        template.addServices(sd);
        try{
            DFAgentDescription[] agents =DFService.search(this,template);
            if (agents.length == 0){
                pending.offer(order);
                return;
            }

            ACLMessage cfp = new ACLMessage(ACLMessage.CFP);
            for (DFAgentDescription a : agents) cfp.addReceiver(a.getName());
            cfp.setContent("JOB:" + order.getOrderId() +":"+order.getDestination().getLatitude() +":"+ order.getDestination().getLongitude());
            cfp.setConversationId("job-" + order.getOrderId());
            send(cfp);
            addBehaviour(new CollectBids(order,agents.length));
        } catch (Exception e) { e.printStackTrace(); }
    }

    private class CollectBids extends Behaviour {
        private final Order order;
        private final int expected;
        private int  received = 0;
        private double best= Double.MAX_VALUE;
        private jade.core.AID winner= null;
        private final List<jade.core.AID> losers = new ArrayList<>();

        CollectBids(Order order, int expected) {
            this.order =order;
            this.expected = expected;
        }

        @Override
        public void action() {
            MessageTemplate mt = MessageTemplate.and(
                    MessageTemplate.or(MessageTemplate.MatchPerformative(ACLMessage.PROPOSE), MessageTemplate.MatchPerformative(ACLMessage.REFUSE)), MessageTemplate.MatchConversationId("job-" + order.getOrderId()));
            ACLMessage msg=myAgent.receive(mt);
            if(msg !=null){
                received++;
                if (msg.getPerformative()==ACLMessage.PROPOSE){
                    double t = Double.parseDouble(msg.getContent().split(":")[2]);
                    if(t < best) {
                        if (winner != null) losers.add(winner);
                        best   = t;
                        winner = msg.getSender();
                    } else {
                        losers.add(msg.getSender());
                    }
                }
            } else {
                block(400);
            }
        }

        @Override public boolean done() { return received >= expected; }

        @Override
        public int onEnd() {
            if(winner !=null){
                ACLMessage accept = new ACLMessage(ACLMessage.ACCEPT_PROPOSAL);
                accept.addReceiver(winner);
                accept.setContent("ASSIGNED:"+order.getOrderId() +":"+order.getDestination().getLatitude() +":"+order.getDestination().getLongitude() +":"+ order.getCustomerId());
                myAgent.send(accept);
                order.assign(winner.getLocalName());
                MapRegistry.updateOrderStatus(order);
                MapRegistry.updateDeliveryPinColor(order.getOrderId(), agentColor(winner.getLocalName()));
                MapRegistry.log("[ASSIGNED] "+order.getOrderId() +" → "+ winner.getLocalName() +" | waited " +order.getWaitTimeSeconds() +"s");
                for (jade.core.AID loser : losers) {
                    ACLMessage rej = new ACLMessage(ACLMessage.REJECT_PROPOSAL);
                    rej.addReceiver(loser);
                    rej.setContent("REJECTED:"+order.getOrderId());
                    myAgent.send(rej);
                }
            } else{
                pending.offer(order);
            }
            return 0;
        }
    }

    private class ReceiveDeliveryConfirmation extends CyclicBehaviour {
        @Override
        public void action(){
            MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.INFORM);
            ACLMessage msg = myAgent.receive(mt);
            if(msg !=null && msg.getContent().startsWith("DELIVERED:")){
                String id = msg.getContent().split(":")[1];
                Order  order = allOrders.get(id);
                if(order !=null) {
                    order.markDelivered();
                    totalDone++;
                    live--;
                    MapRegistry.removeDeliveryPin(id);
                    MapRegistry.updateOrderStatus(order);
                    MapRegistry.recordDelivery(order);
                    MapRegistry.log("[DELIVERED] "+id +" | time: " + order.getTotalTimeSeconds()+"s");
                    System.out.println("[WAREHOUSE] delivered " +id+" live="+live+"/"+maxOrders);
                }
            } else {
                block();
            }
        }
    }

    private java.awt.Color agentColor(String name){
        return switch (name){
            case "Delivery-1"-> new java.awt.Color(239, 68,  68);
            case "Delivery-2"-> new java.awt.Color(0,200, 80);
            case "Delivery-3"-> new java.awt.Color(160,32,240);
            case "Delivery-4"-> new java.awt.Color(34,211, 238);
            case "Delivery-5"-> new java.awt.Color(249,115,22);
            case "Delivery-6"-> new java.awt.Color(236, 72,153);
            case "Delivery-7" -> new java.awt.Color(132,204,22);
            case "Delivery-8"-> new java.awt.Color(56, 189, 248);
            case "Delivery-9"-> new java.awt.Color(251, 113,133);
            case "Delivery-10"-> new java.awt.Color(52,211, 153);
            default-> new java.awt.Color(148,163, 184);
        };
    }

    @Override
    protected void takeDown() {
        try { DFService.deregister(this); } catch (Exception e) {}
        System.out.println("[WAREHOUSE] shutdown. delivered=" + totalDone);
    }
}