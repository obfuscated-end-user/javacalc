/**
 * java version "22.0.2" 2024-07-16
 * Java(TM) SE Runtime Environment (build 22.0.2+9-70)
 * Java HotSpot(TM) 64-Bit Server VM (build 22.0.2+9-70, mixed mode, sharing)
 * javac 22.0.2
 * 
 * TO-DO
 * - movable cursor in display x
 * - negative number support x
 * - use keyboard for input x
 * - show current equation like "1 + 1" below the display x
 * - add sin, cos, tan, sec, csc, cot
 * - nth root, exponents, factorials, (natural) logarithms, constants
 * - binary, octal, decimal, hexadecimal notations
 * - fraction support
 * - modulo operator
 * - parentheses and nesting expressions inside these
 * - imaginary numbers
 * - common formula templates
 * - some buttons don't really do anything
 * - history queue
 * - proper javadocs
 */

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

public class CalculatorGUI extends JFrame implements ActionListener {
	private JTextField display;
	private double num1, num2, result;
	private String operator;
	// true when the next digit should replace the display
	// false when digits should be appended
	private boolean startNewNumber = true;
	// shows the current equation string
	private JLabel equationLabel;

	// builds a readable string like "12 + 3"
	private String currentProblemString(String currentText) {
		// if no operator is selected yet, we only have the current entry
		if (operator == null) return currentText;
		// show num1 operator current entry
		// num1 is stored when you press +, -, ×, ÷
		return num1 + " " + operator + " " + currentText;
	}

	// logs a standard message that includes what's on screen and the current
	// "problem"
	private void logStatus(String eventLabel) {
		String currentText = display.getText();
		System.out.println(
			"[LOG] " + eventLabel +
			" | display=" + currentText +
			" | problem=" + currentProblemString(currentText)
		);
	}

	// formats a number to force to an int if that number is a whole number
	private String formatNumber(double value) {
		// if the value is mathematically an integer, drop the .0
		if (value == Math.rint(value))
			return String.valueOf((long) value);

		// otherwise return as-is
		return String.valueOf(value);
	}

	// inserts text at the current caret position
	private void insertAtCaret(String text) {
		int pos = display.getCaretPosition();
		String current = display.getText();

		// build new string w/ insertion
		String updated = current.substring(0, pos) + text + current.substring(pos);
		display.setText(updated);
		// move caret after inserted text
		display.setCaretPosition(pos + text.length());
	}

	// routes keyboard input into the same path as button presses
	private void handleInput(String command) {
		actionPerformed(
			new ActionEvent(this, ActionEvent.ACTION_PERFORMED, command)
		);
	}

	private void setupKeyBindings() {
		JComponent target = getRootPane();
		InputMap im = target.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
		ActionMap am = target.getActionMap();

		// digits
		for (char c = '0'; c <= '9'; c++) {
			String key = String.valueOf(c);
			im.put(KeyStroke.getKeyStroke(c), key);
			am.put(key, new AbstractAction() {
				@Override
				public void actionPerformed(ActionEvent e) {
					handleInput(key);
				}
			});
		}

		// operators (single and double quotes not interchangeable)
		bind(im, am, '+');
		bind(im, am, '-');
		bind(im, am, '*', "×");
		bind(im, am, '/', "÷");
		bind(im, am, '.');

		// equals
		im.put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "=");
		am.put("=", new AbstractAction() {
			@Override
			public void actionPerformed(ActionEvent e) {
				handleInput("=");
			}
		});

