package com.smartdelivery.gui;

import org.jxmapviewer.JXMapViewer;
import org.jxmapviewer.viewer.*;
import java.awt.event.*;
import java.awt.geom.Point2D;
import java.io.*;
import java.net.*;
import org.jxmapviewer.painter.CompoundPainter;
import org.jxmapviewer.painter.Painter;
import com.smartdelivery.model.Location;
import javax.swing.*;
import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class MapPanel extends JPanel{
    private final JXMapViewer mapViewer;
    private final Map<String, AgentMarker> agentMarkers=new ConcurrentHashMap<>();
    private final Map<String, List<GeoPosition>> agentRoutes = new ConcurrentHashMap<>();
    private final Map<String, DeliveryPin> deliveryPins= new ConcurrentHashMap<>();
    private static class DeliveryPin{
        final GeoPosition position;
        Color color;
        DeliveryPin(GeoPosition pos,Color col){
            position=pos;
            color=col;
        }
    }
    //fixed warehouse
    private static final GeoPosition WAREHOUSE_POS=new GeoPosition(35.7069,4.5428);

    private static class AgentMarker{
        final GeoPosition position;
        final String status;
        final Color color;
        AgentMarker(GeoPosition p,String s, Color c){
            position=p;
            status=s;
            color =c;
        }
    }

    public MapPanel(){
        setLayout(new BorderLayout());
        setBackground(new Color(10,12,20));

        //OSM tile factory.
        TileFactoryInfo info=new TileFactoryInfo(0,17,17,256,true,true, "https://tile.openstreetmap.org/","x","y","z"){
            @Override
            public String getTileUrl(int x,int y,int zoom) {
                int osmZoom =17-zoom;
                return "https://tile.openstreetmap.org/"+osmZoom+"/"+x+"/"+y+".png";
            }
        };
        DefaultTileFactory tf =new DefaultTileFactory(info);
        tf.setThreadPoolSize(4);
        mapViewer = new JXMapViewer();
        mapViewer.setTileFactory(tf);
        mapViewer.setAddressLocation(WAREHOUSE_POS);
        mapViewer.setZoom(12);
        final Point[] dragOrigin={null};
        mapViewer.addMouseListener(new MouseAdapter(){
            @Override public void mousePressed(MouseEvent e){
                dragOrigin[0]=e.getPoint();
                mapViewer.setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));
            }
            @Override public void mouseReleased(MouseEvent e){
                dragOrigin[0] =null;
                mapViewer.setCursor(Cursor.getDefaultCursor());
            }
        });
        mapViewer.addMouseMotionListener(new MouseMotionAdapter(){
            @Override
            public void mouseDragged(MouseEvent e){
                if(dragOrigin[0]==null)
                    return;
                Point current = e.getPoint();
                int dx= dragOrigin[0].x-current.x;
                int dy= dragOrigin[0].y-current.y;
                Rectangle bounds = mapViewer.getViewportBounds();
                int cx = bounds.x+bounds.width /2+dx;
                int cy = bounds.y+bounds.height/2+dy;
                GeoPosition newCenter = mapViewer.getTileFactory().pixelToGeo(new Point2D.Double(cx,cy),mapViewer.getZoom());
                mapViewer.setAddressLocation(newCenter);
                dragOrigin[0]=current;
                mapViewer.repaint();
            }
        });
        mapViewer.addMouseWheelListener(e ->{
            int z = mapViewer.getZoom();
            mapViewer.setZoom(e.getWheelRotation()<0
                    ? Math.max(0,z-1)
                    : Math.min(10,z+1));
        });

        CompoundPainter<JXMapViewer> painter=new CompoundPainter<>();
        painter.addPainter(new RoutePainter());
        painter.addPainter(new PinPainter());
        painter.addPainter(new AgentPainter());
        painter.addPainter(new WarehousePainter());
        mapViewer.setOverlayPainter(painter);
        add(mapViewer,BorderLayout.CENTER);

        JPanel mapWrapper = new JPanel(null){
            @Override
            public void doLayout(){
                super.doLayout();
                for(Component c:getComponents()){
                    if(c == mapViewer) {
                        c.setBounds(0,0,getWidth(),getHeight());
                    }else{
                        Dimension ps=c.getPreferredSize();
                        c.setBounds(getWidth()-ps.width-12,12,ps.width, ps.height);
                    }
                }
            }
        };
        mapWrapper.setOpaque(false);
        mapWrapper.add(mapViewer);
        mapWrapper.add(buildLegendPanel());
        remove(mapViewer);
        add(mapWrapper, BorderLayout.CENTER);

        JPanel controls = new JPanel(new FlowLayout(FlowLayout.LEFT,8,4));
        controls.setBackground(new Color(10, 12, 20));
        JButton zIn = darkButton("+");
        JButton zOut = darkButton("−");
        zIn.addActionListener(e-> mapViewer.setZoom(Math.max(0,mapViewer.getZoom()-1)));
        zOut.addActionListener(e-> mapViewer.setZoom(Math.min(17,mapViewer.getZoom()+1)));
        controls.add(zIn);
        controls.add(zOut);
        add(controls, BorderLayout.SOUTH);
    }
    public synchronized void updateAgentPosition(String id,Location loc,String status, Color color){
        agentMarkers.put(id, new AgentMarker(
                new GeoPosition(loc.getLatitude(),loc.getLongitude()),status,color));
        SwingUtilities.invokeLater(mapViewer::repaint);
    }
    public synchronized void updateAgentRoute(String id,List<GeoPosition> route){
        if(route !=null)
            agentRoutes.put(id, route);
        else agentRoutes.remove(id);
        SwingUtilities.invokeLater(mapViewer::repaint);
    }
    public synchronized void addDeliveryPin(String orderId, Location loc) {
        deliveryPins.put(orderId, new DeliveryPin(
                new GeoPosition(loc.getLatitude(),loc.getLongitude()),
                new Color(251,191,36)));
        SwingUtilities.invokeLater(mapViewer::repaint);
    }

    public synchronized void updateDeliveryPinColor(String orderId,Color color){
        DeliveryPin pin = deliveryPins.get(orderId);
        if(pin != null){
            pin.color=color;
            SwingUtilities.invokeLater(mapViewer::repaint);
        }
    }
    public synchronized void removeDeliveryPin(String orderId) {
        deliveryPins.remove(orderId);
        SwingUtilities.invokeLater(mapViewer::repaint);
    }

    public void incrementDelivered() {}

    public static List<GeoPosition> fetchRoute(Location from,Location to){
        try{
            String url = String.format(
                    "https://router.project-osrm.org/route/v1/driving/%.6f,%.6f;%.6f,%.6f?overview=full&geometries=geojson",
                    from.getLongitude(),from.getLatitude(),
                    to.getLongitude(),to.getLatitude());

            HttpURLConnection con = (HttpURLConnection) new URL(url).openConnection();
            con.setRequestProperty("User-Agent","SmartDeliverySystem/1.0 (academic)");
            con.setConnectTimeout(6000);
            con.setReadTimeout(6000);

            if(con.getResponseCode() != 200)
                return null;

            StringBuilder sb=new StringBuilder();
            try(BufferedReader r =new BufferedReader(new InputStreamReader(con.getInputStream()))){
                String line;
                while((line=r.readLine())!=null)
                    sb.append(line);
            }
            String json = sb.toString();
            int idx =json.indexOf("\"coordinates\":[");
            if (idx <0) return null;
            int start =json.indexOf("[[",idx);
            int end = json.indexOf("]]",start)+2;
            String raw= json.substring(start,end);

            List<GeoPosition> pts = new ArrayList<>();
            for(String pt : raw.replace("[[","").replace("]]","").split("\\],\\[")){
                String[] xy=pt.split(",");
                if(xy.length>=2){
                    double lon=Double.parseDouble(xy[0].trim());
                    double lat= Double.parseDouble(xy[1].trim());
                    pts.add(new GeoPosition(lat,lon));
                }
            }
            return pts.isEmpty() ? null : pts;
        } catch (Exception e) {
            return null;
        }
    }

    //painters
    private class RoutePainter implements Painter<JXMapViewer>{
        @Override
        public void paint(Graphics2D g,JXMapViewer map,int w,int h){
            g =(Graphics2D) g.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
            Rectangle bounds=map.getViewportBounds();

            for(Map.Entry<String,List<GeoPosition>> e :agentRoutes.entrySet()){
                AgentMarker marker = agentMarkers.get(e.getKey());
                Color c =marker != null ? marker.color : Color.GRAY;
                g.setColor(new Color(c.getRed(),c.getGreen(),c.getBlue(),140));
                g.setStroke(new BasicStroke(5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                GeoPosition prev=null;
                for(GeoPosition pos : e.getValue()){
                    if(prev !=null) {
                        Point2D p1 =map.getTileFactory().geoToPixel(prev,map.getZoom());
                        Point2D p2 =map.getTileFactory().geoToPixel(pos,map.getZoom());
                        g.drawLine(
                                (int)(p1.getX()-bounds.getX()),(int)(p1.getY()-bounds.getY()),
                                (int)(p2.getX()-bounds.getX()),(int)(p2.getY()-bounds.getY()));
                    }
                    prev = pos;
                }
            }
            g.dispose();
        }
    }

    private class PinPainter implements Painter<JXMapViewer>{
        @Override
        public void paint(Graphics2D g,JXMapViewer map,int w,int h) {
            g = (Graphics2D) g.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
            Rectangle bounds= map.getViewportBounds();

            for(Map.Entry<String,DeliveryPin> e : deliveryPins.entrySet()){
                DeliveryPin pin =e.getValue();
                Point2D pt =map.getTileFactory().geoToPixel(pin.position, map.getZoom());
                int x=(int)(pt.getX()-bounds.getX());
                int y=(int)(pt.getY()-bounds.getY());
                Color pinColor = pin.color;
                // outter glow
                g.setColor(new Color(pinColor.getRed(),pinColor.getGreen(),pinColor.getBlue(),55));
                g.fillOval(x-14,y-28,28,28);
                //
                g.setColor(pinColor);
                g.fillOval(x-9,y-26,18,18);

                g.setColor(Color.WHITE);
                g.fillOval(x -4,y-21,8,8);

                int[] px={x-4,x+4,x};
                int[] py={y-10,y-10,y};
                g.setColor(pinColor);
                g.fillPolygon(px, py,3);
                g.setFont(new Font("Impact",Font.BOLD,14));
                g.setColor(new Color(0, 0, 0, 150));
                g.drawString(e.getKey().replace("ORD-","#"),x + 13,y-15);
                g.setColor(pinColor);
                g.drawString(e.getKey().replace("ORD-","#"),x + 12,y -16);
            }
            g.dispose();
        }
    }

    //dots
    private class AgentPainter implements Painter<JXMapViewer>{
        @Override
        public void paint(Graphics2D g,JXMapViewer map,int w,int h){
            g=(Graphics2D) g.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            Rectangle bounds=map.getViewportBounds();
            for (Map.Entry<String, AgentMarker> e : agentMarkers.entrySet()){
                AgentMarker m = e.getValue();
                Point2D pt = map.getTileFactory().geoToPixel(m.position, map.getZoom());
                int x =(int)(pt.getX()- bounds.getX());
                int y =(int)(pt.getY()- bounds.getY());

                // glow
                g.setColor(new Color(m.color.getRed(), m.color.getGreen(), m.color.getBlue(), 40));
                g.fillOval(x-26, y-26,52,52);
                //
                g.setColor(new Color(m.color.getRed(), m.color.getGreen(), m.color.getBlue(), 210));
                g.fillOval(x - 15,y -15, 30,30);
                g.setColor(m.color);
                g.setStroke(new BasicStroke(2.5f));
                g.drawOval(x -15, y -15,30,30);
                String label =e.getKey().replace("Delivery-", "D");
                g.setFont(new Font("Impact", Font.BOLD, 16));
                g.setColor(new Color(0,0,0,160)); //shadow
                g.drawString(label,x + 17, y + 6);
                g.setColor(Color.WHITE);
                g.drawString(label,x + 16,y+5);
            }
            g.dispose();
        }
    }

    //wh
    private class WarehousePainter implements Painter<JXMapViewer>{
        @Override
        public void paint(Graphics2D g,JXMapViewer map,int w,int h) {
            g =(Graphics2D) g.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            Rectangle bounds=map.getViewportBounds();

            Point2D pt = map.getTileFactory().geoToPixel(WAREHOUSE_POS,map.getZoom());
            int x =(int)(pt.getX()-bounds.getX());
            int y =(int)(pt.getY()-bounds.getY());

            Color  blue = new Color(0, 97, 255);
            g.setColor(new Color(0, 182, 255, 109));
            g.setStroke(new BasicStroke(1f));
            g.drawOval(x-28,y-28,56,56);
            g.drawOval(x-40,y-40,80, 80);

            g.setColor(new Color(1,172, 251, 207));
            g.fillRoundRect(x-16,y -16,32,32,6,6);
            g.setColor(blue);
            g.setStroke(new BasicStroke(2f));
            g.drawRoundRect(x - 16, y - 16, 32, 32, 6, 6);
            g.setFont(new Font("Impact", Font.BOLD, 14));
            g.setColor(blue);
            g.drawString("WH", x - 12, y + 6);
            g.setFont(new Font("Impact",Font.PLAIN,11));
            g.setColor(new Color(55, 123,228));
            g.drawString("WAREHOUSE",x -24,y+28);
            g.dispose();
        }
    }

    private JPanel buildLegendPanel(){
        Color bg =new Color(12,16,28,210);
        Color border =new Color(50,65,95);
        Color dimText=new Color(100,116,139);
        Color white=new Color(220,230,245);

        JPanel panel = new JPanel(){
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 =(Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(12,16,28,210));
                g2.fillRoundRect(0,0, getWidth(),getHeight(),10,10);
                g2.setColor(border);
                g2.setStroke(new BasicStroke(1f));
                g2.drawRoundRect(0,0,getWidth()-1,getHeight()-1,10,10);
                g2.dispose();
            }
        };
        panel.setLayout(new BoxLayout(panel,BoxLayout.Y_AXIS));
        panel.setOpaque(false);
        panel.setBorder(BorderFactory.createEmptyBorder(10,14,10,14));
        JLabel title = new JLabel("MAP LEGEND");
        title.setForeground(dimText);
        title.setFont(new Font("Segoe UI",Font.BOLD,10));
        title.setAlignmentX(0f);
        panel.add(title);
        panel.add(Box.createVerticalStrut(8));

        Object[][] entries={
                { new Color(56,189,248),"Warehouse"},
                { Color.RED,"Agent 1 (D1)"},
                { new Color(0,200,80),"Agent 2 (D2)"},
                { new Color(160,32, 240),"Agent 3 (D3)"},
                { new Color(251,191, 36),"Pin: waiting (yellow)"},
                { new Color(220,38, 38),"Pin: assigned (agent color)"},
        };
        for(Object[] entry : entries){
            JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT,6,0));
            row.setOpaque(false);
            row.setMaximumSize(new Dimension(160, 20));
            JLabel dot = new JLabel("●");
            dot.setForeground((Color) entry[0]);
            dot.setFont(new Font("Segoe UI",Font.PLAIN,13));
            JLabel lbl = new JLabel((String)entry[1]);
            lbl.setForeground(white);
            lbl.setFont(new Font("Segoe UI",Font.PLAIN,11));
            row.add(dot);
            row.add(lbl);
            panel.add(row);
        }

        panel.setPreferredSize(new Dimension(175,150));
        return panel;
    }
    private static JButton darkButton(String text){
        JButton btn=new JButton(text);
        btn.setBackground(new Color(26,32,52));
        btn.setForeground(new Color(148,163,184));
        btn.setFont(new Font("Segoe UI",Font.BOLD,18));
        btn.setBorder(BorderFactory.createLineBorder(new Color(55,65,90),1));
        btn.setFocusPainted(false);
        btn.setPreferredSize(new Dimension(42,32));
        return btn;
    }
}