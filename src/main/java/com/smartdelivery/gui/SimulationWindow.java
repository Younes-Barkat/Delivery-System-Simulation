package com.smartdelivery.gui;

import javax.swing.*;
import java.awt.*;

public class SimulationWindow extends JFrame {

    private final MapPanel mapPanel;
    private final JTextArea logArea;
    private final JLabel deliveredLabel;
    private int deliveredCount =0;

    public SimulationWindow() {
        super("Smart Delivery System Simulation (ABM)");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1200,800);
        setLayout(new BorderLayout());
        JPanel titleBar=new JPanel(new FlowLayout(FlowLayout.LEFT));
        titleBar.setBackground(new Color(30,58,95));
        JLabel title = new JLabel(" Smart Delivery System | Agent-Based Simulation ");
        title.setForeground(Color.WHITE);
        title.setFont(new Font("Calibri",Font.BOLD,20));
        titleBar.add(title);
        add(titleBar,BorderLayout.NORTH);
        mapPanel=new MapPanel();
        add(mapPanel,BorderLayout.CENTER);

        JPanel statsPanel =new JPanel();
        statsPanel.setLayout(new BoxLayout(statsPanel,BoxLayout.Y_AXIS));
        statsPanel.setPreferredSize(new Dimension(280,800));
        statsPanel.setBackground(new Color(248,250,252));
        statsPanel.setBorder(BorderFactory.createEmptyBorder(10,10,10,10));

        JLabel statsTitle =new JLabel("Simulation Statistics");
        statsTitle.setFont(new Font("Calibri", Font.BOLD,16));
        statsTitle.setForeground(new Color(30,58,95));
        statsPanel.add(statsTitle);
        statsPanel.add(Box.createVerticalStrut(10));

        deliveredLabel = new JLabel("Packages Delivered: 0");
        deliveredLabel.setFont(new Font("Calibri",Font.PLAIN, 14));
        statsPanel.add(deliveredLabel);
        statsPanel.add(Box.createVerticalStrut(10));
        //Legend
        statsPanel.add(new JLabel("Legend:"));
        statsPanel.add(makeLegendItem(new Color(37, 99, 235), "Warehouse (WH)"));
        statsPanel.add(makeLegendItem(Color.RED, "Delivery Agent 1"));
        statsPanel.add(makeLegendItem(Color.GREEN, "Delivery Agent 2"));
        statsPanel.add(makeLegendItem(Color.ORANGE, "Delivery Agent 3"));
        statsPanel.add(makeLegendItem(new Color(234,179,8), "Customer Locations"));
        statsPanel.add(Box.createVerticalStrut(20));

        JLabel logTitle = new JLabel("Agent Event Log:");
        logTitle.setFont(new Font("Calibri",Font.BOLD, 14));
        statsPanel.add(logTitle);
        logArea = new JTextArea(20, 20);
        logArea.setEditable(false);
        logArea.setFont(new Font("Courier New", Font.PLAIN, 11));
        logArea.setBackground(new Color(30,41, 59));
        logArea.setForeground(new Color(226, 232, 240));
        JScrollPane scroll = new JScrollPane(logArea);
        statsPanel.add(scroll);

        add(statsPanel, BorderLayout.EAST);
        setLocationRelativeTo(null);
    }
    private JPanel makeLegendItem(Color c, String text) {
        JPanel p =new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 2));
        p.setBackground(new Color(248, 250, 252));
        JLabel box=new JLabel("  ");
        box.setOpaque(true);
        box.setBackground(c);
        p.add(box);
        p.add(new JLabel(text));
        return p;
    }
    public void log(String message) {
        SwingUtilities.invokeLater(() -> {
            logArea.append(message +"\n");
            logArea.setCaretPosition(logArea.getDocument().getLength());
        });
    }
    public void incrementDelivered() {
        deliveredCount++;
        SwingUtilities.invokeLater(() -> deliveredLabel.setText("Packages delivered:" + deliveredCount));
    }



    public MapPanel getMapPanel(){ return mapPanel; }
}
