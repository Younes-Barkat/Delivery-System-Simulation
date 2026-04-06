package com.smartdelivery.gui;

import com.smartdelivery.model.Order;
import javax.swing.table.JTableHeader;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;
import java.util.LinkedHashMap;
import java.util.ArrayList;
import java.util.List;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import java.util.Map;

public class SimulationWindow extends JFrame {
    private static final Color BG_DARKEST   = new Color(10, 12, 20);
    private static final Color BG_DARK      = new Color(18, 22, 36);
    private static final Color BG_CARD      = new Color(26, 32, 52);
    private static final Color BG_HEADER    = new Color(15, 19, 32);
    private static final Color ACCENT_BLUE  = new Color(56, 189, 248);
    private static final Color ACCENT_GREEN = new Color(52, 211, 153);
    private static final Color ACCENT_RED   = new Color(248, 113, 113);
    private static final Color ACCENT_AMBER = new Color(251, 191, 36);
    private static final Color ACCENT_PURPLE= new Color(167, 139, 250);
    private static final Color TEXT_PRIMARY = new Color(226, 232, 240);
    private static final Color TEXT_DIM     = new Color(100, 116, 139);

    private final MapPanel mapPanel;
    private final JTextArea logArea;
    private final JLabel deliveredLabel;
    private final JLabel activeLabel;
    private final JLabel agentsLabel;
    private final JLabel earningsLabel;

    private final DefaultTableModel tableModel;
    private final Map<String, Integer> rows        = new LinkedHashMap<>();
    private final Map<String, Double>  orderPrices = new LinkedHashMap<>();
    private int done   = 0;
    private int active = 0;

    private final long   startTime = System.currentTimeMillis();
    private double totalWait = 0;
    private double totalDelivery  = 0;
    private long fastest   = Long.MAX_VALUE;
    private long slowest= -1;
    private int numAgents = 0;
    private double totalEarnings = 0;
    private final Map<String, Integer> agentScores   = new LinkedHashMap<>();
    private final Map<String, Double>  agentTrust = new LinkedHashMap<>();
    private final Map<String, Double>  agentEarnings = new LinkedHashMap<>();
    private double highestPrice = -1;
    private double lowestPrice  = Double.MAX_VALUE;

    private JPanel     trustPanelRef;
    private JScrollPane trustScroll;

    public SimulationWindow() {
        super("Smart Delivery System -M'sila-");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1380, 860);
        setMinimumSize(new Dimension(1100, 700));
        setLayout(new BorderLayout(0, 0));
        getContentPane().setBackground(BG_DARKEST);

        JPanel titleBar = new JPanel(new BorderLayout());
        titleBar.setBackground(BG_DARKEST);
        titleBar.setBorder(new EmptyBorder(10, 16, 10, 16));
        JLabel title = new JLabel("Smart Delivery System |Agent-Based Simulation |M'sila, Algeria");
        title.setForeground(TEXT_PRIMARY);
        title.setFont(new Font("Segoe UI", Font.BOLD, 17));
        titleBar.add(title, BorderLayout.WEST);

