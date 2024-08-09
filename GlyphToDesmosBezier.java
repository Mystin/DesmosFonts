// Converts the data value of SVG glyphs into Bezier curves and lines which can be pasted directly into Desmos

import java.util.*;
import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.*;
import java.io.*;
import java.awt.event.*;

public class GlyphToDesmosBezier extends JFrame implements ActionListener {
	
	private ArrayList<String> glyphs;
	private ArrayList<Integer> glyphIndexes;
	
	public GlyphToDesmosBezier() {
		
		glyphs = parseGlyphs(chooseFile());
		glyphIndexes = new ArrayList<Integer>();
		
		setTitle("SVG To Desmos Bezier: Glyph Selector");
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		getContentPane().setPreferredSize(new Dimension(620, 500));
		setResizable(false);
		setLayout(null);
		setLocationRelativeTo(null);
		
		JScrollPane scroll = new JScrollPane();
		scroll.setBounds(0, 0, 620, 400);
		scroll.getVerticalScrollBar().setUnitIncrement(16);
		
		JPanel grid = new JPanel();
		grid.setPreferredSize(new Dimension(600, ((glyphs.size() - 1) / 8 + 1) * 25));
		grid.setLayout(new GridLayout((glyphs.size() - 1) / 8 + 1, 8, 1, 1));
		grid.setBackground(Color.BLACK);
		
		for (int i = 0; i < glyphs.size(); i++) {
			
			String content = glyphs.get(i);
			int start = content.indexOf("unicode=\"") + 9;
			
			grid.add(new GlyphButton(content.substring(start, content.indexOf('\"', start)), i));
		}
		
		scroll.setViewportView(grid);
		add(scroll);
		
		JButton copy = new JButton("Copy To Clipboard");
		copy.setBounds(125, 425, 150, 50);
		copy.addActionListener(this);
		
		JButton cancel = new JButton("Cancel");
		cancel.setBounds(325, 425, 150, 50);
		cancel.addActionListener(this);
		
		add(copy);
		add(cancel);
		
		pack();
		setVisible(true);
	}
	
