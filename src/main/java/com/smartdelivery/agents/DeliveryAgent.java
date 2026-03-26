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

    private static final double SPEED = 30.0; //km/h
    private static final int    STEP  = 250;  //ms between waypoints on screen
    private Location pos;
    private boolean  free  = true;
    private String   color;

    @Override
    protected void setup() {
        pos=WarehouseAgent.WAREHOUSE_LOCATION;
        color=getArguments() !=null && getArguments().length >0
                ? getArguments()[0].toString() :"BLUE";

        MapRegistry.updateAgent(getLocalName(),pos,"Idle",myColor());
        MapRegistry.log("[AGENT] "+ getLocalName() +" ready at warehouse");
        DFAgentDescription dfd =new DFAgentDescription();
        dfd.setName(getAID());
        ServiceDescription sd =new ServiceDescription();
        sd.setType("delivery");
        sd.setName("Delivery-"+getLocalName());
        dfd.addServices(sd);
        try {
            DFService.register(this,dfd);
        }
        catch (Exception e) {
            e.printStackTrace();
        }

        addBehaviour(
                new WaitForJob()
        );
    }
    private java.awt.Color myColor() {
        return switch (color) {
            case "RED"    -> java.awt.Color.RED;
            case "GREEN"  -> new java.awt.Color(0, 200, 80);
            case "ORANGE" -> new java.awt.Color(160, 32, 240);
            case "CYAN"   -> java.awt.Color.CYAN;
            case "YELLOW" -> new java.awt.Color(251, 191, 36);
            default       -> java.awt.Color.WHITE;
        };
    }
    private class WaitForJob extends CyclicBehaviour{
        @Override
        public void action(){
            ACLMessage cfp=myAgent.receive(MessageTemplate.MatchPerformative(ACLMessage.CFP));
            if(cfp != null){
                if (free){
                    String[] p =cfp.getContent().split(":");
                    String oid =p[1];
                    Location dst=new Location(Double.parseDouble(p[2]), Double.parseDouble(p[3]), "dst");
                    double eta=(WarehouseAgent.WAREHOUSE_LOCATION.distanceTo(dst) / SPEED) * 3600.0;
                    ACLMessage bid = cfp.createReply();
                    bid.setPerformative(ACLMessage.PROPOSE);
                    bid.setContent("BID:"+getLocalName()+":"+eta);
                    bid.setConversationId(cfp.getConversationId());
                    send(bid);
                    System.out.println("["+getLocalName()+"]bid for "+oid);
                }else{
                    ACLMessage no=cfp.createReply();
                    no.setPerformative(ACLMessage.REFUSE);
                    no.setContent("BUSY");
                    send(no);
                }
            }

            MessageTemplate mt = MessageTemplate.or(
                    MessageTemplate.MatchPerformative(ACLMessage.ACCEPT_PROPOSAL),
                    MessageTemplate.MatchPerformative(ACLMessage.REJECT_PROPOSAL));
            ACLMessage reply =myAgent.receive(mt);
            if(reply != null && reply.getPerformative() == ACLMessage.ACCEPT_PROPOSAL){
                free =false;
                String[] p = reply.getContent().split(":");
                String oid =p[1];
                Location dst = new Location(Double.parseDouble(p[2]),Double.parseDouble(p[3]), p[4]);
                addBehaviour(new GoDeliver(oid,dst));
            }
            if(cfp == null && reply== null) block();
        }
    }

    // drives to the destination, drops the package, comes back
    private class GoDeliver extends Behaviour{
        private final String oid;
        private final Location dst;
        private List<GeoPosition> path;
        private int i =0;
        private boolean fetched = false;
        private boolean over= false;
        GoDeliver(String oid,Location dst){
            this.oid =oid;
            this.dst =dst;
        }

        @Override
        public void action(){
            if(!fetched){
                fetched =true;
                MapRegistry.updateAgent(getLocalName(),pos,"Routing...",myColor());
                path=MapPanel.fetchRoute(WarehouseAgent.WAREHOUSE_LOCATION,dst);
                if(path==null || path.size()<2){
                    path = straightLine(WarehouseAgent.WAREHOUSE_LOCATION,dst,30);
                }
                MapRegistry.updateAgentRoute(getLocalName(),path);
                MapRegistry.log("[MOVING] "+ getLocalName() +" heading out for "+ oid);
                return;
            }
            if(i<path.size()){
                GeoPosition wp=path.get(i++);
                pos = new Location(wp.getLatitude(),wp.getLongitude(), getLocalName());
                MapRegistry.updateAgent(getLocalName(),pos,"Delivering "+oid, myColor());
                sleep();
            }else{
                pos=dst;
                MapRegistry.updateAgent(getLocalName(),pos,"Delivered",myColor());
                MapRegistry.updateAgentRoute(getLocalName(),null);
                notifyWarehouse(oid);
                addBehaviour(new GoBack());
                over=true;
            }
        }
        @Override public boolean done(){
            return over;
        }
    }

    // after a delivery
    private class GoBack extends Behaviour{
        private List<GeoPosition> path;
        private int i= 0;
        private boolean fetched = false;
        private boolean over = false;
        @Override
        public void action() {
            if(!fetched){
                fetched=true;
                path=MapPanel.fetchRoute(pos, WarehouseAgent.WAREHOUSE_LOCATION);
                if(path == null || path.size()<2){
                    path=straightLine(pos,WarehouseAgent.WAREHOUSE_LOCATION,20);
                }
                MapRegistry.updateAgentRoute(getLocalName(),path);
                MapRegistry.updateAgent(getLocalName(),pos,"Returning...",myColor());
                return;
            }
            if(i< path.size()){
                GeoPosition wp=path.get(i++);
                pos=new Location(wp.getLatitude(), wp.getLongitude(), getLocalName());
                MapRegistry.updateAgent(getLocalName(), pos, "Returning...", myColor());
                sleep();
            }else{
                pos= WarehouseAgent.WAREHOUSE_LOCATION;
                free = true;
                MapRegistry.updateAgent(getLocalName(), pos,"Idle",myColor());
                MapRegistry.updateAgentRoute(getLocalName(),null);
                MapRegistry.log("[AGENT] "+getLocalName()+ " back at warehouse");
                over=true;
            }
        }

        @Override public boolean done(){
            return over;
        }
    }

    private void notifyWarehouse(String oid){
        DFAgentDescription t=new DFAgentDescription();
        ServiceDescription s =new ServiceDescription();
        s.setType("warehouse");
        t.addServices(s);
        try{
            DFAgentDescription[] found = DFService.search(this,t);
            if(found.length>0){
                ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
                msg.addReceiver(found[0].getName());
                msg.setContent("DELIVERED:" +oid);
                send(msg);
            }
        } catch(Exception e){
            e.printStackTrace();
        }
    }
    private List<GeoPosition>straightLine(Location a,Location b,int n){
        List<GeoPosition> pts=new ArrayList<>();
        for(int k=1; k<=n; k++){
            double t =(double) k/n;
            pts.add(new GeoPosition(
                    a.getLatitude()+t*(b.getLatitude()-a.getLatitude()),
                    a.getLongitude()+t* (b.getLongitude()-a.getLongitude())));
        }
        return pts;
    }

    private void sleep(){
        try {
            Thread.sleep(STEP);
        }
        catch (InterruptedException e){
            Thread.currentThread().interrupt();
        }
    }

    public Location getCurrentLocation(){
        return pos;
    }
    public String   getAgentColor(){
        return color;
    }

    @Override
    protected void takeDown(){
        try{
            DFService.deregister(this);
        }
        catch (Exception e) {}
    }
}