package com.smartdelivery.gui;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.RoundRectangle2D;
import java.util.List;

public class SummaryDialog extends JDialog {
    private static final Color BG     = new Color(10, 12, 20);
    private static final Color CARD   = new Color(26, 32, 52);
    private static final Color CARD2  = new Color(20, 26, 44);
    private static final Color EDGE   = new Color(40, 55, 80);
    private static final Color BLUE   = new Color(56, 189, 248);
    private static final Color GREEN  = new Color(52, 211, 153);
    private static final Color AMBER  = new Color(251, 191, 36);
    private static final Color RED    = new Color(248, 113, 113);
    private static final Color PURPLE = new Color(167, 139, 250);
    private static final Color TEXT   = new Color(226, 232, 240);
    private static final Color MUTED  = new Color(100, 116, 139);

    public static class Stats {
        public final int    totalDelivered;
        public final int    totalAgents;
        public final long   elapsedSeconds;
        public final double totalEarnings;
        public final double avgPrice;
        public final double highestPrice;
        public final double lowestPrice;
        public final double avgTrust;
        public final int    onTimeCount;
        public final int    lateCount;
        public final List<String>  agentNames;
        public final List<Integer> agentDeliveries;
        public final List<Double>  agentEarnings;
        public final List<Double>  agentTrusts;

        public Stats(int totalDelivered, int totalAgents, long elapsedSeconds,
                     double totalEarnings, double avgPrice, double highestPrice, double lowestPrice,
                     double avgTrust, int onTimeCount, int lateCount,
                     List<String> agentNames, List<Integer> agentDeliveries,
                     List<Double> agentEarnings, List<Double> agentTrusts) {
            this.totalDelivered  = totalDelivered;
            this.totalAgents     = totalAgents;
            this.elapsedSeconds  = elapsedSeconds;
            this.totalEarnings   = totalEarnings;
            this.avgPrice        = avgPrice;
            this.highestPrice    = highestPrice;
            this.lowestPrice     = lowestPrice;
            this.avgTrust        = avgTrust;
            this.onTimeCount     = onTimeCount;
            this.lateCount       = lateCount;
            this.agentNames      = agentNames;
            this.agentDeliveries = agentDeliveries;
            this.agentEarnings   = agentEarnings;
            this.agentTrusts     = agentTrusts;
        }
    }

