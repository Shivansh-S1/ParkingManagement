import javax.swing.*;
import javax.swing.Timer;
import javax.swing.border.*;
import javax.swing.event.*;
import javax.swing.table.*;
import javax.swing.text.*;
import java.awt.*;
import java.awt.event.*;
import java.io.IOException;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.*;
import java.util.regex.Pattern;

public class Main extends JFrame {
    private static final Color ACCENT = new Color(37, 99, 235), GREEN = new Color(22, 163, 74),
        RED = new Color(220, 38, 38), AMBER = new Color(217, 119, 6), VIOLET = new Color(124, 58, 237),
        NAVY = new Color(17, 28, 50);
    private static final String[][] PAGES = {{"Dashboard", "Dashboard"}, {"Entry", "Vehicle Entry"},
        {"Exit", "Vehicle Exit"}, {"Active", "Active Vehicles"}, {"History", "History"}};
    private static final String[] CATS = {"Car", "Bike", "EV"};

    private final ParkingManager manager = new ParkingManager();
    private boolean dark = false, updating = false;
    private Color BG, CARD, TEXT, MUTED, LINE, FIELD;
    private String currentPage = "Dashboard", selType = "Car";
    private double todayRev, totalRev;

    private final DefaultTableModel activeModel = model(
        new String[]{"Registration", "Owner", "Type", "Slot", "Entry Time", "Duration", "Fee so far"},
        new Class<?>[]{String.class, String.class, String.class, Integer.class, String.class, String.class, String.class});
    private final DefaultTableModel historyModel = model(
        new String[]{"Ticket", "Registration", "Type", "Slot", "Entry", "Exit", "Minutes", "Fee (Rs.)"},
        new Class<?>[]{String.class, String.class, String.class, Integer.class, String.class, String.class, Long.class, Double.class});

    private CardLayout cards; private JPanel content, slotArea;
    private final Map<String, Btn> nav = new LinkedHashMap<>();
    private JLabel lblTotal, lblOcc, lblFree, lblRev, lblCats, lblClock, lblStatus, lblNext, lblRegHint, billLbl, lblHistRev;
    private OccBar bar; private JTextField regF, ownerF, searchF; private Btn parkBtn, exitBtn;
    private final List<TypeToggle> toggles = new ArrayList<>();
    private JComboBox<String> exitBox; private JTable histTable, activeTable;
    private Timer toastTimer;

