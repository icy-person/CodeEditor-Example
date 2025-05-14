package com.example.codeditor.syntax;

import org.antlr.v4.runtime.tree.ParseTreeWalker;
import org.antlr.v4.runtime.tree.TerminalNode;
import java.util.HashSet;
import java.util.Set;

public class SymbolCollector extends JavaParserBaseListener {
    private final Set<String> symbols = new HashSet<>();
    private final Set<String> androidClasses;

    public SymbolCollector(Set<String> androidClasses) {
        this.androidClasses = androidClasses;
    }

    @Override
    public void enterClassDeclaration(JavaParser.ClassDeclarationContext ctx) {
        symbols.add(ctx.identifier().getText());
    }

    @Override
    public void enterMethodDeclaration(JavaParser.MethodDeclarationContext ctx) {
        symbols.add(ctx.identifier().getText());
    }

    @Override
    public void enterTypeType(JavaParser.TypeTypeContext ctx) {
        String type = ctx.getText();
        if (androidClasses.contains(type)) {
            symbols.add(type);
        }
    }

    public Set<String> getSymbols() {
        return symbols;
    }
}