        JButton stopBtn = new JButton("■  END SIMULATION") {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                Color base = new Color(180, 30, 50);
                g2.setColor(getModel().isRollover() ? new Color(220, 50, 70) : base);
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 8, 8));
                g2.setColor(new Color(255, 200, 200));
                g2.setFont(getFont());
                FontMetrics fm = g2.getFontMetrics();
                g2.drawString(getText(),
                        (getWidth() - fm.stringWidth(getText())) / 2,
                        (getHeight() + fm.getAscent() - fm.getDescent()) / 2);
                g2.dispose();
            }
        };
        stopBtn.setFont(new Font("Segoe UI", Font.BOLD, 12));
        stopBtn.setPreferredSize(new Dimension(160, 30));
        stopBtn.setBorderPainted(false);
        stopBtn.setContentAreaFilled(false);
        stopBtn.setFocusPainted(false);
        stopBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        stopBtn.addActionListener(e -> showSummary());
        titleBar.add(stopBtn, BorderLayout.EAST);
        add(titleBar, BorderLayout.NORTH);

        mapPanel = new MapPanel();
        add(mapPanel, BorderLayout.CENTER);

        JPanel right = new JPanel();
        right.setLayout(new BoxLayout(right, BoxLayout.Y_AXIS));
        right.setPreferredSize(new Dimension(420, 860));
        right.setBackground(BG_DARK);
        right.setBorder(new EmptyBorder(12, 12, 12, 12));

        JPanel statsRow = new JPanel(new GridLayout(1, 4, 6, 0));
        statsRow.setMaximumSize(new Dimension(420, 72));
        statsRow.setBackground(BG_DARK);

        deliveredLabel = statCard("0", "Delivered", ACCENT_GREEN);
        activeLabel    = statCard("0", "Active", ACCENT_AMBER);
        agentsLabel    = statCard("0", "Agents",ACCENT_BLUE);
        earningsLabel  = statCard("0", "DZD", ACCENT_PURPLE);

        statsRow.add(wrapStat(deliveredLabel, "Delivered", ACCENT_GREEN));
        statsRow.add(wrapStat(activeLabel,"Active", ACCENT_AMBER));
        statsRow.add(wrapStat(agentsLabel,"Agents",ACCENT_BLUE));
        statsRow.add(wrapStat(earningsLabel,"Earnings",  ACCENT_PURPLE));
        right.add(statsRow);
        right.add(Box.createVerticalStrut(14));

        right.add(sectionLabel("LIVE ORDER STATUS"));
        right.add(Box.createVerticalStrut(4));

        String[] cols = {"Order ID", "Agent", "Status", "Price"};
        tableModel = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };

        JTable table = new JTable(tableModel);
        styleTable(table);
        JScrollPane tableScroll = new JScrollPane(table);
        tableScroll.setBackground(BG_CARD);
        tableScroll.getViewport().setBackground(BG_CARD);
        tableScroll.setBorder(BorderFactory.createLineBorder(new Color(40, 55, 80), 1));
        Dimension tableFixedSize = new Dimension(396, 300);
        tableScroll.setPreferredSize(tableFixedSize);
        tableScroll.setMinimumSize(tableFixedSize);
        tableScroll.setMaximumSize(new Dimension(420, 300));
        styleScrollBar(tableScroll.getVerticalScrollBar());
        styleScrollBar(tableScroll.getHorizontalScrollBar());
        tableScroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        tableScroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        right.add(tableScroll);
        right.add(Box.createVerticalStrut(10));

        right.add(sectionLabel("AGENT TRUST LEVELS"));
        right.add(Box.createVerticalStrut(4));
        buildTrustPanel();
        right.add(trustScroll);
        right.add(Box.createVerticalStrut(10));

        right.add(sectionLabel("EVENT LOG"));
        right.add(Box.createVerticalStrut(4));
        logArea = new JTextArea();
        logArea.setEditable(false);
        logArea.setBackground(new Color(12, 16, 28));
        logArea.setForeground(new Color(148, 163, 184));
        logArea.setFont(new Font("Consolas", Font.PLAIN, 11));
        logArea.setLineWrap(true);
        logArea.setWrapStyleWord(true);
        logArea.setBorder(new EmptyBorder(6, 6, 6, 6));
        JScrollPane logScroll = new JScrollPane(logArea);
        logScroll.setBackground(new Color(12, 16, 28));
        logScroll.setBorder(BorderFactory.createLineBorder(new Color(40, 55, 80), 1));
        styleScrollBar(logScroll.getVerticalScrollBar());
        right.add(logScroll);

        add(right, BorderLayout.EAST);
        setLocationRelativeTo(null);
    }

    private void buildTrustPanel() {
        trustPanelRef = new JPanel();
        trustPanelRef.setLayout(new BoxLayout(trustPanelRef, BoxLayout.Y_AXIS));
        trustPanelRef.setBackground(new Color(18, 22, 36));
        trustPanelRef.setBorder(new EmptyBorder(6, 10, 6, 10));

        trustScroll = new JScrollPane(trustPanelRef);
        trustScroll.setBackground(new Color(18, 22, 36));
        trustScroll.getViewport().setBackground(new Color(18, 22, 36));
        trustScroll.setBorder(BorderFactory.createLineBorder(new Color(40, 55, 80), 1));
        int fixedH = 21 * 5 + 12;
        Dimension d = new Dimension(396, fixedH);
        trustScroll.setPreferredSize(d);
        trustScroll.setMinimumSize(d);
        trustScroll.setMaximumSize(new Dimension(420, fixedH));
        trustScroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        trustScroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        styleScrollBar(trustScroll.getVerticalScrollBar());
    }

    private void refreshTrustPanel() {
        if (trustPanelRef == null) return;
        SwingUtilities.invokeLater(() -> {
            trustPanelRef.removeAll();
            Color[] agentColors = {
                    new Color(239, 68, 68),  new Color(0, 200, 80),    new Color(160, 32, 240),
                    new Color(34, 211, 238), new Color(249, 115, 22),  new Color(236, 72, 153),
                    new Color(132, 204, 22), new Color(56, 189, 248),  new Color(251, 113, 133),
                    new Color(52, 211, 153)
            };
            int i = 0;
            for (Map.Entry<String, Double> e : agentTrust.entrySet()) {
                String name  = e.getKey().replace("Delivery-", "D");
                double trust = e.getValue();
                Color col    = agentColors[i % agentColors.length];

                JPanel row = new JPanel(new BorderLayout(6, 0));
                row.setOpaque(false);
                row.setMaximumSize(new Dimension(400, 18));

                JLabel nameL = new JLabel(name);
                nameL.setForeground(col);
                nameL.setFont(new Font("Consolas", Font.BOLD, 11));
                nameL.setPreferredSize(new Dimension(28, 16));

                JPanel barBg = new JPanel(new BorderLayout());
                barBg.setBackground(new Color(15, 20, 35));
                barBg.setBorder(BorderFactory.createLineBorder(new Color(40, 55, 80), 1));
                int fillW = (int) (trust / 150.0 * 280);
                Color barColor = trust >= 120 ? ACCENT_GREEN : trust >= 80 ? ACCENT_BLUE : ACCENT_RED;
                JPanel fill = new JPanel() {
                    @Override protected void paintComponent(Graphics g) {
                        Graphics2D g2 = (Graphics2D) g.create();
                        g2.setColor(new Color(barColor.getRed(), barColor.getGreen(), barColor.getBlue(), 170));
                        g2.fillRect(0, 0, getWidth(), getHeight());
                        g2.dispose();
                    }
                };
                fill.setPreferredSize(new Dimension(fillW, 14));
                fill.setOpaque(false);
                barBg.add(fill, BorderLayout.WEST);

                JLabel valL = new JLabel(String.format("%.0f", trust), SwingConstants.RIGHT);
                valL.setForeground(barColor);
                valL.setFont(new Font("Consolas", Font.BOLD, 11));
                valL.setPreferredSize(new Dimension(34, 16));

                row.add(nameL, BorderLayout.WEST);
                row.add(barBg, BorderLayout.CENTER);
                row.add(valL, BorderLayout.EAST);
                trustPanelRef.add(row);
                if (i < agentTrust.size() - 1) trustPanelRef.add(Box.createVerticalStrut(3));
                i++;
            }
            trustPanelRef.revalidate();
            trustPanelRef.repaint();
        });
    }

    public void addOrder(Order order) {
        SwingUtilities.invokeLater(() -> {
            int row = tableModel.getRowCount();
            rows.put(order.getOrderId(), row);
            tableModel.addRow(new Object[]{
                    shortId(order.getOrderId()),
                    "—",
                    statusLabel(order.getStatus()),
                    "—"
            });
            active++;
            activeLabel.setText(String.valueOf(active));
        });
    }

    public void updateOrderStatus(Order order) {
        SwingUtilities.invokeLater(() -> {
            Integer row = rows.get(order.getOrderId());
            if (row == null) return;
            String agentLabel = order.getAssignedAgent() != null
                    ? order.getAssignedAgent().replace("Delivery-", "D") : "—";
            tableModel.setValueAt(agentLabel, row, 1);
            tableModel.setValueAt(statusLabel(order.getStatus()), row, 2);
            Double price = orderPrices.get(order.getOrderId());
            if (price != null)
                tableModel.setValueAt(String.format("%.0f DZD", price), row, 3);
        });
    }

    public void setOrderPrice(String orderId, double price) {
        orderPrices.put(orderId, price);
        SwingUtilities.invokeLater(() -> {
            Integer row = rows.get(orderId);
            if (row != null)
                tableModel.setValueAt(String.format("%.0f DZD", price), row, 3);
        });
    }

    public void updateAgentTrust(String agentName, double trust) {
        agentTrust.put(agentName, trust);
        refreshTrustPanel();
    }

    public void incrementDelivered() {
        done++;
        active = Math.max(0, active - 1);
        SwingUtilities.invokeLater(() -> {
            deliveredLabel.setText(String.valueOf(done));
            activeLabel.setText(String.valueOf(active));
        });
    }

    public void recordDelivery(Order order) {
        long wait  = order.getWaitTimeSeconds();
        long total = order.getTotalTimeSeconds();
        String agent = order.getAssignedAgent();

        if (wait >= 0)  totalWait     += wait;
        if (total >= 0) totalDelivery += total;
        if (total >= 0 && total < fastest) fastest = total;
        if (total > slowest) slowest = total;
        if (agent != null) agentScores.merge(agent, 1, Integer::sum);

        Double price = orderPrices.get(order.getOrderId());
        if (price != null) {
            totalEarnings += price;
            if (price > highestPrice) highestPrice = price;
            if (price < lowestPrice)  lowestPrice  = price;
            if (agent != null) agentEarnings.merge(agent, price, Double::sum);
            SwingUtilities.invokeLater(() ->
                    earningsLabel.setText(String.format("%.0f", totalEarnings)));
        }
        incrementDelivered();
    }

    public void log(String message) {
        String time = new java.text.SimpleDateFormat("HH:mm:ss").format(new java.util.Date());
        SwingUtilities.invokeLater(() -> {
            logArea.append("[" + time + "] " + message + "\n");
            logArea.setCaretPosition(logArea.getDocument().getLength());
        });
    }

    public MapPanel getMapPanel() { return mapPanel; }

    public void setAgentCount(int n) {
        numAgents = n;
        for (int i = 1; i <= n; i++) {
            agentScores.put("Delivery-" + i, 0);
            agentTrust.put("Delivery-" + i, 100.0);
            agentEarnings.put("Delivery-" + i, 0.0);
        }
        refreshTrustPanel();
        SwingUtilities.invokeLater(() -> agentsLabel.setText(String.valueOf(n)));
    }

    private void showSummary() {
        long elapsed   = (System.currentTimeMillis() - startTime) / 1000;
        double avgPrice = done > 0 ? totalEarnings / done : 0;
        double highest  = highestPrice < 0 ? 0 : highestPrice;
        double lowest   = lowestPrice == Double.MAX_VALUE ? 0 : lowestPrice;

        double trustSum = agentTrust.values().stream().mapToDouble(Double::doubleValue).sum();
        double avgTrust = agentTrust.isEmpty() ? 100 : trustSum / agentTrust.size();

        List<String>  names      = new ArrayList<>(agentScores.keySet());
        List<Integer> deliveries = new ArrayList<>(agentScores.values());
        List<Double>  earnings   = new ArrayList<>(agentEarnings.values());
        List<Double>  trusts     = new ArrayList<>(agentTrust.values());

        SummaryDialog.Stats stats = new SummaryDialog.Stats(
                done, numAgents, elapsed,
                totalEarnings, avgPrice, highest, lowest,
                avgTrust, names, deliveries, earnings, trusts);
        SummaryDialog.show(this, stats);
    }

    private String shortId(String orderId) { return orderId.replace("ORD-", "#"); }

    private String statusLabel(Order.Status status) {
        return switch (status) {
            case PENDING -> "[ PENDING ]";
            case ASSIGNED -> "[ ASSIGNED ]";
            case IN_TRANSIT -> "[ EN ROUTE ]";
            case DELIVERED -> "[ DONE ]";
        };
    }

    private void styleTable(JTable table) {
        table.setBackground(BG_CARD);
        table.setForeground(TEXT_PRIMARY);
        table.setFont(new Font("Segoe UI", Font.BOLD, 15));
        table.setRowHeight(34);
        table.setShowGrid(false);
        table.setIntercellSpacing(new Dimension(0, 2));
        table.setSelectionBackground(new Color(40, 60, 100));
        table.setSelectionForeground(Color.WHITE);

        JTableHeader header = table.getTableHeader();
        header.setBackground(BG_HEADER);
        header.setForeground(TEXT_DIM);
        header.setFont(new Font("Segoe UI", Font.BOLD, 14));
        header.setPreferredSize(new Dimension(0, 30));
        header.setBorder(BorderFactory.createEmptyBorder());

        int[] widths = {68, 58, 150, 80};
        for (int i = 0; i < widths.length; i++)
            table.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);

        DefaultTableCellRenderer boldCenter = new DefaultTableCellRenderer();
        boldCenter.setHorizontalAlignment(SwingConstants.CENTER);
        boldCenter.setBackground(BG_CARD);
        boldCenter.setForeground(ACCENT_BLUE);
        boldCenter.setFont(new Font("Consolas", Font.BOLD, 15));
        table.getColumnModel().getColumn(0).setCellRenderer(boldCenter);

        DefaultTableCellRenderer agentRenderer = new DefaultTableCellRenderer();
        agentRenderer.setHorizontalAlignment(SwingConstants.CENTER);
        agentRenderer.setBackground(BG_CARD);
        agentRenderer.setForeground(new Color(226, 232, 240));
        agentRenderer.setFont(new Font("Segoe UI", Font.BOLD, 14));
        table.getColumnModel().getColumn(1).setCellRenderer(agentRenderer);

        table.getColumnModel().getColumn(2).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object val, boolean sel, boolean focus, int row, int col) {
                super.getTableCellRendererComponent(t, val, sel, focus, row, col);
                setHorizontalAlignment(SwingConstants.CENTER);
                setFont(new Font("Segoe UI", Font.BOLD, 13));
                String s = val == null ? "" : val.toString();
                if      (s.contains("PENDING"))  { setForeground(ACCENT_AMBER);  setBackground(new Color(40, 35, 15)); }
                else if (s.contains("ASSIGNED")) { setForeground(ACCENT_BLUE);   setBackground(new Color(15, 30, 50)); }
                else if (s.contains("EN ROUTE")) { setForeground(ACCENT_PURPLE); setBackground(new Color(30, 15, 50)); }
                else if (s.contains("DONE"))     { setForeground(ACCENT_GREEN);  setBackground(new Color(15, 40, 30)); }
                else { setForeground(TEXT_DIM); setBackground(BG_CARD); }
                setBorder(BorderFactory.createEmptyBorder(0, 6, 0, 6));
                return this;
            }
        });

        table.getColumnModel().getColumn(3).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object val, boolean sel, boolean focus, int row, int col) {
                super.getTableCellRendererComponent(t, val, sel, focus, row, col);
                setHorizontalAlignment(SwingConstants.CENTER);
                setFont(new Font("Consolas", Font.BOLD, 12));
                setBackground(BG_CARD);
                String s = val == null ? "" : val.toString();
                setForeground(s.equals("—") ? TEXT_DIM : ACCENT_PURPLE);
                return this;
            }
        });
    }

    private JLabel statCard(String value, String label, Color accent) {
        JLabel lbl = new JLabel(value, SwingConstants.CENTER);
        lbl.setForeground(accent);
        lbl.setFont(new Font("Segoe UI", Font.BOLD, 24));
        return lbl;
    }

    private JPanel wrapStat(JLabel valueLabel, String caption, Color accent) {
        JPanel card = new JPanel(new BorderLayout());
        card.setBackground(BG_CARD);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(2, 0, 0, 0, accent),
                new EmptyBorder(6, 4, 6, 4)));
        JLabel cap = new JLabel(caption, SwingConstants.CENTER);
        cap.setForeground(TEXT_DIM);
        cap.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        card.add(valueLabel, BorderLayout.CENTER);
        card.add(cap, BorderLayout.SOUTH);
        return card;
    }

    private void styleScrollBar(JScrollBar bar) {
        bar.setBackground(new Color(18, 22, 36));
        bar.setUI(new javax.swing.plaf.basic.BasicScrollBarUI() {
            @Override
            protected void configureScrollBarColors() {
                thumbColor            = new Color(55, 70, 100);
                trackColor            = new Color(18, 22, 36);
                thumbDarkShadowColor  = new Color(18, 22, 36);
                thumbHighlightColor   = new Color(18, 22, 36);
                thumbLightShadowColor = new Color(18, 22, 36);
            }
            @Override protected JButton createDecreaseButton(int o) { return zeroButton(); }
            @Override protected JButton createIncreaseButton(int o) { return zeroButton(); }
            private JButton zeroButton() {
                JButton b = new JButton();
                b.setPreferredSize(new Dimension(0, 0));
                b.setMinimumSize(new Dimension(0, 0));
                b.setMaximumSize(new Dimension(0, 0));
                return b;
            }
        });
    }

    private JLabel sectionLabel(String text) {
        JLabel lbl = new JLabel(text);
        lbl.setForeground(TEXT_DIM);
        lbl.setFont(new Font("Segoe UI", Font.BOLD, 11));
        lbl.setAlignmentX(0f);
        return lbl;
    }
}