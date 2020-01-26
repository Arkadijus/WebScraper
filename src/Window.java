import javax.swing.*;
import javax.swing.event.TableModelListener;
import javax.swing.table.TableModel;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class Window {
    private JPanel panelMain;
    private JTextField searchBar;
    private JButton scrapeButton;
    private JCheckBox amazonComCheckBox;
    private JCheckBox amazonCoUkCheckBox;
    private JCheckBox ebayComCheckBox;
    private JTable scrapedDataTable;
    private JScrollPane scrollPanel;
    private JButton sortByPriceButton;

    private WebScrapper scrapper = new WebScrapper();

    Window() {
        String[] column ={"Name", "Price", "Shipping Price", "Total"};
        scrapedDataTable.setModel(new TableModel() {
            @Override
            public int getRowCount() {
                return scrapper.itemList.size();
            }

            @Override
            public int getColumnCount() {
                return column.length;
            }

            @Override
            public String getColumnName(int columnIndex) {
                return column[columnIndex];
            }

            @Override
            public Class<?> getColumnClass(int columnIndex) {
                return scrapper.itemList.get(0).getClass();
            }

            @Override
            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return false;
            }

            @Override
            public String getValueAt(int rowIndex, int columnIndex) {
                switch (columnIndex) {
                    case 0:
                        return scrapper.itemList.get(rowIndex).getName();
                    case 1:
                        return String.valueOf(scrapper.itemList.get(rowIndex).getPrice());
                    case 2:
                        return String.valueOf(scrapper.itemList.get(rowIndex).getShippingPrice());
                    case 3:
                        return String.valueOf(scrapper.itemList.get(rowIndex).getFullPrice());
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
                scrapper.setSearch(searchTerm);

                if (amazonComCheckBox.isSelected()) scrapper.scrapeAmazon_com();
                if (amazonCoUkCheckBox.isSelected()) scrapper.scrapeAmazon_co_uk();
                if (ebayComCheckBox.isSelected()) scrapper.scrapeEbay_com();

                scrapper.mostRelevantItemSort();
                scrapedDataTable.updateUI();
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
