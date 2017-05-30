package com.universal.storage;

import junit.framework.TestCase;
import java.io.File;
import java.io.FileWriter;
import com.universal.error.UniversalStorageException;
import com.universal.error.UniversalIOException;
import org.apache.commons.io.FileUtils;
import com.universal.storage.settings.UniversalSettings;

/**
 * This class is the implementation of a storage that will manage files as a FTP folder.
 * This implementation will manage file using a setting to store files within a FTP folder.
 */
public class TestUniversalFTPStorage extends TestCase {
    private void setUpTest(String fileName, String folderName) {
        UniversalStorage us = null;
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

            us = UniversalStorage.Impl.
                    getInstance(new UniversalSettings(new File("src/test/resources/settings.json")));

            us.registerListener(new UniversalStorageListenerAdapter() {
                public void onFolderCreated(UniversalStorageData data) {
                    System.out.println(data.toString());
                }

                public void onFileStored(UniversalStorageData data) {
                    System.out.println(data.toString());
                }

                public void onError(UniversalIOException error) {
                    System.out.println("#### - " + error.getMessage());
                }
            });

            us.storeFile(new File(System.getProperty("user.home"), fileName), folderName);
            us.storeFile(new File(System.getProperty("user.home"), fileName));
        } catch (Exception e) {
            fail(e.getMessage());
        } finally {
            try {
                us.close();
            } catch (Exception ignore) {}            
        }
    }

    public void testRetrieveFileAsFTPProvider() {
        String fileName = System.nanoTime() + ".txt";
        UniversalStorage us = null;
        
        try {
            us = UniversalStorage.Impl.
                    getInstance(new UniversalSettings(new File("src/test/resources/settings.json")));
                    
            setUpTest(fileName, "retrieve/innerfolder");
            us.retrieveFile("retrieve/innerfolder/" + fileName);
        } catch (Exception e) {
            fail(e.getMessage());
        } finally {
            us.close();
        }

        try {
            us = UniversalStorage.Impl.
                    getInstance(new UniversalSettings(new File("src/test/resources/settings.json")));
            us.retrieveFile("retrieve/innerfolder/Target.txttxt");
            fail("This method should throw an error.");
        } catch (UniversalStorageException ignore) {
            
        } finally {
            us.close();
        }

        try {
            us = UniversalStorage.Impl.
                    getInstance(new UniversalSettings(new File("src/test/resources/settings.json")));
            FileUtils.copyInputStreamToFile(us.retrieveFileAsStream("retrieve/innerfolder/" + fileName), 
                new File(System.getProperty("user.home"), fileName));
        } catch (Exception e) {
            e.printStackTrace();
            fail(e.getMessage());
        } finally {
            us.close();
        }
    }

    /**
     * This test will execute the remove file process using a FTP provider.
     */
    public void testRemoveFileAsFTPProvider() {
        String fileName = System.nanoTime() + ".txt";
        setUpTest(fileName, "remove/innerfolder");

        try {
            UniversalStorage us = UniversalStorage.Impl.
                    getInstance(new UniversalSettings(new File("src/test/resources/settings.json")));

            us.removeFile(fileName);
            us.removeFile("remove/innerfolder/" + fileName);
        } catch (UniversalStorageException e) {
            fail(e.getMessage());
        }
    }

    /**
     * This test will execute the create folder process using a FTP provider.
     */
    public void testCreateFolderAsFTPProvider() {
        try {
            UniversalStorage us = UniversalStorage.Impl.
                    getInstance(new UniversalSettings(new File("src/test/resources/settings.json")));

            us.createFolder("myNewFolder");
            us.removeFolder("myNewFolder");
        } catch (UniversalStorageException e) {
            fail(e.getMessage());
        }
    }

    /**
     * This test will clean the storage's context.
     */
    public void testCleanStorageAsFTPProvider() {
        try {
            UniversalStorage us = UniversalStorage.Impl.
                    getInstance(new UniversalSettings(new File("src/test/resources/settings.json")));

            us.clean();
        } catch (UniversalStorageException e) {
            fail(e.getMessage());
        }
    }

    /**
     * This test will wipe the storage's context.
     */
    public void testWipeStorageAsFTPProvider() {
        try {
            UniversalStorage us = UniversalStorage.Impl.
                    getInstance(new UniversalSettings(new File("src/test/resources/settings.json")));

            us.wipe();
        } catch (UniversalStorageException e) {
            fail(e.getMessage());
        }
    }
}