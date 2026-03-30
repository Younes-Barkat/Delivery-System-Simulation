package com.smartdelivery.agents;

import jade.core.Agent;
import jade.core.behaviours.*;
import java.util.*;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.*;
import com.smartdelivery.model.*;

public class WarehouseAgent extends Agent{
    public static final Location WAREHOUSE_LOCATION=new Location(35.7069,4.5428,"Central Warehouse - M'sila");

    //M'sila bounding box measured on OSM
    private static final double LAT_MIN=35.685;
    private static final double LAT_MAX=35.740;
    private static final double LON_MIN =4.505;
    private static final double LON_MAX=4.580;

    private static final int ORDER_SPAWN_DELAY_MS = 6000;
    private static final int DISPATCH_TICK_MS     = 2500;

    private int maxOrders = 6;

    private final Queue<Order>pending=new LinkedList<>();
    private final Map<String, Order>allOrders =new HashMap<>();
    private final Random rng = new Random();

    private int liveOrders = 0;
    private int totalDone = 0;
    private int orderSeq = 1;

    @Override
    protected void setup() {
        Object[] args = getArguments();
        if (args != null && args.length > 0) {
            try { maxOrders = Integer.parseInt(args[0].toString()); }
            catch (NumberFormatException ignored) {}
        }
        System.out.println("[WAREHOUSE] online | max orders: " + maxOrders);
        DFAgentDescription dfd =new DFAgentDescription();
        dfd.setName(getAID());
        ServiceDescription sd=new ServiceDescription();
        sd.setType("warehouse");
        sd.setName("SmartDelivery-Warehouse");
        dfd.addServices(sd);
        try{
            DFService.register(this,dfd);
        }
        catch(Exception e){
            e.printStackTrace();
        }
        addBehaviour(new ReceiveDeliveryConfirmation());
        addBehaviour(new TickerBehaviour(this,DISPATCH_TICK_MS) {
            @Override protected void onTick() {
                if(!pending.isEmpty())dispatchNextOrder();
            }
        });
        addBehaviour(new WakerBehaviour(this,3000){
            @Override
            protected void onWake(){
                int initial = Math.min(3, maxOrders);
                for (int i = 0; i < initial; i++)
                    spawnRandomOrder();
            }
        });
    }

    private void spawnRandomOrder(){
        if (liveOrders >= maxOrders)
            return;
        double lat = LAT_MIN+ rng.nextDouble()*(LAT_MAX-LAT_MIN);
        double lon= LON_MIN+rng.nextDouble()*(LON_MAX-LON_MIN);
        String coords = String.format("%.4f,%.4f",lat,lon);
        Location dest = new Location(lat,lon,coords);
        String orderId ="ORD-" +(orderSeq++);
        Order order = new Order(orderId,coords,dest);
        pending.offer(order);
        allOrders.put(orderId, order);
        liveOrders++;
        MapRegistry.addDeliveryPin(orderId, dest);
        MapRegistry.addOrder(order);
        MapRegistry.log("[NEW ORDER] "+orderId+ " →("+ coords +")");
        System.out.println("[WAREHOUSE] new order "+orderId+" at (" +coords+ ")");
    }
    private void dispatchNextOrder(){
        Order order =pending.poll();
        if(order==null)
            return;

        DFAgentDescription template = new DFAgentDescription();
        ServiceDescription sd =new ServiceDescription();
        sd.setType("delivery");
        template.addServices(sd);
        try{
            DFAgentDescription[] agents =DFService.search(this,template);
            if(agents.length==0) {
                System.out.println("[WAREHOUSE] No agents registered yet — requeueing "+order.getOrderId());
                pending.offer(order);
                return;
            }
            ACLMessage cfp=new ACLMessage(ACLMessage.CFP);
            for (DFAgentDescription a : agents)
                cfp.addReceiver(a.getName());
            cfp.setContent("JOB:"+order.getOrderId()+":"+order.getDestination().getLatitude()+":"+order.getDestination().getLongitude());
            cfp.setConversationId("job-"+order.getOrderId());
            send(cfp);
            System.out.println("[WAREHOUSE] CFP for "+order.getOrderId()
                    +" sent to "+ agents.length+" agents");
            addBehaviour(new CollectProposalsBehaviour(order,agents.length));
        } catch (Exception e){
            e.printStackTrace();
        }
    }
    //reply(PROPOSE or REFUSE) and picks the fastest bidder
    private class CollectProposalsBehaviour extends Behaviour{
        private final Order order;
        private final int expected;
        private int received =0;
        private double bestTime = Double.MAX_VALUE;
        private jade.core.AID winner= null;
        private final List<jade.core.AID> losers = new ArrayList<>();