		// escape - clear
		im.put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "C");
		am.put("C", new AbstractAction() {
			@Override
			public void actionPerformed(ActionEvent e) {
				handleInput("C");
			}
		});

		// backspace
		im.put(KeyStroke.getKeyStroke(KeyEvent.VK_BACK_SPACE, 0), "BACK");
		am.put("BACK", new AbstractAction() {
			@Override
			public void actionPerformed(ActionEvent e) {
				String text = display.getText();
				int pos = display.getCaretPosition();

				// nothing to delete
				if (text.isEmpty() || pos == 0) return;
				// remove character before caret
				String updated = text.substring(0, pos - 1) + text.substring(pos);
				display.setText(updated);
				// move caret back one position
				display.setCaretPosition(pos - 1);

				// if nothing left, show 0
				if (display.getText().isEmpty()) {
					display.setText("0");
					display.setCaretPosition(1);
					startNewNumber = true;
				}
			}
		});
	}

	private void bind(InputMap im, ActionMap am, char key) {
		bind(im, am, key, String.valueOf(key));
	}

	private void bind(InputMap im, ActionMap am, char key, String command) {
		im.put(KeyStroke.getKeyStroke(key), command);
		am.put(command, new AbstractAction() {
			@Override
			public void actionPerformed(ActionEvent e) {
				if ("0123456789.".contains(command)) {
					// digits and decimal go to display
					CalculatorGUI.this.insertAtCaret(command);
					startNewNumber = false;
				} else {
					// operators and equals just trigger this instead
					CalculatorGUI.this.actionPerformed(
						new ActionEvent(
							CalculatorGUI.this,
							ActionEvent.ACTION_PERFORMED,
							command
						)
					);
				}
			}
		});
	}

	public CalculatorGUI() {
		setTitle("Simple calculator");
		// terminate completely when X is clicked
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		// Every Swing container needs a layout manager to control component
		// positioning and sizing. setLayout() configures the JFrame to use
		// BorderLayout, which divides the container into five regions, NORTH,
		// SOUTH, EAST, WEST and CENTER.
		setLayout(new BorderLayout());
		setSize(300, 400);

		// display component
		// create a text field initialized with "0" as the starting value
		display = new JTextField("0");
		display.setFont(new Font("Consolas", Font.BOLD, 20));
		// right-aligns numbers (like a real calculator would)
		display.setHorizontalAlignment(JTextField.RIGHT);
		// allow caret (cursor) movement and insertion
		display.setEditable(true);
		// show caret even when not focused
		display.setCaretColor(Color.BLACK);
		display.setFocusable(true);
		display.setBorder(BorderFactory.createCompoundBorder(
			display.getBorder(),
			BorderFactory.createEmptyBorder(10, 10, 10, 10)
		));
		// disable keyboard input but keep caret functionality
		display.addKeyListener(new KeyAdapter() {
			@Override
			public void keyTyped(KeyEvent e) {
				e.consume();	// ignore typed characters
			}
		});
		// panel to hold equation (top) + main display (bottom)
		JPanel displayPanel = new JPanel();
		displayPanel.setLayout(new BorderLayout());
		// equation lbael
		equationLabel = new JLabel(" ");
		equationLabel.setFont(new Font("Consolas", Font.PLAIN, 12));
		equationLabel.setHorizontalAlignment(SwingConstants.RIGHT);
		equationLabel.setForeground(Color.GRAY);
		// add some padding so it aligns with the display text
		equationLabel.setBorder(
			BorderFactory.createEmptyBorder(5, 10, 10, 10)
		);
		// add components to the display panel
		displayPanel.add(equationLabel, BorderLayout.NORTH);
		displayPanel.add(display, BorderLayout.CENTER);
		// place the whole display panel at the top of the frame
		add(displayPanel, BorderLayout.NORTH);

		// buttons panel
		// GridBagLayout allows components to span multiple rows or columns
		// unlike GridLayout whcih forces all cells to be the same size
		JPanel buttonPanel = new JPanel(new GridBagLayout());
		// add 10 px padding around all edges
		buttonPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
		// GridBagConstraints controls how each component is placed
		GridBagConstraints gbc = new GridBagConstraints();
		// make buttons expand to fill all available space in their grid cell
		gbc.fill = GridBagConstraints.BOTH;
		// spacing between buttons
		gbc.insets = new Insets(5, 5, 5, 5);
		// allow buttons to grow evenly when the window is resized
		gbc.weightx = 1.0;
		gbc.weighty = 1.0;

		// define the calculator layout as rows and columns
		// null represents an empty cell (used as a filler)
		String[][] grid = {
			{ "C", "±", "%", "÷" },
			{ "7", "8", "9", "×" },
			{ "4", "5", "6", "-" },
			{ "1", "2", "3", "+" },
			{ "0", ".", "=", "." } // 0 will span two cols
		};

		for (int row = 0; row < grid.length; row++) {
			for (int col = 0; col < grid[row].length; col++) {
				// get button label at this grid position
				String text = grid[row][col];
				// skip empty cells, used only for layout spacing
				if (text == null) continue;

				// create button object for each string in grid array
				JButton button = new JButton(text);
				// button.setFocusable(false);
				button.setFont(new Font("Tahoma", Font.BOLD, 18));
				// `this` points to the CalculatorGUI instance as the event handler
				// since the class CalculatorGUI implements ActionListener, every
				// CalculatorGUI object automatically becomes an ActionListener
				// see actionPerformed() below
				button.addActionListener(this);

				// set button position
				gbc.gridx = col;
				gbc.gridy = row;

				// make 0 button twice as wide
				if ("0".equals(text)) gbc.gridwidth = 2;
				else gbc.gridwidth = 1;

				// add the button to the panel using the current constraints
				buttonPanel.add(button, gbc);

				// GridBagConstraints is reused, so we must reset gridwidth
				// otherwise the next buttons would also span two columns
				gbc.gridwidth = 1;
			}
		}

		// add to center, change CENTER to something else if you want
		add(buttonPanel, BorderLayout.CENTER);
		setVisible(true);

		// request focus after the frame is visible
		SwingUtilities.invokeLater(() -> {
			buttonPanel.requestFocusInWindow();
			display.setCaretPosition(display.getText().length());
		});

		setVisible(true);
		setupKeyBindings();
	}

	/**
	 * Handles button click events for all calculator operations.
	 * 
	 * @param e the ActionEvent triggered by button click
	 */
	@Override
	public void actionPerformed(ActionEvent e) {
		// returns "1", "2", "+", "=", etc.
		String command = e.getActionCommand();
		String currentText = display.getText();

		if ("0123456789".contains(command)) {	// digit buttons
			if (startNewNumber) {
				// start a new number and place caret at the end
				display.setText(command);
				display.setCaretPosition(command.length());
				startNewNumber = false;
			} else {
				// append digit to current number
				// display.setText(display.getText() + command);
				// insert digit at caret position
				insertAtCaret(command);
			}
		} else if (".".equals(command)) {	// decimal point
			String text = display.getText();
			// only allow one decimal point
			if (text.contains(".")) return;
			if (startNewNumber) {
				// start a new decimal number
				display.setText("0.");
				// "0" at idx 0, "." at 1, so insert at 2
				display.setCaretPosition(2);
				startNewNumber = false;
			} else if (!display.getText().contains(".")) {
				// only allow one decimal point
				display.setText(display.getText() + ".");
			}
		} else if ("+-×÷".contains(command)) {	// operator buttons
			// valid state check
			if (!currentText.equals("0") && !currentText.equals("Error")) {
				// store current display as first number
				num1 = Double.parseDouble(currentText);
				operator = command;	// remember which operator
				equationLabel.setText(currentText + " " + operator);
				startNewNumber = true;
				logStatus("Pressed operator " + command);
			}
		} else if ("=".equals(command)) {	// equals button
			// valid state check
			if (operator != null && !currentText.equals("Error")) {
				try {
					// get second number
					num2 = Double.parseDouble(display.getText());
					equationLabel.setText(
						formatNumber(num1) + " " + operator + " " +
						formatNumber(num2) + " ="
					);
					System.out.println(
						"[LOG] Evaluating: " + num1 + " " +
						operator + " " + num2
					);
					switch (operator) {	// perform calculation
						case "+": result = num1 + num2; break;
						case "-": result = num1 - num2; break;
						case "×": result = num1 * num2; break;
						case "÷":	// prevent division by 0
							if (num2 == 0) {
								display.setText("Error");
								startNewNumber = true;
								return;
							}
							result = num1 / num2;
							break;
					}
					// remove trailing .0 if the result is a whole number
					if (result == (long) result)
						display.setText(String.valueOf((long) result));
					else
						display.setText(String.valueOf(result));
					System.out.println("[LOG] Result: " + display.getText());
					equationLabel.setText(
						formatNumber(num1) + " " + operator + " " +
						formatNumber(num2) + " = " + display.getText()
					);
					// reset operator for new calculation
					operator = null;
					startNewNumber = true;
				} catch (NumberFormatException ex) {
					display.setText("Error");
				}
			}
		} else if ("±".equals(command)) {
			String text = display.getText();
			if (text.equals("0") || text.equals("Error")) return;	// nothing
			if (startNewNumber) {
				display.setText("-");
				display.setCaretPosition(1);
				startNewNumber = false;
				return;
			}
			if (text.startsWith("-"))
				display.setText(text.substring(1));	// remove negative sign
			else
				display.setText("-" + text);		// add negative sign
		} else if ("C".equals(command)) {
			// clear
			logStatus("Pressed C (clear)");
			display.setText("0");
			equationLabel.setText(" ");
			num1 = num2 = result = 0;
			operator = null;
			startNewNumber = true;
		}
	}

	// safely launch GUI on event dispatch thread (EDT)
	public static void main(String[] args) {
		// all Swing things must happen on the EDT
		// main() runs on a different thread, so SwingUtilities.invokeLater()
		// schedules the new CalculatorGUI to execute on the EDT
		// `() -> new CalculatorGUI()` is a lambda expression implementing
		// a Runnable
		SwingUtilities.invokeLater(() -> new CalculatorGUI());
	}
}