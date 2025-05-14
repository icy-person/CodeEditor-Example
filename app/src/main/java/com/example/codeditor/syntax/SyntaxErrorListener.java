package com.example.codeditor.syntax;

import org.antlr.v4.runtime.BaseErrorListener;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Recognizer;
import java.util.ArrayList;
import java.util.List;

public class SyntaxErrorListener extends BaseErrorListener {
    private final List<SyntaxError> errors = new ArrayList<>();

    @Override
    public void syntaxError(Recognizer<?, ?> recognizer, Object offendingSymbol,
                            int line, int charPositionInLine,
                            String msg, RecognitionException e) {
        errors.add(new SyntaxError(line, charPositionInLine, msg));
    }

    public List<SyntaxError> getErrors() {
        return errors;
    }

    public static class SyntaxError {
        private final int line;
        private final int charPosition;
        private final String message;

        public SyntaxError(int line, int charPosition, String message) {
            this.line = line;
            this.charPosition = charPosition;
            this.message = message;
        }

        // Getters...
    }
}