package edu.princeton.safe.internal.cytoscape;

import java.awt.Component;
import java.awt.FileDialog;
import java.awt.Frame;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.Set;

import javax.swing.JFileChooser;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.filechooser.FileFilter;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;

public class UiUtil {
    public static JPanel createJPanel() {
        JPanel panel = new JPanel();
        panel.setOpaque(false);
        return panel;
    }

    private static File getFileAWT(Component parent,
                                   String title,
                                   File initialFile,
                                   final String typeDescription,
                                   final Set<String> extensions,
                                   FileSelectionMode mode)
            throws IOException {
        // Use AWT dialog for Mac since it lets us use Finder's file chooser
        final String fileDialogForDirectories = System.getProperty("apple.awt.fileDialogForDirectories");
        System.setProperty("apple.awt.fileDialogForDirectories",
                           mode == FileSelectionMode.OPEN_DIRECTORY ? "true" : "false");
        try {
            FileDialog dialog;
            switch (mode) {
            case OPEN_FILE:
            case OPEN_DIRECTORY:
                dialog = new FileDialog(getFrame(parent), title, FileDialog.LOAD);
                break;
            case SAVE_FILE:
                dialog = new FileDialog(getFrame(parent), title, FileDialog.SAVE);
                break;
            default:
                throw new RuntimeException(String.valueOf(mode));
            }
            dialog.setFilenameFilter(new FilenameFilter() {
                @Override
                public boolean accept(File directory,
                                      String name) {
                    if (extensions.size() == 0) {
                        return true;
                    }

                    String[] parts = name.split("[.]"); //$NON-NLS-1$
                    String lastPart = parts[parts.length - 1];
                    for (String extension : extensions) {
                        if (extension.equalsIgnoreCase(lastPart)) {
                            return true;
                        }
                    }
                    return false;
                }
            });
            File directory = null;
            if (initialFile.isDirectory()) {
                directory = initialFile;
            }
            if (initialFile.isFile() || !initialFile.exists()) {
                dialog.setFile(initialFile.getName());
                directory = initialFile.getParentFile();
            }
            if (directory != null) {
                dialog.setDirectory(directory.getAbsolutePath());
            }
            dialog.setTitle(title);
            dialog.setVisible(true);
            String file = dialog.getFile();
            if (file == null) {
                return null;
            }
            String targetDirectory = dialog.getDirectory();
            if (targetDirectory == null) {
                return null;
            }
            return new File(String.format("%s%s%s", targetDirectory, File.separator, file)); //$NON-NLS-1$
        } finally {
            System.setProperty("apple.awt.fileDialogForDirectories", fileDialogForDirectories);
        }
    }

    private static File getFileSwing(Component parent,
                                     String title,
                                     File initialFile,
                                     final String typeDescription,
                                     final Set<String> extensions,
                                     FileSelectionMode mode)
            throws IOException {
        JFileChooser chooser = new JFileChooser(initialFile);
        chooser.setDialogTitle(title);
        chooser.setSelectedFile(initialFile);
        if (typeDescription != null && extensions != null && extensions.size() > 0) {
            chooser.setFileFilter(new FileFilter() {
                @Override
                public boolean accept(File file) {
                    String[] parts = file.getName()
                                         .split("[.]"); //$NON-NLS-1$
                    String lastPart = parts[parts.length - 1];
                    for (String extension : extensions) {
                        if (extension.equalsIgnoreCase(lastPart)) {
                            return true;
                        }
                    }
                    return false;
                }

                @Override
                public String getDescription() {
                    return typeDescription;
                }
            });
        }
        int option;
        switch (mode) {
        case OPEN_FILE:
            chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
            option = chooser.showOpenDialog(parent);
            break;
        case OPEN_DIRECTORY:
            chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            option = chooser.showOpenDialog(parent);
            break;
        case SAVE_FILE:
            chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
            option = chooser.showSaveDialog(parent);
            break;
        default:
            throw new RuntimeException(String.valueOf(mode));
        }
        if (option != JFileChooser.APPROVE_OPTION) {
            return null;
        }
        return chooser.getSelectedFile();
    }

    public static File getFile(Component parent,
                               String title,
                               File initialFile,
                               final String typeDescription,
                               final Set<String> extensions,
                               FileSelectionMode mode)
            throws IOException {
        if (isMacOSX()) {
            // Use Finder instead of Swing for Mac.
            return getFileAWT(parent, title, initialFile, typeDescription, extensions, mode);
        } else {
            return getFileSwing(parent, title, initialFile, typeDescription, extensions, mode);
        }
    }

    public static Frame getFrame(Component parent) {
        while (parent != null) {
            if (parent instanceof Frame) {
                return (Frame) parent;
            }
            parent = parent.getParent();
        }
        return null;
    }

    public static boolean isMacOSX() {
        String osName = System.getProperty("os.name"); //$NON-NLS-1$
        return osName.startsWith("Mac OS X"); //$NON-NLS-1$
    }

    public enum FileSelectionMode {
        OPEN_FILE, SAVE_FILE, OPEN_DIRECTORY,
    }

    public static void packColumns(JTable table) {
        int padding = 5;
        int[] widths = new int[table.getColumnCount()];
        for (int j = 0; j < table.getRowCount(); j++) {
            for (int i = 0; i < widths.length; i++) {
                TableCellRenderer renderer = table.getCellRenderer(j, i);
                Object value = table.getValueAt(j, i);
                Component component = renderer.getTableCellRendererComponent(table, value, true, true, j, i);
                widths[i] = (int) Math.max(widths[i], component.getPreferredSize()
                                                               .getWidth()
                        + table.getIntercellSpacing()
                               .getWidth()
                        + padding);
            }
        }

        TableColumnModel columnModel = table.getColumnModel();
        JTableHeader header = table.getTableHeader();
        System.out.println(table.getWidth());
        System.out.println(table.getParent()
                                .getWidth());
        int remaining = table.getParent()
                             .getWidth();
        for (int i = 0; i < widths.length; i++) {
            TableColumn column = columnModel.getColumn(i);
            TableCellRenderer renderer = column.getHeaderRenderer();
            if (renderer == null) {
                renderer = header.getDefaultRenderer();
            }
            Component component = renderer.getTableCellRendererComponent(table, column.getHeaderValue(), true, true, 0,
                                                                         i);
            widths[i] = (int) Math.max(widths[i], component.getPreferredSize()
                                                           .getWidth());

            // First column gets all the extra space, if any
            if (i != 0) {
                remaining -= widths[i];
            }
        }

        widths[0] = (int) Math.max(1, remaining);

        for (int i = 0; i < widths.length; i++) {
            System.out.printf("%d\t%d\n", i, widths[i]);
            TableColumn column = columnModel.getColumn(i);
            header.setResizingColumn(column);
            column.setWidth(widths[i]);
        }

    }

}
