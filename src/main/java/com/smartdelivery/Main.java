package com.smartdelivery;

import javax.swing.*;
import com.smartdelivery.agents.MapRegistry;
import com.smartdelivery.gui.LaunchDialog;
import com.smartdelivery.gui.SimulationWindow;
import jade.core.Profile;
import jade.core.ProfileImpl;
import jade.core.Runtime;
import jade.wrapper.AgentContainer;
import jade.wrapper.AgentController;

public class Main {

    public static void main(String[] args) throws Exception{

        System.setProperty("http.agent","Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36");
        System.setProperty("https.agent","Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36");
        LaunchDialog.Config cfg =LaunchDialog.show(null);
        if(cfg==null){
            System.exit(0);
        }
        int numAgents = cfg.agentCount();
        int maxOrders = cfg.maxOrders();
        SimulationWindow window = new SimulationWindow();
        MapRegistry.setMapPanel(window.getMapPanel());
        MapRegistry.setWindow(window);
        SwingUtilities.invokeLater(()->window.setVisible(true));
        Runtime rt = Runtime.instance();
        rt.setCloseVM(true);
        Profile profile = new ProfileImpl();
        profile.setParameter(Profile.MAIN,"true");
        profile.setParameter(Profile.GUI,"false");
        AgentContainer container = rt.createMainContainer(profile);
        AgentController warehouse = container.createNewAgent(
                "Warehouse",
                "com.smartdelivery.agents.WarehouseAgent",
                new Object[]{ maxOrders });
        warehouse.start();
        window.log("[SYSTEM] Warehouse online (max " + maxOrders + " active orders)");
        Thread.sleep(1200);

        String[] colors = {"RED","GREEN","ORANGE","CYAN","YELLOW","PINK","LIME","SKY","CORAL","MINT"};
        for(int i =0; i< numAgents; i++) {
            AgentController agent = container.createNewAgent("Delivery-"+(i+1), "com.smartdelivery.agents.DeliveryAgent", new Object[]{ colors[i % colors.length]});
            agent.start();
            window.log("[SYSTEM] Agent Delivery-" +(i+1)+" online (" + colors[i % colors.length] + ")");
            Thread.sleep(300);
        }
        window.log("[SYSTEM] "+numAgents +" agents ready.Orders incoming...");
        window.setAgentCount(numAgents);
    }
}