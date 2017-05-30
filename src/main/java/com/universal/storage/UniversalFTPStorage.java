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
    private static final String  PREFIX_FTP_URL = "ftp://"; 
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
     * This method is used to disconnect from FTP host, this is useful to avoid open connections.
     * If you want to connect again to the host, you need to get a new instance.
     */
    public void close() {
        try {
            this.ftp.disconnect();
        } catch (Exception e) {
            e.printStackTrace();
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
            UniversalIOException error = new UniversalIOException(file.getName() + " is a folder.  You should call the createFolder method.");
            this.triggerOnErrorListeners(error);
            throw error;
        }

        if (path == null) {
            path = "";
        }

        InputStream inputStream = null;
        try {
            this.triggerOnStoreFileListeners();
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
            this.triggerOnFileStoredListeners(new UniversalStorageData(file.getName(), 
                            (PREFIX_FTP_URL + this.settings.getFTPHost() + ("".equals(path) ? "" : ("/" + path) + "/" + file.getName())).replaceAll("//", "/"),
                            file.getName(), 
                            ("".equals(path) ? "" : ("/" + path))));
        } catch (Exception e) {
            UniversalIOException error = new UniversalIOException(e.getMessage());
            this.triggerOnErrorListeners(error);
            throw error;
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
            this.triggerOnRemoveFileListeners();
            this.ftp.changeWorkingDirectory("/");
            path = this.settings.getRoot() + (path.startsWith("/") ? "" : ("/" + path));

            boolean success = this.ftp.deleteFile(path);
            if (!success) {
                UniversalIOException error = new UniversalIOException("It couldn't remove this file '" + path + "'");
                this.triggerOnErrorListeners(error);
                throw error;
            }
            this.triggerOnFileRemovedListeners();
        } catch (Exception e) {
            UniversalIOException error = new UniversalIOException(e.getMessage());
            this.triggerOnErrorListeners(error);
            throw error;
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
            UniversalIOException error = new UniversalIOException("Invalid path.  The path shouldn't be empty.");
            this.triggerOnErrorListeners(error);
            throw error;
        }

        if (path == null) {
            path = "";
        }

        try {
            this.triggerOnCreateFolderListeners();
            this.ftp.changeWorkingDirectory("/");
            path = this.settings.getRoot() + (path.startsWith("/") ? "" : ("/" + path));
            this.ftp.makeDirectory(path);
            this.triggerOnFolderCreatedListeners(new UniversalStorageData(path, 
                        (PREFIX_FTP_URL + this.settings.getFTPHost() + ("".equals(path) ? "" : ("/" + path))).replaceAll("//", "/"),
                        path, 
                        ("".equals(path) ? "" : ("/" + path))));
        } catch (Exception e) {
            UniversalIOException error = new UniversalIOException(e.getMessage());
            this.triggerOnErrorListeners(error);
            throw error;
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
            this.triggerOnRemoveFolderListeners();
            this.ftp.changeWorkingDirectory("/");
            path = this.settings.getRoot() + (path.startsWith("/") ? "" : ("/" + path));
            deleteDirectory(path);
            this.triggerOnFolderRemovedListeners();
        } catch (Exception e) {
            UniversalIOException error = new UniversalIOException(e.getMessage());
            this.triggerOnErrorListeners(error);
            throw error;
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
            UniversalIOException error = new UniversalIOException("Invalid path.  Looks like you're trying to retrieve a folder.");
            this.triggerOnErrorListeners(error);
            throw error;
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
            UniversalIOException error = new UniversalIOException(e.getMessage());
            this.triggerOnErrorListeners(error);
            throw error;
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
            UniversalIOException error = new UniversalIOException("Invalid path.  Looks like you're trying to retrieve a folder.");
            this.triggerOnErrorListeners(error);
            throw error;
        }

        try {
            this.ftp.changeWorkingDirectory("/");
            path = this.settings.getRoot() + (path.startsWith("/") ? "" : ("/" + path));
            return this.ftp.retrieveFileStream(path);
        } catch (Exception e) {
            e.printStackTrace();
            UniversalIOException error = new UniversalIOException(e.getMessage());
            this.triggerOnErrorListeners(error);
            throw error;
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
            UniversalIOException error = new UniversalIOException(e.getMessage());
            this.triggerOnErrorListeners(error);
            throw error;
        }
    }

    /**
     * This method wipes the root folder of a storage, basically, will remove all files and folder in it.  
     * Be careful with this method because in too many cases this action won't provide a rollback action.
     */
    public void wipe() throws UniversalIOException {
        try {
            this.ftp.changeWorkingDirectory("/" + this.settings.getRoot());
            
            FTPFile[] result = this.ftp.listFiles();

            for (FTPFile f : result) {
                if (f != null) {
                    if (f.getType() == FTPFile.DIRECTORY_TYPE) {
                        deleteDirectory(f.getName());
                    } else if (f.getType() == FTPFile.FILE_TYPE) {
                        this.ftp.deleteFile(f.getName());
                    }
                }
            }
        } catch (Exception e) {
            UniversalIOException error = new UniversalIOException(e.getMessage());
            this.triggerOnErrorListeners(error);
            throw error;
        }
    }

    private void deleteDirectory(String path) throws IOException {
        FTPFile[] files = this.ftp.listFiles(path);
        if (files.length > 0) {
            for (FTPFile ftpFile : files) {
                if (ftpFile.isDirectory()) {
                    deleteDirectory(path + "/" + ftpFile.getName());
                } else {
                    String deleteFilePath = path + "/" + ftpFile.getName();
                    this.ftp.deleteFile(deleteFilePath);
                }
            }
        }

        this.ftp.removeDirectory(path);
    }
}