package com.smartdelivery;

import javax.swing.*;
import com.smartdelivery.agents.MapRegistry;
import com.smartdelivery.gui.SimulationWindow;
import jade.core.Profile;
import jade.core.ProfileImpl;
import jade.core.Runtime;
import jade.wrapper.AgentContainer;
import jade.wrapper.AgentController;

public class Main{

    public static void main(String[] args) throws Exception{

        System.setProperty("http.agent","Mozilla/5.0(Windows NT 10.0; Win64; x64) AppleWebKit/537.36");
        System.setProperty("https.agent","Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36");

        // GUI thread
        SimulationWindow window =new SimulationWindow();
        MapRegistry.setMapPanel(window.getMapPanel());
        MapRegistry.setWindow(window);
        SwingUtilities.invokeLater(() ->window.setVisible(true));

        Runtime rt = Runtime.instance();
        rt.setCloseVM(true);
        Profile profile = new ProfileImpl();
        profile.setParameter(Profile.MAIN,"true");
        profile.setParameter(Profile.GUI,"false");
        AgentContainer container = rt.createMainContainer(profile);

        // wh agent
        AgentController warehouse =container.createNewAgent("Warehouse","com.smartdelivery.agents.WarehouseAgent",null);
        warehouse.start();
        window.log("[SYSTEM] warehouse ag ready");
        Thread.sleep(1200);

        String[] colors = {"RED", "GREEN", "ORANGE"};
        for(int i=0; i<3;i++){
            AgentController agent=container.createNewAgent("Delivery-"+ (i+1),"com.smartdelivery.agents.DeliveryAgent",new Object[]{colors[i]});
            agent.start();
            window.log("[SYSTEM] Delivery ag -"+(i+1)+ " ready (" +colors[i]+ ")");
            Thread.sleep(300);
        }
        window.log("[SYSTEM] Orders will start....");
    }
}