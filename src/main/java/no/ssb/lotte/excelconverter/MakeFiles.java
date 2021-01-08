package no.ssb.lotte.excelconverter;

import org.apache.poi.util.StringUtil;

import java.awt.*;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.LinkedList;
import java.util.logging.Logger;

public class MakeFiles {
    private static final Logger LOGGER = Logger.getLogger( MakeFiles.class.getName() );

    public static StringBuilder makeTabeller(XLSXFile xlsx, XLSXFile.ExcelSheets sheetName, String tabellNavn, String tabellValg, String Sann)
            throws SpreadsheetException {
        String[][] sheet = xlsx.getSheet(sheetName);
        StringBuilder sb = new StringBuilder();
        LinkedList<Point> list = locateTabeller(sheet, tabellNavn, tabellValg);
        int columnNavn = list.get(0).y;
        int columnValg = list.get(1).y;

        for (int i = 0; i < sheet.length; i++) {
            if (sheet[i][columnValg].equalsIgnoreCase(Sann))
                sb.append(sheet[i][columnNavn] + ";" + "\n");
        }

        if (sb.toString().isEmpty())
            throw new SpreadsheetException("Ingen tabeller ble valgt");
        else
            return sb;
    }

    public static StringBuilder makeVerdier(XLSXFile xlsx, XLSXFile.ExcelSheets sheetName, int varColumn, int startColumn,
                                            String varNameSimuleringer, String variabelNavn, String variabelVerdi)
            throws SpreadsheetException {
        StringBuilder sb = new StringBuilder();

        String[][] sheet = xlsx.getSheet(sheetName);
        LinkedList<Point> simuleringer = locateVariablesRow(sheet, varColumn, startColumn, varNameSimuleringer);
        if (simuleringer.size() == 0)
            throw new SpreadsheetException ("Ingen simuleringsalternativer ble funnet");
        LinkedList<Point> variabler = locateTabeller(sheet, variabelNavn, variabelVerdi);

        //NumberFormat numFormat = NumberFormat.getNumberInstance(new Locale("no_NO"));
        //NumberFormat numFormat = NumberFormat.getNumberInstance(Locale.GERMAN);
        //NumberFormat numFormat = NumberFormat.getNumberInstance(Locale.ENGLISH);
        DecimalFormat numFormat1 = new DecimalFormat();
        numFormat1.setGroupingUsed(false);
        //numFormat.setMaximumIntegerDigits(4);
        DecimalFormatSymbols decfs = numFormat1.getDecimalFormatSymbols();
        decfs.setDecimalSeparator(',');

        DecimalFormat numFormat2 = new DecimalFormat("#.E0");
        numFormat2.getDecimalFormatSymbols().setExponentSeparator(",");
        numFormat2.setDecimalSeparatorAlwaysShown(false);

        int yVar = variabler.get(0).y;
        for (int sim = 0; sim < simuleringer.size(); sim++) {
            int ySim = simuleringer.get(sim).y;
            for (int i=variabler.get(0).x+1; i<sheet.length; i++) {
                if (! sheet[i][yVar].isEmpty())
                    if (! sheet[i][ySim].isEmpty()) {
                        Double val = xlsx.getValue(sheetName, i, ySim);
                        String sVal;
                        if (val <= 1000000) {
                            sVal = numFormat1.format(val);
                        }
                        else {
                            sVal = numFormat2.format(val);
                        }
                            sb.append((sim + 1) + ";" + sheet[i][yVar] + ";" + sVal + ";" + "\n");

                    }
            }
        }
        return sb;
    }

    public static StringBuilder makeSimulering(XLSXFile xlsx, XLSXFile.ExcelSheets sheetName, int varColumn, int startColumn, String varName)
            throws SpreadsheetException {
        StringBuilder sb = new StringBuilder();

        String[][] sheet = xlsx.getSheet(sheetName);
        LinkedList<Point> list = locateVariablesRow(sheet, varColumn, startColumn, varName);
        if (list.size() == 0)
            throw new SpreadsheetException ("Ingen simuleringsalternativer ble funnet");
        for (int i=0; i<list.size(); i++)
            sb.append(sheet[list.get(i).x][list.get(i).y] + ";" + (i+1) + ";" + "\n");

        return sb;
    }

    public static StringBuilder makeBestillingTxt(int usergroup, XLSXFile xlsx, XLSXFile.ExcelSheets sheetName,
                                                  String bestillingName, String referanseAlternativName) throws SpreadsheetException {
        String[][] sheet = xlsx.getSheet(sheetName);
        String orderName = locateCaseInsensitive(sheet, 0, bestillingName);
        if (orderName.isEmpty())
            orderName = ConverterMain.TOMTBESTILLINGSNAVN;
        String ref = locateCaseInsensitive(sheet, 0, referanseAlternativName);

        StringBuilder sb = new StringBuilder();
        sb.append(usergroup + ";" + orderName + ";" + ref  + ";");
        return sb;
    }


    public static LinkedList<Point> locateTabeller(String[][] str, String tabellNavn, String tabellValg) throws SpreadsheetException {
        LinkedList<Point> list = new LinkedList<Point>();
        int column0 = -1, column1 = -1;
        for (int i = 0; i < str.length; i++) {
            column0 = locateColumn(str, i, tabellNavn);
            column1 = locateColumn(str, i, tabellValg);
            if ((column0 != -1) && (column1 != -1)) {
                list.add(new Point(i, column0));
                list.add(new Point(i, column1));
                break;
            }
        }
        if (list.isEmpty())
            throw new SpreadsheetException(tabellNavn + " and/or " + tabellValg + " not found.");
        else
            return list;
    }

    //Locates a variable 'name' in excel spreadsheet 'str' the elements that are seached are 'str[x][varColumn]' where
    //x is a variable starting from the top when found it returns a linked list of all the names in row x starting from
    //column 'startColumn' the list can be zero length if no text is found.
    private static LinkedList<Point> locateVariablesRow(String[][] str, int varColumn, int startColumn, String name) throws SpreadsheetException {
        int row = -1;
        for (int i=0; i<str.length; i++) {
            if (StringUtil.startsWithIgnoreCase(str[i][varColumn], name)) {
                row = i;
                break;
            }
        }
        if (row == -1)
            throw new SpreadsheetException("Teksten \"" + name + "\" ble ikke funnet i kolonne " + (varColumn+1) + " i excel dokumentet");

        LinkedList<Point> list = new LinkedList<>();
        for (int j = startColumn; j < str[row].length; j++) {
            if (!str[row][j].isEmpty()) {
                list.add(new Point(row, j));
            }
        }
        return list;
    }

    //Locates a string 'name' in spreadsheet 'str' by searching the elements in column 'column'
    public static String locateCaseInsensitive(String[][] str, int column, String name) throws SpreadsheetException {
        for (int i = 0; i < str.length; i++) {
            if (str[i][column].equalsIgnoreCase(name))
                return str[i][column + 1];
        }
        throw new SpreadsheetException("Variable " + name + " not found in column " + column + " in the excel document");
    }

    private static int locateColumn(String[][] str, int row, String name) {
        for (int j=0 ; j<str[row].length; j++) {
            if (str[row][j].equalsIgnoreCase(name))
                return j;
        }
        return -1;
    }

    private static String stripExtension (String str) {
        int pos = str.lastIndexOf(".");
        if (pos == -1)
            return str;
        else
            return str.substring(0, pos);
    }
}
