package miniJava.SyntacticAnalyzer;

import miniJava.ErrorReporter;

public class Parser {
    private final Scanner _scanner;
    private final ErrorReporter _errors;
    private Token _currentToken;

    public Parser(Scanner scanner, ErrorReporter errors) {
        this._scanner = scanner;
        this._errors = errors;
    }

    public void parse() {
        _currentToken = _scanner.scan();
        try {
            // The first thing we need to parse is the Program symbol
            parseProgram();
        } catch (SyntaxError e) {
        }
    }

    // Program ::= (ClassDeclaration)* eot
    private void parseProgram() throws SyntaxError {
        // TODO: Keep parsing class declarations until eot
        while (_currentToken.getTokenType() != TokenType.EOT) {
            parseClassDeclaration();
        }
        accept(TokenType.EOT);
    }

    // ClassDeclaration ::= class identifier { (FieldDeclaration|MethodDeclaration)* }
    private void parseClassDeclaration() throws SyntaxError {
        // TODO: Take in a "class" token (check by the TokenType)
        accept(TokenType.CLASS);
        //  What should be done if the first token isn't "class"?

        // TODO: Take in an identifier token
        accept(TokenType.ID);
        // TODO: Take in a {
        accept(TokenType.LCURL);
        // TODO: Parse either a FieldDeclaration or MethodDeclaration
        while (_currentToken.getTokenType() != TokenType.RCURL) {
            parseFieldDeclaration();
        }
        // TODO: Take in a }
        accept(TokenType.RCURL);
    }

    private void parseFieldDeclaration() throws SyntaxError {
        parseVisibility();
        parseAccess();
        if (_currentToken.getTokenType() == TokenType.VOID) {
            accept(TokenType.VOID);
            accept(TokenType.ID);
            parseMethodDeclaration();
        } else {
            parseType();
            accept(TokenType.ID);
            if (_currentToken.getTokenType() == TokenType.LPAREN) {
                parseMethodDeclaration();
            } else {
                accept(TokenType.SEMICOLON);
            }
        }
    }

    private void parseMethodDeclaration() throws SyntaxError {
        accept(TokenType.LPAREN);
        if (_currentToken.getTokenType() != TokenType.RPAREN) {
            parseParameterList();
        }
        accept(TokenType.RPAREN);
        accept(TokenType.LCURL);
        while (_currentToken.getTokenType() != TokenType.RCURL) {
            parseStatement();
        }
        accept(TokenType.RCURL);
    }

    private void parseVisibility() throws SyntaxError {
        if (_currentToken.getTokenType() == TokenType.VISIBILITY) {
            accept(TokenType.VISIBILITY);
        }
    }

    private void parseAccess() throws SyntaxError {
        if (_currentToken.getTokenType() == TokenType.STATIC) {
            accept(TokenType.STATIC);
        }
    }

    private void parseType() throws SyntaxError {
        TokenType type = _currentToken.getTokenType();
        if (type == TokenType.BOOLEAN) {
            accept(TokenType.BOOLEAN);
        } else if (type == TokenType.INT) {
            accept(TokenType.INT);
            if (_currentToken.getTokenType() == TokenType.LBRACK) {
                accept(TokenType.LBRACK);
                accept(TokenType.RBRACK);
            }
        } else if (type == TokenType.ID) {
            accept(TokenType.ID);
            if (_currentToken.getTokenType() == TokenType.LBRACK) {
                accept(TokenType.LBRACK);
                accept(TokenType.RBRACK);
            }
        }
    }

    private void parseParameterList() throws SyntaxError {
        parseType();
        accept(TokenType.ID);
        while (_currentToken.getTokenType() == TokenType.COMMA) {
            accept(TokenType.COMMA);
            parseType();
            accept(TokenType.ID);
        }
    }

    private void parseArgumentList() throws SyntaxError {
        parseExpression();
        while (_currentToken.getTokenType() == TokenType.COMMA) {
            accept(TokenType.COMMA);
            parseExpression();
        }
    }

