package com.example.codeditor;

import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.app.Activity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.codeditor.syntax.SyntaxErrorListener;
import com.example.codeditor.completion.SuggestionAdapter;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;
import org.fife.ui.rsyntaxtextarea.parser.ParseResult;
import org.fife.ui.rsyntaxtextarea.parser.Parser;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.jar.JarFile;
import java.util.stream.Collectors;

public class MainActivity extends Activity {

    private RSyntaxTextArea codeEditor;
    private RecyclerView suggestionList;
    private SuggestionAdapter suggestionAdapter;
    private Set<String> androidClasses = new HashSet<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Setup Editor
        RTextScrollPane scrollPane = findViewById(R.id.editorScrollPane);
        codeEditor = scrollPane.getTextArea();
        codeEditor.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_JAVA);
        codeEditor.setAntiAliasingEnabled(true);
        codeEditor.setCodeFoldingEnabled(true);
        codeEditor.setTheme(RSyntaxTextArea.THEME_DARK);

        // Load Android Classes
        loadAndroidClasses();

        // Setup Suggestions
        suggestionList = findViewById(R.id.suggestionList);
        suggestionList.setLayoutManager(new LinearLayoutManager(this));
        suggestionAdapter = new SuggestionAdapter();
        suggestionList.setAdapter(suggestionAdapter);

        // Auto-Completion
        codeEditor.addTextChangedListener(new TextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
                new CodeAnalysisTask().execute();
            }
            // Other methods...
        });
    }

    private void loadAndroidClasses() {
        try (InputStream is = getAssets().open("android.jar");
             JarFile jar = new JarFile(new File(getCacheDir(), "android.jar"))) {
            jar.stream().forEach(entry -> {
                if (entry.getName().endsWith(".class")) {
                    String className = entry.getName()
                            .replace("/", ".")
                            .replace(".class", "");
                    androidClasses.add(className);
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private class CodeAnalysisTask extends AsyncTask<Void, Void, List<String>> {
        private String code;
        private int cursorPos;
        private List<SyntaxError> errors = new ArrayList<>();

        @Override
        protected void onPreExecute() {
            code = codeEditor.getText();
            cursorPos = codeEditor.getCaretPosition();
        }

        @Override
        protected List<String> doInBackground(Void... voids) {
            List<String> suggestions = new ArrayList<>();
            
            try {
                // Parse Code
                JavaLexer lexer = new JavaLexer(CharStreams.fromString(code));
                CommonTokenStream tokens = new CommonTokenStream(lexer);
                JavaParser parser = new JavaParser(tokens);
                
                // Error Checking
                SyntaxErrorListener errorListener = new SyntaxErrorListener();
                parser.addErrorListener(errorListener);
                ParseTreeWalker walker = new ParseTreeWalker();
                SymbolCollector collector = new SymbolCollector(androidClasses);
                walker.walk(collector, parser.compilationUnit());
                errors = errorListener.getErrors();

                // Auto-Completion
                String prefix = extractPrefix(code, cursorPos);
                suggestions = collector.getSymbols().stream()
                        .filter(s -> s.startsWith(prefix))
                        .collect(Collectors.toList());
                        
            } catch (Exception e) {
                e.printStackTrace();
            }
            
            return suggestions;
        }

        @Override
        protected void onPostExecute(List<String> suggestions) {
            // Show Suggestions
            if (suggestions.isEmpty()) {
                suggestionList.setVisibility(View.GONE);
            } else {
                suggestionAdapter.setSuggestions(suggestions);
                suggestionList.setVisibility(View.VISIBLE);
            }
            
            // Show Errors
            codeEditor.getUnderlineHighlighter().removeAllHighlights();
            for (SyntaxError error : errors) {
                int start = codeEditor.getLineStartOffset(error.getLine() - 1);
                int end = start + error.getCharPosition();
                codeEditor.getUnderlineHighlighter().addHighlight(
                    start, end, new Color(255, 0, 0)
                );
            }
        }

        private String extractPrefix(String code, int cursorPos) {
            int start = cursorPos - 1;
            while (start >= 0 && Character.isJavaIdentifierPart(code.charAt(start))) {
                start--;
            }
            return code.substring(start + 1, cursorPos);
        }
    }

    // Theme Menu
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.theme_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.theme_dark:
                codeEditor.setTheme(RSyntaxTextArea.THEME_DARK);
                return true;
            case R.id.theme_light:
                codeEditor.setTheme(RSyntaxTextArea.THEME_LIGHT);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
}