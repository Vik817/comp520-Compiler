package miniJava.SyntacticAnalyzer;

import miniJava.ErrorReporter;

import java.io.IOException;
import java.io.InputStream;

public class Scanner {
    private final InputStream _in;
    private final ErrorReporter _errors;
    private StringBuilder _currentText;
    private char _currentChar;

    private boolean eot = false;

    public Scanner(InputStream in, ErrorReporter errors) {
        this._in = in;
        this._errors = errors;
        this._currentText = new StringBuilder();

        nextChar();
    }

    public Token scan() {
        // TODO: This function should check the current char to determine what the token could be.

        // TODO: Consider what happens if the current char is whitespace
        this._currentText = new StringBuilder();
        while ((_currentChar == ' ' || _currentChar == '\n' ||
                _currentChar == '\t' || _currentChar == '\r') && !eot) {
            skipIt();
        }
        // TODO: Consider what happens if there is a comment (// or /* */)
        if (_currentChar == '/') {
            skipIt();
            if (_currentChar == '/') {
                while (_currentChar != '\n' && _currentChar != '\r' && !eot) {
                    skipIt();
                }
                return scan();
            } else if (_currentChar == '*') {
                skipIt();
                while (true) {
                    if (_currentChar == '*') {
                        skipIt();
                        if (_currentChar == '/') {
                            skipIt();
                            return scan();
                        }
                    } else if (eot) {
                        return makeToken(TokenType.ERROR);
                    } else {
                        skipIt();
                    }
                }
            } else {
                _currentText = new StringBuilder("/");
                return makeToken(TokenType.OPERATOR);
            }
        }
        // TODO: What happens if there are no more tokens?
        if (eot) {
            takeIt();
            return (makeToken(TokenType.EOT));
        }
        // TODO: Determine what the token is. For example, if it is a number
        //  keep calling takeIt() until _currentChar is not a number. Then
        //  create the token via makeToken(TokenType.IntegerLiteral) and return it.
        // If it starts with a letter
        // return, if, while, static, public, private, new, this, void
        if ((_currentChar >= 'A' && _currentChar <= 'Z') ||
                (_currentChar >= 'a' && _currentChar <= 'z')) {
            takeIt();
            while (isAlphaNumeric() || _currentChar == '_') {
                takeIt();
            }
            switch (_currentText.toString()) {
                case "class":
                    return makeToken(TokenType.CLASS);
                case "return":
                    return makeToken(TokenType.RETURN);
                case "if":
                    return makeToken(TokenType.IF);
                case "else":
                    return makeToken(TokenType.ELSE);
                case "while":
                    return makeToken(TokenType.WHILE);
                case "static":
                    return makeToken(TokenType.STATIC);
                case "public":
                case "private":
                    return makeToken(TokenType.VISIBILITY);
                case "new":
                    return makeToken(TokenType.NEW);
                case "this":
                    return makeToken(TokenType.THIS);
                case "void":
                    return makeToken(TokenType.VOID);
                case "true":
                case "false":
                    return makeToken(TokenType.TRUEFALSE);
                case "int":
                    return makeToken(TokenType.INT);
                case "boolean":
                    return makeToken(TokenType.BOOLEAN);

                default: //id
                    return makeToken(TokenType.ID);
            }
        }

        switch (_currentChar) {

            case '0':
            case '1':
            case '2':
            case '3':
            case '4':
            case '5':
            case '6':
            case '7':
            case '8':
            case '9':
                while (isDigit()) {
                    takeIt();
                }
                return makeToken(TokenType.NUM);
            case '(':
                takeIt();
                return makeToken(TokenType.LPAREN);
            case ')':
                takeIt();
                return makeToken(TokenType.RPAREN);
            case '[':
                takeIt();
                return makeToken(TokenType.LBRACK);
            case ']':
                takeIt();
                return makeToken(TokenType.RBRACK);
            case '{':
                takeIt();
                return makeToken(TokenType.LCURL);
            case '}':
                takeIt();
                return makeToken(TokenType.RCURL);
            case ';':
                takeIt();
                return makeToken(TokenType.SEMICOLON);
            case ',':
                takeIt();
                return makeToken(TokenType.COMMA);
            case '.':
                takeIt();
                return makeToken(TokenType.DOT);
            case '+':
            case '*':
                takeIt();
                return makeToken(TokenType.OPERATOR);
            case '-':
                takeIt();
                return makeToken(TokenType.NEGATIVE);

        }
        if (_currentChar == '>') {
            takeIt();
            if (_currentChar == '=') {
                takeIt();
                return makeToken(TokenType.OPERATOR);
            }
            return makeToken(TokenType.OPERATOR);
        } else if (_currentChar == '<') {
            takeIt();
            if (_currentChar == '=') {
                takeIt();
                return makeToken(TokenType.OPERATOR);
            }
            return makeToken(TokenType.OPERATOR);
        } else if (_currentChar == '=') {
            takeIt();
            if (_currentChar == '=') {
                takeIt();
                return makeToken(TokenType.OPERATOR);
            }
            return makeToken(TokenType.ONEEQUAL);
        } else if (_currentChar == '!') {
            takeIt();
            if (_currentChar == '=') {
                takeIt();
                return makeToken(TokenType.OPERATOR);
            }
            return makeToken(TokenType.EXCLAMATION);
        } else if (_currentChar == '&') {
            takeIt();
            if (_currentChar == '&') {
                takeIt();
                return makeToken(TokenType.OPERATOR);
            }
        } else if (_currentChar == '|') {
            takeIt();
            if (_currentChar == '|') {
                takeIt();
                return makeToken(TokenType.OPERATOR);
            }
        }

        _errors.reportError("Unrecognized Character: " + _currentChar + " in input.");
        return makeToken(TokenType.ERROR);
    }

    private boolean isAlphaNumeric() {
		return (_currentChar >= 'A' && _currentChar <= 'Z') ||
				(_currentChar >= 'a' && _currentChar <= 'z') ||
				(_currentChar >= '0' && _currentChar <= '9');
	}

    private boolean isDigit() {
        return _currentChar >= '0' && _currentChar <= '9';
    }

    private void takeIt() {
        _currentText.append(_currentChar);
        nextChar();
    }

    private void skipIt() {
        nextChar();
    }

    private void nextChar() {
        try {
            int c = _in.read();
            _currentChar = (char) c;

            // TODO: What happens if c == -1?
            if (c == -1) {
                eot = true;
            }
            // TODO: What happens if c is not a regular ASCII character?
        } catch (IOException e) {
            // TODO: Report an error here
            _errors.reportError("Scan Error: I/O Exception!");
            eot = true;
        }
    }

    private Token makeToken(TokenType toktype) {
        // TODO: return a new Token with the appropriate type and text
        //  contained in
        return new Token(toktype, this._currentText.toString());
    }
}
