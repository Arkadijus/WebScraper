import javax.swing.*;
import javax.swing.event.TableModelListener;
import javax.swing.table.TableModel;
import java.awt.Desktop;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

public class Window {
    private JPanel panelMain;
    private JTextField searchBar;
    private JButton scrapeButton;
    private JCheckBox amazonComCheckBox;
    private JCheckBox amazonCoUkCheckBox;
    private JCheckBox ebayComCheckBox;
    private JTable scrapedDataTable;

    private WebScraper scraper = new WebScraper();

    Window() {
        String[] tableHeader ={"Name", "Price", "Shipping Price", "Full price converted"};
        scrapedDataTable.setModel(new TableModel() {
            @Override
            public int getRowCount() {
                return scraper.getItemList().size();
            }

            @Override
            public int getColumnCount() {
                return tableHeader.length;
            }

            @Override
            public String getColumnName(int columnIndex) {
                return tableHeader[columnIndex];
            }

            @Override
            public Class<?> getColumnClass(int columnIndex) {
                return scraper.getItemList().get(0).getClass();
            }

            @Override
            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return false;
            }

            @Override
            public String getValueAt(int rowIndex, int columnIndex) {
                switch (columnIndex) {
                    case 0:
                        return scraper.getItemList().get(rowIndex).getName();
                    case 1:
                        return scraper.getItemList().get(rowIndex).getPrice() + " " + scraper.getItemList().get(rowIndex).getCurrency();
                    case 2:
                        return scraper.getItemList().get(rowIndex).getShippingPrice() + " " + scraper.getItemList().get(rowIndex).getCurrency();
                    case 3:
                        return scraper.getItemList().get(rowIndex).getFullPriceConverted() + " " + "Eur";
                    default:
                        return "0";
                }
            }

            @Override
            public void setValueAt(Object aValue, int rowIndex, int columnIndex) {

            }

            @Override
            public void addTableModelListener(TableModelListener l) {

            }

            @Override
            public void removeTableModelListener(TableModelListener l) {

            }
        });

        scrapedDataTable.setShowGrid(false);
        scrapedDataTable.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);

        scrapeButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String searchTerm = searchBar.getText().replace(' ', '+');
                scraper.setSearch(searchTerm);

                if (amazonComCheckBox.isSelected()) scraper.scrapeAmazon_com();
                if (amazonCoUkCheckBox.isSelected()) scraper.scrapeAmazon_co_uk();
                if (ebayComCheckBox.isSelected()) scraper.scrapeEbay_com();

                scraper.sortByPrice();
                scrapedDataTable.updateUI();
            }
        });

        scrapedDataTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                int row = scrapedDataTable.rowAtPoint(new Point(e.getX(), e.getY()));

                if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                    try {
                        Desktop.getDesktop().browse(new URI(scraper.getItemList().get(row).getUrl()));
                    } catch (IOException | URISyntaxException ex) {
                        ex.printStackTrace();
                    }
                }
            }
        });
    }

    public static void main(String[] args) {
        Window window = new Window();
        JFrame frame = new JFrame("WebScraper");
        frame.setContentPane(window.panelMain);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }
}
