package com.smartdelivery.gui;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.RoundRectangle2D;
import java.util.List;

public class SummaryDialog extends JDialog {
    private static final Color BG = new Color(10,12,20);
    private static final Color CARD = new Color(26, 32, 52);
    private static final Color CARD2 = new Color(20, 26, 44);
    private static final Color EDGE = new Color(40, 55, 80);
    private static final Color BLUE = new Color(56, 189, 248);
    private static final Color GREEN = new Color(52, 211, 153);
    private static final Color AMBER = new Color(251, 191, 36);
    private static final Color RED = new Color(248, 113, 113);
    private static final Color PURPLE = new Color(167, 139, 250);
    private static final Color TEXT = new Color(226, 232, 240);
    private static final Color MUTED = new Color(100, 116, 139);

    public static class Stats {
        public final int totalDelivered;
        public final int totalAgents;
        public final long elapsedSeconds;
        public final double totalEarnings;
        public final double avgPrice;
        public final double highestPrice;
        public final double lowestPrice;
        public final double avgTrust;
        public final List<String> agentNames;
        public final List<Integer> agentDeliveries;
        public final List<Double> agentEarnings;
        public final List<Double> agentTrusts;

        public Stats(int totalDelivered, int totalAgents, long elapsedSeconds,
                     double totalEarnings, double avgPrice, double highestPrice, double lowestPrice,
                     double avgTrust,
                     List<String> agentNames, List<Integer> agentDeliveries,
                     List<Double> agentEarnings, List<Double> agentTrusts) {
            this.totalDelivered = totalDelivered;
            this.totalAgents = totalAgents;
            this.elapsedSeconds = elapsedSeconds;
            this.totalEarnings = totalEarnings;
            this.avgPrice = avgPrice;
            this.highestPrice = highestPrice;
            this.lowestPrice = lowestPrice;
            this.avgTrust = avgTrust;
            this.agentNames = agentNames;
            this.agentDeliveries = agentDeliveries;
            this.agentEarnings = agentEarnings;
            this.agentTrusts = agentTrusts;
        }
    }

