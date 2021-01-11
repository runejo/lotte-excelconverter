package no.ssb.lotte.excelconverter;

import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;

public class ArgsParserTest {
    @Before
    public void setUp() {
    }

    @Test
    public void noArgument_ParseCommandlineArguments_returnNullArgument() {
        //ArgsParser parseCommandlineArguments (String[] args);
        ArgsParser empty = ArgsParser.parseCommandlineArguments(new String[] {""});
        assertEquals(empty, ArgsParser.WrongNumArguments);
    }
}