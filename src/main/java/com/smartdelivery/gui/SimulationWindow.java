package com.smartdelivery.gui;

import com.smartdelivery.model.Order;
import javax.swing.table.JTableHeader;
import java.awt.*;
import java.util.LinkedHashMap;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import java.util.Map;

public class SimulationWindow extends JFrame{
    private static final Color BG_DARKEST = new Color(10, 12,20);// bg
    private static final Color BG_DARK= new Color(18, 22,36);  //panel bg
    private static final Color BG_CARD = new Color(26,32,52);  //table row
    private static final Color BG_HEADER = new Color(15,19,32); //tb header
    private static final Color ACCENT_BLUE = new Color(56,189,248);
    private static final Color ACCENT_GREEN= new Color(52,211,153);
    private static final Color ACCENT_RED = new Color(248,113,113);
    private static final Color ACCENT_AMBER= new Color(251,191,36);
    private static final Color TEXT_PRIMARY= new Color(226,232, 240);
    private static final Color TEXT_DIM= new Color(100, 116,139);
    private final MapPanel mapPanel;
    private final JTextArea logArea;
    private final JLabel deliveredLabel;
    private final JLabel activeLabel;
    private final JLabel agentsLabel;

    private final DefaultTableModel  tableModel;
    private final Map<String, Integer>rows= new LinkedHashMap<>();
    private int done= 0;
    private int active = 0;

    public SimulationWindow(){
        super("Smart Delivery System -M'sila-");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1380,860);
        setMinimumSize(new Dimension(1100,700));
        setLayout(new BorderLayout(0,0));
        getContentPane().setBackground(BG_DARKEST);
        JPanel titleBar =new JPanel(new BorderLayout());
        titleBar.setBackground(BG_DARKEST);
        titleBar.setBorder(new EmptyBorder(10,16,10, 16));

        JLabel title = new JLabel("Smart Delivery System |Agent-Based Simulation |M'sila, Algeria");
        title.setForeground(TEXT_PRIMARY);
        title.setFont(new Font("Segoe UI", Font.BOLD,15));
        titleBar.add(title, BorderLayout.WEST);
        add(titleBar, BorderLayout.NORTH);

        mapPanel=new MapPanel();
        add(mapPanel,BorderLayout.CENTER);

        JPanel right =new JPanel();
        right.setLayout(new BoxLayout(right,BoxLayout.Y_AXIS));
        right.setPreferredSize(new Dimension(420,860));
        right.setBackground(BG_DARK);
        right.setBorder(new EmptyBorder(12,12,12,12));

        JPanel statsRow = new JPanel(new GridLayout(1, 3,8,0));
        statsRow.setMaximumSize(new Dimension(420,72));
        statsRow.setBackground(BG_DARK);

        deliveredLabel = statCard("0","Delivered",ACCENT_GREEN);
        activeLabel = statCard("0","Active",ACCENT_AMBER);
        agentsLabel= statCard("3","Agents",ACCENT_BLUE);

        statsRow.add(wrapStat(deliveredLabel,"Delivered",ACCENT_GREEN));
        statsRow.add(wrapStat(activeLabel,"Active",ACCENT_AMBER));
        statsRow.add(wrapStat(agentsLabel,"Agents",ACCENT_BLUE));
        right.add(statsRow);
        right.add(Box.createVerticalStrut(14));

        //order table
        right.add(sectionLabel("LIVE ORDER STATUS"));
        right.add(Box.createVerticalStrut(4));

        String[] cols ={"Order ID","Agent","Status","Time"};
        tableModel = new DefaultTableModel(cols,0){
            @Override public boolean isCellEditable(int r,int c){
                return false;
            }
        };

        JTable table =new JTable(tableModel);
        styleTable(table);
        JScrollPane tableScroll = new JScrollPane(table);
        tableScroll.setBackground(BG_CARD);
        tableScroll.getViewport().setBackground(BG_CARD);
        tableScroll.setBorder(BorderFactory.createLineBorder(new Color(40,55, 80),1));
        Dimension tableFixedSize= new Dimension(396,340);
        tableScroll.setPreferredSize(tableFixedSize);
        tableScroll.setMinimumSize(tableFixedSize);
        tableScroll.setMaximumSize(new Dimension(420,340));

        styleScrollBar(tableScroll.getVerticalScrollBar());
        styleScrollBar(tableScroll.getHorizontalScrollBar());
        tableScroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        tableScroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        right.add(tableScroll);
        right.add(Box.createVerticalStrut(12));

