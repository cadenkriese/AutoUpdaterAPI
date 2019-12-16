package cc.flogi.dev.autoupdater;

import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;

/**
 * @author Caden Kriese (flogic)
 *
 * Created on 12/15/19
 */
public class LibraryDownloadTest {
    @Test
    public void downloadPremiumSupportTest() throws IOException {
        //Should be at least 10mb
        final long minSize = 10_000_000;

        File file = File.createTempFile("auapi-premium-", ".jar");
        try {
            UtilLibraries.downloadLibrary(file);
            System.out.println("DOWNLOADED PLUGIN LENGTH = " + file.length());
            Assert.assertTrue(file.length() > minSize);
        } finally {
            file.delete();
        }
    }
}
