package fop.project_cleaner.ui;

import java.awt.Color;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import javax.swing.JTextPane;
import javax.swing.text.MutableAttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;

class TextPaneStream extends ByteArrayOutputStream {
    private JTextPane textPane;
    private MutableAttributeSet attributes;

    public TextPaneStream(JTextPane textPane) {
        this(textPane, null);
    }

    public TextPaneStream(JTextPane textPane, Color fontColor) {
        this.textPane = textPane;
        attributes = new SimpleAttributeSet();
        if (fontColor != null) {
            StyleConstants.setForeground(attributes, fontColor);
        }
    }

    @Override
    public void flush() throws IOException {
        var document = textPane.getStyledDocument();
        try {
            document.insertString(document.getLength(), toString(), attributes);
        } catch (BadLocationException e) {
        }
        reset();
    }
}
