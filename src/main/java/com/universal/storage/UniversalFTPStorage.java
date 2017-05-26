package com.universal.storage;

import com.universal.util.PathValidator;
import com.universal.error.UniversalIOException;
import com.universal.storage.settings.UniversalSettings;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.util.Arrays;
import java.util.List;
import org.apache.commons.io.FileUtils;
import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPCmd;
import org.apache.commons.net.ftp.FTPFile;

/**
 * This class is the implementation of a storage that will manage files within a FTP folder.
 * This implementation will manage file using a FTP folder as a root storage.
 */
public class UniversalFTPStorage extends UniversalStorage {
    private FTPClient ftp;;

    /**
     * This constructor receives the settings for this new FileStorage instance.
     * 
     * @param settings for this new FileStorage instance.
     */
    public UniversalFTPStorage(UniversalSettings settings) {
        super(settings);
        initializeFTPConnection();
    }

    /**
     * This method initializes a ftp connection according to the current settings.
     */
    private void initializeFTPConnection() {
        try {
            ftp = new FTPClient();
            ftp.connect(this.settings.getFTPHost(), this.settings.getFTPPort());
            ftp.login(this.settings.getFTPUser(), this.settings.getFTPPassword());

            if (this.settings.isFTPPassive()) {
                ftp.enterLocalPassiveMode();
            }
            
            ftp.setFileType(FTP.BINARY_FILE_TYPE);
        } catch (Exception e) {
            e.printStackTrace();
            throw new IllegalStateException(e.getMessage());
        }
    }

    /**
     * This method stores a file within the storage provider according to the current settings.
     * The method will replace the file if already exists within the root.
     * 
     * For exemple:
     * 
     * path == null
     * File = /var/wwww/html/index.html
     * Root = /storage/
     * Copied File = /storage/index.html
     * 
     * path == "myfolder"
     * File = /var/wwww/html/index.html
     * Root = /storage/
     * Copied File = /storage/myfolder/index.html
     * 
     * If this file is a folder, a error will be thrown informing that should call the createFolder method.
     * 
     * Validations:
     * Validates if root is a bucket.
     * 
     * @param file to be stored within the storage.
     * @param path is the path for this new file within the root.
     * @throws UniversalIOException when a specific IO error occurs.
     */
    void storeFile(File file, String path) throws UniversalIOException {
        if (file.isDirectory()) {
            throw new UniversalIOException(file.getName() + " is a folder.  You should call the createFolder method.");
        }

        if (path == null) {
            path = "";
        }

        InputStream inputStream = null;
        try {
            this.ftp.changeWorkingDirectory("/");

            path = this.settings.getRoot() + (path.startsWith("/") ? "" : ("/" + path));

            boolean exists = this.ftp.changeWorkingDirectory(path);
            if (!exists) {
                String [] folderNames = path.split("/");
                for (String folderName : folderNames) {
                    exists = this.ftp.changeWorkingDirectory(folderName);
                    if (!exists) {
                        ftp.makeDirectory(folderName);
                        this.ftp.changeWorkingDirectory(folderName);
                    }
                }
            }

            inputStream = new FileInputStream(file);
            this.ftp.storeFile(file.getName(), inputStream);
        } catch (Exception e) {
            throw new UniversalIOException(e.getMessage());
        } finally {
            try {
                inputStream.close();
            } catch (Exception ignore) {}
        }
    }

    /**
     * This method stores a file according to the provided path within the storage provider 
     * according to the current settings.
     * 
     * @param path pointing to the file which will be stored within the storage.
     * @throws UniversalIOException when a specific IO error occurs.
     */
    void storeFile(String path) throws UniversalIOException {
        this.storeFile(new File(path), null);
    }

    /**
     * This method stores a file according to the provided path within the storage provider according to the current settings.
     * 
     * @param path pointing to the file which will be stored within the storage.
     * @param targetPath is the path within the storage.
     * 
     * @throws UniversalIOException when a specific IO error occurs.
     */
    void storeFile(String path, String targetPath) throws UniversalIOException {
        PathValidator.validatePath(path);
        PathValidator.validatePath(targetPath);

        this.storeFile(new File(path), targetPath);
    }

    /**
     * This method removes a file from the storage.  This method will use the path parameter 
     * to localte the file and remove it from the storage.  The deletion process will delete the last
     * version of this object.
     * 
     * Root = /ftpstorage/
     * path = myfile.txt
     * Target = /ftpstorage/myfile.txt
     * 
     * Root = /ftpstorage/
     * path = myfolder/myfile.txt
     * Target = /ftpstorage/myfolder/myfile.txt 
     * 
     * @param path is the object's path within the storage.  
     * @throws UniversalIOException when a specific IO error occurs.
     */
    void removeFile(String path) throws UniversalIOException {
        PathValidator.validatePath(path);

        if (path == null) {
            path = "";
        }

        try {
            this.ftp.changeWorkingDirectory("/");
            path = this.settings.getRoot() + (path.startsWith("/") ? "" : ("/" + path));

            boolean success = this.ftp.deleteFile(path);
            if (!success) {
                throw new UniversalIOException("It couldn't remove this file '" + path + "'");
            }
        } catch (Exception e) {
            throw new UniversalIOException(e.getMessage());
        }      
    }

