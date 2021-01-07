package main.java.no.ssb.lotte.excelconverter;

import java.io.IOException;
import java.util.HashMap;
import java.util.logging.*;


public class ConverterMain {
    private static final int LOGGER_LIMIT_PER_FILE = 10000;
    private static final int LOGGER_MAX_NUM_LOG_FILES = 5;

    public static String[] TESTARGUMENTS =  {"-v","Bestillingtest.xlsx","1","out.zip","err.txt"};
    //VERBOSE, INPUTFILE, USERGROUP, OUTPUTZIPFILE, ERRORFILE

    public static String STANDARDOUTFILE = "out.zip";
    public static String STANDARDERRFILE = "err.txt";
    public static String TOMTBESTILLINGSNAVN = "<Tomt bestillingsnavn>";

    private static final String BESTILLINGTXT = "bestilling.txt";
    private static final String BESTILLING = "BESTILLING";
    private static final String REFERANSEALTERNALTIV = "REFERANSEALTERNATIV";
    private static final String SIMULERINGTXT = "simulering.txt";
    private static final String NAVNSIMULERINGER = "NAVN PÅ SIMULERINGER";
    private static final String TABELLERTXT = "tabeller.txt";
    private static final String TABELLNAVN = "TABELLNAVN";
    private static final String TABELLVALG = "TABELLVALG";
    private static final String SANN = "TRUE";
    private static final String VERDIERTXT = "verdier.txt";
    private static final String VARIABELNAVN = "VARIABELNAVN";
    private static final String VERDI = "VERDI";

    public static final String[] XLSXFileSheetNames = {"Tabeller", "Bestilling"};
    public static final String VERSION = "0.3";


    private static final Logger LOGGER = Logger.getLogger( ConverterMain.class.getName() );
    public static void main2(String[] args) throws IOException, SpreadsheetException {
        XLSXFile xlsxParser = null;
        xlsxParser = XLSXFile.Factory("Bestillingtest2.xlsx");
        String[][] sheet = xlsxParser.getSheet(XLSXFile.ExcelSheets.Bestilling);
        System.out.println("test " + sheet[0][0] + " " + sheet[0][1]);
        for (int i=3; i<7; i++)
            System.out.println("test " + sheet[2][i] + " " + i);
    }
    public static void main(String[] args) {
        //Check that definitions are not mismatched.
        if (XLSXFileSheetNames.length != XLSXFile.ExcelSheets.values().length) {
            System.err.println("Assert: XLSXFileSheetNames.length != XLSXFile.ExcelSheets.values().length failed");
            System.exit(3);
        }
        //args = new String[]{"Bestillingtest2.xlsx", "1"};
        //First parse arguments and set up logging
        //String[] ar = {"-v", "-test"}; //For testing
        //String[] ar = {"-v", "..\\Bestilling.xlsx", "1", "out.zip", "err.txt"}; args = ar;
        ArgsParser conf = ArgsParser.parseCommandlineArguments(args);
        try {
            configureLogging(conf.isVerbose(), conf.isLogfile());
        } catch (IOException exception) {
            LOGGER.severe(exception.toString());
            FileHandling.systemErrorFile(exception, conf.getErrorFile());
            System.exit(3);
        }
        LOGGER.info("Starting main");
        LOGGER.info(conf.toStringAll());
        LOGGER.info("Working Directory = " + System.getProperty("user.dir"));

        if (conf.returnCode != 0) {
            System.out.println(conf.returnString);
            System.exit(conf.returnCode);
        } else {
            //Validated input and established logging, now read the input file
            XLSXFile xlsxParser = null;
            HashMap<String, StringBuilder> filesToBeWritten = null;

            try {
                xlsxParser = XLSXFile.Factory(conf.getInputFile());
                filesToBeWritten = getFileInfo(conf.getUsergroup(), xlsxParser);
            } catch (IOException exception) {
                LOGGER.log(Level.SEVERE, "Severe spreadsheet exception", exception);
                FileHandling.systemErrorFile(exception, conf.getErrorFile());
                System.exit(3);
            } catch (SpreadsheetException exception) {
                LOGGER.log(Level.SEVERE, "User spreadsheet exception", exception);
                FileHandling.userErrorFile(exception, conf.getErrorFile());
                System.exit(2);
            }

            //Write zipfile
            FileHandling fileHandling = new FileHandling(conf);
            try {
                fileHandling.writeZipFile(filesToBeWritten, conf.getInputFile());
                LOGGER.info("ZipFile written, all ok, finished");
                System.exit(0);
            } catch (IOException exception) {
                LOGGER.log(Level.SEVERE, "writeZipFile failed", exception);
                FileHandling.systemErrorFile(exception, conf.getErrorFile());
                System.exit(3);
            }
        }
    }

    private ConverterMain() {}

    public static void configureLogging(boolean verbose, boolean logfile) throws IOException {
        Logger globalLogger = Logger.getLogger("");
        Handler[] handlers = globalLogger.getHandlers();
        for (Handler handler : handlers) {
            if (handler instanceof ConsoleHandler)
                handler.setLevel(Level.SEVERE);
        }
        if (verbose) {
            Handler systemOut = new StreamHandler(System.out, new SimpleFormatter());
            globalLogger.addHandler(systemOut);
        }
        if (logfile) {
            String logPattern="%h/converter%g.log";//directory + File.separator + LOG_PREFIX+ getTimestamp()+ ".%u.%g.log";
            Handler fileLog = new FileHandler(logPattern, LOGGER_LIMIT_PER_FILE, LOGGER_MAX_NUM_LOG_FILES);
        }
    }


    public static HashMap<String, StringBuilder> getFileInfo(int usergroup, XLSXFile xlsxParser) throws SpreadsheetException {
        HashMap<String, StringBuilder> map = new HashMap<>();
        StringBuilder bestilling = MakeFiles.makeBestillingTxt(usergroup, xlsxParser, XLSXFile.ExcelSheets.Bestilling, BESTILLING, REFERANSEALTERNALTIV);
        map.put(BESTILLINGTXT, bestilling);
        StringBuilder simulering = MakeFiles.makeSimulering(xlsxParser, XLSXFile.ExcelSheets.Bestilling, 0, 3, NAVNSIMULERINGER);
        map.put(SIMULERINGTXT, simulering);
        StringBuilder tabeller = MakeFiles.makeTabeller(xlsxParser, XLSXFile.ExcelSheets.Tabeller, TABELLNAVN, TABELLVALG, SANN);
        map.put(TABELLERTXT, tabeller);
        StringBuilder verdier = MakeFiles.makeVerdier(xlsxParser, XLSXFile.ExcelSheets.Bestilling, 0, 3, NAVNSIMULERINGER,
                VARIABELNAVN, VERDI);
        map.put(VERDIERTXT, verdier);
        return map;
    }


}
