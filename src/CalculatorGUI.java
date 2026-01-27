/**
 * java version "22.0.2" 2024-07-16
 * Java(TM) SE Runtime Environment (build 22.0.2+9-70)
 * Java HotSpot(TM) 64-Bit Server VM (build 22.0.2+9-70, mixed mode, sharing)
 * javac 22.0.2
 * 
 * TO-DO
 * - movable cursor in display (mutually exclusive with your approach rn, try again later)
 * - negative number support x
 * - use keyboard for input x
 * - show current equation like "1 + 1" below the display x
 * - add sin, cos, tan, sec, csc, cot x
 * - add degrees to radians and vice versa
 * - nth root, exponents, factorials, (natural) logarithms, constants
 * - binary, octal, decimal, hexadecimal notations
 * - fraction support
 * - modulo operator x
 * - parentheses and nesting expressions inside these (PEMDAS) x
 * - imaginary numbers
 * - common formula templates
 * - some buttons don't really do anything
 * - history queue
 * - proper javadocs
 * - clipboard support
 * 
 * BUGS
 * - "." on keyboard doesn't work x
 * - plus-minus button bugged (doesn't flip) x
 * - parentheses evaluates to error x
 * - plus-minus evaluates to error x
 * - backspace doesn't work x
 */

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.math.BigDecimal;	// because doubles aren't enough
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.Stack;

enum TokenType { NUMBER, OPERATOR, FUNCTION, LPAREN, RPAREN }

public class CalculatorGUI extends JFrame implements ActionListener {
	private JTextField display;
	private double num1, num2, result;
	private String operator;
	// true when the next digit should replace the display
	// false when digits should be appended
	private boolean startNewNumber = true;
	// shows the current equation string
	private JLabel equationLabel;
	private StringBuilder expression = new StringBuilder();
	// not just for trig but you can worry about that later
	private static final Set<String> TRIG_FUNCTIONS = Set.of("sin", "cos", "tan", "csc", "sec", "cot");

	static class Token {
		TokenType type;
		String value;

		Token(TokenType type, String value) {
			this.type = type;
			this.value = value;
		}
	}

	// builds a readable string like "12 + 3"
	private String currentProblemString(String currentText) {
		// if no operator is selected yet, we only have the current entry
		if (operator == null) return currentText;
		// show num1 operator current entry
		// num1 is stored when you press +, -, ×, ÷
		return num1 + " " + operator + " " + currentText;
	}

	// logs a standard message that includes what's on screen and the current "problem"
	private void logStatus(String eventLabel) {
		String currentText = display.getText();
		System.out.println("[LOG] " + eventLabel + " | display=" + currentText +
			" | problem=" + currentProblemString(currentText));
	}

	// formats a number to force to an int if that number is a whole number
	private String formatNumber(BigDecimal value) {
		if (value == null) return "Error";
    	return value.stripTrailingZeros().toPlainString(); // exact decimal display
	}

