package com.smartdelivery.gui;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.RoundRectangle2D;

// shown once at startup — lets the user pick agent count and max orders
// returns null if they close/cancel, otherwise returns a Config record
public class LaunchDialog extends JDialog {

    public record Config(int agentCount, int maxOrders) {}

    private static final Color BG        = new Color(10, 12, 20);
    private static final Color PANEL     = new Color(18, 22, 36);
    private static final Color CARD      = new Color(26, 32, 52);
    private static final Color BORDER    = new Color(40, 55, 80);
    private static final Color BLUE      = new Color(56, 189, 248);
    private static final Color GREEN     = new Color(52, 211, 153);
    private static final Color AMBER     = new Color(251, 191, 36);
    private static final Color TEXT      = new Color(226, 232, 240);
    private static final Color DIM       = new Color(100, 116, 139);

    private static final Color[] AGENT_COLORS = {
            new Color(248, 113, 113),  // 1 agent  — red
            new Color(56,  189, 248),  // 2 agents — blue
            GREEN,                     // 3 agents — green
            AMBER,                     // 4 agents — amber
            new Color(167, 139, 250),  // 5 agents — purple
    };

    private Config result = null;
    private int agents  = 3;
    private int orders  = 6;

    // dot indicators for agent count picker
    private final JPanel dotRow   = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0));
    // number label for orders
    private JLabel ordersNum;

    public LaunchDialog(Frame owner) {
        super(owner, "Smart Delivery System", true);
        setUndecorated(true);
        setBackground(new Color(0, 0, 0, 0));

        JPanel root = new JPanel(new BorderLayout()) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(BG);
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 16, 16));
                g2.dispose();
            }
        };
        root.setOpaque(false);
        root.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER, 1),
                new EmptyBorder(36, 44, 36, 44)));

        // ── header ────────────────────────────────────────────
        JPanel header = new JPanel();
        header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));
        header.setOpaque(false);

        JLabel icon = new JLabel("◈", SwingConstants.CENTER);
        icon.setForeground(BLUE);
        icon.setFont(new Font("Segoe UI Symbol", Font.PLAIN, 32));
        icon.setAlignmentX(CENTER_ALIGNMENT);

        JLabel titleLbl = new JLabel("Smart Delivery System", SwingConstants.CENTER);
        titleLbl.setForeground(TEXT);
        titleLbl.setFont(new Font("Segoe UI", Font.BOLD, 20));
        titleLbl.setAlignmentX(CENTER_ALIGNMENT);

        JLabel sub = new JLabel("M'sila  •  Agent-Based Simulation", SwingConstants.CENTER);
        sub.setForeground(DIM);
        sub.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        sub.setAlignmentX(CENTER_ALIGNMENT);

        header.add(icon);
        header.add(Box.createVerticalStrut(8));
        header.add(titleLbl);
        header.add(Box.createVerticalStrut(4));
        header.add(sub);

        // ── divider ───────────────────────────────────────────
        JSeparator sep = new JSeparator();
        sep.setForeground(BORDER);
        sep.setBackground(BORDER);

        // ── controls ──────────────────────────────────────────
        JPanel controls = new JPanel();
        controls.setLayout(new BoxLayout(controls, BoxLayout.Y_AXIS));
        controls.setOpaque(false);

        controls.add(Box.createVerticalStrut(28));
        controls.add(agentSection());
        controls.add(Box.createVerticalStrut(24));
        controls.add(ordersSection());
        controls.add(Box.createVerticalStrut(32));
        controls.add(startButton());

        root.add(header,   BorderLayout.NORTH);
        root.add(sep,      BorderLayout.CENTER);
        root.add(controls, BorderLayout.SOUTH);

        setContentPane(root);
        pack();
        setSize(420, 460);
        setLocationRelativeTo(owner);

        // allow dragging the undecorated dialog
        Point[] drag = {null};
        root.addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent e) { drag[0] = e.getPoint(); }
        });
        root.addMouseMotionListener(new MouseMotionAdapter() {
            public void mouseDragged(MouseEvent e) {
                if (drag[0] == null) return;
                Point loc = getLocation();
                setLocation(loc.x + e.getX() - drag[0].x, loc.y + e.getY() - drag[0].y);
            }
        });
    }

    // ── agent count section ───────────────────────────────────
    private JPanel agentSection() {
        JPanel p = card();

        JLabel label = sectionLabel("DELIVERY AGENTS");
        JLabel hint  = new JLabel("select how many agents are in the field", SwingConstants.CENTER);
        hint.setForeground(DIM);
        hint.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        hint.setAlignmentX(CENTER_ALIGNMENT);

        dotRow.setOpaque(false);
        dotRow.setAlignmentX(CENTER_ALIGNMENT);
        buildDots();

        p.add(label);
        p.add(Box.createVerticalStrut(6));
        p.add(hint);
        p.add(Box.createVerticalStrut(16));
        p.add(dotRow);
        return p;
    }

    private void buildDots() {
        dotRow.removeAll();
        for (int n = 1; n <= 5; n++) {
            final int val = n;
            boolean selected = (n == agents);
            Color col = selected ? AGENT_COLORS[n - 1] : DIM;

            JLabel dot = new JLabel(selected ? "●" : "○", SwingConstants.CENTER) {
                @Override protected void paintComponent(Graphics g) {
                    super.paintComponent(g);
                }
            };
            dot.setForeground(col);
            dot.setFont(new Font("Segoe UI Symbol", Font.PLAIN, selected ? 32 : 26));
            dot.setToolTipText(val + " agent" + (val > 1 ? "s" : ""));
            dot.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            dot.addMouseListener(new MouseAdapter() {
                public void mouseClicked(MouseEvent e) {
                    agents = val;
                    buildDots();
                    dotRow.revalidate();
                    dotRow.repaint();
                }
                public void mouseEntered(MouseEvent e) {
                    dot.setForeground(AGENT_COLORS[val - 1]);
                }
                public void mouseExited(MouseEvent e) {
                    dot.setForeground(val == agents ? AGENT_COLORS[val - 1] : DIM);
                }
            });

            // number below each dot
            JPanel col2 = new JPanel();
            col2.setLayout(new BoxLayout(col2, BoxLayout.Y_AXIS));
            col2.setOpaque(false);
            JLabel num = new JLabel(String.valueOf(n), SwingConstants.CENTER);
            num.setForeground(selected ? TEXT : DIM);
            num.setFont(new Font("Consolas", Font.BOLD, 11));
            num.setAlignmentX(CENTER_ALIGNMENT);
            dot.setAlignmentX(CENTER_ALIGNMENT);
            col2.add(dot);
            col2.add(num);
            dotRow.add(col2);
        }
    }

    // ── max orders section ────────────────────────────────────
    private JPanel ordersSection() {
        JPanel p = card();

        JLabel label = sectionLabel("MAX ACTIVE ORDERS");
        JLabel hint  = new JLabel("orders waiting or in transit at the same time", SwingConstants.CENTER);
        hint.setForeground(DIM);
        hint.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        hint.setAlignmentX(CENTER_ALIGNMENT);

        JPanel row = new JPanel(new FlowLayout(FlowLayout.CENTER, 16, 0));
        row.setOpaque(false);
        row.setAlignmentX(CENTER_ALIGNMENT);

        JButton minus = arrowBtn("−");
        ordersNum = new JLabel(String.valueOf(orders), SwingConstants.CENTER);
        ordersNum.setForeground(AMBER);
        ordersNum.setFont(new Font("Consolas", Font.BOLD, 28));
        ordersNum.setPreferredSize(new Dimension(48, 36));

        JButton plus  = arrowBtn("+");

        minus.addActionListener(e -> {
            if (orders > 1) { orders--; ordersNum.setText(String.valueOf(orders)); }
        });
        plus.addActionListener(e -> {
            if (orders < 20) { orders++; ordersNum.setText(String.valueOf(orders)); }
        });

        row.add(minus);
        row.add(ordersNum);
        row.add(plus);

        p.add(label);
        p.add(Box.createVerticalStrut(6));
        p.add(hint);
        p.add(Box.createVerticalStrut(12));
        p.add(row);
        return p;
    }

    // ── start button ──────────────────────────────────────────
    private JPanel startButton() {
        JPanel wrap = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        wrap.setOpaque(false);

        JButton btn = new JButton("START SIMULATION") {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(getModel().isRollover()
                        ? new Color(30, 180, 240)
                        : BLUE);
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 10, 10));
                g2.setColor(new Color(10, 12, 20));
                g2.setFont(getFont());
                FontMetrics fm = g2.getFontMetrics();
                int x = (getWidth()  - fm.stringWidth(getText())) / 2;
                int y = (getHeight() + fm.getAscent() - fm.getDescent()) / 2;
                g2.drawString(getText(), x, y);
                g2.dispose();
            }
        };
        btn.setText("START SIMULATION");
        btn.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btn.setPreferredSize(new Dimension(220, 42));
        btn.setBorderPainted(false);
        btn.setContentAreaFilled(false);
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.addActionListener(e -> {
            result = new Config(agents, orders);
            dispose();
        });

        wrap.add(btn);
        return wrap;
    }

    // ── helpers ───────────────────────────────────────────────

    private JPanel card() {
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setBackground(CARD);
        p.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER, 1),
                new EmptyBorder(16, 20, 16, 20)));
        p.setMaximumSize(new Dimension(340, 200));
        p.setAlignmentX(CENTER_ALIGNMENT);
        return p;
    }

    private JLabel sectionLabel(String text) {
        JLabel l = new JLabel(text, SwingConstants.CENTER);
        l.setForeground(DIM);
        l.setFont(new Font("Segoe UI", Font.BOLD, 10));
        l.setAlignmentX(CENTER_ALIGNMENT);
        return l;
    }

    private JButton arrowBtn(String symbol) {
        JButton b = new JButton(symbol) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(getModel().isRollover() ? BORDER : new Color(35, 45, 70));
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 8, 8));
                g2.setColor(getModel().isRollover() ? TEXT : DIM);
                g2.setFont(getFont());
                FontMetrics fm = g2.getFontMetrics();
                int x = (getWidth()  - fm.stringWidth(getText())) / 2;
                int y = (getHeight() + fm.getAscent() - fm.getDescent()) / 2;
                g2.drawString(getText(), x, y);
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

    // call this to show the dialog and get the result
    public static Config show(Frame owner) {
        LaunchDialog d = new LaunchDialog(owner);
        d.setVisible(true); // blocks until disposed
        return d.result;    // null if user closed it
    }
}