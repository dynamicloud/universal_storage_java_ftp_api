package com.universal.storage;

import junit.framework.TestCase;
import java.io.File;
import java.io.FileWriter;
import com.universal.error.UniversalStorageException;
import org.apache.commons.io.FileUtils;
import com.universal.util.FileUtil;
import com.universal.storage.settings.UniversalSettings;

/**
 * This class is the implementation of a storage that will manage files as a FTP folder.
 * This implementation will manage file using a setting to store files within a FTP folder.
 */
public class TestUniversalFTPStorage extends TestCase {
    private static UniversalStorage us = null;
    protected void setUp() {        
        try {
            if (us == null) {
                us = UniversalStorage.Impl.
                    getInstance(new UniversalSettings(new File("src/test/resources/settings.json")));
            }
        } catch (Exception e) {
            e.printStackTrace();
            fail(e.getMessage());
        }
    }

    private void setUpTest(String fileName, String folderName) {
        try {
            File newFile = new File(System.getProperty("user.home"), fileName);
            if (newFile.exists()) {
                newFile.delete();
            }

            FileWriter fw = null;
            try {
                newFile.createNewFile();
                fw = new FileWriter(newFile);
                fw.write("Hello World!");
                fw.flush();
            } catch (Exception e) {
                fail(e.getMessage());
            } finally {
                try {
                    fw.close();
                } catch (Exception ignore){}
            }

            us.storeFile(new File(System.getProperty("user.home"), fileName), folderName);
            us.storeFile(new File(System.getProperty("user.home"), fileName));
            us.storeFile(System.getProperty("user.home") + "/" + fileName, folderName);
            us.storeFile(System.getProperty("user.home") + "/" + fileName);
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    public void testDummy() {
        setUpTest("fileName", "myNewFolder/innerFolder");
    }
}