package com.smartdelivery.gui;

import org.jxmapviewer.JXMapViewer;
import org.jxmapviewer.OSMTileFactoryInfo;
import org.jxmapviewer.viewer.*;
import org.jxmapviewer.painter.*;
import org.jxmapviewer.painter.Painter;
import com.smartdelivery.model.Location;
import javax.swing.*;
import java.awt.*;
import java.awt.geom.Point2D;
import java.util.*;

public class MapPanel extends JPanel{
    private final JXMapViewer mapViewer;
    private final Map<String,AgentMarker> agentMarkers =new HashMap<>();
    private final Set<GeoPosition> customerPositions =new HashSet<>();

    public MapPanel() {
        setLayout(new BorderLayout());
        TileFactoryInfo info=new OSMTileFactoryInfo();
        DefaultTileFactory tileFactory=new DefaultTileFactory(info);
        tileFactory.setThreadPoolSize(8); //tile loading
        mapViewer= new JXMapViewer();
        mapViewer.setTileFactory(tileFactory);
        GeoPosition msila =new GeoPosition(35.7069, 4.5428);
        mapViewer.setAddressLocation(msila);
        mapViewer.setZoom(4);
        CompoundPainter<JXMapViewer> painter = new CompoundPainter<>();
        painter.addPainter(new CustomerPainter());
        painter.addPainter(new AgentPainter());
        painter.addPainter(new WarehousePainter());
        mapViewer.setOverlayPainter(painter);

        add(mapViewer, BorderLayout.CENTER);
        JPanel controls= new JPanel();
        JButton zoomIn= new JButton("+");
        JButton zoomOut = new JButton("-");
        zoomIn.addActionListener(e ->mapViewer.setZoom(mapViewer.getZoom() -1));
        zoomOut.addActionListener(e -> mapViewer.setZoom(mapViewer.getZoom()+ 1));
        controls.add(new JLabel("Map Zoom:"));
        controls.add(zoomIn);
        controls.add(zoomOut);
        add(controls, BorderLayout.SOUTH);
    }
    public synchronized void updateAgentPosition(String agentId,Location loc,String status,Color color) {
        agentMarkers.put(agentId,new AgentMarker(
                new GeoPosition(loc.getLatitude(),loc.getLongitude()),status, color));
        SwingUtilities.invokeLater(mapViewer::repaint);
    }
    public synchronized void addCustomer(Location loc){
        customerPositions.add(new GeoPosition(loc.getLatitude(),loc.getLongitude()));
        SwingUtilities.invokeLater(mapViewer::repaint);
    }

    private class AgentPainter implements Painter<JXMapViewer>{
        @Override
        public void paint(Graphics2D g,JXMapViewer map,int w,  int h) {
            g = (Graphics2D) g.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);
            Rectangle bounds = map.getViewportBounds();
            for (Map.Entry<String,AgentMarker> e: agentMarkers.entrySet()) {
                AgentMarker m= e.getValue();
                Point2D pt = map.getTileFactory().geoToPixel(
                        m.position, map.getZoom());
                int x= (int)(pt.getX()- bounds.getX());
                int y =(int)(pt.getY()- bounds.getY());
                //elivery truck icon
                g.setColor(m.color);
                g.fillOval(x -12, y -12, 24,24);
                g.setColor(Color.WHITE);
                g.setFont(new Font("Arial", Font.BOLD,10));
                g.drawString("🚚",x- 8, y+ 4);
                g.setColor(Color.BLACK);
                g.drawString(e.getKey(), x +14, y);
            }
            g.dispose();
        }
    }

    private class CustomerPainter implements Painter<JXMapViewer> {
        @Override
        public void paint(Graphics2D g, JXMapViewer map,int w, int h) {
            g = (Graphics2D) g.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);
            Rectangle bounds = map.getViewportBounds();
            for (GeoPosition pos : customerPositions) {
                Point2D pt = map.getTileFactory().geoToPixel(pos, map.getZoom());
                int x =(int)(pt.getX()-bounds.getX());
                int y =(int)(pt.getY()- bounds.getY());
                g.setColor(new Color(234, 179, 8));
                g.fillRect(x -8,y - 8,16,16);
                g.setColor(Color.WHITE);
                g.drawString("🏠",x-8,y + 5);
            }
            g.dispose();
        }
    }
    private class WarehousePainter implements Painter<JXMapViewer> {
        @Override
        public void paint(Graphics2D g,JXMapViewer map,int w, int h) {
            g = (Graphics2D) g.create();
            GeoPosition wh=new GeoPosition(36.7538,3.0588);
            Rectangle bounds=map.getViewportBounds();
            Point2D pt =map.getTileFactory().geoToPixel(wh, map.getZoom());
            int x=(int)(pt.getX()-bounds.getX());
            int y=(int)(pt.getY()-bounds.getY());
            g.setColor(new Color(37,99,235));
            g.fillRect(x - 15,y -15,30,30);
            g.setColor(Color.WHITE);
            g.setFont(new Font("Arial",Font.BOLD,10));
            g.drawString("WH",x-10,y+5);
            g.dispose();
        }
    }
    record AgentMarker(GeoPosition position,String status,Color color) {}
}
