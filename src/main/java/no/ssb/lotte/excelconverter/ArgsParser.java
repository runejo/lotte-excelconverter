package no.ssb.lotte.excelconverter;

import java.util.*;

/**
 * The {@code Src.ArgsParser} class examines the arguments given to the main class.
 * The class is used by calling the two static functions:
 *      static void configureLogging (String [] args)
 *      static Src.ArgsParser parseCommandlineArguments (String[] args)
 *
 * The configureLogging functions turns off the error logs for all but severe. If the args contain "-v" it logs to System.out
 *
 * The parseCommandLineArgument does not handle logging, but parses the arguments and returns the args as a String in
 * configString it also parses the information into the Configuration subclass if everything goes well or returns
 * an Src.ArgsParser as argument for further actions by the main class.
 *
 * Configuration:
 * If 0 arguments return NullArgument
 * If 1 argument is "-v" and there are no further arguments return Verbose
 * If "-v" is one of the arguments then it is removed for the next cases:
 * If 1 argument is "-help" return Help
 * If 1 argument is "-test" configure test parameters and return Test
 * If 4 arguments add to InputFile, Usergroup, OutputZipFile, ErrorFile
 *
 * Returnvalue 0 for Ok and OutputZipFile produced
 * Returnvalue 1 for no file produced (e.g. for -help)
 * Returnvalue 2 for ErrorFile produced because of errors in the Excel file
 * Returnvalue 3 for ErrorFile produced because of system errors (often file handling)
 **/

public enum ArgsParser {
    NullArgument (1, "Versjon " + ConverterMain.VERSION + "\n" +
            "Skriv -help for mer informasjon, Standard input parametere er: excel_fil_inn brukergruppe [utfil_zip error_fil]"),
    Verbose (1, ""),
    WrongArguments (1, "Feil i brukergruppe argumentet, skriv -v for logg og/eller -hjelp for hjelp"),
    WrongNumArguments (1, "Feil antall argumenter, skriv -v for logg og/eller -hjelp for hjelp"),
    Help (1, "-hjelp : Gir denne beskivelsen.\n-test for testparametere (kjør -test for å se standardverdier.\nStandard kall er:\n"+
            "excel_fil_inn brukergruppe [utfil_zip] [error_fil]\n"+
            "legg på -v som parameter for å se logg informasjon til kommandolinje.\n"+
            "legg på -log som parameter for å logge til fil\n"+
            "excel_fil_inn er navn til excel filen som skal prosesseres\n"+
            "brukergruppe er et tall 1 for SSB, 2 for FIN og 3 for andre\n"+
            "utfil_zip er navn på filen som blir produsert hvis prosessering av excel filen går som forventet\n"+
            "error_fil er navn på filen som blir produsert hvis prosessering av excel filen ikke går som forventet\n"+
            "hvis utfil_zip og/eller error_fil ikke blir oppgitt brukes hhv. \"out.zip\" og \"err.txt\" som standardverdier\n" +
            "\n" +
            "Returverdier:\n" + " 0 for ok and utfil_zip produsert\n" +
            "1 for ingen fil produsert (f.eks. for kommandoen -help)\n" +
            "2 for error_fil produsert pga. feil i Excel arkene\n" +
            "3 for error_fil produsert pga. system feil (ofte ved fil håndtering)"
            ),
    Ok(0, "");

    public final int returnCode;
    public final String returnString;

    private static boolean Conf_Verbose;
    private static boolean Conf_Logfile;
    private static int Conf_Usergroup;     //1 for SSB, 2 for FIN, 3 for other
    private static String Conf_InputFile; //Name of Input excel file
    private static String Conf_OutputZipFile; //Name of Output File
    private static String Conf_ErrorFile; //Name of Error File
    private static String Conf_ArgString = "";



    ArgsParser(int retCode, String st) {
        st = substitute(st);
        returnCode = retCode;
        returnString = st;
    }

