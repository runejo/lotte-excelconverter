package no.ssb.lotte.excelconverter;

import org.apache.poi.EncryptedDocumentException;
import org.apache.poi.ss.usermodel.*;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.EnumMap;
import java.util.Map;
import java.util.logging.Logger;

public class XLSXFile {
    private static final Logger LOGGER = Logger.getLogger( XLSXFile.class.getName() );
    private static final String STRING_EMPTY = "";
    private static DecimalFormat numFormat1 = null;
    private static DecimalFormat numFormat2 = null;

    private EnumMap<ExcelSheets, Cell[][]> sheetMapToCell = new EnumMap<>(ExcelSheets.class);
    private EnumMap<ExcelSheets, String[][]> sheetMapToString = new EnumMap<>(ExcelSheets.class);
    private EnumMap<ExcelSheets, Integer> sheetMap;
    private Workbook wb;

    //List of ExcelSheets that are required by the program.
    public enum ExcelSheets {
        Tabeller(ConverterMain.XLSXFileSheetNames[0]), Bestilling(ConverterMain.XLSXFileSheetNames[1]);
        public final String name;
        ExcelSheets(String bestilling) {
            name = bestilling;
        }
    }

    //Private constructor for XLSXFile accessible only by calling Factory method
    //Ensures that the workbook is loaded and that all values in the sheetsList is present
    //Accepts a workbook and a map of required excel sheets and which integer they correspond to
    //sheetMapToCell is not populated with cell arrays in constructor
    private XLSXFile(Workbook workbook, EnumMap<ExcelSheets, Integer> sheetsList) {
        wb = workbook;
        sheetMap = sheetsList;
        for (Map.Entry<ExcelSheets, Integer> sheetEntry : sheetsList.entrySet()) {
            sheetMapToString.put(sheetEntry.getKey(), stringConverter(wb.getSheetAt(sheetEntry.getValue())));
        }
    }

    public String[][] getSheet(ExcelSheets sheet) {
        return sheetMapToString.get(sheet);
    }

    //Only called if Cell access to spreadsheet is needed
    public Cell[][] getCells(ExcelSheets sheet) {
        Cell[][] cellArray = sheetMapToCell.get(sheet);
        if (cellArray == null) {
            cellArray = sheetConverter(wb.getSheetAt(sheetMap.get(sheet)));
            sheetMapToCell.put(sheet, cellArray);
        }
        return cellArray;
    }

    private String[][] stringConverter(Sheet sheet) {
        DataFormatter objDefaultFormat = new DataFormatter();
        FormulaEvaluator objFormulaEvaluator = wb.getCreationHelper().createFormulaEvaluator();

        String[][] str = new String[sheet.getLastRowNum()+1][];
        for (int i=0; i<sheet.getLastRowNum()+1; i++) {
            Row row = sheet.getRow(i);
            str[i] = new String[row.getLastCellNum() +1];
            for (int j=0; j<row.getLastCellNum() +1; j++) {
                Cell cell = row.getCell(j);
                if (cell == null)
                    str[i][j] = STRING_EMPTY;
                else {
                    objFormulaEvaluator.evaluate(cell);
                    str[i][j] = objDefaultFormat.formatCellValue(cell,objFormulaEvaluator).trim();
                }
            }
        }
        return str;
    }

    private Cell[][] sheetConverter(Sheet sheet) {
        Row row = null;
        Cell[][] cell = new Cell[sheet.getLastRowNum()+1][];
        for (int i = 0; i < sheet.getLastRowNum()+1; i++) {
            row = sheet.getRow(i);
            cell[i] = new Cell[row.getLastCellNum() +1];
            for (int j = 0; j < row.getLastCellNum() +1; j++) {
                cell[i][j] = row.getCell(j);
            }
        }
        return cell;
    }

    //Only public constructor for XLSXFile
    //Verifies that the file exists, is a xlsx file and contains the correct spreadsheets before returning an XLSXFile
    //object
    public static XLSXFile Factory(String f) throws IOException, SpreadsheetException {
        Workbook wb = null;
        Path fileIn = Paths.get(f);
        if ((!Files.exists(fileIn)) && (!Files.isRegularFile(fileIn)))
            throw new IOException ("Inputfile does not exist or is not regular file");
        InputStream inputStream = Files.newInputStream(fileIn);

        try {
            wb = WorkbookFactory.create(inputStream);
        } catch (EncryptedDocumentException exception) {
            throw new SpreadsheetException("Dokumentet kunne ikke dekodes som et Excel dokument følgende error ble gitt:\n" + exception.getMessage());
        }
        inputStream.close();

        //Code for checking that all excel sheets are present if not return error
        EnumMap<ExcelSheets, Integer> sheetNameMap = new EnumMap<>(ExcelSheets.class);
        for (ExcelSheets sh : ExcelSheets.values())
            for (int i=0; i<wb.getNumberOfSheets(); i++) {
                if (wb.getSheetName(i).equalsIgnoreCase(sh.name()))
                    sheetNameMap.put(sh, i);
            }
        String missing = STRING_EMPTY;
        for (ExcelSheets sh : ExcelSheets.values()) {
            if (! sheetNameMap.containsKey(sh))
                missing += sh.name() + " ";
        }
        if (missing != STRING_EMPTY)
            throw new SpreadsheetException("Excel dokumentet inneholdt ikke følgende faner:" + missing);

        //All checks are performed so XLSXFile can be returned without errors.
        return new XLSXFile(wb, sheetNameMap);
    }

    public Double getValue(ExcelSheets sheet, int i, int j) throws SpreadsheetException{
        Sheet sheetx = wb.getSheetAt(sheetMap.get(sheet));
        Row row = sheetx.getRow(i);
        Cell cell = row.getCell(j);
        switch (cell.getCellType()) {
            case _NONE:
            case BLANK:
            case ERROR:
                return 0d;
            case STRING:
                try {
                    return Double.valueOf(cell.toString());
                } catch (NumberFormatException error) {
                    throw new SpreadsheetException("Verdien i regneark " + sheet.name + " og i celle " + (i+1) + ", " + (j+1));
                }
            case BOOLEAN:
                if(cell.getBooleanCellValue())
                    return 1d;
                else
                    return 0d;
            case FORMULA:
            case NUMERIC:
                return cell.getNumericCellValue();
            default:
                throw new AssertionError("Should never be reached");
        }
    }

    public String getValueString (ExcelSheets sheet, int i, int j) throws SpreadsheetException {
            //NumberFormat numFormat = NumberFormat.getNumberInstance(new Locale("no_NO"));
            //NumberFormat numFormat = NumberFormat.getNumberInstance(Locale.GERMAN);
            if ((numFormat1 == null) || (numFormat2 == null)) {
                numFormat1 = new DecimalFormat();
                numFormat1.setGroupingUsed(false);
                DecimalFormatSymbols decfs = numFormat1.getDecimalFormatSymbols();
                decfs.setDecimalSeparator(',');
                numFormat1.setMaximumFractionDigits(10);
                //System.out.println("Max " + numFormat1.getMaximumIntegerDigits() + " " + numFormat1.getMaximumFractionDigits());

                numFormat2 = new DecimalFormat("#.##########E0");
                numFormat2.getDecimalFormatSymbols().setExponentSeparator(",");
                //numFormat2.setDecimalSeparatorAlwaysShown(false);
            }
            Double val = getValue(sheet, i, j);
            String sVal;
            if (val <= 1e12) {
                sVal = numFormat1.format(val);
            }
            else {
                sVal = numFormat2.format(val);
            }
            return sVal;
    }

}
