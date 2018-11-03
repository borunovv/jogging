package com.borunovv.jogging.timings.filter;

import com.borunovv.core.util.Assert;

class Tokenizer {
    private String data;
    private int pos = 0;

    public Tokenizer(String data) {
        this.data = data;
    }

    public String nextToken() {
        skipDelimiters();
        Assert.isTrue(!eof(), "Filter syntax error. Unexpected end of string.");
        String token;
        // Check short (1-char length) tokens
        char ch = data.charAt(pos);
        if (ch == '(' || ch == ')') {
            pos++;
            token = data.substring(pos - 1, pos);
        } else {
            int start = pos;
            while (!eof() && !isDelimiter(ch) && !isBracket(ch)) {
                pos++;
                if (! eof()) {
                    ch = data.charAt(pos);
                }
            }
            token = data.substring(start, pos);
        }
        skipDelimiters();
        return token;
    }

    private boolean isBracket(char ch) {
        return ch =='(' || ch == ')';
    }

    private void skipDelimiters() {
        while (!eof()) {
            char ch = data.charAt(pos);
            if (!isDelimiter(ch)) {
                break;
            }
            pos++;
        }
    }

    private boolean isDelimiter(char ch) {
        return (ch == ' ' || ch == '\t' || ch == '\r' || ch == '\n');
    }

    public boolean eof() {
        return pos == data.length();
    }

    public String peekToken() {
        int oldPos = pos;
        String token = nextToken();
        pos = oldPos;
        return token;
    }
}
