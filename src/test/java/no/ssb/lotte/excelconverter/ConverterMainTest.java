package no.ssb.lotte.excelconverter;

//Uses System Rules (a collection of JUnit rules for testing code that juses java.lang.System

import static org.junit.Assert.*;
import org.junit.Rule;
import org.junit.Test;
import org.junit.contrib.java.lang.system.ExpectedSystemExit;
import org.junit.contrib.java.lang.system.SystemOutRule;


public class ConverterMainTest {
    @Rule
    public final SystemOutRule systemOutRule = new SystemOutRule().enableLog();

    @Rule
    public final ExpectedSystemExit exit = ExpectedSystemExit.none();

    @Test
    public void textToSystemOutTest() {
        String helloWorld = "Hello world!";
        System.out.println(helloWorld);
        assertEquals(helloWorld + System.lineSeparator(), systemOutRule.getLog());
        systemOutRule.clearLog();
    }

    @Test
    public void systemExitTest() {
        exit.expectSystemExitWithStatus(1);
        System.exit(1);
    }
}
