/**
 * java version "22.0.2" 2024-07-16
 * Java(TM) SE Runtime Environment (build 22.0.2+9-70)
 * Java HotSpot(TM) 64-Bit Server VM (build 22.0.2+9-70, mixed mode, sharing)
 * javac 22.0.2
 * 
 * TO-DO
 * - movable cursor in display
 * - use keyboard for input
 * - show current equation like "1 + 1" below the display
 * - more functions
 * - some buttons don't really do anything
 * - proper javadocs
 */

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

public class CalculatorGUI extends JFrame implements ActionListener {
	private JTextField display;
	private double num1, num2, result;
	private String operator;
	// trye when the next digit should replace the display
	// false when digits should be appended
	private boolean startNewNumber = true;

	// builds a readable string like "12 + 3"
	private String currentProblemString(String currentText) {
		// if no operator is selected yet, we only have the current entry
		if (operator == null)
			return currentText;
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

	public CalculatorGUI() {
		setTitle("Simple calculator");
		// terminate completely when X is clicked
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		// Every Swing container needs a layout manager to control component
		// positioning and sizing. This line configures the JFrame to use
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
		// prevent user typing directly into the display
		display.setEditable(false);
		display.setBorder(BorderFactory.createCompoundBorder(
			display.getBorder(),
			BorderFactory.createEmptyBorder(10, 10, 10, 10)
		));
		// place this component in the NORTH region of BorderLayout, spanning
		// full width at the top
		add(display, BorderLayout.NORTH);

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
				button.setFont(new Font("Arial", Font.BOLD, 18));
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
	}

	/**
	 * Handles button click events for all calculator operations.
	 * 
	 * <p>This method processes clicks from numeric buttons (0-9), operator buttons (+, -, ×, ÷),
	 * equals (=), clear (C), and other function buttons. It updates the display, manages
	 * calculator state (num1, num2, operator, result), and performs arithmetic calculations.</p>
	 * 
	 * <p><b>Number buttons (0-9):</b> Appends digits to current display or replaces it if
	 * starting new entry, after error, or after operator selection.</p>
	 * 
	 * <p><b>Operator buttons (+, -, ×, ÷):</b> Stores first operand (num1), sets operator,
	 * and resets display for second operand entry.</p>
	 * 
	 * <p><b>Equals (=):</b> Calculates result using stored operator and second operand (num2),
	 * displays result formatted to 2 decimal places, handles division by zero with "Error".</p>
	 * 
	 * <p><b>Clear (C):</b> Resets display to "0" and clears all calculator state variables.</p>
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
				// replace display when starting a new number
				display.setText(command);
				startNewNumber = false;
			} else {
				// append digit to current number
				display.setText(display.getText() + command);
			}
		} else if (".".equals(command)) {	// decimal point
			if (startNewNumber) {
				// start a new decimal number
				display.setText("0.");
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
				startNewNumber = true;
				logStatus("Pressed operator " + command);
			}
		} else if ("=".equals(command)) {	// equals button
			// valid state check
			if (operator != null && !currentText.equals("Error")) {
				try {
					// get second number
					num2 = Double.parseDouble(display.getText());
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
					// reset operator for new calculation
					operator = null;
					startNewNumber = true;
				} catch (NumberFormatException ex) {
					display.setText("Error");
				}
			}
		} else if ("C".equals(command)) {
			// clear
			logStatus("Pressed C (clear)");
			display.setText("0");
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