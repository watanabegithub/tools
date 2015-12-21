package tools.fileencrypt;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import javax.crypto.Cipher;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Unit test for simple App.
 */
public class AppTest 
    extends TestCase
{
    /**
     * Create the test case
     *
     * @param testName name of the test case
     */
    public AppTest( String testName )
    {
        super( testName );
    }

    /**
     * @return the suite of tests being tested
     */
    public static Test suite()
    {
        return new TestSuite( AppTest.class );
    }

    /**
     * Rigourous Test :-)
     */
    public void testApp()
    {
        App app = new App();
        try {
            Path path = app.getCurrentPasswordFile();
//            path = app.createNextPasswordFile();
            String s = path.toString();

        } catch(IOException e) {
            e.printStackTrace();
        }
        assertTrue( true );
    }

    public void testNeedEncryptMode() {
        App app = new App();
        assertEquals(app.needEncryptMode(Paths.get("foo.txt")), Cipher.ENCRYPT_MODE);
        assertEquals(app.needEncryptMode(Paths.get("foo.txt.encrypted")), Cipher.DECRYPT_MODE);
    }
}
