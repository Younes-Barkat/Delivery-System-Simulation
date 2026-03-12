package com.smartdelivery.agents;

import jade.core.Agent;
import jade.core.behaviours.*;
import jade.lang.acl.ACLMessage;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.*;
import com.smartdelivery.model.Location;

public class CustomerAgent extends Agent {
    //predefined customer locations in M'sila array
    public static final Location[] CUSTOMER_LOCATIONS= {
            new Location(35.7069,4.5428,"ville de rose"),
            new Location(35.7156,4.5312,"cité El Amel"),
            new Location(35.6980,4.5601,"cité 1000 maison"),
            new Location(35.7230,4.5190,"Cité El-Wiam"),
            new Location(35.6890,4.5720,"cité Ezzouhour"),
            new Location(35.7310,4.5480,"Cité OPGI"),
    };
    private Location myLocation;
    private int customerIndex;

    @Override
    protected void setup() {
        Object[] args =getArguments();
        customerIndex = (args !=null && args.length >0) ?
                Integer.parseInt(args[0].toString()): 0;
        myLocation =CUSTOMER_LOCATIONS[
                customerIndex% CUSTOMER_LOCATIONS.length];
        System.out.println("[CUSTOMER-" + getLocalName() + "]located at: " + myLocation.getName());
        //(allow agents to start first by waiting 5 secondes)
        addBehaviour(new WakerBehaviour(this,5000 +customerIndex * 2000) {
            @Override
            protected void onWake(){ placeOrder(); }
        });
    }
    private void placeOrder() {
        DFAgentDescription template = new DFAgentDescription();
        ServiceDescription sd=new ServiceDescription();
        sd.setType("warehouse");
        template.addServices(sd);
        try {
            DFAgentDescription[] results = DFService.search(this,template);
            if (results.length >0){
                ACLMessage order=new ACLMessage(ACLMessage.REQUEST);
                order.addReceiver(results[0].getName());
                order.setContent("ORDER:" + getLocalName() +":" +
                        myLocation.getLatitude() +":" +
                        myLocation.getLongitude());
                send(order);
                System.out.println("[CUSTOMER-" +getLocalName() +
                        "] Order placed to " +myLocation.getName());
            }
        } catch (Exception e) { e.printStackTrace(); }
    }
    public Location getMyLocation() { return myLocation; }
}
