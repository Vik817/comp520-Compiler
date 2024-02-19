package miniJava.SyntacticAnalyzer;

import miniJava.ErrorReporter;
import miniJava.AbstractSyntaxTrees.Package;
import miniJava.AbstractSyntaxTrees.*;

import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;

public class Parser {
    private final Scanner _scanner;
    private final ErrorReporter _errors;
    private Token _currentToken;

    public FieldDeclList fieldList = new FieldDeclList();
    public MethodDeclList methodList = new MethodDeclList();
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
    private Package parseProgram() throws SyntaxError {
        // TODO: Keep parsing class declarations until eot
        ClassDeclList classList = new ClassDeclList();
        while (_currentToken.getTokenType() != TokenType.EOT) {
            classList.add(parseClassDeclaration());
        }
        accept(TokenType.EOT);
        return new Package(classList, null);
    }

    // ClassDeclaration ::= class identifier { (FieldDeclaration|MethodDeclaration)* }
    private ClassDecl parseClassDeclaration() throws SyntaxError {
        // TODO: Take in a "class" token (check by the TokenType)
        accept(TokenType.CLASS);
        //  What should be done if the first token isn't "class"?

        // TODO: Take in an identifier token
        String className = _currentToken.getTokenText();
        accept(TokenType.ID);
        // TODO: Take in a {
        accept(TokenType.LCURL);
        // TODO: Parse either a FieldDeclaration or MethodDeclaration
        while (_currentToken.getTokenType() != TokenType.RCURL) {
            parseFieldDeclaration();
        }
        // TODO: Take in a }
        accept(TokenType.RCURL);
        return new ClassDecl(className, fieldList, methodList, null);
    }

    private MemberDecl parseFieldDeclaration() throws SyntaxError {
        FieldDecl fieldD;
        boolean isPriv = parseVisibility();
        boolean isStatic = parseAccess();
        if (_currentToken.getTokenType() == TokenType.VOID) {
            accept(TokenType.VOID);
            String name = _currentToken.getTokenText();
            accept(TokenType.ID);
            //Create a fieldDecl and use it as a parameter in making a MethodDecl
            fieldD = new FieldDecl(isPriv, isStatic, new BaseType(TypeKind.VOID, null), name, null);
            methodList.add(parseMethodDeclaration(fieldD));
            return null;
        } else {
            TypeDenoter t = parseType();
            String name = _currentToken.getTokenText();
            accept(TokenType.ID);
            fieldD = new FieldDecl(isPriv, isStatic, t, name, null);
            if (_currentToken.getTokenType() == TokenType.LPAREN) {
                methodList.add(parseMethodDeclaration(fieldD));
                return null;
            } else {
                accept(TokenType.SEMICOLON);
            }
            //Add to FieldDeclList
            fieldList.add(fieldD);
            return fieldD;
        }
    }

    private MethodDecl parseMethodDeclaration(MemberDecl mem) throws SyntaxError {
        accept(TokenType.LPAREN);
        ParameterDeclList parList = new ParameterDeclList();
        StatementList stateList = new StatementList();
        if (_currentToken.getTokenType() != TokenType.RPAREN) {
            parList = parseParameterList();
        }
        accept(TokenType.RPAREN);
        accept(TokenType.LCURL);
        while (_currentToken.getTokenType() != TokenType.RCURL) {
            stateList.add(parseStatement());
        }
        accept(TokenType.RCURL);
        return new MethodDecl(mem, parList, stateList, null);
    }

    private boolean parseVisibility() throws SyntaxError {
        if (_currentToken.getTokenType() == TokenType.VISIBILITY) {
            if(_currentToken.getTokenText().equals("private")) {
                accept(TokenType.VISIBILITY);
                return false;
            }
            accept(TokenType.VISIBILITY);
        }
        return true;
    }

    private boolean parseAccess() throws SyntaxError {
        if (_currentToken.getTokenType() == TokenType.STATIC) {
            accept(TokenType.STATIC);
            return true;
        }
        return false;
    }

    // NEED TO FIX PARSETYPE
    private TypeDenoter parseType() throws SyntaxError {
        TokenType type = _currentToken.getTokenType();
        TypeDenoter tD;
        if (type == TokenType.BOOLEAN) {
            accept(TokenType.BOOLEAN);
            tD = new TypeDenoter(TypeKind.BOOLEAN, null) {
                @Override
                public <A, R> R visit(Visitor<A, R> v, A o) {
                    return null;
                }
            };
            return tD;

        } else if (type == TokenType.INT) {
            accept(TokenType.INT);
            if (_currentToken.getTokenType() == TokenType.LBRACK) {
                accept(TokenType.LBRACK);
                accept(TokenType.RBRACK);
                tD = new TypeDenoter(TypeKind.ARRAY, null) {
                    @Override
                    public <A, R> R visit(Visitor<A, R> v, A o) {
                        return null;
                    }
                };
                return tD;
            }
            tD = new TypeDenoter(TypeKind.INT, null) {
                @Override
                public <A, R> R visit(Visitor<A, R> v, A o) {
                    return null;
                }
            };
            return tD;
        } else if (type == TokenType.ID) {
            accept(TokenType.ID);
            if (_currentToken.getTokenType() == TokenType.LBRACK) {
                accept(TokenType.LBRACK);
                accept(TokenType.RBRACK);
                tD = new TypeDenoter(TypeKind.ARRAY, null) {
                    @Override
                    public <A, R> R visit(Visitor<A, R> v, A o) {
                        return null;
                    }
                };
                return tD;
            }
            tD = new TypeDenoter(TypeKind.CLASS, null) {
                @Override
                public <A, R> R visit(Visitor<A, R> v, A o) {
                    return null;
                }
            };
            return tD;
        }
        return null;
    }

    private ParameterDeclList parseParameterList() throws SyntaxError {
        ParameterDeclList pList = new ParameterDeclList();
        TypeDenoter t = parseType();
        String name = _currentToken.getTokenText();
        accept(TokenType.ID);
        pList.add(new ParameterDecl(t, name, null));
        while (_currentToken.getTokenType() == TokenType.COMMA) {
            accept(TokenType.COMMA);
            t = parseType();
            name = _currentToken.getTokenText();
            accept(TokenType.ID);
            pList.add(new ParameterDecl(t, name, null));
        }
        return pList;
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

    private Statement parseStatement() throws SyntaxError {
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
        } else { //while (Expression) Statement
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
        //Below is for when you have Expression binop Expression
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