    public Main() {
        setTitle("SmartPark | Parking Management System");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1180, 760); setMinimumSize(new Dimension(980, 640)); setLocationRelativeTo(null);
        buildUI("Dashboard");
        new Timer(1000, e -> tick()).start();
    }

    // ---------- theme & building ----------
    private void applyTheme() {
        BG = dark ? new Color(15, 23, 42) : new Color(243, 246, 251);
        CARD = dark ? new Color(30, 41, 59) : Color.WHITE;
        TEXT = dark ? new Color(241, 245, 249) : new Color(15, 23, 42);
        MUTED = dark ? new Color(148, 163, 184) : new Color(100, 116, 139);
        LINE = dark ? new Color(51, 65, 85) : new Color(222, 229, 238);
        FIELD = dark ? new Color(15, 23, 42) : Color.WHITE;
    }

    private void buildUI(String page) {
        applyTheme(); toggles.clear(); nav.clear();
        JPanel root = new JPanel(new BorderLayout()); root.setBackground(BG);
        root.add(header(), BorderLayout.NORTH);
        root.add(sidebar(), BorderLayout.WEST);
        cards = new CardLayout(); content = new JPanel(cards); content.setOpaque(false);
        content.setBorder(new EmptyBorder(18, 18, 12, 18));
        content.add(dashboardPage(), "Dashboard"); content.add(entryPage(), "Entry");
        content.add(exitPage(), "Exit"); content.add(activePage(), "Active"); content.add(historyPage(), "History");
        root.add(content, BorderLayout.CENTER);
        root.add(statusBar(), BorderLayout.SOUTH);
        for (int i = 0; i < PAGES.length; i++) {
            final String key = PAGES[i][0];
            root.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_1 + i, InputEvent.CTRL_DOWN_MASK), key);
            root.getActionMap().put(key, new AbstractAction() { public void actionPerformed(ActionEvent e) { show(key); } });
        }
        setContentPane(root); refreshAll(); show(page); tick(); revalidate(); repaint();
    }

    private JPanel header() {
        JPanel p = new JPanel(new BorderLayout()); p.setBackground(NAVY); p.setBorder(new EmptyBorder(14, 22, 14, 22));
        JLabel t = label("SMARTPARK", 24, true, Color.WHITE);
        JLabel s = label("Parking Management System  |  Java OOP Project", 12, false, new Color(170, 187, 210));
        JPanel l = new JPanel(new GridLayout(2, 1)); l.setOpaque(false); l.add(t); l.add(s);
        lblClock = label("", 13, true, new Color(203, 213, 225));
        Btn theme = new Btn(dark ? "Light mode" : "Dark mode", null);
        theme.setToolTipText("Switch theme");
        theme.addActionListener(e -> { dark = !dark; buildUI(currentPage); });
        JPanel r = new JPanel(new FlowLayout(FlowLayout.RIGHT, 14, 4)); r.setOpaque(false); r.add(lblClock); r.add(theme);
        p.add(l, BorderLayout.WEST); p.add(r, BorderLayout.EAST); return p;
    }

    private JPanel sidebar() {
        JPanel p = new JPanel(); p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setBackground(new Color(22, 35, 58)); p.setBorder(new EmptyBorder(16, 12, 16, 12));
        p.setPreferredSize(new Dimension(190, 0));
        int i = 1;
        for (String[] pg : PAGES) {
            Btn b = new Btn(pg[1], null); b.setHorizontalAlignment(SwingConstants.LEFT);
            b.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42)); b.setAlignmentX(0f);
            b.setToolTipText("Ctrl+" + (i++)); final String key = pg[0];
            b.addActionListener(e -> show(key)); nav.put(key, b); p.add(b); p.add(Box.createVerticalStrut(6));
        }
        p.add(Box.createVerticalGlue());
        p.add(label("<html><div style='width:150px;color:#8da2c0'>Tip: click any slot on the dashboard to park or check out.</div></html>", 11, false, MUTED));
        return p;
    }

    private JPanel statusBar() {
        JPanel p = new JPanel(new BorderLayout()); p.setBackground(CARD);
        p.setBorder(new CompoundBorder(new MatteBorder(1, 0, 0, 0, LINE), new EmptyBorder(7, 18, 7, 18)));
        lblStatus = label("Ready.", 12, true, MUTED);
        p.add(lblStatus, BorderLayout.WEST); p.add(label("Ctrl+1 ... Ctrl+5 switch pages", 11, false, MUTED), BorderLayout.EAST);
        return p;
    }

    private void show(String page) {
        currentPage = page; cards.show(content, page);
        for (Map.Entry<String, Btn> e : nav.entrySet()) { e.getValue().base = e.getKey().equals(page) ? ACCENT : null; e.getValue().repaint(); }
        if (page.equals("Entry")) SwingUtilities.invokeLater(() -> regF.requestFocusInWindow());
    }

    private void toast(String msg, Color c) {
        lblStatus.setText(msg); lblStatus.setForeground(c);
        if (toastTimer != null) toastTimer.stop();
        toastTimer = new Timer(5000, e -> { lblStatus.setText("Ready."); lblStatus.setForeground(MUTED); });
        toastTimer.setRepeats(false); toastTimer.start();
    }

    // ---------- pages ----------
    private JPanel dashboardPage() {
        JPanel p = new JPanel(new BorderLayout(0, 14)); p.setOpaque(false);
        lblTotal = label("0", 30, true, ACCENT); lblOcc = label("0", 30, true, RED);
        lblFree = label("0", 30, true, GREEN); lblRev = label("Rs. 0", 30, true, VIOLET);
        JPanel stats = new JPanel(new GridLayout(1, 4, 14, 0)); stats.setOpaque(false);
        stats.add(stat("Total slots", lblTotal)); stats.add(stat("Occupied", lblOcc));
        stats.add(stat("Available", lblFree)); stats.add(stat("Revenue today", lblRev));
        p.add(stats, BorderLayout.NORTH);

        Card c = new Card(new BorderLayout(0, 10));
        bar = new OccBar(); lblCats = label("", 12, true, MUTED);
        JPanel top = new JPanel(new BorderLayout(0, 6)); top.setOpaque(false);
        top.add(label("Live occupancy", 15, true, TEXT), BorderLayout.NORTH); top.add(bar, BorderLayout.CENTER);
        top.add(lblCats, BorderLayout.SOUTH); c.add(top, BorderLayout.NORTH);
        slotArea = new JPanel(); slotArea.setLayout(new BoxLayout(slotArea, BoxLayout.Y_AXIS)); slotArea.setOpaque(false);
        for (String cat : CATS) {
            JLabel h = label(cat.equals("EV") ? "ELECTRIC VEHICLE BAYS" : cat.toUpperCase() + " BAYS", 11, true, MUTED);
            h.setBorder(new EmptyBorder(10, 2, 6, 0)); h.setAlignmentX(0f); slotArea.add(h);
            JPanel g = new JPanel(new GridLayout(1, 6, 10, 10)); g.setOpaque(false); g.setAlignmentX(0f);
            int n = 0;
            for (ParkingSlot s : manager.getSlots()) if (s.getCategory().equals(cat)) { g.add(new Tile(s)); n++; }
            for (; n < 6; n++) g.add(Box.createGlue());
            g.setMaximumSize(new Dimension(Integer.MAX_VALUE, 78)); slotArea.add(g);
        }
        JScrollPane sp = new JScrollPane(slotArea); sp.setBorder(null); sp.setOpaque(false);
        sp.getViewport().setOpaque(false); sp.getVerticalScrollBar().setUnitIncrement(16); c.add(sp, BorderLayout.CENTER);
        c.add(label("<html><font color='" + hex(GREEN) + "'>&#9632;</font> Available &nbsp;&nbsp; <font color='" + hex(RED)
            + "'>&#9632;</font> Occupied &nbsp;&nbsp; Click a free slot to park  |  click an occupied slot to check out</html>", 12, false, MUTED), BorderLayout.SOUTH);
        p.add(c, BorderLayout.CENTER); return p;
    }

    private JPanel entryPage() {
        JPanel outer = new JPanel(new GridBagLayout()); outer.setOpaque(false);
        Card c = new Card(new GridBagLayout()); c.setBorder(new EmptyBorder(24, 30, 24, 30));
        GridBagConstraints g = new GridBagConstraints(); g.gridx = 0; g.fill = GridBagConstraints.HORIZONTAL; g.insets = new Insets(5, 0, 5, 0);
        g.gridy = 0; c.add(label("Register vehicle entry", 20, true, TEXT), g);
        g.gridy = 1; c.add(label("A suitable slot is allocated automatically.", 12, false, MUTED), g);
        g.gridy = 2; g.insets = new Insets(14, 0, 3, 0); c.add(label("Vehicle type", 12, true, MUTED), g);
        JPanel tg = new JPanel(new GridLayout(1, 3, 10, 0)); tg.setOpaque(false); ButtonGroup bg = new ButtonGroup();
        for (String t : new String[]{"Car", "Bike", "Electric Vehicle"}) {
            TypeToggle b = new TypeToggle(t); b.setSelected(t.equals(selType)); bg.add(b); toggles.add(b); tg.add(b);
            b.addActionListener(e -> { selType = t; validateEntry(); });
        }
        g.gridy = 3; g.insets = new Insets(3, 0, 5, 0); c.add(tg, g);
        g.gridy = 4; g.insets = new Insets(10, 0, 3, 0); c.add(label("Registration number", 12, true, MUTED), g);
        regF = field(24); ((AbstractDocument) regF.getDocument()).setDocumentFilter(new DocumentFilter() {
            @Override public void insertString(FilterBypass fb, int o, String s, AttributeSet a) throws BadLocationException { super.insertString(fb, o, s == null ? null : s.toUpperCase(), a); }
            @Override public void replace(FilterBypass fb, int o, int l, String s, AttributeSet a) throws BadLocationException { super.replace(fb, o, l, s == null ? null : s.toUpperCase(), a); }
        });
        g.gridy = 5; g.insets = new Insets(3, 0, 2, 0); c.add(regF, g);
        lblRegHint = label(" ", 11, false, MUTED); g.gridy = 6; c.add(lblRegHint, g);
        g.gridy = 7; g.insets = new Insets(8, 0, 3, 0); c.add(label("Owner name", 12, true, MUTED), g);
        ownerF = field(24); g.gridy = 8; g.insets = new Insets(3, 0, 5, 0); c.add(ownerF, g);
        lblNext = label(" ", 13, true, GREEN); g.gridy = 9; g.insets = new Insets(10, 0, 6, 0); c.add(lblNext, g);
        parkBtn = new Btn("Allocate Parking Slot", GREEN); g.gridy = 10; c.add(parkBtn, g);
        listen(regF, this::validateEntry); listen(ownerF, this::validateEntry);
        regF.addActionListener(e -> ownerF.requestFocusInWindow());
        ownerF.addActionListener(e -> { if (parkBtn.isEnabled()) parkBtn.doClick(); });
        parkBtn.addActionListener(e -> {
            try {
                Vehicle v = ParkingManager.create(selType, regF.getText(), ownerF.getText());
                ParkingTicket t = manager.parkVehicle(v);
                regF.setText(""); ownerF.setText(""); refreshAll(); show("Dashboard");
                toast("Parked " + v.getRegistrationNumber() + " in slot #" + t.getSlotNumber() + "  |  Ticket " + t.getTicketId(), GREEN);
            } catch (Exception ex) { toast(ex.getMessage(), RED); }
        });
        outer.add(c); return outer;
    }

    private JPanel exitPage() {
        JPanel outer = new JPanel(new GridBagLayout()); outer.setOpaque(false);
        Card c = new Card(new GridBagLayout()); c.setBorder(new EmptyBorder(24, 30, 24, 30));
        GridBagConstraints g = new GridBagConstraints(); g.gridx = 0; g.fill = GridBagConstraints.HORIZONTAL; g.insets = new Insets(5, 0, 5, 0);
        g.gridy = 0; c.add(label("Vehicle exit and billing", 20, true, TEXT), g);
        g.gridy = 1; c.add(label("Pick a parked vehicle or type its registration - the bill updates live.", 12, false, MUTED), g);
        exitBox = new JComboBox<>(); exitBox.setEditable(true); exitBox.setFont(new Font("SansSerif", Font.PLAIN, 14));
        JTextComponent ed = (JTextComponent) exitBox.getEditor().getEditorComponent();
        ed.setBorder(new EmptyBorder(6, 8, 6, 8)); exitBox.setPreferredSize(new Dimension(340, 38));
        listen(ed, () -> { if (!updating) updateBill(); });
        exitBox.addActionListener(e -> { if (!updating) updateBill(); });
        g.gridy = 2; g.insets = new Insets(14, 0, 8, 0); c.add(exitBox, g);
        billLbl = new JLabel(); billLbl.setForeground(TEXT); billLbl.setVerticalAlignment(SwingConstants.TOP);
        billLbl.setBorder(new CompoundBorder(new LineBorder(LINE, 1, true), new EmptyBorder(12, 14, 12, 14)));
        billLbl.setPreferredSize(new Dimension(340, 195)); g.gridy = 3; c.add(billLbl, g);
        exitBtn = new Btn("Process Exit & Pay", ACCENT); g.gridy = 4; g.insets = new Insets(10, 0, 6, 0); c.add(exitBtn, g);
        g.gridy = 5; g.insets = new Insets(4, 0, 0, 0);
        c.add(label("<html>Rates: Car Rs. 30/h &nbsp;|&nbsp; Bike Rs. 10/h &nbsp;|&nbsp; EV Rs. 20/h. Any started hour counts as a full hour.</html>", 11, false, MUTED), g);
        exitBtn.addActionListener(e -> {
            try {
                String bill = manager.exitVehicle(exitText());
                refreshAll();
                JOptionPane.showMessageDialog(this, "<html><div style='width:230px'><b>Payment receipt</b><br><br>" + esc(bill).replace("\n", "<br>") + "</div></html>",
                    "Parking bill", JOptionPane.INFORMATION_MESSAGE);
                toast("Checked out " + bill.split("\n")[0].substring(9) + " - slot is free again.", GREEN); show("Dashboard");
            } catch (Exception ex) { toast(ex.getMessage(), RED); }
        });
        outer.add(c); return outer;
    }

    private JPanel activePage() {
        JPanel p = new JPanel(new BorderLayout(0, 10)); p.setOpaque(false);
        p.add(label("Vehicles currently parked  -  double-click a row to check out", 15, true, TEXT), BorderLayout.NORTH);
        activeTable = styleTable(new JTable(activeModel));
        activeTable.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                int r = activeTable.getSelectedRow();
                if (e.getClickCount() == 2 && r >= 0) { exitBox.setSelectedItem(activeModel.getValueAt(activeTable.convertRowIndexToModel(r), 0)); show("Exit"); }
            }
        });
        p.add(scroll(activeTable), BorderLayout.CENTER); return p;
    }

    private JPanel historyPage() {
        JPanel p = new JPanel(new BorderLayout(0, 10)); p.setOpaque(false);
        JPanel top = new JPanel(new BorderLayout(12, 0)); top.setOpaque(false);
        searchF = field(22); searchF.setToolTipText("Filter by registration, type, ticket, date...");
        lblHistRev = label("", 13, true, VIOLET);
        Btn export = new Btn("Export CSV", ACCENT);
        JPanel l = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0)); l.setOpaque(false);
        l.add(label("Search", 12, true, MUTED)); l.add(searchF); l.add(lblHistRev);
        top.add(l, BorderLayout.WEST); top.add(export, BorderLayout.EAST); p.add(top, BorderLayout.NORTH);
        histTable = styleTable(new JTable(historyModel));
        TableRowSorter<DefaultTableModel> sorter = new TableRowSorter<>(historyModel); histTable.setRowSorter(sorter);
        listen(searchF, () -> { String q = searchF.getText().trim();
            sorter.setRowFilter(q.isEmpty() ? null : RowFilter.regexFilter("(?i)" + Pattern.quote(q))); });
        export.addActionListener(e -> {
            JFileChooser fc = new JFileChooser(); fc.setSelectedFile(new java.io.File("parking-history.csv"));
            if (fc.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;
            List<String> out = new ArrayList<>(); out.add("Ticket,Registration,Type,Slot,Entry,Minutes,Fee,Exit"); out.addAll(manager.getHistory());
            try { Files.write(fc.getSelectedFile().toPath(), out); toast("History exported to " + fc.getSelectedFile().getName(), GREEN); }
            catch (IOException ex) { toast("Export failed: " + ex.getMessage(), RED); }
        });
        p.add(scroll(histTable), BorderLayout.CENTER); return p;
    }

    // ---------- behaviour ----------
    private void onSlotClick(ParkingSlot s) {
        if (s.isOccupied()) { exitBox.setSelectedItem(s.getVehicle().getRegistrationNumber()); show("Exit"); }
        else {
            selType = s.getCategory().equals("EV") ? "Electric Vehicle" : s.getCategory();
            for (TypeToggle t : toggles) t.setSelected(t.getActionCommand().equals(selType));
            validateEntry(); show("Entry");
        }
    }

    private void validateEntry() {
        String r = regF.getText().trim(); boolean ok = r.matches("[A-Z0-9][A-Z0-9 \\-]{3,13}");
        regF.setBorder(fieldBorder(!r.isEmpty() && !ok));
        lblRegHint.setText(r.isEmpty() ? "e.g. DL 01 AB 1234" : ok ? "Looks good" : "Use 4-14 letters, digits, spaces or hyphens");
        lblRegHint.setForeground(r.isEmpty() ? MUTED : ok ? GREEN : RED);
        int slot = manager.peekSlot(selType);
        lblNext.setText(slot > 0 ? "Next slot: #" + slot : "No " + selType + " slot is free right now");
        lblNext.setForeground(slot > 0 ? GREEN : RED);
        parkBtn.setEnabled(ok && !ownerF.getText().trim().isEmpty() && slot > 0);
    }

    private String exitText() { return String.valueOf(exitBox.getEditor().getItem()).trim().toUpperCase(); }

    private void updateBill() {
        ParkingTicket t = manager.getTicket(exitText());
        if (t == null) {
            billLbl.setText("<html><span style='color:" + hex(MUTED) + "'>No active vehicle selected.<br>Choose one from the list above.</span></html>");
            exitBtn.setEnabled(false); return;
        }
        long m = t.getParkedMinutes();
        billLbl.setText("<html><table cellpadding='3'>" + row("Owner", esc(t.getVehicle().getOwnerName())) + row("Type", t.getVehicle().getVehicleType())
            + row("Slot", "#" + t.getSlotNumber()) + row("Entered", t.getEntryText()) + row("Duration", dur(m))
            + "</table><div style='font-size:20px;margin-top:6px'><b>Rs. " + String.format(Locale.US, "%.2f", t.getFee()) + "</b></div></html>");
        exitBtn.setEnabled(true);
    }

    private void tick() {
        lblClock.setText(LocalDateTime.now().format(DateTimeFormatter.ofPattern("EEE, dd MMM yyyy  HH:mm:ss")));
        refreshActive(); updateBill(); if (slotArea != null) slotArea.repaint();
    }

    private void refreshActive() {
        List<ParkingTicket> list = new ArrayList<>(manager.getActiveTickets());
        boolean same = list.size() == activeModel.getRowCount();
        for (int i = 0; same && i < list.size(); i++) same = list.get(i).getVehicle().getRegistrationNumber().equals(activeModel.getValueAt(i, 0));
        if (same) {
            for (int i = 0; i < list.size(); i++) { activeModel.setValueAt(dur(list.get(i).getParkedMinutes()), i, 5);
                activeModel.setValueAt("Rs. " + String.format(Locale.US, "%.2f", list.get(i).getFee()), i, 6); }
            return;
        }
        activeModel.setRowCount(0);
        for (ParkingTicket t : list) activeModel.addRow(new Object[]{t.getVehicle().getRegistrationNumber(), t.getVehicle().getOwnerName(),
            t.getVehicle().getVehicleType(), t.getSlotNumber(), t.getEntryText(), dur(t.getParkedMinutes()), "Rs. " + String.format(Locale.US, "%.2f", t.getFee())});
    }

    private void refreshAll() {
        int total = manager.getSlots().size(), occ = 0; Map<String, int[]> cat = new LinkedHashMap<>();
        for (String c : CATS) cat.put(c, new int[2]);
        for (ParkingSlot s : manager.getSlots()) { cat.get(s.getCategory())[1]++; if (s.isOccupied()) { occ++; cat.get(s.getCategory())[0]++; } }
        lblTotal.setText("" + total); lblOcc.setText("" + occ); lblFree.setText("" + (total - occ));
        bar.set(occ / (double) total);
        lblCats.setText("Cars " + cat.get("Car")[0] + "/" + cat.get("Car")[1] + "    Bikes " + cat.get("Bike")[0] + "/" + cat.get("Bike")[1]
            + "    EVs " + cat.get("EV")[0] + "/" + cat.get("EV")[1] + "    (occupied / capacity)");
        refreshActive();
        historyModel.setRowCount(0); todayRev = 0; totalRev = 0;
        String today = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd-MM-yyyy"));
        for (String line : manager.getHistory()) {
            String[] c = line.split(",", -1);
            try {
                if (c.length < 7) continue;
                double fee = Double.parseDouble(c[6]); String exit = c.length > 7 ? c[7] : "-";
                historyModel.addRow(new Object[]{c[0], c[1], c[2], Integer.parseInt(c[3]), c[4], exit, Long.parseLong(c[5]), fee});
                totalRev += fee; if ((c.length > 7 ? c[7] : c[4]).startsWith(today)) todayRev += fee;
            } catch (NumberFormatException ignore) { }
        }
        lblRev.setText("Rs. " + String.format(Locale.US, "%.0f", todayRev));
        lblHistRev.setText("Total revenue: Rs. " + String.format(Locale.US, "%.2f", totalRev) + "   |   " + historyModel.getRowCount() + " sessions");
        for (TypeToggle t : toggles) t.refreshText();
        // refresh exit list, keeping what the user typed
        String keep = String.valueOf(exitBox.getEditor().getItem()); updating = true;
        exitBox.removeAllItems(); for (ParkingTicket t : manager.getActiveTickets()) exitBox.addItem(t.getVehicle().getRegistrationNumber());
        exitBox.setSelectedItem(keep.equals("null") ? "" : keep); updating = false;
        updateBill(); validateEntry();
    }

    // ---------- helpers ----------
    private static DefaultTableModel model(String[] cols, Class<?>[] types) {
        return new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
            @Override public Class<?> getColumnClass(int c) { return types[c]; }
        };
    }
    private static String dur(long m) { return m < 60 ? m + " min" : (m / 60) + "h " + String.format("%02d", m % 60) + "m"; }
    private static String hex(Color c) { return String.format("#%02x%02x%02x", c.getRed(), c.getGreen(), c.getBlue()); }
    private static String esc(String s) { return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;"); }
    private String row(String k, String v) { return "<tr><td style='color:" + hex(MUTED) + "'>" + k + "</td><td><b>" + v + "</b></td></tr>"; }
    private static void listen(JTextComponent c, Runnable r) {
        c.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { r.run(); } public void removeUpdate(DocumentEvent e) { r.run(); }
            public void changedUpdate(DocumentEvent e) { r.run(); }
        });
    }
    private JLabel label(String t, int size, boolean bold, Color c) {
        JLabel l = new JLabel(t); l.setFont(new Font("SansSerif", bold ? Font.BOLD : Font.PLAIN, size)); l.setForeground(c); return l;
    }
    private Border fieldBorder(boolean bad) { return new CompoundBorder(new LineBorder(bad ? RED : LINE, 1, true), new EmptyBorder(8, 10, 8, 10)); }
    private JTextField field(int cols) {
        JTextField f = new JTextField(cols); f.setFont(new Font("SansSerif", Font.PLAIN, 14)); f.setBackground(FIELD);
        f.setForeground(TEXT); f.setCaretColor(TEXT); f.setBorder(fieldBorder(false)); return f;
    }
    private JPanel stat(String title, JLabel value) {
        Card c = new Card(new BorderLayout(0, 4)); c.add(label(title, 12, true, MUTED), BorderLayout.NORTH); c.add(value, BorderLayout.CENTER); return c;
    }
    private JScrollPane scroll(JTable t) {
        JScrollPane sp = new JScrollPane(t); sp.setBorder(new LineBorder(LINE, 1, true)); sp.getViewport().setBackground(CARD); return sp;
    }
    private JTable styleTable(JTable t) {
        t.setRowHeight(36); t.setShowGrid(false); t.setIntercellSpacing(new Dimension(0, 0)); t.setFont(new Font("SansSerif", Font.PLAIN, 13));
        t.setFillsViewportHeight(true); t.setBackground(CARD); t.setAutoCreateRowSorter(!(t.getRowSorter() != null));
        DefaultTableCellRenderer r = new DefaultTableCellRenderer() {
            @Override public Component getTableCellRendererComponent(JTable tb, Object v, boolean sel, boolean foc, int row, int col) {
                super.getTableCellRendererComponent(tb, v instanceof Double ? String.format(Locale.US, "%.2f", v) : v, sel, foc, row, col);
                setBorder(new EmptyBorder(0, 12, 0, 12));
                setBackground(sel ? new Color(ACCENT.getRed(), ACCENT.getGreen(), ACCENT.getBlue(), 200) : row % 2 == 0 ? CARD : BG);
                setForeground(sel ? Color.WHITE : TEXT); setOpaque(true); return this;
            }
        };
        t.setDefaultRenderer(Object.class, r); t.setDefaultRenderer(Integer.class, r); t.setDefaultRenderer(Long.class, r); t.setDefaultRenderer(Double.class, r);
        t.getTableHeader().setPreferredSize(new Dimension(0, 38));
        t.getTableHeader().setDefaultRenderer(new DefaultTableCellRenderer() {
            @Override public Component getTableCellRendererComponent(JTable tb, Object v, boolean s, boolean f, int row, int col) {
                super.getTableCellRendererComponent(tb, v, s, f, row, col); setBackground(NAVY); setForeground(Color.WHITE);
                setFont(new Font("SansSerif", Font.BOLD, 12)); setBorder(new EmptyBorder(0, 12, 0, 12)); return this;
            }
        });
        return t;
    }

    // ---------- custom components ----------
    private static void aa(Graphics2D g) { g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON); }

    private class Card extends JPanel {
        Card(LayoutManager l) { super(l); setOpaque(false); setBorder(new EmptyBorder(16, 18, 16, 18)); }
        @Override protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create(); aa(g); g.setColor(CARD); g.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 18, 18);
            g.setColor(LINE); g.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 18, 18); g.dispose(); super.paintComponent(g0);
        }
    }

    private class Btn extends JButton {
        Color base; boolean hover;
        Btn(String t, Color base) {
            super(t); this.base = base; setContentAreaFilled(false); setBorderPainted(false); setFocusPainted(false);
            setFont(new Font("SansSerif", Font.BOLD, 13)); setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)); setBorder(new EmptyBorder(10, 16, 10, 16));
            addMouseListener(new MouseAdapter() { public void mouseEntered(MouseEvent e) { hover = true; repaint(); } public void mouseExited(MouseEvent e) { hover = false; repaint(); } });
        }
        @Override protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create(); aa(g);
            Color c = base == null ? (hover ? new Color(255, 255, 255, 30) : new Color(0, 0, 0, 0)) : hover ? base.brighter() : base;
            if (!isEnabled()) c = dark ? new Color(51, 65, 85) : new Color(190, 198, 210);
            g.setColor(c); g.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
            setForeground(base == null ? new Color(214, 223, 236) : Color.WHITE); g.dispose(); super.paintComponent(g0);
        }
    }

    private class TypeToggle extends JToggleButton {
        final String type;
        TypeToggle(String type) {
            this.type = type; setActionCommand(type); setContentAreaFilled(false); setBorderPainted(false); setFocusPainted(false);
            setFont(new Font("SansSerif", Font.BOLD, 13)); setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)); setPreferredSize(new Dimension(130, 62));
        }
        void refreshText() {
            int n = manager.freeCount(type); String sub = n + " free";
            setText("<html><center>" + (type.startsWith("Electric") ? "EV" : type) + "<br><span style='font-size:10px'>" + sub + "</span></center></html>");
        }
        @Override protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create(); aa(g);
            g.setColor(isSelected() ? ACCENT : FIELD); g.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 12, 12);
            g.setColor(isSelected() ? ACCENT : LINE); g.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 12, 12);
            setForeground(isSelected() ? Color.WHITE : TEXT); g.dispose(); super.paintComponent(g0);
        }
    }

    private class Tile extends JComponent {
        final ParkingSlot s; boolean hover;
        Tile(ParkingSlot s) {
            this.s = s; setPreferredSize(new Dimension(120, 78)); setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)); setToolTipText("");
            addMouseListener(new MouseAdapter() {
                public void mouseEntered(MouseEvent e) { hover = true; repaint(); } public void mouseExited(MouseEvent e) { hover = false; repaint(); }
                public void mouseClicked(MouseEvent e) { onSlotClick(s); }
            });
        }
        @Override public String getToolTipText(MouseEvent e) {
            if (!s.isOccupied()) return "Slot #" + s.getSlotNumber() + " is free - click to park";
            ParkingTicket t = manager.getTicket(s.getVehicle().getRegistrationNumber());
            return t == null ? null : "<html><b>" + esc(s.getVehicle().getOwnerName()) + "</b><br>" + s.getVehicle().getVehicleType() + "<br>Since " + t.getEntryText()
                + "<br>Fee so far: Rs. " + String.format(Locale.US, "%.2f", t.getFee()) + "</html>";
        }
        @Override protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create(); aa(g); boolean occ = s.isOccupied(); Color b = occ ? RED : GREEN;
            g.setColor(new Color(b.getRed(), b.getGreen(), b.getBlue(), hover ? 75 : 38)); g.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 16, 16);
            g.setColor(b); g.setStroke(new BasicStroke(hover ? 2.4f : 1.4f)); g.drawRoundRect(1, 1, getWidth() - 3, getHeight() - 3, 16, 16);
            g.setFont(new Font("SansSerif", Font.BOLD, 18)); g.setColor(TEXT); g.drawString("#" + s.getSlotNumber(), 12, 27);
            g.setFont(new Font("SansSerif", Font.BOLD, 10)); g.setColor(b); String cat = s.getCategory().toUpperCase();
            g.drawString(cat, getWidth() - g.getFontMetrics().stringWidth(cat) - 12, 24);
            if (occ) {
                ParkingTicket t = manager.getTicket(s.getVehicle().getRegistrationNumber());
                g.setFont(new Font("SansSerif", Font.BOLD, 12)); g.setColor(TEXT); g.drawString(s.getVehicle().getRegistrationNumber(), 12, 50);
                g.setFont(new Font("SansSerif", Font.PLAIN, 11)); g.setColor(MUTED); g.drawString(t == null ? "" : dur(t.getParkedMinutes()), 12, 67);
            } else {
                g.setFont(new Font("SansSerif", Font.BOLD, 12)); g.setColor(b); g.drawString("Available", 12, 50);
                g.setFont(new Font("SansSerif", Font.PLAIN, 11)); g.setColor(MUTED); g.drawString("Click to park", 12, 67);
            }
            g.dispose();
        }
    }

    private class OccBar extends JComponent {
        double v, target;
        final Timer anim = new Timer(15, e -> { v += (target - v) * 0.18; if (Math.abs(target - v) < 0.002) { v = target; ((Timer) e.getSource()).stop(); } repaint(); });
        OccBar() { setPreferredSize(new Dimension(100, 26)); }
        void set(double x) { target = x; anim.start(); }
        @Override protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create(); aa(g); int w = getWidth(), h = getHeight();
            g.setColor(LINE); g.fillRoundRect(0, 0, w, h, h, h);
            g.setColor(v < .6 ? GREEN : v < .85 ? AMBER : RED); g.fillRoundRect(0, 0, Math.max(h, (int) (w * v)), h, h, h);
            g.setFont(new Font("SansSerif", Font.BOLD, 12)); g.setColor(Color.WHITE);
            g.drawString(Math.round(v * 100) + "% full", 12, h / 2 + 4); g.dispose();
        }
    }

    public static void main(String[] args) {
        try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); } catch (Exception ignored) { }
        SwingUtilities.invokeLater(() -> new Main().setVisible(true));
    }
}