    private void parseReference() throws SyntaxError {
        if (_currentToken.getTokenType() == TokenType.ID) {
            accept(TokenType.ID);
        } else if (_currentToken.getTokenType() == TokenType.THIS) {
            accept(TokenType.THIS);
        }
        while (_currentToken.getTokenType() == TokenType.DOT) {
            accept(TokenType.DOT);
            accept(TokenType.ID);
        }

    }

    private void parseStatement() throws SyntaxError {
        TokenType type = _currentToken.getTokenType();
        if (type == TokenType.LCURL) { // Statement  ::= { Statement* }
            accept(TokenType.LCURL);
            while (_currentToken.getTokenType() != TokenType.RCURL) {
                parseStatement();
            }
            accept(TokenType.RCURL);
        } else if (type == TokenType.BOOLEAN || type == TokenType.INT) { // ::= Type (BOOL or INT) id = Expression
            parseType();
            accept(TokenType.ID);
            accept(TokenType.ONEEQUAL);
            parseExpression();
            accept(TokenType.SEMICOLON);
        } else if (type == TokenType.THIS) { // Reference (THIS)
            parseReference();
            if (_currentToken.getTokenType() == TokenType.ONEEQUAL) { // ::= Reference = Expression;
                accept(TokenType.ONEEQUAL);
                parseExpression();
                accept(TokenType.SEMICOLON);
            } else if (_currentToken.getTokenType() == TokenType.LBRACK) { // ::= Reference [Expression] = Expression;
                accept(TokenType.LBRACK);
                parseExpression();
                accept(TokenType.RBRACK);
                accept(TokenType.ONEEQUAL);
                parseExpression();
                accept(TokenType.SEMICOLON);
            } else if (_currentToken.getTokenType() == TokenType.LPAREN) { // ::= Reference (ArgumentList?);
                accept(TokenType.LPAREN);
                while (_currentToken.getTokenType() != TokenType.RPAREN) {
                    parseArgumentList();
                }
                accept(TokenType.RPAREN);
                accept(TokenType.SEMICOLON);
            }
        } else if (type == TokenType.ID) { // Differentiating between Type and Reference
            accept(TokenType.ID);
            if (_currentToken.getTokenType() == TokenType.ID) { //Know it is Type
                accept(TokenType.ID);
                accept(TokenType.ONEEQUAL);
                parseExpression();
                accept(TokenType.SEMICOLON);
            } else if (_currentToken.getTokenType() == TokenType.LBRACK) { // Checking if Type or Ref
                accept(TokenType.LBRACK);
                if (_currentToken.getTokenType() == TokenType.RBRACK) { //Know this is a Type
                    accept(TokenType.RBRACK);
                    accept(TokenType.ID);
                    accept(TokenType.ONEEQUAL);
                    parseExpression();
                    accept(TokenType.SEMICOLON);
                } else { // Reference [ Expression ] = Expression;
                    parseExpression();
                    accept(TokenType.RBRACK);
                    accept(TokenType.ONEEQUAL);
                    parseExpression();
                    accept(TokenType.SEMICOLON);
                }
            } else { // Reference 100%
                while(_currentToken.getTokenType() == TokenType.DOT) {
                    accept(TokenType.DOT);
                    accept(TokenType.ID);
                }
                if (_currentToken.getTokenType() == TokenType.ONEEQUAL) { // Reference = Expression;
                    accept(TokenType.ONEEQUAL);
                    parseExpression();
                    accept(TokenType.SEMICOLON);
                } else if (_currentToken.getTokenType() == TokenType.LBRACK) { // Reference [Expression] = Expression;
                    accept(TokenType.LBRACK);
                    parseExpression();
                    accept(TokenType.RBRACK);
                    accept(TokenType.ONEEQUAL);
                    parseExpression();
                    accept(TokenType.SEMICOLON);
                } else if (_currentToken.getTokenType() == TokenType.LPAREN) { // Reference (ArgumentList?);
                    accept(TokenType.LPAREN);
                    while (_currentToken.getTokenType() != TokenType.RPAREN) {
                        parseArgumentList();
                    }
                    accept(TokenType.RPAREN);
                    accept(TokenType.SEMICOLON);
                }
            }
        } else if (type == TokenType.RETURN) { // return Expression? ;
            accept(TokenType.RETURN);
            while (_currentToken.getTokenType() != TokenType.SEMICOLON) {
                parseArgumentList();
            }
            accept(TokenType.SEMICOLON);
        } else if (type == TokenType.IF) { // if (Expression) Statement (else Statement)?
            accept(TokenType.IF);
            accept(TokenType.LPAREN);
            parseExpression();
            accept(TokenType.RPAREN);
            parseStatement();
            if (_currentToken.getTokenType() == TokenType.ELSE) {
                accept(TokenType.ELSE);
                parseStatement();
            }
        } else {
            accept(TokenType.WHILE);
            accept(TokenType.LPAREN);
            parseExpression();
            accept(TokenType.RPAREN);
            parseStatement();
        }
    }

