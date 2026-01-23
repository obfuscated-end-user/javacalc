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
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setLayout(new BorderLayout());
		setSize(300, 400);

		// display
		display = new JTextField("0");
		display.setFont(new Font("Arial", Font.BOLD, 20));
		display.setHorizontalAlignment(JTextField.RIGHT);
		display.setEditable(false);
		add(display, BorderLayout.NORTH);

		// buttons panel
		JPanel buttonPanel = new JPanel(new GridLayout(5, 4, 5, 5));
		buttonPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

		String[] buttons = {
			"C", "±", "%", "÷",
			"7", "8", "9", "×",
			"4", "5", "6", "-",
			"1", "2", "3", "+",
			"0", ".", "=", " "
		};

		for (String btnText : buttons) {
			JButton button = new JButton(btnText);
			button.setFont(new Font("Arial", Font.BOLD, 18));
			button.addActionListener(this);
			buttonPanel.add(button);
		}

		add(buttonPanel, BorderLayout.CENTER);
		setVisible(true);
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		String command = e.getActionCommand();
		String currentText = display.getText();

		if ("0123456789".contains(command)) {
			// number button
			if (currentText.equals("0") || currentText.equals("Error") || operator != null) {
				display.setText(command);
			} else {
				display.setText(currentText + command);
			}
		} else if ("+-×÷".contains(command)) {
			// operator button
			if (!currentText.equals("0") && !currentText.equals("Error")) {
				num1 = Double.parseDouble(currentText);
				operator = command;
				display.setText("0");
			}
		} else if ("=".equals(command)) {
			// equals button
			if (operator != null && !currentText.equals("Error")) {
				try {
					num2 = Double.parseDouble(display.getText());
					switch (operator) {
						case "+": result = num1 + num2; break;
						case "-": result = num1 - num2; break;
						case "×": result = num1 * num2; break;
						case "÷":
							if (num2 != 0) result = num1 / num2;
							else { display.setText("Error"); return; }
							break;
					}
					display.setText(String.format("%.2f", result));
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

	public static void main(String[] args) {
		SwingUtilities.invokeLater(() -> new CalculatorGUI());
	}
}