	// routes keyboard input into the same path as button presses
	private void handleInput(String command) {
		actionPerformed(new ActionEvent(this, ActionEvent.ACTION_PERFORMED, command));
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
				public void actionPerformed(ActionEvent e) { handleInput(key); }
			});
		}

		// operators (single and double quotes not interchangeable)
		bind(im, am, '+');
		bind(im, am, '-');
		bind(im, am, '*', "×");
		bind(im, am, '/', "÷");
		bind(im, am, '%');

		// decimal point
		im.put(KeyStroke.getKeyStroke(KeyEvent.VK_PERIOD, 0), ".");
		am.put(".", new AbstractAction() {
			@Override
			public void actionPerformed(ActionEvent e) { handleInput("."); }
		});

		// equals
		im.put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "=");
		am.put("=", new AbstractAction() {
			@Override
			public void actionPerformed(ActionEvent e) { handleInput("="); }
		});

		// escape - clear
		im.put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "C");
		am.put("C", new AbstractAction() {
			@Override
			public void actionPerformed(ActionEvent e) { handleInput("C"); }
		});

		// parentheses
		im.put(KeyStroke.getKeyStroke(KeyEvent.VK_9, InputEvent.SHIFT_DOWN_MASK), "(");
		am.put("(", new AbstractAction() {
			@Override
			public void actionPerformed(ActionEvent e) { handleInput("("); }
		});
		im.put(KeyStroke.getKeyStroke(KeyEvent.VK_0, InputEvent.SHIFT_DOWN_MASK), ")");
		am.put(")", new AbstractAction() {
			@Override
			public void actionPerformed(ActionEvent e) { handleInput(")"); }
		});

		// backspace
		im.put(KeyStroke.getKeyStroke(KeyEvent.VK_BACK_SPACE, 0), "BACK");
		am.put("BACK", new AbstractAction() {
			@Override
			public void actionPerformed(ActionEvent e) {
				if (expression.length() == 0) return;

				// delete last character
				expression.deleteCharAt(expression.length() - 1);
				// clean up trailing spaces for operators
				while (expression.length() > 0 && expression.charAt(expression.length() - 1) == ' ')
					expression.deleteCharAt(expression.length() - 1);

				if (expression.length() == 0) {
					display.setText("0");
					startNewNumber = true;
				} else {
					display.setText(expression.toString());
					startNewNumber = false;
				}
			}
		});
	}

	private void bind(InputMap im, ActionMap am, char key) { bind(im, am, key, String.valueOf(key)); }

	private void bind(InputMap im, ActionMap am, char key, String command) {
		im.put(KeyStroke.getKeyStroke(key), command);
		am.put(command, new AbstractAction() {
			@Override
			public void actionPerformed(ActionEvent e) { handleInput(command); }
		});
	}

	private BigDecimal evaluateExpression(String expr) {
		var tokens = tokenize(expr);
		var postfix = toPostFix(tokens);
		return evaluatePostfix(postfix);
	}

	private List<Token> tokenize(String expr) {
		List<Token> tokens = new ArrayList<>();
		int i = 0;

		while (i < expr.length()) {
			char c = expr.charAt(i);

			if (Character.isWhitespace(c)) {
				i++;
				continue;
			}

			// number (supports decimals)
			if (Character.isDigit(c) || c == '.') {
				StringBuilder num = new StringBuilder();
				while(i < expr.length() && (Character.isDigit(expr.charAt(i)) || expr.charAt(i) == '.'))
					num.append(expr.charAt(i++));
				Token t = new Token(TokenType.NUMBER, num.toString());
				maybeInsertImplicitMultiply(tokens, t);
				tokens.add(t);
				continue;
			}
			// unary minus handling
			if (c == '-' && (tokens.isEmpty() ||
				tokens.get(tokens.size() - 1).type == TokenType.OPERATOR ||
				tokens.get(tokens.size() - 1).type == TokenType.LPAREN)
			) {
				StringBuilder num = new StringBuilder("-");
				i++;
				while (i < expr.length() && (Character.isDigit(expr.charAt(i)) || expr.charAt(i) == '.'))
					num.append(expr.charAt(i++));
				tokens.add(new Token(TokenType.NUMBER, num.toString()));
				continue;
			}
			// operators
			if ("+-*/%".indexOf(c) != -1) {
				tokens.add(new Token(TokenType.OPERATOR, String.valueOf(c)));
				i++;
				continue;
			}
			// parentheses
			if (c == '(') {
				Token t = new Token(TokenType.LPAREN, "(");
				maybeInsertImplicitMultiply(tokens, t);
				tokens.add(t);
				i++;
				continue;
			}
			if (c == ')') {
				tokens.add(new Token(TokenType.RPAREN, ")"));
				i++;
				continue;
			}
			// function names (sin, cos, tan, etc.)
			if (Character.isLetter(c)) {
				StringBuilder name = new StringBuilder();
				while (i < expr.length() && Character.isLetter(expr.charAt(i)))
					name.append(expr.charAt(i++));
				String func = name.toString().toLowerCase();
				Token t = new Token(TokenType.FUNCTION, func);
				maybeInsertImplicitMultiply(tokens, t);
				tokens.add(t);
				continue;
			}
			throw new IllegalArgumentException("Invalid character: " + c);
		}
		return tokens;
	}

	private int precedence(Token t) {
		if (t.type == TokenType.FUNCTION) return 3;
		if (t.type != TokenType.OPERATOR) return 0;

		return switch (t.value) {
			case "+", "-" -> 1;
			case "*", "/", "%" -> 2;
			default -> 0;
		};
	}

	private List<Token> toPostFix(List<Token> tokens) {
		List<Token> output = new ArrayList<>();
		Stack<Token> ops = new Stack<>();

		for (Token t : tokens) {
			switch (t.type) {
				case NUMBER -> output.add(t);
				case OPERATOR -> {
					while (
						!ops.isEmpty() && (ops.peek().type == TokenType.OPERATOR || ops.peek().type == TokenType.FUNCTION) &&
						precedence(ops.peek()) >= precedence(t)
					)
						output.add(ops.pop());
					ops.push(t);
				}
				case LPAREN -> ops.push(t);
				case RPAREN -> {
					while (!ops.isEmpty() && ops.peek().type != TokenType.LPAREN)
						output.add(ops.pop());
					if (ops.isEmpty()) throw new IllegalArgumentException("Mismatched parentheses");
					ops.pop();
					// if function is on top pop it too
					if (!ops.isEmpty() && ops.peek().type == TokenType.FUNCTION)
						output.add(ops.pop());
				}
				case FUNCTION -> ops.push(t);
			}
		}

		while (!ops.isEmpty()) {
			if (ops.peek().type == TokenType.LPAREN) throw new IllegalArgumentException("Mismatched parentheses");
			output.add(ops.pop());
		}

		return output;
	}

	private BigDecimal evaluatePostfix(List<Token> postfix) {
		Stack<BigDecimal> stack = new Stack<>();

		for (Token t : postfix) {
			if (t.type == TokenType.NUMBER)
				stack.push(new BigDecimal(t.value));
			else if (t.type == TokenType.FUNCTION) {
				BigDecimal a = stack.pop();
				double radians = Math.toRadians(a.doubleValue());
				double result;

				switch (t.value) {
					case "sin" -> result = Math.sin(radians);
					case "cos" -> result = Math.cos(radians);
					case "tan" -> result = Math.tan(radians);
					case "sec" -> result = 1.0 / Math.cos(radians);
					case "csc" -> result = 1.0 / Math.sin(radians);
					case "cot" -> result = 1.0 / Math.tan(radians);
					default -> throw new IllegalArgumentException("Unknown function: " + t.value);
				}

				stack.push(BigDecimal.valueOf(result));
			} else {	// order matters here, DO NOT SWAP!
				BigDecimal b = stack.pop();
				BigDecimal a = stack.pop();

				switch (t.value) {
					case "+" -> stack.push(a.add(b));
					case "-" -> stack.push(a.subtract(b));
					case "*" -> stack.push(a.multiply(b));
					case "/" -> {
						if (b.compareTo(BigDecimal.ZERO) == 0) throw new ArithmeticException("Division by zero");
						stack.push(a.divide(b, 50, RoundingMode.HALF_UP));
					}
					case "%" -> {
						if (b.compareTo(BigDecimal.ZERO) == 0) throw new ArithmeticException("Division by zero");
						stack.push(a.remainder(b));
					}
				}
			}
		}

		return stack.pop();
	}

	private void maybeInsertImplicitMultiply(List<Token> tokens, Token next) {
		if (tokens.isEmpty()) return;

		Token prev = tokens.get(tokens.size() - 1);

		boolean implicit = (prev.type == TokenType.NUMBER || prev.type == TokenType.RPAREN) && (next.type == TokenType.LPAREN || next.type == TokenType.NUMBER);

		if (implicit)
			tokens.add(new Token(TokenType.OPERATOR, "*"));
	}

	public CalculatorGUI() {
		setTitle("JavaCalc");
		// terminate completely when X is clicked
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		// Every Swing container needs a layout manager to control component positioning and sizing. `setLayout()`
		// configures the JFrame to use BorderLayout, which divides the container into five regions, NORTH, SOUTH, EAST,
		// WEST and CENTER.
		setLayout(new BorderLayout());
		setSize(500, 400);

		// display component
		// create a text field initialized with "0" as the starting value
		display = new JTextField("0");
		display.setFont(new Font("Consolas", Font.BOLD, 20));
		// right-aligns numbers (like a real calculator would)
		display.setHorizontalAlignment(JTextField.RIGHT);
		display.setEditable(false);
		display.setCaretColor(Color.BLACK);
		display.setFocusable(true);
		display.setBorder(BorderFactory.createCompoundBorder(
			display.getBorder(),
			BorderFactory.createEmptyBorder(10, 10, 10, 10)
		));
		// disable keyboard input but keep caret functionality
		display.addKeyListener(new KeyAdapter() {
			@Override
			public void keyTyped(KeyEvent e) { e.consume(); }	// ignore typed characters
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
		equationLabel.setBorder(BorderFactory.createEmptyBorder(5, 10, 10, 10));
		// add components to the display panel
		displayPanel.add(equationLabel, BorderLayout.NORTH);
		displayPanel.add(display, BorderLayout.CENTER);
		// place the whole display panel at the top of the frame
		add(displayPanel, BorderLayout.NORTH);

		// buttons panel
		// GridBagLayout allows components to span multiple rows or columns, unlike GridLayout whcih forces all cells to
		// be the same size
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
			{ "C", "±", "%", null, "sin", "cos", "tan"},
			{ "(", ")", null, null, "csc", "sec", "cot" },	// placeholders
			{ "7", "8", "9", "÷" },
			{ "4", "5", "6", "×" },
			{ "1", "2", "3", "-" },
			{ "0", ".", "=", "+" } // 0 will span two cols
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
				// `this` points to the CalculatorGUI instance as the event handler since the class CalculatorGUI
				// implements ActionListener, every CalculatorGUI object automatically becomes an ActionListener
				// see actionPerformed() below
				button.addActionListener(this);

				// set button position
				gbc.gridx = col;
				gbc.gridy = row;

				// make 0 button twice as wide
				// if ("0".equals(text)) gbc.gridwidth = 2;
				// else gbc.gridwidth = 1;

				// add the button to the panel using the current constraints
				buttonPanel.add(button, gbc);

				// GridBagConstraints is reused, so we must reset gridwidth, otherwise the next buttons would also span
				// two columns
				gbc.gridwidth = 1;
			}
		}

		// add to center, change CENTER to something else if you want
		add(buttonPanel, BorderLayout.CENTER);
		setVisible(true);

		// request focus after the frame is visible
		SwingUtilities.invokeLater(() -> { buttonPanel.requestFocusInWindow(); });

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
				expression.append(command);
				display.setText(expression.toString());
				startNewNumber = false;
			} else {
				expression.append(command);
				display.setText(expression.toString());
			}
		} else if (".".equals(command)) {	// decimal point
			// find last operator to isolate current number
			int lastOp = Math.max(
				Math.max(expression.lastIndexOf("+"), expression.lastIndexOf("-")),
				Math.max(expression.lastIndexOf("×"), expression.lastIndexOf("÷"))
			);
			String currentNumber = lastOp == -1 ? expression.toString() : expression.substring(lastOp + 1).trim();

			// prevent multiple decimals in some number
			if (currentNumber.contains(".")) return;

			if (startNewNumber || expression.length() == 0) expression.append("0.");
			else expression.append(".");

			display.setText(expression.toString());
			startNewNumber = false;
		} else if ("+-×÷%".contains(command)) {	// operator buttons
			if (expression.length() == 0) return;

			// prevent double operators
			char last = expression.charAt(expression.length() - 1);
			if ("+-×÷ ".indexOf(last) != -1) return;

			expression.append(" ").append(command).append(" ");
			display.setText(expression.toString());
			startNewNumber = true;
		} else if (TRIG_FUNCTIONS.contains(command)) {	// trigonemetric functions
			expression.append(command).append("(");
			display.setText(expression.toString());
			startNewNumber = true;
		} else if ("()".contains(command)) {
			expression.append(command);
			display.setText(expression.toString());
			startNewNumber = false;
		} else if ("=".equals(command)) {	// equals button
			try {
				String expr = expression.toString().replace("×", "*").replace("÷", "/");
				BigDecimal result = evaluateExpression(expr);

				display.setText(formatNumber(result));
				equationLabel.setText(expression + " =");
				expression.setLength(0);	// reset
				expression.append(formatNumber(result));
				startNewNumber = true;
			} catch (Exception ex) {
				display.setText("Error");
				expression.setLength(0);
			}
		} else if ("±".equals(command)) {
			if (expression.length() == 0) return;
			try {
				// find start of current number
				int lastOp = Math.max(
					Math.max(expression.lastIndexOf("+"), expression.lastIndexOf("×")),
					Math.max(expression.lastIndexOf("÷"), expression.lastIndexOf("- "))
				);
				int start = lastOp == -1 ? 0 : lastOp + 1;
				// skip process
				while (start < expression.length() && expression.charAt(start) == ' ') start++;

				// fixes the stacked negative signs thing ("-----1")
				if (expression.charAt(start) == '-')	// already negative
					expression.deleteCharAt(start);		// make positive
				else expression.insert(start, '-');		// else the other way around

				// if (expression.charAt(start) == '-') expression.deleteCharAt(start);
				// else expression.insert(start, '-');

				display.setText(expression.toString());
			} catch (Exception ex) {
				return;
			}
		} else if ("C".equals(command)) {
			// clear
			logStatus("Pressed C (clear)");
			expression.setLength(0);
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
		// main() runs on a different thread, so SwingUtilities.invokeLater() schedules the new CalculatorGUI to execute
		// on the EDT
		// `() -> new CalculatorGUI()` is a lambda expression implementing a Runnable
		SwingUtilities.invokeLater(() -> new CalculatorGUI());
	}
}