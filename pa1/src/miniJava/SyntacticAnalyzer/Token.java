package miniJava.SyntacticAnalyzer;

public class Token {
    private final TokenType _type;
    private final String _text;

    //private final SourcePosition _position;

    public Token(TokenType type, String text) {
        // TODO: Store the token's type and text
        this._type = type;
        this._text = text;
    }

    public TokenType getTokenType() {
        // TODO: Return the token type
        return this._type;
    }

    public String getTokenText() {
        // TODO: Return the token text
        return this._text;
    }

    public SourcePosition getTokenPosition() {
        return null;
    }
}
