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
 * 		- debug values: 0, 1, 30, 90, 180, 360
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
 * - i think you really need to split this into multiple files
 * - resolve variable name ambiguity
 * - how to group digits by three (no comma)
 * 
 * BUGS
 * - "." on keyboard doesn't work x
 * - plus-minus button bugged (doesn't flip) x
 * - parentheses evaluates to error x
 * - plus-minus evaluates to error x
 * - backspace doesn't work x
 * - typing `(1 ^ 2)!` gives an error (FIND A WAY TO EVAL ANYTHING INSIDE PARENTHESES FIRST)
 * - if last answer is 0, typing anything doesn't replace the 0, instead it appends it to the end (1*0 = 0, type 9, display shows "09")
 * 
 * DEBUG PROBLEMS
 * 2! + 3! = 8
 * (1^2)! = 1
 * (22 / 7)^2 = 9.87755102041
 * sin(1) + cos(30) = 0.88347781022
 * tan(180) = 0
 * sin(cos(30)tan(90)) = 0.06744751948
 * 
 * undefined/"Error"
 *	1/0
 *	csc(0)
 *	cot(0)
 *	-1!
 * CAN'T TYPE THESE:
 * (√1)!
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

// classifies different parts of an expression for the parser
enum TokenType { NUMBER, OPERATOR, FUNCTION, FACTORIAL, LPAREN, RPAREN, ROOT }

public class CalculatorGUI extends JFrame implements ActionListener {
	private JTextField display;
	// true when the next digit should replace the display
	// false when digits should be appended
	private boolean startNewNumber = true;
	// shows the current equation string
	private JLabel equationLabel;
	private StringBuilder expression = new StringBuilder();
	// for "Ans" button
	private BigDecimal lastAnswer = BigDecimal.ZERO;
	// not just for trig but you can worry about that later
	private static final Set<String> TRIG_FUNCTIONS = Set.of("sin", "cos", "tan", "csc", "sec", "cot");

	// represents an individual piece of a math expression, paired with the TokenType enum
	static class Token {
		TokenType type;
		String value;

		Token(TokenType type, String value) {
			this.type = type;
			this.value = value;
		}
	}

	// formats a number to force to an int if that number is a whole number
	private String formatNumber(BigDecimal value) {
		if (value == null) return "Error";
		return value.stripTrailingZeros().toPlainString();
	}

	// routes keyboard input into the same path as button presses
	private void handleInput(String key) { actionPerformed(new ActionEvent(this, ActionEvent.ACTION_PERFORMED, key)); }

	// sets up comprehensive keyboard shortcuts so users can type calculator expressions instead of clicking buttons
	private void setupKeyBindings() {
		// catch keys globally across entire window
		JComponent target = getRootPane();
		// these two is Swing's keyboard shortcut system for mapping keys to actions without `KeyListener`
		InputMap im = target.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
		ActionMap am = target.getActionMap();

		// add bindings for these keys
		// digits
		for (char c = '0'; c <= '9'; c++) bind(im, am, KeyStroke.getKeyStroke(c), String.valueOf(c));
		// operators (single and double quotes not interchangeable)
		bind(im, am, KeyStroke.getKeyStroke('+'), "+");
		bind(im, am, KeyStroke.getKeyStroke('-'), "-");
		bind(im, am, KeyStroke.getKeyStroke('*'), "×");
		bind(im, am, KeyStroke.getKeyStroke('/'), "÷");
		bind(im, am, KeyStroke.getKeyStroke('%'), "%");
		// decimal point
		bind(im, am, KeyStroke.getKeyStroke('.'), ".");
		// parentheses
		bind(im, am, KeyStroke.getKeyStroke('('), "(");
		bind(im, am, KeyStroke.getKeyStroke(')'), ")");
		// factorial (!)
		bind(im, am, KeyStroke.getKeyStroke('!'), "!");
		// square root
		bind(im, am, KeyStroke.getKeyStroke('r', InputEvent.CTRL_DOWN_MASK), "√");
		// escape - clear
		bind(im, am, KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "C");
		// backspace
		bind(im, am, KeyStroke.getKeyStroke(KeyEvent.VK_BACK_SPACE, 0), "BACK");
		// equals
		bind(im, am, KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "=");
	}

	// maps keys and commands to an action
	private void bind(InputMap im, ActionMap am, KeyStroke key, String command) {
		im.put(key, command);
		am.put(command, new AbstractAction() {
			@Override
			public void actionPerformed(ActionEvent e) { handleInput(command); }
		});
	}

	private StringBuilder extractNumber(String expr, int[] pos, boolean includeMinus) {
		StringBuilder num = new StringBuilder();
		if (includeMinus) num.append('-');
		// build number char-by-char
		while (pos[0] < expr.length() && (Character.isDigit(expr.charAt(pos[0])) || expr.charAt(pos[0]) == '.'))
			num.append(expr.charAt(pos[0]++));	// grab all digits/decimals
		return num;
	}

	// tokenizes an expression string
	private List<Token> tokenize(String expr) {
		List<Token> tokens = new ArrayList<>();
		int i = 0;

		if (expr.startsWith("Ans")) {	// special case
			// replace "Ans" with whatever number
			tokens.add(new Token(TokenType.NUMBER, lastAnswer.toString()));
			i = 3;	// skip "Ans"
			maybeInsertImplicitMultiply(tokens, tokens.get(tokens.size() - 1));
		}

		while (i < expr.length()) {
			char c = expr.charAt(i);

			// space/tab/newline
			if (Character.isWhitespace(c)) {
				i++;		// skip/do nothing
				continue;	// next char
			}
			// number (supports decimals)
			if (Character.isDigit(c) || c == '.') {
				int[] pos = {i};
				StringBuilder num = extractNumber(expr, pos, false);
				i = pos[0];	// update i
				Token t = new Token(TokenType.NUMBER, num.toString());
				maybeInsertImplicitMultiply(tokens, t);	// ex. 2(3) becomes 2*(3), see that function below
				tokens.add(t);
				continue;
			}
			// unary minus handling
			if (c == '-' && (tokens.isEmpty() ||	// start of expression: "-5 + 2"
				tokens.get(tokens.size() - 1).type == TokenType.OPERATOR ||	// after operator: "2 + -5"
				tokens.get(tokens.size() - 1).type == TokenType.LPAREN)	// after left parenthesis: sin(-30)
			) {	// treat as number
				i++;	// skip past the '-' char
				int[] pos = {i};
				StringBuilder num = extractNumber(expr, pos, true);	 // start with "-"
				i = pos[0];	// grab all digits/decimals that follow
				Token t = new Token(TokenType.NUMBER, num.toString());
				maybeInsertImplicitMultiply(tokens, t);
				tokens.add(t);	// "-3.14" as one token
				continue;
			}
			// operators
			if ("+-*/%^".indexOf(c) != -1) {
				tokens.add(new Token(TokenType.OPERATOR, String.valueOf(c)));
				i++;
				continue;
			}
			// parentheses
			if (c == '(') {
				Token t = new Token(TokenType.LPAREN, "(");
				maybeInsertImplicitMultiply(tokens, t);	// see below
				tokens.add(t);
				i++;
				continue;
			}
			if (c == ')') {
				tokens.add(new Token(TokenType.RPAREN, ")"));
				i++;
				continue;
			}
			if (c == '!') {
				tokens.add(new Token(TokenType.FACTORIAL, "!"));
				i++;
				continue;
			}
			if (c == '√') {	// nth root, handles both √x and √(y)x
				tokens.add(new Token(TokenType.ROOT, "√"));
				i++;
				// check for nth root syntax
				if (i < expr.length() && expr.charAt(i) == '[') {
					i++;	// skip [
					int[] pos = {i};
					StringBuilder index = extractNumber(expr, pos, true);	// allow negative index
					i = pos[0];
					if (i >= expr.length() || expr.charAt(i) != ']')
						throw new IllegalArgumentException("Missing ] after root index");
					i++;	// skip ]
					// store as "√[index]"
					tokens.get(tokens.size() - 1).value = "√[" + index + "]";
				}
				maybeInsertImplicitMultiply(tokens, tokens.get(tokens.size() - 1));
				continue;
			}
			// function names (sin, cos, tan, etc.)
			if (Character.isLetter(c)) {	// starts with a letter, which all functions here do as of now
				StringBuilder name = new StringBuilder();
				while (i < expr.length() && Character.isLetter(expr.charAt(i)))
					name.append(expr.charAt(i++));	// grab all letters
				Token t = new Token(TokenType.FUNCTION, name.toString());	// make token
				maybeInsertImplicitMultiply(tokens, t);	// 2sin(30) -> 2 * sin(30)
				tokens.add(t);
				continue;
			}
			throw new IllegalArgumentException("Invalid character: " + c);
		}
		return tokens;
	}

	// determine operator precedence (PEMDAS)
	private int precedence(Token t) {
		if (t.type == TokenType.FACTORIAL) return 5;	// highest precedence
		if (t.type == TokenType.ROOT) return 4;
		if (t.type == TokenType.FUNCTION) return 4;
		if (t.type != TokenType.OPERATOR) return 0;
		return switch (t.value) {
			case "+", "-" -> 1;
			case "*", "/", "%" -> 2;
			case "^" -> 3;
			default -> 0;	// lowest
		};
	}

	// shunting yard algorithm
	private List<Token> toPostFix(List<Token> tokens) {
		List<Token> output = new ArrayList<>();
		Stack<Token> ops = new Stack<>();	// operator stack
		for (Token t : tokens) {
			switch (t.type) {
				case NUMBER -> output.add(t);
				case OPERATOR -> {
					while (
						!ops.isEmpty() &&	// stack is not empty
						(ops.peek().type == TokenType.OPERATOR || ops.peek().type == TokenType.FUNCTION) &&	// top of stack is an operator or function
						precedence(ops.peek()) >= precedence(t)	// top has equal or higher precedence
					)
						output.add(ops.pop());	// pop from ops and add to output
					ops.push(t);
				}
				case LPAREN -> ops.push(t);
				case RPAREN -> {
					// pop all operators until matching "("
					while (!ops.isEmpty() && ops.peek().type != TokenType.LPAREN)
						output.add(ops.pop());
					if (ops.isEmpty()) throw new IllegalArgumentException("Mismatched parentheses");
					ops.pop();	// remove "("
					// handle functions/! that were before "("
					if (!ops.isEmpty() && ops.peek().type == TokenType.FUNCTION) output.add(ops.pop());	// sin(30)
					if (!ops.isEmpty() && ops.peek().type == TokenType.FACTORIAL) output.add(ops.pop());	// (2 + 1)!
				}
				case FACTORIAL -> {
					// factorial is postfix and highest precedence
					// only pop other factorials
					while (!ops.isEmpty() && ops.peek().type == TokenType.FACTORIAL)
						output.add(ops.pop());
					ops.push(t);
				}
				case FUNCTION -> ops.push(t);
				case ROOT -> ops.push(t);
			}
		}
		// flush remaining operators to output at end of expression and catches unmatched "("s
		while (!ops.isEmpty()) {
			if (ops.peek().type == TokenType.LPAREN) throw new IllegalArgumentException("Mismatched parentheses");
			output.add(ops.pop());
		}

		// REMOVE LATER LOL
		System.out.print("Output: [");
		for (int i = 0; i < output.size(); i++) {
			System.out.print(output.get(i).value);
			if (i < output.size() - 1) System.out.print(", ");
		}
		System.out.println("]");
	
		return output;
	}

	// evaluate postfix expressions using a single stack, left to right scan, no precedence needed
	// https://en.wikipedia.org/wiki/Reverse_Polish_notation
	private BigDecimal evaluatePostfix(List<Token> postfix) {
		Stack<BigDecimal> stack = new Stack<>();
		for (Token t : postfix) {
			switch (t.type) {
				case NUMBER -> stack.push(new BigDecimal(t.value));
				case FUNCTION -> {
					BigDecimal a = stack.pop();
					double rad = Math.toRadians(a.doubleValue());
					double res;
					switch (t.value) {
						case "sin" -> res = Math.sin(rad);
						case "cos" -> res = Math.cos(rad);
						case "tan" -> res = Math.tan(rad);
						case "sec" -> res = 1.0 / Math.cos(rad);
						case "csc" -> res = 1.0 / Math.sin(rad);
						case "cot" -> res = 1.0 / Math.tan(rad);
						default -> throw new IllegalArgumentException("Unknown function: " + t.value);
					}
					stack.push(BigDecimal.valueOf(res));
				}
				case ROOT -> {
					BigDecimal x = stack.pop();	// radicand, thing inside √
					BigDecimal res;

					String rootStr = t.value;
					if (rootStr.equals("√")) {
						// square root √x
						if (x.compareTo(BigDecimal.ZERO) < 0)
							throw new ArithmeticException("Square root of a negative number");
						res = BigDecimal.valueOf(Math.sqrt(x.doubleValue()));
					} else {
						// nth root √[n]x
						String indexStr = rootStr.substring(2, rootStr.length() - 1);	// extract n from √[n]x
						BigDecimal n = new BigDecimal(indexStr);

						if (n.compareTo(BigDecimal.ZERO) == 0)
							throw new ArithmeticException("Root index cannot be zero");

						// handle negative numbers: only for odd integer roots
						boolean allowNegative = n.scale() == 0 && (n.intValueExact() % 2 == 1);
						if (x.compareTo(BigDecimal.ZERO) < 0 && !allowNegative)
							throw new ArithmeticException("Root of negative number only valid for odd integer roots");

						// nth root = x^(1/n)
						double exponent = 1.0 / n.doubleValue();
						res = BigDecimal.valueOf(Math.pow(x.doubleValue(), exponent));
					}
					stack.push(res.stripTrailingZeros());
				}
				case FACTORIAL -> {
					BigDecimal a = stack.pop();//.stripTrailingZeros();
					stack.push(factorial(a));
				}
				case OPERATOR -> {	// order matters here, DO NOT SWAP!
					BigDecimal b = stack.pop();
					BigDecimal a = stack.pop();
					switch (t.value) {
						case "+" -> stack.push(a.add(b));
						case "-" -> stack.push(a.subtract(b));
						case "*" -> stack.push(a.multiply(b));
						case "/" -> {
							if (b.compareTo(BigDecimal.ZERO) == 0) throw new ArithmeticException("Division by zero");
							// up to 50 digits
							stack.push(a.divide(b, 50, RoundingMode.HALF_UP));
						} case "%" -> {
							if (b.compareTo(BigDecimal.ZERO) == 0) throw new ArithmeticException("Division by zero");
							stack.push(a.remainder(b));
						} case "^" -> {
							// BigDecimal has no exact power for non-integers
							// you might lose precision here
							double res = Math.pow(a.doubleValue(), b.doubleValue());
							stack.push(BigDecimal.valueOf(res).stripTrailingZeros());
						}
					}
				}
			}
		}
		if (stack.size() != 1) throw new IllegalStateException("Invalid expression, stack=" + stack);
		return stack.pop();
	}

	// automatically inserts hidden multiplication for common math notation patterns
	private void maybeInsertImplicitMultiply(List<Token> tokens, Token next) {
		if (tokens.isEmpty()) return;	// nothing, skip
		Token prev = tokens.get(tokens.size() - 1);	// look at last token
		boolean implicit =
			(prev.type == TokenType.NUMBER || prev.type == TokenType.RPAREN || prev.type == TokenType.FACTORIAL) &&	// previous: number/parenthesis/factorial
			(next.type == TokenType.LPAREN || next.type == TokenType.NUMBER || next.type == TokenType.FUNCTION);	// next parenthesis/number/function
		if (implicit) tokens.add(new Token(TokenType.OPERATOR, "*"));	// insert *
	}

	// handles factorials (!), as much as possible, don't pass in ridiculously large values
	private static BigDecimal factorial(BigDecimal n) {
		if (n.compareTo(BigDecimal.ZERO) < 0)
			throw new ArithmeticException("Factorial undefined for negative numbers");
		// integer
		if (n.scale() == 0 || (n.scale() == 0 && n.remainder(BigDecimal.ONE).compareTo(BigDecimal.ZERO) == 0)) {
			int value = n.intValueExact();
			BigDecimal res = BigDecimal.ONE;
			if (value <= 1) return res;
			for (int i = 2; i <= value; i++) res = res.multiply(BigDecimal.valueOf(i));
			return res;
		}
		// gamma approximation for decimals (convert n! to Γ(n+1))
		// https://en.wikipedia.org/wiki/Gamma_function
		// close enough
		// https://en.wikipedia.org/wiki/Stirling%27s_approximation#Stirling's_formula_for_the_gamma_function
		double x = n.doubleValue() + 1.0;
		return BigDecimal.valueOf(Math.exp((x - 0.5) * Math.log(x) - x + 0.5 * Math.log(2 * Math.PI) + (1.0/12.0) - (1.0/360.0) * (1.0 / (x * x))));
	}

	private boolean isValidAfterOperator(char lastChar) { return "+-×÷%^() ".indexOf(lastChar) == -1; }

	private int findLastOperator(String expr) {
		return Math.max(
			Math.max(expr.lastIndexOf("+"), expr.lastIndexOf("-")),
			Math.max(expr.lastIndexOf("×"), expr.lastIndexOf("÷"))
		);
	}

	private void updateDisplay() { display.setText(expression.toString()); }

	public CalculatorGUI() {
		setTitle("JavaCalc");
		// terminate completely when X is clicked
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		// Every Swing container needs a layout manager to control component positioning and sizing. `setLayout()`
		// configures the JFrame to use BorderLayout, which divides the container into five regions, NORTH, SOUTH, EAST,
		// WEST and CENTER.
		setLayout(new BorderLayout());
		setSize(600, 500);

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
		// equation label
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

		// define the calculator layout as rows and columns, null represents an empty cell (used as a filler)
		// button positions not final
		String[][] grid = {
			{ "C", "±", "%", "^", "sin", "cos", "tan"},
			{ "(", ")", "<<", ">>", "csc", "sec", "cot" },
			{ "7", "8", "9", "÷", "!", "√" },
			{ "4", "5", "6", "×", "Ans", "BACK" },
			{ "1", "2", "3", "-" },
			{ "0", ".", "=", "+" }
		};

		for (int row = 0; row < grid.length; row++) {
			for (int col = 0; col < grid[row].length; col++) {
				// get button label at this grid position
				String text = grid[row][col];
				// skip empty cells, used only for layout spacing
				if (text == null) continue;
				// create button object for each string in grid array
				JButton button = new JButton(text);
				button.setFont(new Font("Tahoma", Font.BOLD, 18));
				// `this` points to the CalculatorGUI instance as the event handler since the class CalculatorGUI
				// implements ActionListener, every CalculatorGUI object automatically becomes an ActionListener
				// see actionPerformed() below
				button.addActionListener(this);
				// set button position
				gbc.gridx = col;
				gbc.gridy = row;
				// add the button to the panel using the current constraints
				buttonPanel.add(button, gbc);
			}
		}
		// add to center, change CENTER to something else if you want
		add(buttonPanel, BorderLayout.CENTER);
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
				updateDisplay();
				startNewNumber = false;
			} else {
				expression.append(command);
				updateDisplay();
			}
		} else if (".".equals(command)) {	// decimal point
			// find last operator to isolate current number
			int lastOp = findLastOperator(expression.toString());
			// extract current number being edited after the last operator
			// if true, whole expression is current number
			// if false, everything after the last operator
			String currentNumber = lastOp == -1 ? expression.toString() : expression.substring(lastOp + 1).trim();
			// prevent multiple decimals in some number
			if (currentNumber.contains(".")) return;
			// if nothing on display do "0.", else just append "." on existing number
			if (startNewNumber || expression.length() == 0) expression.append("0.");
			else expression.append(".");
			updateDisplay();
			startNewNumber = false;
		} else if ("+-×÷%^".contains(command)) {	// operator buttons
			if (expression.length() == 0) return;	// empty
			// check last character
			char last = expression.charAt(expression.length() - 1);
			// check if last char is an operator or space and blocks double operators
			if (!isValidAfterOperator(last)) return;
			// do something like " + " (space operator space)
			expression.append(" ").append(command).append(" ");
			updateDisplay();
			startNewNumber = true;
		} else if (TRIG_FUNCTIONS.contains(command)) {	// trigonemetric functions
			expression.append(command).append("(");
			updateDisplay();
			startNewNumber = true;
		} else if ("()".contains(command)) {
			expression.append(command);
			updateDisplay();
			startNewNumber = false;
		} else if ("!".equals(command)) {
			if (expression.length() == 0) return;
			char last = expression.charAt(expression.length() - 1);
			// check if last char is an operator or right parentheses and block them
			if (!isValidAfterOperator(last)) return;
			expression.append("!");
			updateDisplay();
			startNewNumber = false;
		} else if ("√".equals(command)) {
			if (expression.length() == 0 || startNewNumber) {
				expression.setLength(0);	// clear any existing "0"
				expression.append("√");
				updateDisplay();
				startNewNumber = true;
			} else {
				char last = expression.charAt(expression.length() - 1);
				if (!isValidAfterOperator(last)) return;
				expression.append("√");
				updateDisplay();
				startNewNumber = true;
			}
		} else if ("Ans".equals(command)) {
			if (lastAnswer != null) {
				expression.append(formatNumber(lastAnswer));
				updateDisplay();
				startNewNumber = false;
			}
		} else if ("BACK".equals(command)) {
			// if empty do nothing
			if (expression.length() == 0) return;
			// delete last character
			expression.deleteCharAt(expression.length() - 1);
			// clean up trailing spaces for operators
			while (expression.length() > 0 && expression.charAt(expression.length() - 1) == ' ')
				expression.deleteCharAt(expression.length() - 1);

			if (expression.length() == 0) {	// if empty
				display.setText("0");		// show "0"
				startNewNumber = true;
			} else {
				updateDisplay();
				startNewNumber = false;
			}
		} else if ("±".equals(command)) {
			if (expression.length() == 0) return;
			try {
				// find end of last number
				int end = expression.length();
				// skip trailing spaces
				while (end > 0 && expression.charAt(end - 1) == ' ') end--;
				// skip digits/decimal to find number start
				while (end > 0 && (Character.isDigit(expression.charAt(end - 1)) || expression.charAt(end - 1) == '.' || expression.charAt(end - 1) == '-')) end--;
				// now end points to start of last number including unary minus
				if (end == expression.length()) return;	// no number found
				// toggle the sign at the start of the number
				if (expression.charAt(end) == '-') expression.deleteCharAt(end);
				else expression.insert(end, '-');
				updateDisplay();
			} catch (Exception ex) {
				System.err.println("± FAIL: " + ex);
				return;
			}
		} else if ("=".equals(command)) {	// equals button
			if (expression.length() == 0) {
				display.setText("0");
				return;
			}
			try {
				String expr = expression.toString().replace("×", "*").replace("÷", "/");
				// converts user input to a precise result using shunting yard algorithm
				// https://en.wikipedia.org/wiki/Shunting_yard_algorithm
				// ex. "sin(30) + 2!" becomes:
				//	-> [sin, (, 30, ), +, 2, !]
				//	-> [30, sin, 2, !, +]
				//	-> 2.49999999999999994 or 2.5
				BigDecimal result = evaluatePostfix(toPostFix(tokenize(expr)));
				lastAnswer = result;
				display.setText(formatNumber(result));
				equationLabel.setText(expression + " =");
				expression.setLength(0);	// reset
				expression.append(formatNumber(result));
				startNewNumber = true;
			} catch (Exception ex) {
				display.setText("Error");
				expression.setLength(0);
			}
		} else if ("C".equals(command)) {
			// clear
			expression.setLength(0);
			display.setText("0");
			equationLabel.setText(" ");
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