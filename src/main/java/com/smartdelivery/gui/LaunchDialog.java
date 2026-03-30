package com.smartdelivery.gui;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.RoundRectangle2D;

public class LaunchDialog extends JDialog {
    public record Config(int agentCount, int maxOrders) {}
    private static final Color BG = new Color(10, 12, 20);
    private static final Color CARD = new Color(26, 32, 52);
    private static final Color EDGE = new Color(40, 55, 80);
    private static final Color BLUE = new Color(56, 189, 248);
    private static final Color GREEN = new Color(52, 211, 153);
    private static final Color AMBER = new Color(251, 191, 36);
    private static final Color TEXT = new Color(226, 232, 240);
    private static final Color MUTED = new Color(100, 116, 139);

    private static final Color[] DOT_COLORS = {
            new Color(248, 113, 113),
            new Color(56,  189, 248),
            GREEN,
            AMBER,
            new Color(167, 139, 250),
            new Color(251, 113, 133),
            new Color(34,  211, 238),
            new Color(163, 230,  53),
            new Color(249, 115,  22),
            new Color(236,  72, 153),
    };

    private Config result = null;
    private int    agents = 3;
    private int    orders = 6;
    private final JPanel  dotRow  = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 0));
    private       JLabel  numLbl;

    public LaunchDialog(Frame owner) {
        super(owner, "Smart Delivery System", true);
        setUndecorated(true);
        setBackground(new Color(0, 0, 0, 0));
        JPanel root = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(BG);
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 18, 18));
                g2.dispose();
            }
        };
        root.setLayout(new BoxLayout(root, BoxLayout.Y_AXIS));
        root.setOpaque(false);
        root.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(EDGE, 1),
                new EmptyBorder(30, 38, 30, 38)));
        root.add(headerPanel());
        root.add(Box.createVerticalStrut(20));
        root.add(agentCard());
        root.add(Box.createVerticalStrut(14));
        root.add(ordersCard());
        root.add(Box.createVerticalStrut(24));
        root.add(startButton());

        setContentPane(root);
        pack();
        setMinimumSize(new Dimension(460,getHeight()));
        setLocationRelativeTo(owner);

        Point[] grab = {null};
        root.addMouseListener(new MouseAdapter(){
            public void mousePressed(MouseEvent e){ grab[0] = e.getPoint(); }
        });
        root.addMouseMotionListener(new MouseMotionAdapter() {
            public void mouseDragged(MouseEvent e){
                if (grab[0] == null) return;
                Point loc = getLocation();
                setLocation(loc.x + e.getX() - grab[0].x, loc.y + e.getY() - grab[0].y);
            }
        });
    }

    private JPanel headerPanel() {
        JPanel h = new JPanel();
        h.setLayout(new BoxLayout(h, BoxLayout.Y_AXIS));
        h.setOpaque(false);
        JLabel gem = new JLabel("◈", SwingConstants.CENTER);
        gem.setForeground(BLUE);
        gem.setFont(new Font("Segoe UI Symbol", Font.PLAIN, 36));
        gem.setAlignmentX(CENTER_ALIGNMENT);
        JLabel title = new JLabel("Smart Delivery System", SwingConstants.CENTER);
        title.setForeground(TEXT);
        title.setFont(new Font("Segoe UI", Font.BOLD, 26));
        title.setAlignmentX(CENTER_ALIGNMENT);
        JLabel sub = new JLabel("M'sila  •  Agent-Based Simulation", SwingConstants.CENTER);
        sub.setForeground(MUTED);
        sub.setFont(new Font("Segoe UI", Font.BOLD, 13));
        sub.setAlignmentX(CENTER_ALIGNMENT);
        h.add(gem);
        h.add(Box.createVerticalStrut(6));
        h.add(title);
        h.add(Box.createVerticalStrut(3));
        h.add(sub);
        return h;
    }

    private JPanel agentCard() {
        JPanel card = makeCard();

        JLabel title = cardTitle("DELIVERY AGENTS");
        JLabel hint  = cardHint("select how many agents are in the field");

        dotRow.setOpaque(false);
        dotRow.setAlignmentX(CENTER_ALIGNMENT);
        rebuildDots();

        card.add(title);
        card.add(Box.createVerticalStrut(3));
        card.add(hint);
        card.add(Box.createVerticalStrut(12));
        card.add(dotRow);
        return card;
    }

    private void rebuildDots() {
        dotRow.removeAll();
        for (int n = 1; n <= 10; n++){
            final int v = n;
            boolean selected = (n == agents);
            JLabel dot = new JLabel(selected ? "●" : "○", SwingConstants.CENTER);
            dot.setForeground(selected ? DOT_COLORS[n - 1] : MUTED);
            dot.setFont(new Font("Segoe UI Symbol", Font.PLAIN, selected ? 28 : 22));
            dot.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            dot.addMouseListener(new MouseAdapter() {
                public void mouseClicked(MouseEvent e) {
                    agents = v;
                    rebuildDots();
                    dotRow.revalidate();
                    dotRow.repaint();
                }
                public void mouseEntered(MouseEvent e) { dot.setForeground(DOT_COLORS[v - 1]); }
                public void mouseExited(MouseEvent e)  { dot.setForeground(v == agents ? DOT_COLORS[v - 1] : MUTED); }
            });

            JLabel num = new JLabel(String.valueOf(n), SwingConstants.CENTER);
            num.setForeground(selected ? TEXT : MUTED);
            num.setFont(new Font("Consolas", Font.BOLD, 10));
            num.setAlignmentX(CENTER_ALIGNMENT);
            dot.setAlignmentX(CENTER_ALIGNMENT);

            JPanel col = new JPanel();
            col.setLayout(new BoxLayout(col, BoxLayout.Y_AXIS));
            col.setOpaque(false);
            col.add(dot);
            col.add(num);
            dotRow.add(col);
        }
    }
    private JPanel ordersCard() {
        JPanel card = makeCard();
        JLabel title = cardTitle("MAX ACTIVE ORDERS");
        JLabel hint  = cardHint("orders waiting or in transit at the same time");
        JPanel row = new JPanel(new FlowLayout(FlowLayout.CENTER, 16, 0));
        row.setOpaque(false);
        row.setAlignmentX(CENTER_ALIGNMENT);
        JButton minus = stepBtn("−");
        numLbl = new JLabel(String.valueOf(orders), SwingConstants.CENTER);
        numLbl.setForeground(AMBER);
        numLbl.setFont(new Font("Consolas", Font.BOLD, 32));
        numLbl.setPreferredSize(new Dimension(54, 40));
        JButton plus = stepBtn("+");
        minus.addActionListener(e -> { if (orders > 1)  { orders--; numLbl.setText(String.valueOf(orders)); } });
        plus.addActionListener(e  -> { if (orders < 15) { orders++; numLbl.setText(String.valueOf(orders)); } });
        row.add(minus);
        row.add(numLbl);
        row.add(plus);
        card.add(title);
        card.add(Box.createVerticalStrut(3));
        card.add(hint);
        card.add(Box.createVerticalStrut(10));
        card.add(row);
        return card;
    }

    private JPanel startButton() {
        JPanel wrap = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        wrap.setOpaque(false);

        JButton btn = new JButton("START SIMULATION") {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(getModel().isRollover() ? new Color(30, 180, 240) : BLUE);
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 10, 10));
                g2.setColor(BG);
                g2.setFont(getFont());
                FontMetrics fm = g2.getFontMetrics();
                g2.drawString(getText(),
                        (getWidth()  - fm.stringWidth(getText())) / 2,
                        (getHeight() + fm.getAscent() - fm.getDescent()) / 2);
                g2.dispose();
            }
        };
        btn.setFont(new Font("Segoe UI", Font.BOLD, 15));
        btn.setPreferredSize(new Dimension(250, 46));
        btn.setBorderPainted(false);
        btn.setContentAreaFilled(false);
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.addActionListener(e -> { result = new Config(agents, orders); dispose(); });

        wrap.add(btn);
        return wrap;
    }

    private JPanel makeCard() {
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setBackground(CARD);
        p.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(EDGE, 1),
                new EmptyBorder(14, 18, 14, 18)));
        p.setAlignmentX(CENTER_ALIGNMENT);
        p.setMaximumSize(new Dimension(400, 300));
        return p;
    }

    private JLabel cardTitle(String txt) {
        JLabel l = new JLabel(txt, SwingConstants.CENTER);
        l.setForeground(MUTED);
        l.setFont(new Font("Segoe UI", Font.BOLD, 12));
        l.setAlignmentX(CENTER_ALIGNMENT);
        return l;
    }

    private JLabel cardHint(String txt) {
        JLabel l = new JLabel(txt, SwingConstants.CENTER);
        l.setForeground(MUTED);
        l.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        l.setAlignmentX(CENTER_ALIGNMENT);
        return l;
    }

    private JButton stepBtn(String sym) {
        JButton b = new JButton(sym) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(getModel().isRollover() ? EDGE : new Color(35, 45, 70));
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 8, 8));
                g2.setColor(getModel().isRollover() ? TEXT : MUTED);
                g2.setFont(getFont());
                FontMetrics fm = g2.getFontMetrics();
                g2.drawString(getText(),
                        (getWidth()  - fm.stringWidth(getText())) / 2,
                        (getHeight() + fm.getAscent() - fm.getDescent()) / 2);
                g2.dispose();
            }
        };
        b.setFont(new Font("Segoe UI", Font.BOLD, 20));
        b.setPreferredSize(new Dimension(36, 36));
        b.setBorderPainted(false);
        b.setContentAreaFilled(false);
        b.setFocusPainted(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return b;
    }

    public static Config show(Frame owner) {
        LaunchDialog d = new LaunchDialog(owner);
        d.setVisible(true);
        return d.result;
    }
}