    public SummaryDialog(Frame owner, Stats s) {
        super(owner, "Simulation Summary", true);
        setUndecorated(true);
        setBackground(new Color(0,0,0,0));

        JPanel root = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(BG);
                g2.fill(new RoundRectangle2D.Float(0,0,getWidth(),getHeight(),18,18));
                g2.dispose();
            }
        };
        root.setLayout(new BoxLayout(root, BoxLayout.Y_AXIS));
        root.setOpaque(false);
        root.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(EDGE,1),
                new EmptyBorder(28,36,28,36)));
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
        setMinimumSize(new Dimension(500,getHeight()));
        setLocationRelativeTo(owner);

        Point[] grab = {null};
        root.addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent e) { grab[0]=e.getPoint(); }
        });
        root.addMouseMotionListener(new MouseMotionAdapter() {
            public void mouseDragged(MouseEvent e) {
                if (grab[0]==null) return;
                Point p = getLocation();
                setLocation(p.x+e.getX()-grab[0].x, p.y+e.getY()-grab[0].y);
            }
        });
    }

    private JPanel header() {
        JPanel h = new JPanel();
        h.setLayout(new BoxLayout(h, BoxLayout.Y_AXIS));
        h.setOpaque(false);
        JLabel icon = new JLabel("◉", SwingConstants.CENTER);
        icon.setForeground(GREEN);
        icon.setFont(new Font("Segoe UI Symbol", Font.PLAIN, 32));
        icon.setAlignmentX(CENTER_ALIGNMENT);
        JLabel title = new JLabel("Simulation Complete", SwingConstants.CENTER);
        title.setForeground(TEXT);
        title.setFont(new Font("Segoe UI", Font.BOLD, 24));
        title.setAlignmentX(CENTER_ALIGNMENT);
        JLabel sub = new JLabel("M'sila • Smart Delivery • Agent-Based", SwingConstants.CENTER);
        sub.setForeground(MUTED);
        sub.setFont(new Font("Segoe UI", Font.BOLD, 12));
        sub.setAlignmentX(CENTER_ALIGNMENT);
        h.add(icon);
        h.add(Box.createVerticalStrut(6));
        h.add(title);
        h.add(Box.createVerticalStrut(3));
        h.add(sub);
        return h;
    }

    private JPanel topStats(Stats s) {
        JPanel row = new JPanel(new GridLayout(1, 3, 10, 0));
        row.setOpaque(false);
        row.setAlignmentX(CENTER_ALIGNMENT);
        row.setMaximumSize(new Dimension(460, 90));
        row.add(bigStat(String.valueOf(s.totalDelivered), "Deliveries", GREEN));
        row.add(bigStat(formatTime(s.elapsedSeconds), "Duration", BLUE));
        row.add(bigStat(String.valueOf(s.totalAgents), "Agents", PURPLE));
        return row;
    }

    private JPanel earningsStats(Stats s) {
        JPanel card = card();
        card.add(sectionLbl("PERFORMANCE BREAKDOWN"));
        card.add(Box.createVerticalStrut(12));
        double rate = s.elapsedSeconds > 0 ? (double)s.totalDelivered/(s.elapsedSeconds/60.0) : 0;
        card.add(statRow("Total earnings", String.format("%.0f DZD", s.totalEarnings), GREEN));
        card.add(Box.createVerticalStrut(5));
        card.add(statRow("Avg. price / order", String.format("%.1f DZD", s.avgPrice), AMBER));
        card.add(Box.createVerticalStrut(5));
        card.add(statRow("Highest order price", String.format("%.1f DZD", s.highestPrice), BLUE));
        card.add(Box.createVerticalStrut(5));
        card.add(statRow("Lowest order price", String.format("%.1f DZD", s.lowestPrice), RED));
        card.add(Box.createVerticalStrut(5));
        card.add(statRow("Avg. agent trust", String.format("%.1f", s.avgTrust), PURPLE));
        card.add(Box.createVerticalStrut(5));
        card.add(statRow("Throughput", String.format("%.1f", rate) + " / min", MUTED));
        return card;
    }

    private JPanel agentBreakdown(Stats s) {
        JPanel card = card();
        card.add(sectionLbl("AGENT LEADERBOARD"));
        card.add(Box.createVerticalStrut(12));
        int best = s.agentDeliveries.stream().mapToInt(Integer::intValue).max().orElse(1);
        Color[] colors = {
                new Color(239, 68, 68), new Color(0, 200, 80), new Color(160, 32, 240),
                new Color(34, 211, 238), new Color(249, 115, 22), new Color(236, 72, 153),
                new Color(132, 204, 22), new Color(56, 189, 248), new Color(251, 113, 133),
                new Color(52, 211, 153)
        };
        for (int i=0; i<s.agentNames.size(); i++) {
            String name = s.agentNames.get(i).replace("Delivery-", "D");
            int count = s.agentDeliveries.get(i);
            double earn = (s.agentEarnings!=null && i<s.agentEarnings.size()) ? s.agentEarnings.get(i) : 0;
            double trust = (s.agentTrusts!=null && i<s.agentTrusts.size()) ? s.agentTrusts.get(i) : 100;
            Color col = colors[i % colors.length];
            card.add(agentBar(name, count, best, earn, trust, col));
            if (i < s.agentNames.size()-1) card.add(Box.createVerticalStrut(5));
        }
        return card;
    }

    private JPanel agentBar(String name, int count, int best, double earn, double trust, Color col) {
        JPanel row = new JPanel(new BorderLayout(8, 0));
        row.setOpaque(false);
        row.setMaximumSize(new Dimension(440, 28));

        JLabel nameL = new JLabel(name);
        nameL.setForeground(col);
        nameL.setFont(new Font("Consolas", Font.BOLD, 12));
        nameL.setPreferredSize(new Dimension(36, 20));

        JPanel barWrap = new JPanel(new BorderLayout());
        barWrap.setBackground(new Color(15, 20, 35));
        barWrap.setBorder(BorderFactory.createLineBorder(EDGE,1));
        int fillW = best > 0 ? (int)((double)count/best*300) : 0;
        JPanel fill = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setColor(new Color(col.getRed(), col.getGreen(), col.getBlue(), 180));
                g2.fillRect(0,0,getWidth(),getHeight());
                g2.dispose();
            }
        };
        fill.setPreferredSize(new Dimension(fillW, 18));
        fill.setOpaque(false);
        barWrap.add(fill, BorderLayout.WEST);

        Color trustColor = trust>=120 ? GREEN : trust>=80 ? BLUE : RED;
        JLabel infoL = new JLabel(count + " del | " + String.format("%.0f", earn) + " DZD | t=" + String.format("%.0f", trust), SwingConstants.RIGHT);
        infoL.setForeground(trustColor);
        infoL.setFont(new Font("Consolas", Font.PLAIN, 10));
        infoL.setPreferredSize(new Dimension(150, 20));

        row.add(nameL, BorderLayout.WEST);
        row.add(barWrap, BorderLayout.CENTER);
        row.add(infoL, BorderLayout.EAST);
        return row;
    }

    private JPanel closeBtn(Frame owner) {
        JPanel wrap = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0));
        wrap.setOpaque(false);
        JButton exit = roundBtn("EXIT", new Color(248, 113, 113), new Color(30, 15, 20));
        JButton again = roundBtn("RUN AGAIN", BLUE, new Color(10, 22, 36));
        exit.addActionListener(e -> { dispose(); System.exit(0); });
        again.addActionListener(e -> { dispose(); owner.dispose(); });
        wrap.add(again);
        wrap.add(exit);
        return wrap;
    }

    private JButton roundBtn(String label, Color fg, Color bg) {
        JButton b = new JButton(label) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(getModel().isRollover() ? fg.brighter() : bg);
                g2.fill(new RoundRectangle2D.Float(0,0,getWidth(),getHeight(),10,10));
                g2.setColor(getModel().isRollover() ? BG : fg);
                g2.setFont(getFont());
                FontMetrics fm = g2.getFontMetrics();
                g2.drawString(getText(),
                        (getWidth()-fm.stringWidth(getText()))/2,
                        (getHeight()+fm.getAscent()-fm.getDescent())/2);
                g2.dispose();
            }
        };
        b.setFont(new Font("Segoe UI", Font.BOLD, 13));
        b.setPreferredSize(new Dimension(130, 40));
        b.setBorderPainted(false);
        b.setContentAreaFilled(false);
        b.setFocusPainted(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return b;
    }

    private JPanel bigStat(String value, String label, Color accent) {
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setBackground(CARD);
        p.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(2,0,0,0, accent),
                new EmptyBorder(10,8,10,8)));
        JLabel val = new JLabel(value, SwingConstants.CENTER);
        val.setForeground(accent);
        val.setFont(new Font("Segoe UI", Font.BOLD, 26));
        val.setAlignmentX(CENTER_ALIGNMENT);
        JLabel lbl = new JLabel(label, SwingConstants.CENTER);
        lbl.setForeground(MUTED);
        lbl.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        lbl.setAlignmentX(CENTER_ALIGNMENT);
        p.add(val);
        p.add(lbl);
        return p;
    }

    private JPanel statRow(String label, String value, Color accent) {
        JPanel row = new JPanel(new BorderLayout());
        row.setOpaque(false);
        row.setMaximumSize(new Dimension(440, 22));
        JLabel l = new JLabel(label);
        l.setForeground(MUTED);
        l.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        JLabel v = new JLabel(value, SwingConstants.RIGHT);
        v.setForeground(accent);
        v.setFont(new Font("Consolas", Font.BOLD, 13));
        row.add(l, BorderLayout.WEST);
        row.add(v, BorderLayout.EAST);
        return row;
    }

    private JPanel card() {
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setBackground(CARD2);
        p.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(EDGE,1),
                new EmptyBorder(14,16,14,16)));
        p.setAlignmentX(CENTER_ALIGNMENT);
        p.setMaximumSize(new Dimension(460, 400));
        return p;
    }

    private JLabel sectionLbl(String txt) {
        JLabel l = new JLabel(txt, SwingConstants.LEFT);
        l.setForeground(MUTED);
        l.setFont(new Font("Segoe UI", Font.BOLD, 11));
        l.setAlignmentX(0f);
        return l;
    }

    private String formatTime(long secs) {
        if (secs < 60) return secs + "s";
        long m = secs/60, s =secs%60;
        if (m < 60) return m + "m "+s + "s";
        return (m/60) + "h " + (m%60) + "m";
    }

    public static void show(Frame owner, Stats s) {
        new SummaryDialog(owner, s).setVisible(true);
    }
}