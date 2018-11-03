package com.borunovv.jogging.timings.filter;

import com.borunovv.core.util.Assert;

class Parser {
    private Tokenizer tokenizer;

    public Parser(String data) {
        this.tokenizer = new Tokenizer(data);
    }

    public Node parse() {
        Node expr = expression();
        Assert.isTrue(eof(), syntaxError("There are redundant symbols at the end, expected end of string."));
        return expr;
    }

    private String syntaxError(String msg) {
        return "Filter syntax error. " + msg;
    }

    private String nextToken() {
        return tokenizer.nextToken();
    }

    private String peekToken() {
        return tokenizer.peekToken();
    }

    private boolean eof() {
        return tokenizer.eof();
    }

    public void expected(String expectedToken) {
        String nextToken = tokenizer.nextToken();
        Assert.isTrue(nextToken.equals(expectedToken),
                syntaxError("Expected token: " + expectedToken + ", actual: " + nextToken));
    }

    public void expectedIgnoreCase(String expectedToken) {
        String nextToken = tokenizer.nextToken();
        Assert.isTrue(nextToken.equalsIgnoreCase(expectedToken),
                syntaxError("Expected token: " + expectedToken + ", actual: " + nextToken));
    }

    // <expression> ::= <term> OR <expression> | <term>
    // <term> ::= <factor> AND <term> | <factor>
    // <factor> ::= (<expression>) | <comparison>
    // <comparison>::= column op value
    private Node expression() {
        Node term = term();
        if (eof()) return term;
        if (peekToken().equalsIgnoreCase("OR")) {
            expectedIgnoreCase("OR");
            Node expr = expression();
            return new NodeOR(term, expr);
        }
        return term;
    }

    // <term> ::= <factor> AND <term> | <factor>
    private Node term() {
        Node factor = factor();
        if (eof()) return factor;
        if (peekToken().equalsIgnoreCase("AND")) {
            expectedIgnoreCase("AND");
            Node term = term();
            return new NodeAND(factor, term);
        }
        return factor;
    }

    // <factor> ::= (<expression>) | <comparison>
    private Node factor() {
        if (peekToken().equals("(")) {
            nextToken();
            Node expr = expression();
            expected(")");
            return new NodeBracket(expr);
        } else {
            return comparison();
        }
    }

    // <comparison>::= column op value
    private Node comparison() {
        String columnName = nextToken();
        String cmpOperation = compareOp();
        String value = nextToken();
        Assert.isTrue(!(value.equals("(") || value.equals(")")), syntaxError("Expected value to compare with."));
        return new NodeComparison(columnName, cmpOperation, value);
    }

    private String compareOp() {
        String op = nextToken();
        Assert.isTrue(op.equalsIgnoreCase("gt")
                || op.equalsIgnoreCase("ge")
                || op.equalsIgnoreCase("lt")
                || op.equalsIgnoreCase("le")
                || op.equalsIgnoreCase("eq")
                || op.equalsIgnoreCase("ne"),
                syntaxError("Undefined compare operation: '" + op + "'. Please use: [gt,lt,ge,le,eq,ne]."));
        return op;
    }


    public interface Node {
        void render(IRenderContext context);
    }

    public static class NodeBracket implements Node {
        private Node node;

        public NodeBracket(Node node) {
            this.node = node;
        }

        @Override
        public void render(IRenderContext context) {
            context.write("(");
            node.render(context);
            context.write(")");
        }
    }

    public static class NodeComparison implements Node {
        private String columnName;
        private String cmpOperation;
        private String value;

        public NodeComparison(String columnName, String cmpOperation, String value) {
            this.columnName = columnName;
            this.cmpOperation = cmpOperation;
            this.value = value;
        }

        @Override
        public void render(IRenderContext context) {
            context.comparison(columnName, cmpOperation, value);
        }
    }

    public static abstract class NodeBool implements Node {
        private String operation;
        private Node left;
        private Node right;

        public NodeBool(String operation, Node left, Node right) {
            this.operation = operation;
            this.left = left;
            this.right = right;
        }

        @Override
        public void render(IRenderContext context) {
            left.render(context);
            context.write(" " + operation + " ");
            right.render(context);
        }
    }

    public static class NodeOR extends NodeBool {
        public NodeOR(Node left, Node right) {
            super("OR", left, right);
        }
    }

    public static class NodeAND extends NodeBool {
        public NodeAND(Node left, Node right) {
            super("AND", left, right);
        }
    }
}
