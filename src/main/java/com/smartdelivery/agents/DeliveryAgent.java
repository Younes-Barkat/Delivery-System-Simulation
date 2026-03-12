package com.smartdelivery.agents;

import jade.core.Agent;
import jade.core.behaviours.*;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.*;
import com.smartdelivery.model.Location;

public class DeliveryAgent extends Agent {
    //SPEED 30 km in simulation
    private static final double SPEED_KMH = 30.0;
    private Location currentLocation;
    private boolean isAvailable = true;
    private String agentColor;
    private AgentPositionListener positionListener;
    public interface AgentPositionListener {
        void onPositionUpdate(String agentId, Location location, String status);
    }
    @Override
    protected void setup() {
        currentLocation =WarehouseAgent.WAREHOUSE_LOCATION;
        Object[] args= getArguments();
        agentColor = (args != null && args.length > 0) ?
                args[0].toString() : "BLUE";

        System.out.println("[DELIVERY-" + getLocalName() + "]started at warehouse");
        DFAgentDescription dfd = new DFAgentDescription();
        dfd.setName(getAID());
        ServiceDescription sd = new ServiceDescription();
        sd.setType("delivery");
        sd.setName("Delivery-"+ getLocalName());
        dfd.addServices(sd);
        try { DFService.register(this, dfd); }
        catch (Exception e) { e.printStackTrace(); }
        addBehaviour(new ListenForJobBehaviour());
    }
    private class ListenForJobBehaviour extends CyclicBehaviour {
        @Override
        public void action() {
            MessageTemplate mt =MessageTemplate.MatchPerformative(ACLMessage.CFP);
            ACLMessage cfp = myAgent.receive(mt);
            if (cfp != null && isAvailable) {
                String[] parts= cfp.getContent().split(":");
                String orderId=parts[1];
                double destLat =Double.parseDouble(parts[2]);
                double destLon=Double.parseDouble(parts[3]);
                Location dest = new Location(destLat, destLon, "Dest-" + orderId);
                double distKm =WarehouseAgent.WAREHOUSE_LOCATION.distanceTo(dest);
                double timeHours =distKm /SPEED_KMH;
                double timeSeconds =timeHours * 3600;

                ACLMessage proposal = cfp.createReply();
                proposal.setPerformative(ACLMessage.PROPOSE);
                proposal.setContent("BID:" + getLocalName() + ":" + timeSeconds);
                proposal.setConversationId(cfp.getConversationId());
                send(proposal);
                System.out.println("[DELIVERY-"+ getLocalName() + "] proposed for " +
                        orderId + " (est. "+(int)timeSeconds +"s)");

            } else if (cfp !=null && !isAvailable) {
                // Busy
                ACLMessage refuse = cfp.createReply();
                refuse.setPerformative(ACLMessage.REFUSE);
                refuse.setContent("BUSY");
                send(refuse);
            }


            MessageTemplate acceptMt = MessageTemplate.or(
                    MessageTemplate.MatchPerformative(ACLMessage.ACCEPT_PROPOSAL),
                    MessageTemplate.MatchPerformative(ACLMessage.REJECT_PROPOSAL));
            ACLMessage response = myAgent.receive(acceptMt);
            if (response != null) {
                if (response.getPerformative() == ACLMessage.ACCEPT_PROPOSAL) {
                    isAvailable= false;
                    String[] p = response.getContent().split(":");
                    String orderId =p[1];
                    double lat =Double.parseDouble(p[2]);
                    double lon =Double.parseDouble(p[3]);
                    String custId = p[4];
                    Location dest= new Location(lat, lon, custId);
                    addBehaviour(
                            new ExecuteDeliveryBehaviour(
                            orderId, dest, custId));
                }
            }

            if (cfp == null && response == null) block();
        }
    }

    private class ExecuteDeliveryBehaviour extends Behaviour {
        private final String orderId;
        private final Location destination;
        private final String customerId;
        private int step = 0;
        private static final int TOTAL_STEPS = 20;
        private boolean done = false;

        ExecuteDeliveryBehaviour(String orderId,Location dest,String custId){
            this.orderId= orderId;
            this.destination=dest;
            this.customerId=custId;
        }

        @Override
        public void action() {
            if (step <TOTAL_STEPS) {
                double t =(double)(step+1) /TOTAL_STEPS;
                double lat =lerp(
                        WarehouseAgent.WAREHOUSE_LOCATION.getLatitude(),
                        destination.getLatitude(),t);
                double lon =lerp(
                        WarehouseAgent.WAREHOUSE_LOCATION.getLongitude(),
                        destination.getLongitude(),t);
                currentLocation = new Location(lat,lon,getLocalName());

                if (positionListener !=null) {
                    positionListener.onPositionUpdate(
                            getLocalName(), currentLocation,
                            "Delivering "+ orderId);
                }
                step++;
                try { Thread.sleep(500); }
                catch (InterruptedException e) { Thread.currentThread().interrupt(); }
            } else {
                currentLocation = destination;
                System.out.println("[DELIVERY-" +getLocalName()+"] DELIVERED: " +
                        orderId + " to " + customerId);

                DFAgentDescription template =new DFAgentDescription();
                ServiceDescription sd =new ServiceDescription();
                sd.setType("warehouse");
                template.addServices(sd);
                try {
                    DFAgentDescription[] results =
                            DFService.search(myAgent, template);
                    if (results.length> 0) {
                        ACLMessage inform =new ACLMessage(ACLMessage.INFORM);
                        inform.addReceiver(results[0].getName());
                        inform.setContent("DELIVERED:"+orderId);
                        send(inform);
                    }
                } catch (Exception e) {e.printStackTrace();}

                isAvailable = true;
                done = true;
            }
        }
        @Override
        public boolean done() {return done; }
        private double lerp(double a,double b,double t) {
            return a +t*(b-a);
        }
    }

    public void setPositionListener(AgentPositionListener l) {
        this.positionListener = l; }
    public Location getCurrentLocation() { return currentLocation; }
    public String getAgentColor()         { return agentColor; }

    @Override
    protected void takeDown() {
        try {DFService.deregister(this);}
        catch (Exception e) {}
        System.out.println("[DELIVERY-"+getLocalName()+ "] Terminated.");
    }
}
