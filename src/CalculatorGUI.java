import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

/**
 * HOW TO RUN
 * javac .\CalculatorGUI.java
 * java CalculatorGUI
 * 
 * ONE LINE:
 * javac .\CalculatorGUI.java; if ($?) { java CalculatorGUI }
 * 
 * java version "22.0.2" 2024-07-16
 * Java(TM) SE Runtime Environment (build 22.0.2+9-70)
 * Java HotSpot(TM) 64-Bit Server VM (build 22.0.2+9-70, mixed mode, sharing)
 * javac 22.0.2
 */

public class CalculatorGUI extends JFrame implements ActionListener {
	private JTextField display;
	private double num1, num2, result;
	private String operator;

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
		display.setFont(new Font("Arial", Font.BOLD, 20));
		// right-aligns numbers (like a real calculator would)
		display.setHorizontalAlignment(JTextField.RIGHT);
		// prevent user typing directly into the display
		display.setEditable(false);
		// place this component in the NORTH region of BorderLayout, spanning
		// full width at the top
		add(display, BorderLayout.NORTH);

		// buttons panel
		// create panel using GridLayout with 5 rows, 4 cols and 5 px gaps
		// between cells
		JPanel buttonPanel = new JPanel(new GridLayout(5, 4, 5, 5));
		// add 10 px padding around all edges
		buttonPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

		// fix how wide some of these should appear on the window later
		String[] buttons = {
			"C", "±", "%", "÷",
			"7", "8", "9", "×",
			"4", "5", "6", "-",
			"1", "2", "3", "+",
			"0", ".", "=",
		};

		for (String btnText : buttons) {
			// create button object for each string in buttons array
			JButton button = new JButton(btnText);
			button.setFont(new Font("Arial", Font.BOLD, 18));
			// `this` points to the CalculatorGUI instance as the event handler
			// since the class CalculatorGUI implements ActionListener, every
			// CalculatorGUI object automatically becomes an ActionListener
			// see actionPerformed() below
			button.addActionListener(this);
			// add this button to the button panel
			buttonPanel.add(button);
		}

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
			// number button
			if (
				currentText.equals("0") ||		// display shows "0"
				currentText.equals("Error") ||	// error state
				operator != null				// after operator (+, -, etc.)
			) {
				display.setText(command);		// replace display with number
			} else {	// else append to existing number
				display.setText(currentText + command);
			}
		} else if ("+-×÷".contains(command)) {	// operator buttons
			// valid state check
			if (!currentText.equals("0") && !currentText.equals("Error")) {
				// store current display as first number
				num1 = Double.parseDouble(currentText);
				operator = command;		// remember which operator
				display.setText("0");	// reset display for second number
			}
		} else if ("=".equals(command)) {	// equals button
			// valid state check
			if (operator != null && !currentText.equals("Error")) {
				try {
					// get second number
					num2 = Double.parseDouble(display.getText());
					switch (operator) {	// perform calculation
						case "+": result = num1 + num2; break;
						case "-": result = num1 - num2; break;
						case "×": result = num1 * num2; break;
						case "÷":	// prevent division by 0
							if (num2 != 0) result = num1 / num2;
							else { display.setText("Error"); return; }
							break;
					}
					// format result to 2 decimal places
					display.setText(String.format("%.2f", result));
					// reset operator for new calculation
					operator = null;
				} catch (NumberFormatException ex) {
					display.setText("Error");
				}
			}
		} else if ("C".equals(command)) {
			// clear
			display.setText("0");
			num1 = num2 = result = 0;
			operator = null;
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