package com.kclog;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.text.NumberFormat;
import java.util.Locale;
import java.util.Map;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.ScrollPaneConstants;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.PluginPanel;

class KcLogPanel extends PluginPanel
{
	private final KcLogPlugin plugin;
	private final JPanel allTimeList = newList();
	private final JPanel todayList = newList();
	private final JLabel total = new JLabel("0");
	private final JLabel caption = new JLabel("TOTAL KILLS");
	private final JLabel monsters = new JLabel("0 creatures");
	private final JTextField search = new JTextField();
	private final JTabbedPane tabs = new JTabbedPane();
	private final NumberFormat numbers = NumberFormat.getIntegerInstance();
	private Map<String, Integer> allTime = Map.of();
	private Map<String, Integer> today = Map.of();

	KcLogPanel(KcLogPlugin plugin)
	{
		this.plugin = plugin;
		setLayout(new BorderLayout(0, 12));
		setBorder(new EmptyBorder(14, 10, 10, 10));
		setBackground(ColorScheme.DARK_GRAY_COLOR);

		JPanel top = new JPanel();
		top.setOpaque(false);
		top.setLayout(new BoxLayout(top, BoxLayout.Y_AXIS));
		JLabel title = new JLabel("KC LOG");
		title.setFont(FontManager.getDefaultBoldFont().deriveFont(16f));
		title.setForeground(ColorScheme.TEXT_COLOR);
		top.add(title);
		top.add(Box.createVerticalStrut(18));

		caption.setFont(FontManager.getDefaultFont().deriveFont(10f));
		caption.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
		top.add(caption);
		total.setFont(FontManager.getDefaultBoldFont().deriveFont(30f));
		total.setForeground(ColorScheme.BRAND_ORANGE);
		top.add(total);
		monsters.setFont(FontManager.getDefaultFont().deriveFont(11f));
		monsters.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
		top.add(monsters);
		top.add(Box.createVerticalStrut(14));

		JPanel searchBar = new JPanel(new BorderLayout(8, 0));
		searchBar.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		searchBar.setBorder(new EmptyBorder(8, 8, 8, 8));
		searchBar.add(new JLabel(new SearchIcon()), BorderLayout.WEST);
		search.setFont(FontManager.getDefaultFont());
		search.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		search.setForeground(ColorScheme.TEXT_COLOR);
		search.setCaretColor(ColorScheme.TEXT_COLOR);
		search.setBorder(null);
		search.setToolTipText("Search creatures");
		search.getAccessibleContext().setAccessibleName("Search creatures");
		search.getDocument().addDocumentListener(new DocumentListener()
		{
			@Override public void insertUpdate(DocumentEvent e) { rebuild(); }
			@Override public void removeUpdate(DocumentEvent e) { rebuild(); }
			@Override public void changedUpdate(DocumentEvent e) { rebuild(); }
		});
		searchBar.add(search, BorderLayout.CENTER);
		top.add(searchBar);
		for (Component component : top.getComponents())
		{
			if (component instanceof javax.swing.JComponent)
			{
				((javax.swing.JComponent) component).setAlignmentX(LEFT_ALIGNMENT);
			}
		}
		add(top, BorderLayout.NORTH);

		tabs.setUI(new javax.swing.plaf.basic.BasicTabbedPaneUI()
		{
			@Override protected void installDefaults()
			{
				super.installDefaults();
				lightHighlight = ColorScheme.BORDER_COLOR;
				shadow = ColorScheme.DARK_GRAY_COLOR;
				darkShadow = ColorScheme.DARK_GRAY_COLOR;
				focus = ColorScheme.BRAND_ORANGE;
			}

			@Override protected void paintTabBackground(Graphics g, int placement, int index, int x, int y, int w, int h, boolean selected)
			{
				g.setColor(selected ? ColorScheme.DARKER_GRAY_COLOR : ColorScheme.DARK_GRAY_COLOR);
				g.fillRect(x, y, w, h);
			}

			@Override protected void paintText(Graphics g, int placement, java.awt.Font font, java.awt.FontMetrics fm, int index, String text, java.awt.Rectangle rect, boolean selected)
			{
				g.setFont(font);
				g.setColor(selected ? ColorScheme.BRAND_ORANGE : ColorScheme.LIGHT_GRAY_COLOR);
				g.drawString(text, rect.x, rect.y + fm.getAscent());
			}
		});
		tabs.addTab("All time", scroll(allTimeList));
		tabs.addTab("Today", scroll(todayList));
		tabs.setFont(FontManager.getDefaultBoldFont().deriveFont(12f));
		tabs.setForeground(ColorScheme.TEXT_COLOR);
		tabs.setBackground(ColorScheme.DARK_GRAY_COLOR);
		tabs.addChangeListener(e -> rebuild());
		add(tabs, BorderLayout.CENTER);

		JButton resetAll = new JButton("Reset all counts");
		resetAll.setUI(new javax.swing.plaf.basic.BasicButtonUI());
		resetAll.setBorder(new EmptyBorder(9, 6, 9, 6));
		resetAll.setFocusPainted(false);
		resetAll.setFont(FontManager.getDefaultFont().deriveFont(11f));
		resetAll.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
		resetAll.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		resetAll.addActionListener(e ->
		{
			if (JOptionPane.showConfirmDialog(this, "Reset all-time and daily counts for every creature?", "Reset KC Log", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION)
			{
				plugin.reset(null);
			}
		});
		JPanel footer = new JPanel(new BorderLayout());
		footer.setOpaque(false);
		footer.add(resetAll, BorderLayout.CENTER);
		add(footer, BorderLayout.SOUTH);
		rebuild();
	}

	private static JPanel newList()
	{
		JPanel panel = new JPanel();
		panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
		panel.setOpaque(false);
		return panel;
	}

	private static JScrollPane scroll(javax.swing.JComponent component)
	{
		JScrollPane scrollPane = new JScrollPane(component, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
		scrollPane.setBorder(new EmptyBorder(8, 0, 0, 0));
		scrollPane.setViewportBorder(null);
		scrollPane.setOpaque(false);
		scrollPane.getViewport().setBackground(ColorScheme.DARK_GRAY_COLOR);
		scrollPane.getVerticalScrollBar().setUnitIncrement(24);
		return scrollPane;
	}

	void update(Map<String, Integer> allTime, Map<String, Integer> today)
	{
		this.allTime = allTime;
		this.today = today;
		rebuild();
	}

	private void rebuild()
	{
		fill(todayList, today, true);
		fill(allTimeList, allTime, false);
		Map<String, Integer> shown = tabs.getSelectedIndex() == 1 ? today : allTime;
		total.setText(numbers.format(shown.values().stream().mapToLong(Integer::longValue).sum()));
		caption.setText(tabs.getSelectedIndex() == 1 ? "KILLS TODAY" : "TOTAL KILLS");
		monsters.setText(numbers.format(shown.size()) + (shown.size() == 1 ? " creature" : " creatures"));
	}

	private void fill(JPanel target, Map<String, Integer> data, boolean isToday)
	{
		target.removeAll();
		String query = search.getText().trim().toLowerCase(Locale.ROOT);
		data.entrySet().stream()
			.filter(entry -> entry.getKey().toLowerCase(Locale.ROOT).contains(query))
			.sorted((a, b) ->
			{
				int comparison = Integer.compare(b.getValue(), a.getValue());
				return comparison != 0 ? comparison : a.getKey().compareToIgnoreCase(b.getKey());
			})
			.forEach(entry ->
			{
				target.add(row(entry.getKey(), entry.getValue()));
				target.add(Box.createVerticalStrut(4));
			});

		if (target.getComponentCount() == 0)
		{
			JLabel empty = new JLabel(data.isEmpty() ? (isToday ? "No kills today yet" : "No kills logged yet") : "No matching creatures");
			empty.setFont(FontManager.getDefaultFont().deriveFont(12f));
			empty.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
			empty.setBorder(new EmptyBorder(18, 6, 18, 6));
			empty.setAlignmentX(LEFT_ALIGNMENT);
			target.add(empty);
		}
		target.revalidate();
		target.repaint();
	}

	private JPanel row(String name, int count)
	{
		JPanel row = new JPanel(new BorderLayout(7, 0));
		row.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		row.setBorder(new EmptyBorder(8, 8, 8, 8));

		JPanel words = new JPanel(new BorderLayout(4, 0));
		words.setOpaque(false);
		JLabel label = new JLabel(name);
		label.setFont(FontManager.getDefaultFont().deriveFont(12f));
		label.setForeground(ColorScheme.TEXT_COLOR);
		words.add(label, BorderLayout.CENTER);
		JLabel countLabel = new JLabel(numbers.format(count));
		countLabel.setFont(FontManager.getDefaultBoldFont().deriveFont(13f));
		countLabel.setForeground(ColorScheme.BRAND_ORANGE);
		countLabel.setBorder(new EmptyBorder(0, 6, 0, 0));
		words.add(countLabel, BorderLayout.EAST);
		row.add(words, BorderLayout.CENTER);

		row.setToolTipText(name + " · " + numbers.format(count) + (count == 1 ? " kill" : " kills"));
		JPopupMenu menu = new JPopupMenu();
		JMenuItem reset = new JMenuItem("Reset " + name);
		reset.addActionListener(e ->
		{
			if (JOptionPane.showConfirmDialog(this, "Reset all-time and daily counts for " + name + "?", "Reset creature", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION)
			{
				plugin.reset(name);
			}
		});
		menu.add(reset);
		row.setComponentPopupMenu(menu);
		inheritMenu(row);
		row.setAlignmentX(LEFT_ALIGNMENT);
		row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
		row.setPreferredSize(new Dimension(190, 44));
		row.setMinimumSize(new Dimension(0, 44));
		return row;
	}

	private static void inheritMenu(Container parent)
	{
		for (Component component : parent.getComponents())
		{
			if (component instanceof javax.swing.JComponent)
			{
				((javax.swing.JComponent) component).setInheritsPopupMenu(true);
				if (component instanceof Container)
				{
					inheritMenu((Container) component);
				}
			}
		}
	}

	private static final class SearchIcon implements Icon
	{
		@Override public int getIconWidth() { return 14; }
		@Override public int getIconHeight() { return 14; }

		@Override
		public void paintIcon(Component component, Graphics graphics, int x, int y)
		{
			Graphics2D g = (Graphics2D) graphics.create();
			g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			g.setColor(ColorScheme.LIGHT_GRAY_COLOR);
			g.setStroke(new BasicStroke(1.5f));
			g.drawOval(x + 1, y + 1, 8, 8);
			g.drawLine(x + 8, y + 8, x + 12, y + 12);
			g.dispose();
		}
	}
}