    private static String substitute(String str) {
        final char[][] SUBSTITUTE = {{'æ', '\u00E6'}, {'Æ', '\u00C6'}, {'ø', '\u00F8'},
                {'Ø', '\u00D8'}, {'å', '\u00E5'}, {'Å', '\u00D5'}};
        for (char[] ch : SUBSTITUTE) {
            str = str.replace(ch[0], ch[1]);
        }
        return str;
    }

    public static ArgsParser parseCommandlineArguments (String[] args) {
        Conf_Verbose = false;
        Conf_Logfile = false;
        Conf_ArgString = "";
        if (args.length == 0) {
            System.out.println(NullArgument.returnString);
            System.out.println("Skriv inn nye parametere til programmet nedenfor:");
            String[] newArgs = FileHandling.getCommandLineInput();
            if (newArgs.length == 0)
                return NullArgument;
            else
                return parseCommandlineArguments(newArgs);
        }

        for (String arg : args)
            Conf_ArgString += arg + " ";

        ArrayList<String> args2 = new ArrayList<String>(Arrays.asList(args));
        String verbose = null;
        String logFile = null;
        for (String st : args2) {
            if (st.equalsIgnoreCase("-v"))
                verbose = st;
            if (st.equalsIgnoreCase("-log"))
                logFile = st;
        }
        if (verbose != null) {
            args2.remove(verbose);
            Conf_Verbose = true;
        }
        if (logFile != null) {
            args2.remove(logFile);
            Conf_Logfile = true;
        }
        if (args2.size() == 0) //Not 0 arguments but either "-v" or "-log" or both
            return Verbose;

        if (args2.size() == 1) {
            if (args2.get(0).equalsIgnoreCase("-help")) {
                return Help;
            } else if (args2.get(0).equalsIgnoreCase("-test")) {
                return ArgsParser.parseCommandlineArguments(ConverterMain.TESTARGUMENTS);
            } else
                return WrongNumArguments;
                //Conf_Usergroup = ConverterMain.TEST_USERGROUP;
                //Conf_InputFile = ConverterMain.TEST_INPUT_FILE;
                //Conf_OutputZipFile = ConverterMain.TEST_OUTPUT_ZIP_FILE;
                //Conf_ErrorFile = ConverterMain.TEST_ERROR_FILE;
                //Conf_TemporaryFile = ConverterMain.TEST_TEMPORARY_FILE;
                //return Test;
        }

        Conf_InputFile = args2.get(0);

        Conf_Usergroup = Integer.valueOf(args2.get(1));
        if ((Conf_Usergroup <= 0) || (Conf_Usergroup >= 4))
            return WrongArguments;

        switch (args2.size()) {
            case 2:
                Conf_OutputZipFile = ConverterMain.STANDARDOUTFILE;
                Conf_ErrorFile = ConverterMain.STANDARDERRFILE;
                return Ok;
            case 3:
                Conf_OutputZipFile = args2.get(2);
                Conf_ErrorFile = ConverterMain.STANDARDERRFILE;
                return Ok;
            case 4:
                Conf_OutputZipFile = args2.get(2);
                Conf_ErrorFile = args2.get(3);
                return Ok;
            default:
                return WrongNumArguments;
        }
    }

    public String getArgString() {
        return Conf_ArgString;
    }

    public boolean isVerbose() {
        return Conf_Verbose;
    }

    public boolean isLogfile() {
        return Conf_Logfile;
    }

    public int getUsergroup() {
        return Conf_Usergroup;
    }

    public String getInputFile() {
        return Conf_InputFile;
    }

    public String getOutputZipFile() {
        return Conf_OutputZipFile;
    }

    public String getErrorFile() {
        return Conf_ErrorFile;
    }

    public String toStringAll() {
        return "Type: " + this.toString() + " returnCode: " + returnCode +
                " Conf_Verbose: " + Conf_Verbose + "\nConf_ArgString: " + Conf_ArgString +
                "\nConf_InputFile: " + Conf_InputFile + "\nConf_OutputZipFile: " + Conf_OutputZipFile +
                "\nConf_ErrorFile: " + Conf_ErrorFile + "\nReturnString: " + returnString;
    }

}