        CollectProposalsBehaviour(Order order,int expected){
            this.order=order;
            this.expected=expected;
        }

        @Override
        public void action(){
            MessageTemplate mt=MessageTemplate.and(
                    MessageTemplate.or(
                            MessageTemplate.MatchPerformative(ACLMessage.PROPOSE),
                            MessageTemplate.MatchPerformative(ACLMessage.REFUSE)),
                    MessageTemplate.MatchConversationId("job-"+order.getOrderId()));

            ACLMessage msg =myAgent.receive(mt);
            if(msg !=null){
                received++;
                if(msg.getPerformative() == ACLMessage.PROPOSE){
                    double time = Double.parseDouble(msg.getContent().split(":")[2]);
                    if(time<bestTime){
                        if (winner!=null)
                            losers.add(winner);
                        bestTime=time;
                        winner = msg.getSender();
                    }else{
                        losers.add(msg.getSender());
                    }
                }
            }else{
                block(400);
            }
        }

        @Override public boolean done() {
            return received>=expected;
        }
        @Override
        public int onEnd(){
            if(winner != null){
                ACLMessage accept=new ACLMessage(ACLMessage.ACCEPT_PROPOSAL);
                accept.addReceiver(winner);
                accept.setContent("ASSIGNED:"+order.getOrderId()+":"+order.getDestination().getLatitude()+":"+order.getDestination().getLongitude()+":"+order.getCustomerId());
                myAgent.send(accept);
                order.assign(winner.getLocalName());
                MapRegistry.updateOrderStatus(order);
                //"D1"=RED,"D2"=GREEN,"D3"=PURPLE
                java.awt.Color pinColor=agentNameToColor(winner.getLocalName());
                MapRegistry.updateDeliveryPinColor(order.getOrderId(),pinColor);
                MapRegistry.log("[ASSIGNED] "+order.getOrderId()+"→ Agent "+winner.getLocalName()+" | waited "+order.getWaitTimeSeconds()+"s");
                for(jade.core.AID loser:losers){
                    ACLMessage rej = new ACLMessage(ACLMessage.REJECT_PROPOSAL);
                    rej.addReceiver(loser);
                    rej.setContent("REJECTED:"+order.getOrderId());
                    myAgent.send(rej);
                }
            }else{
                System.out.println("[WAREHOUSE]all agents busy,requeueing "+order.getOrderId());
                pending.offer(order);
            }
            return 0;
        }
    }
    private class ReceiveDeliveryConfirmation extends CyclicBehaviour{
        @Override
        public void action() {
            MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.INFORM);
            ACLMessage msg = myAgent.receive(mt);
            if(msg != null && msg.getContent().startsWith("DELIVERED:")){
                String orderId = msg.getContent().split(":")[1];
                Order order=allOrders.get(orderId);
                if(order!=null) {
                    order.markDelivered();
                    totalDone++;
                    liveOrders--;
                    MapRegistry.removeDeliveryPin(orderId);
                    MapRegistry.updateOrderStatus(order);
                    MapRegistry.incrementDelivered();
                    MapRegistry.log("[DELIVERED] "+orderId+" | total time: "+order.getTotalTimeSeconds()+"s");
                    System.out.println("[WAREHOUSE] Order "+orderId +" confirmed delivered. Total: "+totalDone);

                    addBehaviour(new WakerBehaviour(myAgent,ORDER_SPAWN_DELAY_MS){
                        @Override protected void onWake(){
                            spawnRandomOrder();
                        }
                    });
                }
            }else{
                block();
            }
        }
    }

    private java.awt.Color agentNameToColor(String name) {
        return switch (name) {
            case "Delivery-1" -> java.awt.Color.RED;
            case "Delivery-2" -> new java.awt.Color(0, 200, 80);
            case "Delivery-3" -> new java.awt.Color(160, 32, 240);
            case "Delivery-4" -> java.awt.Color.CYAN;
            case "Delivery-5" -> new java.awt.Color(251, 191, 36);
            default           -> java.awt.Color.WHITE;
        };
    }

    @Override
    protected void takeDown(){
        try {
            DFService.deregister(this);
        }
        catch (Exception e) {}
        System.out.println("[WAREHOUSE] off .Completed deliveries: " + totalDone);
    }
}