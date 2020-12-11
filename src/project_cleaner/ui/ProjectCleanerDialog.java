package project_cleaner.ui;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Insets;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.GroupLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTextField;
import javax.swing.JTextPane;
import javax.swing.WindowConstants;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.text.DefaultCaret;

import project_cleaner.ProjectCleaner;

import static javax.swing.GroupLayout.Alignment.BASELINE;
import static javax.swing.GroupLayout.Alignment.LEADING;
import static javax.swing.GroupLayout.Alignment.TRAILING;

public class ProjectCleanerDialog extends JFrame {
    private static final long serialVersionUID = -2309973188235861250L;

    private JTextField submissionsFileField;
    private JTextField solutionFileField;
    private JTextField fileListFileField;
    private JTextField outputDirectoryField;

    private JButton executeButton;

    private JTextPane logTextPane;

    public ProjectCleanerDialog() {
        super("Eclipse Project Cleaner");
        setup();
        updateExecuteButtonState();
    }

    public void start() {
        pack();
        setSize(800, 500);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setVisible(true);
    }

    public PrintStream getOut() {
        return new PrintStream(
            new TextPaneStream(logTextPane), true);
    }

    public PrintStream getErr() {
        return new PrintStream(
            new TextPaneStream(logTextPane, Color.RED), true);
    }

    private void setup() {
        var contentPane = getContentPane();
        contentPane.setLayout(new BoxLayout(contentPane, BoxLayout.PAGE_AXIS));
        contentPane.add(fileChooserPane());
        contentPane.add(new JSeparator());
        contentPane.add(controlPane());
        contentPane.add(new JSeparator());
        contentPane.add(logPane());
    }

    private void updateExecuteButtonState() {
        executeButton.setEnabled(
            !submissionsFileField.getText().isBlank()
            && !solutionFileField.getText().isBlank()
            && !fileListFileField.getText().isBlank()
            && !outputDirectoryField.getText().isBlank());
    }

