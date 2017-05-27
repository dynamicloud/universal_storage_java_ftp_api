# Universal Storage Java API
## FTP provider

[![Build Status](https://travis-ci.org/dynamicloud/universal_storage_java_ftp_api.svg?branch=master)](https://travis-ci.org/dynamicloud/universal_storage_java_ftp_api)
![Version](https://img.shields.io/badge/api-v1.0.0-brightgreen.svg)

Universal storage provides you an interface for storing files according to your needs. With this Universal Storage Java API, you will be able to develop programs in Java and use an interface for storing your files within a FTP folder as storage.

<hr>

**This documentation has the following content:**

1. [Maven project](maven-project)
2. [Test API](#test-api)
3. [Settings](#settings)
4. [Explanation for setting keys](#explanation-for-setting-keys)
5. [How to use](#how-to-use)

# Maven project
This API follows the Maven structure to ease its installation within your project.

# Test API
If you want to test the API, follow these steps:

1. Open with a text editor the settings.json located on test/resources/settings.json
```json
{
	"provider": "ftp",
	"root": "universalstorage",
	"tmp": "src/test/resources/tmp",
	"ftp": {
		"ftp_user": "",
		"ftp_password": "",
		"ftp_host": "",
		"ftp_port": 21,
		"ftp_passive": true
	}
}
```
2. The root and tmp keys are the main data to be filled.  Create a local folder called **tmp** and paste its path on the key **tmp**.
3. Create a folder i.e: **universalstorage** in your FTP root folder, copy the name and then paste it on root attribute.
4. Save the settings.json file.

**Now execute the following command:**

`mvn clean test` 

# Settings
**These are the steps for setting up Universal Storage in your project:**
1. You must create a file called settings.json (can be any name) and paste the following. 
```json
{
	"provider": "ftp",
	"root": "universalstorage",
	"tmp": "src/test/resources/tmp",
	"ftp": {
		"ftp_user": "",
		"ftp_password": "",
		"ftp_host": "",
		"ftp_port": 21,
		"ftp_passive": true
	}
}
```
2. The root and tmp keys are the main data to be filled.  Create a local folder called **tmp** and paste its path on the key **tmp**.
3. Create a folder i.e: **universalstorage** in your FTP root folder, copy the name and then paste it on root attribute.
4. Save the file settings.json
5. Add the maven dependency in your pom.xml file.

```xml
<dependency>
   <groupId>org.dynamicloud.api</groupId>
   <artifactId>universalstorage.ftp</artifactId>
   <version>1.0.0</version>
</dependency>
```

# Explanation for setting keys
`ftp_user` ftp username. 

`ftp_password` ftp password.

`ftp_host` fto host.

`ftp_port` ftp port.

`ftp_passive` this flag tells the server to open a data port to which the client will connect to conduct data transfers.

The root folder is the storage where the files will be stored.

The tmp folder is where temporary files will be stored.

### This api will get the ftp_user, ftp_password and ftp_host through either this file or using the environment variable `ftp_user`, `ftp_password` and `ftp_host`

# How to use
**Examples for Storing files:**

1. Passing the settings programmatically
```java
UniversalStorage us = null;
try {
      us = UniversalStorage.Impl.
          getInstance(new UniversalSettings(new File("/home/test/resources/settings.json")));
      us.storeFile(new File("/home/test/resources/settings.json"), "myfolder/innerfolder");
      us.storeFile(new File("/home/test/resources/settings.json"));
      us.storeFile(new File("/home/test/resources/settings.json").getAbsolutePath(), "myfolder/innerfolder");
      us.storeFile(new File("/home/test/resources/settings.json").getAbsolutePath());
} catch (UniversalStorageException e) {
    fail(e.getMessage());
} finally {
	us.close();
}
```
2. The settings could be passed through either jvm parameter or environment variable.
3. If you want to pass the settings.json path through jvm parameter, in your java command add the following parameter:
     `-Duniversal.storage.settings=/home/test/resources/settings.json`
4. If your want to pass the settings.json path through environment variable, add the following variable:
     `universal_storage_settings=/home/test/resources/settings.json`

```java
UniversalStorage us = null;
try {
      us = UniversalStorage.Impl.getInstance();
      us.storeFile(new File("/home/test/resources/settings.json"), "myfolder/innerfolder");
      us.storeFile(new File("/home/test/resources/settings.json"));
      us.storeFile(new File("/home/test/resources/settings.json").getAbsolutePath(), "myfolder/innerfolder");
      us.storeFile(new File("/home/test/resources/settings.json").getAbsolutePath());
} catch (UniversalStorageException e) {
    fail(e.getMessage());
} finally {
	us.close();
}
```

**Remove file:**
```java
UniversalStorage us = null;
try {
      us = UniversalStorage.Impl.getInstance();
      us.removeFile("/home/test/resources/settings.json");
} catch (UniversalStorageException e) {
    e.printStackTrace();
} finally {
	us.close();
}

```

**Create folder:**

```java
UniversalStorage us = null;
try {
      us = UniversalStorage.Impl.getInstance();
      us.createFolder("/myNewFolder");
} catch (UniversalStorageException e) {
    e.printStackTrace();
} finally {
  us.close();
}

```

**Remove folder:**
```java
UniversalStorage us = null;
try {
      us = UniversalStorage.Impl.getInstance();
      us.removeFolder("/myNewFolder");
} catch (UniversalStorageException e) {
    e.printStackTrace();
} finally {
  us.close();
}
```

**Retrieve file:**

This file will be stored into the tmp folder.
```java
UniversalStorage us = null;
try {
      us = UniversalStorage.Impl.getInstance();
      File file = us.retrieveFile("myFolder/file.txt");
} catch (UniversalStorageException e) {
    e.printStackTrace();
} finally {
  us.close();
}
```

**Retrieve file as InputStream:**

This inputstream will use a file that was stored into the tmp folder.
```java
UniversalStorage us = null;
try {
      us = UniversalStorage.Impl.getInstance();
      InputSstream stream = us.retrieveFileAsStream("myFolder/file.txt");
} catch (UniversalStorageException e) {
    e.printStackTrace();
} finally {
  us.close();
}
```

**Clean up tmp folder:**
```java
UniversalStorage us = null;
try {
      us = UniversalStorage.Impl.getInstance();
      us.clean();
} catch (UniversalStorageException e) {
    e.printStackTrace();
} finally {
  us.close();
}
```