	public void actionPerformed(ActionEvent ae) {
		
		if (ae.getActionCommand().equals("Cancel"))
			System.exit(-1);
		
		else if (glyphIndexes.size() > 0) {
			
			String out = processGlyph(glyphs.get(glyphIndexes.get(0)));
			
			for (int i = 1; i < glyphIndexes.size(); i++)
				out += "\n" + processGlyph(glyphs.get(glyphIndexes.get(i)));
			
	        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(out), null);
	        System.exit(-1);
		}
	}
	
	public static void main(String[] args) {
		
		try {
			UIManager.setLookAndFeel("com.sun.java.swing.plaf.windows.WindowsLookAndFeel");
			
		} catch(Exception e) {
			
			System.out.println("That look and feel is not found");
			System.exit(-1);
		}
		
		new GlyphToDesmosBezier();
	}
	
	// Returns the user's chosen SVG file
	private static File chooseFile() {
		
		// Set up chooser
		JFileChooser jfc = new JFileChooser();
		jfc.setFileFilter(new javax.swing.filechooser.FileFilter() {public String getDescription() {return "SVG Images (*.svg)";} public boolean accept(File f) {if (f.isDirectory()) return true; else return f.getName().toLowerCase().endsWith(".svg");}});
		int result = jfc.showOpenDialog(null);
		
		if (result == JFileChooser.APPROVE_OPTION)
			return jfc.getSelectedFile();
		
		System.exit(-1);
		return null;
	}
	
	// Parse the appropriate glyph tags from the given SVG file
	private static ArrayList<String> parseGlyphs(File f){
		
		String text = "";
		Scanner s = null;
		
		try {
			s = new Scanner(f);
			
		} catch (FileNotFoundException e) {
			
			System.out.println(e);
			System.exit(-1);
		}
		
		while (s.hasNextLine()) text += s.nextLine();
		
		// Parse collected text for glyph tags containing all requirements, add to output
		int start = text.indexOf("<glyph");
		ArrayList<String> out = new ArrayList<String>();
		
		while (start != -1) {
			
			start += 6;
			int end = text.indexOf('>', start) ;
			String contents = text.substring(start, end);
			
			if (contents.indexOf("unicode=") != -1 && contents.indexOf("d=") != -1)
				out.add(contents);
			
			start = text.indexOf("<glyph", end + 1);
		}
		
		return out;
	}
	
	// Processes the data in an SVG glyph tag into a Desmos expression
	private static String processGlyph(String d) {
		
		char ch = d.charAt(d.indexOf("unicode=\"") + 9);
		String out = "g_{lyph" + ch + "}\\left(P,\\ t,\\ s_{cale}\\right)=P+\\frac{s_{cale}}{1500}\\left\\{";
		
		int dStart = d.indexOf(" d=\"") + 4;
		d = d.substring(dStart, d.indexOf("\"", dStart));
		
		// Parse data into curves and lines
		double[] start = new double[2];
		double[] ctrl = new double[4];
		double[] end = new double[2];
		String[] nums;
		int next = nextCommand(d, 0);
		int total = 0;
		int count = 1;
		char lastCh;

		// Total piecewise branches
		double x = 0;
		double y = 0;
		while (next != -1) {
			
			ch = d.charAt(next);
			next++;
			
			// If command creates piecewise branch, add to total and track pos change
			if ("lvhqtcs".indexOf(ch) != -1) {
				nums = d.substring(next, nextCommand(d, next)).split(" ");
				
				if (ch == 'v')
					y += Double.parseDouble(nums[nums.length - 1]);
				
				else if (ch == 'h')
					x += Double.parseDouble(nums[0]);
				
				else {
					
					x += Double.parseDouble(nums[nums.length - 2]);
					y += Double.parseDouble(nums[nums.length - 1]);
				}
				
				total++;
			}
			// Check if z is already connected, draw line to M if not
			else if (ch == 'z') {
				
				if (x == 0 && y == 0)
					d = d.substring(0, next - 1) + d.substring(next);
				
				else {
					
					d = d.substring(0, next - 1) + 'l' + (0 - x) + ' ' + (0 - y) + d.substring(next);
					x = 0;
					y = 0;
					total++;
				}
			}
			else {System.out.println("Problematic command: " + ch); System.exit(-1);}
			
			next = nextCommand(d, next);
		}
		
		next = nextCommand(d, 0);
		while (next != -1) {
			
			// Identify command, isolate numbers, and progress command iterator
			lastCh = ch;
			ch = d.charAt(next);
			
			next++;
			int temp = nextCommand(d, next);
			
			if (temp != -1)
				nums = d.substring(next, temp).split(" ");
			else
				nums = d.substring(next).split(" ");
			
			next = nextCommand(d, next);
			
			// Command cases
			if (ch == 'M') {
				
				start[0] = Double.parseDouble(nums[0]);
				start[1] = Double.parseDouble(nums[1]);
			}
			else if (ch == 'l') {
				
				end[0] = Double.parseDouble(nums[0]);
				end[1] = Double.parseDouble(nums[1]);
				
				out = addLine(out, count, total, start, end);
				count++;
				
				start[0] += end[0];
				start[1] += end[1];
			}
			else if (ch == 'v') {
				
				end[0] = 0;
				end[1] = Double.parseDouble(nums[0]);
				
				out = addLine(out, count, total, start, end);
				count++;
				
				start[1] += end[1];
			}
			else if (ch == 'h') {
				
				end[0] = Double.parseDouble(nums[0]);
				end[1] = 0;
				
				out = addLine(out, count, total, start, end);
				count++;
				
				start[0] += end[0];
			}
			else if (ch == 'q') {
				
				ctrl[0] = start[0] + Double.parseDouble(nums[0]);
				ctrl[1] = start[1] + Double.parseDouble(nums[1]);
				end[0] = start[0] + Double.parseDouble(nums[2]);
				end[1] = start[1] + Double.parseDouble(nums[3]);
				
				out = addBezier2(out, count, total, start, ctrl, end);
				count++;
				
				start[0] = end[0];
				start[1] = end[1];
			}
			else if (ch == 't') {
				
				if (lastCh == 'q' || lastCh == 't') {
					ctrl[0] = 2 * start[0] - ctrl[0];
					ctrl[1] = 2 * start[1] - ctrl[1];
				} else {
					ctrl[0] = start[0];
					ctrl[1] = start[1];
				}
				
				end[0] = start[0] + Double.parseDouble(nums[0]);
				end[1] = start[1] + Double.parseDouble(nums[1]);
				
				out = addBezier2(out, count, total, start, ctrl, end);
				count++;
				
				start[0] = end[0];
				start[1] = end[1];
			}
			else if (ch == 'c') {
				
				ctrl[0] = start[0] + Double.parseDouble(nums[0]);
				ctrl[1] = start[1] + Double.parseDouble(nums[1]);
				ctrl[2] = start[0] + Double.parseDouble(nums[2]);
				ctrl[3] = start[1] + Double.parseDouble(nums[3]);
				end[0] = start[0] + Double.parseDouble(nums[4]);
				end[1] = start[1] + Double.parseDouble(nums[5]);
				
				out = addBezier3(out, count, total, start, ctrl, end);
				count++;
				
				start[0] = end[0];
				start[1] = end[1];
			}
			else if (ch == 's') {
				
				if (lastCh == 'c' || lastCh == 's') {
					ctrl[0] = 2 * start[0] - ctrl[2];
					ctrl[1] = 2 * start[1] - ctrl[3];
				} else {
					ctrl[0] = start[0];
					ctrl[1] = start[1];
				}
				
				ctrl[2] = start[0] + Double.parseDouble(nums[0]);
				ctrl[3] = start[1] + Double.parseDouble(nums[1]);
				end[0] = start[0] + Double.parseDouble(nums[2]);
				end[1] = start[1] + Double.parseDouble(nums[3]);
				
				out = addBezier3(out, count, total, start, ctrl, end);
				count++;
				
				start[0] = end[0];
				start[1] = end[1];
			}
		}
		
		return out + "\\right\\}";
	}
	
	// Adds a new linear statement
	private static String addLine(String out, int count, int total, double[] start, double[] relEnd) {
		
		if (count != 1) out += ",\\ ";
		
		int gcd = gcd(count, total);
		int tempCount = count / gcd;
		int tempTotal = total / gcd;
		
		if (count != total) out += "t<\\frac{" + tempCount + "}{" + tempTotal + "}: ";
		
		out += "\\left(" + start[0] + ",\\ " + start[1] + "\\right)+";
		
		if (count != 1) out += "\\left(" + total + "t-" + (count - 1) + "\\right)";
		else out += total + "t";
		
		return out + "\\left(" + relEnd[0] + ",\\ " + relEnd[1] + "\\right)";
	}

	// Adds a new quadratic bezier curve segment
	private static String addBezier2(String out, int count, int total, double[] start, double[] ctrl, double[] end) {

		if (count != 1) out += ",\\ ";
		
		int gcd = gcd(count, total);
		int tempCount = count / gcd;
		int tempTotal = total / gcd;
		
		if (count != total) out += "t<\\frac{" + tempCount + "}{" + tempTotal + "}: ";
		
		out += "b_{ezier2}\\left(\\left(" + start[0] + ",\\ " + start[1] + "\\right),\\ \\left(" + ctrl[0] + ",\\ " + ctrl[1] + "\\right),\\ \\left(" + end[0] + ",\\ " + end[1] + "\\right),\\ " + total + "t";
		
		if (count != 1) out += "-" + (count - 1);
		
		return out + "\\right)";
	}
	
	// Adds a new cubic bezier curve segment
	private static String addBezier3(String out, int count, int total, double[] start, double[] ctrl, double[] end) {

		if (count != 1) out += ",\\ ";
		
		int gcd = gcd(count, total);
		int tempCount = count / gcd;
		int tempTotal = total / gcd;
		
		if (count != total) out += "t<\\frac{" + tempCount + "}{" + tempTotal + "}: ";
		
		out += "b_{ezier3}\\left(\\left(" + start[0] + ",\\ " + start[1] + "\\right),\\ \\left(" + ctrl[0] + ",\\ " + ctrl[1] + "\\right),\\ \\left(" + ctrl[2] + ",\\ " + ctrl[3] + "\\right),\\ \\left(" + end[0] + ",\\ " + end[1] + "\\right),\\ " + total + "t";
		
		if (count != 1) out += "-" + (count - 1);
		
		return out + "\\right)";
	}
	
	// Finds the index of the next command (M, l, v, h, q, t, c, s, z), starting search at start
	private static int nextCommand(String d, int start) {
		
		char[] commands = {'M', 'l', 'v', 'h', 'q', 't', 'c', 's', 'z'};
		int toRet = d.length();
		
		for (int i = 0; i < commands.length; i++) {
			
			int temp = d.indexOf(commands[i], start);
			if (temp < toRet && temp != -1)
				toRet = temp;
		}
		
		if (toRet == d.length()) return -1;
		return toRet;
	}
	
	private static int gcd(int a, int b) {
		
		if (b == 0)
			return a;
		
		return gcd(b, a % b);
	}
	
	private class GlyphButton extends JPanel implements MouseListener{
		
		private boolean selected = false;
		private int index;
		
		private GlyphButton(String lab, int ind) {
			
			setBackground(Color.WHITE);
			this.addMouseListener(this);
			
			JLabel j = new JLabel(lab);
			add(j);
			
			index = ind;
		}
		
		public void mouseClicked(MouseEvent m) {
			
			if (selected) {
				
				selected = false;
				glyphIndexes.remove((Integer) index);
				setBackground(Color.WHITE);
			}
			else {
				
				selected = true;
				glyphIndexes.add(index);
				setBackground(Color.GREEN);
			}
		}

		public void mouseEntered(MouseEvent m) {}
		public void mouseExited(MouseEvent m) {}
		public void mousePressed(MouseEvent m) {}
		public void mouseReleased(MouseEvent m) {}
	}
}