        //event log
        right.add(sectionLabel("EVENT LOG"));
        right.add(Box.createVerticalStrut(4));
        logArea= new JTextArea();
        logArea.setEditable(false);
        logArea.setBackground(new Color(12,16,28));
        logArea.setForeground(new Color(148,163, 184));
        logArea.setFont(new Font("Consolas", Font.PLAIN, 11));
        logArea.setLineWrap(true);
        logArea.setWrapStyleWord(true);
        logArea.setBorder(new EmptyBorder(6, 6, 6, 6));

        JScrollPane logScroll= new JScrollPane(logArea);
        logScroll.setBackground(new Color(12,16,28));
        logScroll.setBorder(BorderFactory.createLineBorder(new Color(40,55,80),1));
        styleScrollBar(logScroll.getVerticalScrollBar());
        right.add(logScroll);
        add(right, BorderLayout.EAST);
        //trick
        new Timer(1000,e ->tickTimers()).start();
        setLocationRelativeTo(null);
    }

    //public API
    public void addOrder(Order order){
        SwingUtilities.invokeLater(()->{
            int row =tableModel.getRowCount();
            rows.put(order.getOrderId(), row);
            tableModel.addRow(new Object[]{
                    shortId(order.getOrderId()),
                    "—",
                    statusLabel(order.getStatus()),
                    order.getAgeSeconds()+"s"
            });
            active++;
            activeLabel.setText(String.valueOf(active));
        });
    }

    public void updateOrderStatus(Order order){
        SwingUtilities.invokeLater(()->{
            Integer row = rows.get(order.getOrderId());
            if(row==null)
                return;
            String agentLabel=order.getAssignedAgent() !=null
                    ? order.getAssignedAgent().replace("Delivery-","D"):"—";
            tableModel.setValueAt(agentLabel,row, 1);
            tableModel.setValueAt(statusLabel(order.getStatus()), row,2);
        });
    }
    public void incrementDelivered(){
        done++;
        active =Math.max(0,active-1);
        SwingUtilities.invokeLater(()->{
            deliveredLabel.setText(String.valueOf(done));
            activeLabel.setText(String.valueOf(active));
        });
    }

    public void log(String message){
        String time =new java.text.SimpleDateFormat("HH:mm:ss").format(new java.util.Date());
        SwingUtilities.invokeLater(()->{
            logArea.append("[" +time +"] "+ message+"\n");
            logArea.setCaretPosition(logArea.getDocument().getLength());
        });
    }
    public MapPanel getMapPanel() {
        return mapPanel;
    }

    private void tickTimers(){
        for (Map.Entry<String, Integer> e : rows.entrySet()) {
            int row = e.getValue();
            if(row>= tableModel.getRowCount())
                continue;
            Object status = tableModel.getValueAt(row,2);
            if(status != null && status.toString().contains("DONE"))
                continue;
            Object val =tableModel.getValueAt(row,3);
            if(val == null)
                continue;
            try{
                int secs =Integer.parseInt(val.toString().replace("s",""))+1;
                tableModel.setValueAt(secs+"s",row,3);
            } catch(NumberFormatException ignored) {}
        }
    }

    private String shortId(String orderId){
        return orderId.replace("ORD-","#");
    }
    private String statusLabel(Order.Status status){
        return switch (status) {
            case PENDING->"[ PENDING ]";
            case ASSIGNED->"[ ASSIGNED ]";
            case IN_TRANSIT->"[ EN ROUTE ]";
            case DELIVERED->"[ DONE ]";
        };
    }

    private void styleTable(JTable table){
        table.setBackground(BG_CARD);
        table.setForeground(TEXT_PRIMARY);
        table.setFont(new Font("Segoe UI", Font.BOLD, 14));
        table.setRowHeight(34);
        table.setShowGrid(false);
        table.setIntercellSpacing(new Dimension(0,2));
        table.setSelectionBackground(new Color(40,60,100));
        table.setSelectionForeground(Color.WHITE);

        JTableHeader header =table.getTableHeader();
        header.setBackground(BG_HEADER);
        header.setForeground(TEXT_DIM);
        header.setFont(new Font("Segoe UI", Font.BOLD, 13));
        header.setPreferredSize(new Dimension(0, 30));
        header.setBorder(BorderFactory.createEmptyBorder());

        int[] widths ={72,60,150,50};
        for (int i=0;i< widths.length; i++){
            table.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);
        }
        DefaultTableCellRenderer boldCenter =new DefaultTableCellRenderer();
        boldCenter.setHorizontalAlignment(SwingConstants.CENTER);
        boldCenter.setBackground(BG_CARD);
        boldCenter.setForeground(ACCENT_BLUE);
        boldCenter.setFont(new Font("Consolas", Font.BOLD, 14));
        table.getColumnModel().getColumn(0).setCellRenderer(boldCenter);
        DefaultTableCellRenderer agentRenderer = new DefaultTableCellRenderer();
        agentRenderer.setHorizontalAlignment(SwingConstants.CENTER);
        agentRenderer.setBackground(BG_CARD);
        agentRenderer.setForeground(new Color(226, 232, 240));
        agentRenderer.setFont(new Font("Segoe UI", Font.BOLD, 13));
        table.getColumnModel().getColumn(1).setCellRenderer(agentRenderer);

        table.getColumnModel().getColumn(2).setCellRenderer(new DefaultTableCellRenderer(){
            @Override
            public Component getTableCellRendererComponent(JTable t, Object val,boolean sel,boolean focus,int row,int col){
                super.getTableCellRendererComponent(t,val,sel, focus,row,col);
                setHorizontalAlignment(SwingConstants.CENTER);
                setFont(new Font("Segoe UI", Font.BOLD,12));
                String s =val == null ? "":val.toString();
                if(s.contains("PENDING")){ setForeground(ACCENT_AMBER);setBackground(new Color(40,35,15));}
                else if(s.contains("ASSIGNED")) { setForeground(ACCENT_BLUE);setBackground(new Color(15, 30, 50));}
                else if(s.contains("EN ROUTE")) { setForeground(new Color(167,139,250));setBackground(new Color(30,15,50));}
                else if(s.contains("DONE")) { setForeground(ACCENT_GREEN);setBackground(new Color(15,40,30)); }
                else{ setForeground(TEXT_DIM);setBackground(BG_CARD); }
                setBorder(BorderFactory.createEmptyBorder(0,6,0,6));
                return this;
            }
        });

        DefaultTableCellRenderer ageRenderer =new DefaultTableCellRenderer();
        ageRenderer.setHorizontalAlignment(SwingConstants.CENTER);
        ageRenderer.setBackground(BG_CARD);
        ageRenderer.setForeground(TEXT_DIM);
        ageRenderer.setFont(new Font("Consolas",Font.PLAIN,12));
        table.getColumnModel().getColumn(3).setCellRenderer(ageRenderer);
    }

    private JLabel statCard(String value,String label,Color accent){
        JLabel lbl =new JLabel(value, SwingConstants.CENTER);
        lbl.setForeground(accent);
        lbl.setFont(new Font("Segoe UI",Font.BOLD,26));
        return lbl;
    }

    private JPanel wrapStat(JLabel valueLabel,String caption,Color accent){
        JPanel card=new JPanel(new BorderLayout());
        card.setBackground(BG_CARD);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(2,0,0,0,accent),
                new EmptyBorder(6,4,6,4)));

        JLabel cap= new JLabel(caption, SwingConstants.CENTER);
        cap.setForeground(TEXT_DIM);
        cap.setFont(new Font("Segoe UI", Font.PLAIN,10));

        card.add(valueLabel,BorderLayout.CENTER);
        card.add(cap, BorderLayout.SOUTH);
        return card;
    }

    private void styleScrollBar(JScrollBar bar){
        bar.setBackground(new Color(18, 22,36));
        bar.setForeground(new Color(18, 22,36));
        bar.setUI(new javax.swing.plaf.basic.BasicScrollBarUI(){
            @Override
            protected void configureScrollBarColors(){
                thumbColor= new Color(55, 70,100);
                trackColor= new Color(18, 22, 36);
                thumbDarkShadowColor= new Color(18,22,36);
                thumbHighlightColor= new Color(18,22,36);
                thumbLightShadowColor= new Color(18,22,36);
            }

            @Override protected JButton createDecreaseButton(int o){
                return zeroButton();
            }
            @Override protected JButton createIncreaseButton(int o){
                return zeroButton();
            }
            private JButton zeroButton(){
                JButton b =new JButton();
                b.setPreferredSize(new Dimension(0,0));
                b.setMinimumSize(new Dimension(0,0));
                b.setMaximumSize(new Dimension(0,0));
                return b;
            }
        });
    }
    private JLabel sectionLabel(String text){
        JLabel lbl = new JLabel(text);
        lbl.setForeground(TEXT_DIM);
        lbl.setFont(new Font("Segoe UI", Font.BOLD,10));
        lbl.setAlignmentX(0f);
        return lbl;
    }

    private JPanel legendRow(Color dotColor, String text){
        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT,6,1));
        row.setBackground(BG_DARK);
        row.setMaximumSize(new Dimension(420,22));
        JLabel dot = new JLabel("●");
        dot.setForeground(dotColor);
        dot.setFont(new Font("Segoe UI",Font.PLAIN,14));
        JLabel lbl = new JLabel(text);
        lbl.setForeground(TEXT_DIM);
        lbl.setFont(new Font("Segoe UI",Font.PLAIN,11));
        row.add(dot);
        row.add(lbl);
        return row;
    }
}