    public SummaryDialog(Frame owner, Stats s) {
        super(owner, "Simulation Summary", true);
        setUndecorated(true);
        setBackground(new Color(0, 0, 0, 0));

        JPanel root = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(BG);
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 18, 18));
                g2.setColor(EDGE);
                g2.draw(new RoundRectangle2D.Float(0, 0, getWidth()-1, getHeight()-1, 18, 18));
                g2.dispose();
            }
        };
        root.setLayout(new BoxLayout(root, BoxLayout.Y_AXIS));
        root.setOpaque(false);
        root.setBorder(new EmptyBorder(28, 30, 28, 30));

        root.add(header());
        root.add(Box.createVerticalStrut(20));
        root.add(topStats(s));
        root.add(Box.createVerticalStrut(14));
        root.add(earningsStats(s));
        root.add(Box.createVerticalStrut(14));
        root.add(agentBreakdown(s));
        root.add(Box.createVerticalStrut(24));
        root.add(closeBtn(owner));

        setContentPane(root);
        pack();
        setSize(600, getHeight());
        setLocationRelativeTo(owner);

        Point[] grab = {null};
        root.addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent e) { grab[0] = e.getPoint(); }
        });
        root.addMouseMotionListener(new MouseMotionAdapter() {
            public void mouseDragged(MouseEvent e) {
                if (grab[0] == null) return;
                Point p = getLocation();
                setLocation(p.x + e.getX() - grab[0].x, p.y + e.getY() - grab[0].y);
            }
        });
    }

    private JPanel header() {
        JPanel h = new JPanel();
        h.setLayout(new BoxLayout(h, BoxLayout.Y_AXIS));
        h.setOpaque(false);
        JLabel icon = new JLabel("✦", SwingConstants.CENTER);
        icon.setForeground(BLUE);
        icon.setFont(new Font("Segoe UI Symbol", Font.PLAIN, 32));
        icon.setAlignmentX(CENTER_ALIGNMENT);
        JLabel title = new JLabel("Simulation Report", SwingConstants.CENTER);
        title.setForeground(TEXT);
        title.setFont(new Font("Segoe UI", Font.BOLD, 24));
        title.setAlignmentX(CENTER_ALIGNMENT);
        h.add(icon);
        h.add(Box.createVerticalStrut(6));
        h.add(title);
        return h;
    }

    private JPanel topStats(Stats s) {
        JPanel row = new JPanel(new GridLayout(1, 3, 15, 0));
        row.setOpaque(false);
        row.setMaximumSize(new Dimension(540, 85));
        row.add(bigStat(String.valueOf(s.totalDelivered), "Deliveries", GREEN));
        row.add(bigStat(formatTime(s.elapsedSeconds), "Duration", AMBER)); // Yellow for Time
        row.add(bigStat(String.format("%.0f", s.totalEarnings), "Total DZD", PURPLE)); // Purple for Earnings
        return row;
    }

    private JPanel earningsStats(Stats s) {
        JPanel card = card();
        card.add(sectionLbl("PERFORMANCE BREAKDOWN"));
        card.add(Box.createVerticalStrut(10));

        // Earnings related in Purple
        card.add(statRow("Total Earnings", String.format("%.0f DZD", s.totalEarnings), PURPLE));
        card.add(statRow("Avg. Price / Order", String.format("%.1f DZD", s.avgPrice), PURPLE));
        card.add(statRow("Highest Order", String.format("%.1f DZD", s.highestPrice), PURPLE));
        card.add(statRow("Lowest Order", String.format("%.1f DZD", s.lowestPrice), PURPLE));

        // Trust Level in Blue with big T
        card.add(statRow("Trust Level Average", String.format("%.1f", s.avgTrust), BLUE));

        // Others
        card.add(statRow("On Time Deliveries", String.valueOf(s.onTimeCount), GREEN));
        card.add(statRow("Late Deliveries", String.valueOf(s.lateCount), RED));
        return card;
    }

    private JPanel agentBreakdown(Stats s) {
        JPanel card = card();
        card.add(sectionLbl("AGENT PERFORMANCE LEADERBOARD"));
        card.add(Box.createVerticalStrut(12));

        // Manual loop fix for the .stream() error
        int best = 1;
        if (s.agentDeliveries != null && !s.agentDeliveries.isEmpty()) {
            for (Integer count : s.agentDeliveries) {
                if (count > best) best = count;
            }
        }

        Color[] colors = { RED, GREEN, PURPLE, BLUE, AMBER, new Color(236, 72, 153), new Color(34, 211, 238) };

        for (int i = 0; i < s.agentNames.size(); i++) {
            String name = s.agentNames.get(i).replace("Delivery-", "D");
            int count = s.agentDeliveries.get(i);
            double earn = s.agentEarnings.get(i);
            double trust = s.agentTrusts.get(i);
            Color col = colors[i % colors.length];

            card.add(agentBar(name, count, best, earn, trust, col));
            if (i < s.agentNames.size() - 1) card.add(Box.createVerticalStrut(8));
        }
        return card;
    }

    private JPanel agentBar(String name, int count, int best, double earn, double trust, Color col) {
        JPanel row = new JPanel(new BorderLayout(10, 0));
        row.setOpaque(false);
        row.setMaximumSize(new Dimension(540, 30));

        JLabel nameL = new JLabel(name);
        nameL.setForeground(col);
        nameL.setFont(new Font("Consolas", Font.BOLD, 16));
        nameL.setPreferredSize(new Dimension(40, 30));

        JPanel barContainer = new JPanel(new BorderLayout());
        barContainer.setOpaque(false);
        int fillW = (int)((double)count / best * 200);
        JPanel bar = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(30, 38, 60));
                g2.fillRoundRect(0, 10, getWidth(), 10, 6, 6);
                g2.setColor(col);
                g2.fillRoundRect(0, 10, fillW, 10, 6, 6);
                g2.dispose();
            }
        };
        bar.setOpaque(false);
        barContainer.add(bar, BorderLayout.CENTER);

        // Leaderboard font bigger and bolder. Used "T=" as requested.
        JLabel statsL = new JLabel(count + " Del | " + String.format("%.0f", earn) + " DZD | T=" + String.format("%.1f", trust));
        statsL.setForeground(TEXT);
        statsL.setFont(new Font("Consolas", Font.BOLD, 14));
        statsL.setHorizontalAlignment(SwingConstants.RIGHT);
        statsL.setPreferredSize(new Dimension(240, 30));

        row.add(nameL, BorderLayout.WEST);
        row.add(barContainer, BorderLayout.CENTER);
        row.add(statsL, BorderLayout.EAST);
        return row;
    }

    private JPanel closeBtn(Frame owner) {
        JPanel wrap = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 0));
        wrap.setOpaque(false);
        JButton again = roundBtn("RUN AGAIN", BLUE, new Color(15, 30, 50));
        JButton exit  = roundBtn("EXIT", RED, new Color(40, 20, 25));

        again.addActionListener(e -> { dispose(); owner.dispose(); });
        exit.addActionListener(e -> System.exit(0));

        wrap.add(again);
        wrap.add(exit);
        return wrap;
    }

    private JButton roundBtn(String label, Color fg, Color bg) {
        JButton b = new JButton(label) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(getModel().isRollover() ? fg : bg);
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 10, 10));
                g2.setColor(getModel().isRollover() ? BG : fg);
                g2.setFont(getFont());
                FontMetrics fm = g2.getFontMetrics();
                g2.drawString(getText(), (getWidth()-fm.stringWidth(getText()))/2, (getHeight()+fm.getAscent()-fm.getDescent())/2);
                g2.dispose();
            }
        };
        b.setFont(new Font("Segoe UI", Font.BOLD, 14));
        b.setPreferredSize(new Dimension(140, 42));
        b.setBorderPainted(false);
        b.setContentAreaFilled(false);
        b.setFocusPainted(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return b;
    }

    private JPanel bigStat(String value, String label, Color accent) {
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setOpaque(false);
        JLabel val = new JLabel(value, SwingConstants.CENTER);
        val.setForeground(accent);
        val.setFont(new Font("Segoe UI", Font.BOLD, 30));
        val.setAlignmentX(CENTER_ALIGNMENT);
        JLabel lbl = new JLabel(label, SwingConstants.CENTER);
        lbl.setForeground(MUTED);
        lbl.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lbl.setAlignmentX(CENTER_ALIGNMENT);
        p.add(val); p.add(lbl);
        return p;
    }

    private JPanel statRow(String label, String value, Color accent) {
        JPanel row = new JPanel(new BorderLayout());
        row.setOpaque(false);
        row.setMaximumSize(new Dimension(540, 26));
        JLabel l = new JLabel(label);
        l.setForeground(MUTED);
        l.setFont(new Font("Segoe UI", Font.PLAIN, 15));
        JLabel v = new JLabel(value, SwingConstants.RIGHT);
        v.setForeground(accent);
        v.setFont(new Font("Consolas", Font.BOLD, 16));
        row.add(l, BorderLayout.WEST);
        row.add(v, BorderLayout.EAST);
        return row;
    }

    private JPanel card() {
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setBackground(CARD2);
        p.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(EDGE, 1),
                new EmptyBorder(12, 16, 12, 16)));
        p.setAlignmentX(CENTER_ALIGNMENT);
        p.setMaximumSize(new Dimension(540, 400));
        return p;
    }

    private JLabel sectionLbl(String txt) {
        JLabel l = new JLabel(txt);
        l.setForeground(MUTED);
        l.setFont(new Font("Segoe UI", Font.BOLD, 11));
        return l;
    }

    // THE MISSING METHOD IS BACK!
    private String formatTime(long secs) {
        return String.format("%02d:%02d", secs / 60, secs % 60);
    }

    public static void displayReport(Frame owner, Stats s) {
        new SummaryDialog(owner, s).setVisible(true);
    }
}