    /**
     * This method creates a new folder within the storage using the passed path. If the new folder name already
     * exists within the storage, this  process will skip the creation step.
     * 
     * Root = /gdstorage/
     * path = /myFolder
     * Target = /gdstorage/myFolder
     * 
     * Root = /gdstorage/
     * path = /folders/myFolder
     * Target = /gdstorage/folders/myFolder
     * 
     * @param path is the folder's path.
     * @param storeFiles is a flag to store the files after folder creation.
     * 
     * @throws UniversalIOException when a specific IO error occurs.
     * @throws IllegalArgumentException is path has an invalid value.
     */
    void createFolder(String path) throws UniversalIOException {
        PathValidator.validatePath(path);

        if ("".equals(path.trim())) {
            throw new UniversalIOException("Invalid path.  The path shouldn't be empty.");
        }

        if (path == null) {
            path = "";
        }

        try {
            this.ftp.changeWorkingDirectory("/");
            path = this.settings.getRoot() + (path.startsWith("/") ? "" : ("/" + path));
            this.ftp.makeDirectory(path);
        } catch (Exception e) {
            throw new UniversalIOException(e.getMessage());
        }   
    }

    /**
     * This method removes the folder located on that path.
     * The folder should be empty in order for removing.
     * 
     * Root = /storage/
     * path = myFolder
     * Target = /storage/myFolder
     * 
     * Root = /storage/
     * path = folders/myFolder
     * Target = /storage/folders/myFolder
     * 
     * @param path of the folder.
     */
    void removeFolder(String path) throws UniversalIOException {
        PathValidator.validatePath(path);

        if ("".equals(path.trim())) {
            return;
        }

        if (path == null) {
            path = "";
        }

        try {
            this.ftp.changeWorkingDirectory("/");
            path = this.settings.getRoot() + (path.startsWith("/") ? "" : ("/" + path));
            this.ftp.removeDirectory(path);
        } catch (Exception e) {
            throw new UniversalIOException(e.getMessage());
        }
    }

    /**
     * This method retrieves a file from the storage.
     * The method will retrieve the file according to the passed path.  
     * A file will be stored within the settings' tmp folder.
     * 
     * @param path in context.
     * @returns a file pointing to the retrieved file.
     */
    public File retrieveFile(String path) throws UniversalIOException {
        PathValidator.validatePath(path);

        if ("".equals(path.trim())) {
            return null;
        }

        if (path.trim().endsWith("/")) {
            throw new UniversalIOException("Invalid path.  Looks like you're trying to retrieve a folder.");
        }

        int index = path.lastIndexOf("/");
        String fileName = path;
        if (index > -1) {
            fileName = path.substring(index + 1);
        }

        InputStream stream = retrieveFileAsStream(path);
        File retrievedFile = new File(this.settings.getTmp(), fileName);

        try {
            FileUtils.copyInputStreamToFile(stream, retrievedFile);
        } catch (Exception e) {
            throw new UniversalIOException(e.getMessage());
        } finally {
            try {
                stream.close();
            } catch (Exception ignore) {}
        }

        return retrievedFile;
    }

    /**
     * This method retrieves a file from the storage as InputStream.
     * The method will retrieve the file according to the passed path.  
     * A file will be stored within the settings' tmp folder.
     * 
     * @param path in context.
     * @returns an InputStream pointing to the retrieved file.
     */
    public InputStream retrieveFileAsStream(String path) throws UniversalIOException {
        PathValidator.validatePath(path);

        if ("".equals(path.trim())) {
            return null;
        }

        if (path.trim().endsWith("/")) {
            throw new UniversalIOException("Invalid path.  Looks like you're trying to retrieve a folder.");
        }

        try {
            this.ftp.changeWorkingDirectory("/");
            path = this.settings.getRoot() + (path.startsWith("/") ? "" : ("/" + path));
            this.ftp.retrieveFileStream(path);
            return null;
        } catch (Exception e) {
            throw new UniversalIOException(e.getMessage());
        }     
    }

    /**
     * This method cleans the context of this storage.  This method doesn't remove any file from the storage.
     * The method will clean the tmp folder to release disk usage.
     */
    public void clean() throws UniversalIOException  {
        try {
            FileUtils.cleanDirectory(new File(this.settings.getTmp()));
        } catch (Exception e) {
            throw new UniversalIOException(e.getMessage());
        }
    }
}