    private void parseExpression() throws SyntaxError {
        //Should check for an OPERATOR at the end of each one, to see if we have Expression binop Expression
        TokenType theType = _currentToken.getTokenType();
        if (theType == TokenType.LPAREN) { // ( Expression )
            accept(TokenType.LPAREN);
            parseExpression();
            accept(TokenType.RPAREN);
        } else if (theType == TokenType.NUM) {
            accept(TokenType.NUM);
        } else if (theType == TokenType.TRUEFALSE) {
            accept(TokenType.TRUEFALSE);
        } else if (theType == TokenType.NEW) {
            accept(TokenType.NEW);
            if (_currentToken.getTokenType() == TokenType.ID) {
                accept(TokenType.ID);
                if (_currentToken.getTokenType() == TokenType.LBRACK) { // new id[Expression]
                    accept(TokenType.LBRACK);
                    parseExpression();
                    accept(TokenType.RBRACK);
                } else { // new id()
                    accept(TokenType.LPAREN);
                    accept(TokenType.RPAREN);
                }
            } else if (_currentToken.getTokenType() == TokenType.INT) { // new int[Expression]
                accept(TokenType.INT);
                accept(TokenType.LBRACK);
                parseExpression();
                accept(TokenType.RBRACK);
            }
        } else if (theType == TokenType.NEGATIVE) { //unop Expression
            accept(TokenType.NEGATIVE);
            parseExpression();
        } else if (theType == TokenType.EXCLAMATION) { //unop Expression
            accept(TokenType.EXCLAMATION);
            parseExpression();
        } else if (theType == TokenType.ID || theType == TokenType.THIS) { // Reference
            parseReference();
            if (_currentToken.getTokenType() == TokenType.LBRACK) { // Reference [Expression]
                accept(TokenType.LBRACK);
                parseExpression();
                accept(TokenType.RBRACK);
            } else if (_currentToken.getTokenType() == TokenType.LPAREN) { // Reference (ArgumentList?)
                accept(TokenType.LPAREN);
                while (_currentToken.getTokenType() != TokenType.RPAREN) {
                    parseArgumentList();
                }
                accept(TokenType.RPAREN);
            }
        } else {
            accept(TokenType.NUM); //Error checking
        }
        if (_currentToken.getTokenType() == TokenType.OPERATOR || _currentToken.getTokenType() == TokenType.NEGATIVE) {
            switch (_currentToken.getTokenType()) {
                case OPERATOR:
                    while (_currentToken.getTokenType() == TokenType.OPERATOR) {
                        accept(TokenType.OPERATOR);
                        parseExpression();
                    }
                case NEGATIVE:
                    while (_currentToken.getTokenType() == TokenType.NEGATIVE) {
                        accept(TokenType.NEGATIVE);
                        parseExpression();
                    }
            }
        }


    }

    // This method will accept the token and retrieve the next token.
    //  Can be useful if you want to error check and accept all-in-one.
    private void accept(TokenType expectedType) throws SyntaxError {
        if (_currentToken.getTokenType() == expectedType) {
            //System.out.println(_currentToken.getTokenType());
            //System.out.println(_currentToken.getTokenText());
            _currentToken = _scanner.scan();
            return;
        }

        // TODO: Report an error here.
        //  "Expected token X, but got Y"
        _errors.reportError("Expected: " + expectedType +
                " but got " + _currentToken.getTokenType());
        throw new SyntaxError();
    }

    class SyntaxError extends Error {
        private static final long serialVersionUID = -6461942006097999362L;
    }
}
