package no.ssb.lotte.excelconverter;

import java.io.*;
import java.nio.file.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class FileHandling implements Closeable {
    private static final Logger LOGGER = Logger.getLogger( ZipOutputStream.class.getName() );


    private Path fileIn;
    private Path fileOut;
    private Path fileErr;
    ZipOutputStream zipOut;

    public FileHandling (ArgsParser conf) {
        fileIn = Paths.get(conf.getInputFile());
        //if (!fileIn.exists())
        //    throw new FileNotFoundException("Kan ikke åpne inputfilen, inputfilen eksisterer ikke");
        fileOut = Paths.get(conf.getOutputZipFile());
        fileErr = Paths.get(conf.getErrorFile());
        zipOut = null;
    }

    public static String[] getCommandLineInput() {
        Scanner in = new Scanner(System.in);
        String s = in.nextLine();
        return s.split("\\s+");

    }

    public void writeZipFile (HashMap<String, StringBuilder> files, String inputfile) throws IOException {
        this.openZip();
        this.copyInFileToZip();
        for (Map.Entry<String, StringBuilder> entry : files.entrySet())
            this.AddContentToZip(entry.getKey(), entry.getValue());
        this.close();
    }
    private void copyInFileToZip() throws IOException {
        if (!Files.isRegularFile(fileIn))
            throw new IOException("Kan ikke åpne inputfilen for å kopiere den til zip, inputfilen eksisterer ikke");
        InputStream inFileStream = Files.newInputStream(fileIn);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        //inFileStream.transferTo(out); //transferTo not valid in java 8
        //Copying stream in java 8 below
        byte[] buf = new byte[8192];
        int length;
        while ((length = inFileStream.read(buf)) > 0) {
            out.write(buf, 0, length);
        }

        LOGGER.info("Writing filename (in zip file): " + fileIn.getFileName());
        ZipEntry file = new ZipEntry(fileIn.toFile().getName());
        zipOut.putNextEntry(file);
        zipOut.write(out.toByteArray());
        zipOut.closeEntry();
        inFileStream.close();
    }

    public Path getFileIn() {
        return fileIn;
    }

    public void AddContentToZip(String filename, StringBuilder sb) throws IOException {
        LOGGER.info("Writing filename (in zip file): " + filename);
        ZipEntry file = new ZipEntry(filename);
        zipOut.putNextEntry(file);
        zipOut.write(sb.toString().getBytes());
        zipOut.closeEntry();
    }

    private void openZip() throws IOException {
        OutputStream out = Files.newOutputStream(fileOut);
        zipOut = new ZipOutputStream(out);
    }


    private void testAddFile(String filename, StringBuilder sb) {
        LOGGER.info("Writing to log, filename: " + filename);
        LOGGER.info(sb.toString());
    }

    public static void systemErrorFile(Exception exception, String fileErr) {
        try {
            OutputStream err = Files.newOutputStream(Paths.get(fileErr));
            err.write(exception.getMessage().getBytes());
            err.write("\n".getBytes());

            StackTraceElement[] stackTrace = exception.getStackTrace();
            for (StackTraceElement el : stackTrace) {
                String str = el.toString() + "\n";
                err.write(str.getBytes());
            }

            err.close();
        }  catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Cannot write error file (systemError)", e);
            System.exit(3);
        }
    }

    @Override
    public void close() throws IOException {
            zipOut.close();
    }


    public static void userErrorFile(SpreadsheetException exception, String fileErr) {
        try {
            OutputStream err = Files.newOutputStream(Paths.get(fileErr));
            err.write("Kan ikke gjennomføre konvertering av excel fil, pga. følgende feil:\n".getBytes());
            err.write(exception.getMessage().getBytes());
            err.write("\n".getBytes());
            err.close();
        }  catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Cannot write error file (userError)", e);
            System.exit(3);
        }
    }
}