    private JPanel logPane() {
        var panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.PAGE_AXIS));

        logTextPane = new JTextPane();
        logTextPane.setEditable(false);
        logTextPane.setMargin(new Insets(5, 5, 5, 5));
        logTextPane.setPreferredSize(
            new Dimension(Short.MAX_VALUE, Short.MAX_VALUE));

        var caret = (DefaultCaret) logTextPane.getCaret();
        caret.setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE);

        panel.add(new JScrollPane(logTextPane));

        var saveLogButton = new JButton("Log speichern…");

        saveLogButton.addActionListener(e -> {
            var outputDirectory = new File(outputDirectoryField.getText());
            var logFile = outputDirectory.toPath().resolve("log.txt").toFile();
            var chooser = new JFileChooser(outputDirectory);
            chooser.setSelectedFile(logFile);
            var returnState = chooser.showSaveDialog(this);
            if (returnState == JFileChooser.APPROVE_OPTION) {
                try {
                    Files.writeString(
                        chooser.getSelectedFile().toPath(),
                        logTextPane.getText());
                } catch (IOException ex) {
                    System.err.println("Log konnte nicht gespeichert werden.");
                }
            }
        });

        saveLogButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        panel.add(saveLogButton);

        return panel;
    }

    private JPanel controlPane() {
        var panel = new JPanel();

        var extractCheckBox = new JCheckBox("Abgaben entpacken");
        extractCheckBox.setSelected(true);

        executeButton = new JButton("Ausführen!");
        executeButton.addActionListener(e -> {
            try {
                var cleaner = new ProjectCleaner(
                    new File(submissionsFileField.getText()),
                    new File(solutionFileField.getText()),
                    new File(fileListFileField.getText()),
                    new File(outputDirectoryField.getText()));
                cleaner.setCompressProjects(extractCheckBox.isSelected());
                cleaner.cleanSubmissions();
            } catch (IOException ex) {
                System.err.println(ex.getMessage());
            }
        });

        panel.setLayout(new BoxLayout(panel, BoxLayout.LINE_AXIS));
        panel.add(extractCheckBox);
        panel.add(Box.createHorizontalGlue());
        panel.add(executeButton);
        return panel;
    }

    private JPanel fileChooserPane() {
        var submissionsFileLabel = new JLabel("Moodle-Abgaben:");
        var solutionFileLabel = new JLabel("Musterlösung:");
        var fileListFileLabel = new JLabel("Dateiliste:");
        var outputDirectoryLabel = new JLabel("Ausgabeverzeichnis:");

        submissionsFileField = new JTextField();
        solutionFileField = new JTextField();
        fileListFileField = new JTextField();
        outputDirectoryField = new JTextField();

        var submissionsFileChooserButton = new JButton("Auswählen…");
        var solutionFileFieldChooserButton = new JButton("Auswählen…");
        var fileListFileChooserButton = new JButton("Auswählen…");
        var outputDirectoryChooserButton = new JButton("Auswählen…");

        submissionsFileChooserButton.addActionListener(e -> {
            var chooser = new JFileChooser(submissionsFileField.getText());
            chooser.setDialogTitle("Wo liegen die studentischen Abgaben?");
            chooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
            chooser.setFileFilter(new FileNameExtensionFilter(
                "Projekt-Ordner oder ZIP-archivierte Projekte", "zip"));

            var returnState = chooser.showDialog(this, "Auswählen");
            if (returnState == JFileChooser.APPROVE_OPTION) {
                submissionsFileField
                    .setText(chooser.getSelectedFile().getAbsolutePath());
                updateExecuteButtonState();
            }
        });

        solutionFileFieldChooserButton.addActionListener(e -> {
            var chooser = new JFileChooser(solutionFileField.getText());
            chooser.setDialogTitle("Wo liegen die studentischen Abgaben?");
            chooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
            chooser.setFileFilter(new FileNameExtensionFilter(
                "Projekt-Ordner oder ZIP-archivierte Projekte", "zip"));

            var returnState = chooser.showDialog(this, "Auswählen");
            if (returnState == JFileChooser.APPROVE_OPTION) {
                solutionFileField
                    .setText(chooser.getSelectedFile().getAbsolutePath());
                updateExecuteButtonState();
            }
        });

        fileListFileChooserButton.addActionListener(e -> {
            var chooser = new JFileChooser(fileListFileField.getText());
            chooser.setDialogTitle("Wo liegen die studentischen Abgaben?");
            chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
            chooser.setFileFilter(new FileNameExtensionFilter(
                "Projekt-Ordner oder ZIP-archivierte Projekte", "txt"));

            var returnState = chooser.showDialog(this, "Auswählen");
            if (returnState == JFileChooser.APPROVE_OPTION) {
                fileListFileField
                    .setText(chooser.getSelectedFile().getAbsolutePath());
                updateExecuteButtonState();
            }
        });

        outputDirectoryChooserButton.addActionListener(e -> {
            var chooser = new JFileChooser(outputDirectoryField.getText());
            chooser.setDialogTitle("Wo liegen die studentischen Abgaben?");
            chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);

            var returnState = chooser.showDialog(this, "Auswählen");
            if (returnState == JFileChooser.APPROVE_OPTION) {
                outputDirectoryField
                    .setText(chooser.getSelectedFile().getAbsolutePath());
                updateExecuteButtonState();
            }
        });

        var panel = new JPanel();

        var layout = new GroupLayout(panel);
        panel.setLayout(layout);

        layout.setAutoCreateGaps(true);
        layout.setAutoCreateContainerGaps(true);

        layout.setHorizontalGroup(layout.createSequentialGroup()
            .addGroup(layout.createParallelGroup(TRAILING)
                .addComponent(submissionsFileLabel)
                .addComponent(solutionFileLabel)
                .addComponent(fileListFileLabel)
                .addComponent(outputDirectoryLabel))
            .addGroup(layout.createParallelGroup(LEADING)
                .addComponent(submissionsFileField)
                .addComponent(solutionFileField)
                .addComponent(fileListFileField)
                .addComponent(outputDirectoryField))
            .addGroup(layout.createParallelGroup(LEADING)
                .addComponent(submissionsFileChooserButton)
                .addComponent(solutionFileFieldChooserButton)
                .addComponent(fileListFileChooserButton)
                .addComponent(outputDirectoryChooserButton)));

        layout.setVerticalGroup(layout.createSequentialGroup()
            .addGroup(layout.createParallelGroup(BASELINE)
                .addComponent(submissionsFileLabel)
                .addComponent(submissionsFileField)
                .addComponent(submissionsFileChooserButton))
            .addGroup(layout.createParallelGroup(BASELINE)
                .addComponent(solutionFileLabel)
                .addComponent(solutionFileField)
                .addComponent(solutionFileFieldChooserButton))
            .addGroup(layout.createParallelGroup(BASELINE)
                .addComponent(fileListFileLabel)
                .addComponent(fileListFileField)
                .addComponent(fileListFileChooserButton))
            .addGroup(layout.createParallelGroup(BASELINE)
                .addComponent(outputDirectoryLabel)
                .addComponent(outputDirectoryField)
                .addComponent(outputDirectoryChooserButton)));

        return panel;
    }
}
