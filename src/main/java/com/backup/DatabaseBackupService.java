package com.backup;

import ch.qos.logback.core.net.SyslogOutputStream;
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.InputStreamContent;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;
import com.google.api.services.drive.model.FileList;
import com.backup.MapResponse;


import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Service
public class DatabaseBackupService {
    @Value("${host}")
    String host;
    @Value("${port}")
    String port;
    @Value("${database}")
    String database;
    @Value("${spring.datasource.username}")
    String username;
    @Value("${spring.datasource.password}")
    String dbPassword;

    String backupFilePath = "../vaistra-keepnotes.sql";
    String pgDumpPath = "D:/pgsql/bin/pg_dump.exe"; // Specify the full path to pg_dump executable in your project directory

    InputStream inputStream;

    /*-----------------------------------------GOOGLE DRIVE PROPERTIES0-----------------------------------------------*/
    private static final String APPLICATION_NAME = "autobackup";

    private static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();

    private static final String TOKENS_DIRECTORY_PATH = "tokens";


    private static final List<String> SCOPES =
            Collections.singletonList(DriveScopes.DRIVE_FILE);
    private static final String CREDENTIALS_FILE_PATH = "/credential.json";

    private  final String backupFileName = "vaistra-keepnotes.sql";

    public MapResponse backup() {
        try {

            // Command to run pg_dump
            String[] command = new String[]{
                    pgDumpPath,
                    "--host=" + host,
                    "--port=" + port,
                    "--username=" + username,
                    "--dbname=" + database,
                    "--file=" + backupFilePath,
                    "--format=plain",
                    "--encoding=UTF-8"
            };

            ProcessBuilder processBuilder = new ProcessBuilder(command);
            processBuilder.environment().put("PGPASSWORD", dbPassword);
            processBuilder.redirectErrorStream(true);

            Process process = processBuilder.start();

            // Ensure that the InputStream is fully read before continuing
            try (InputStreamReader streamReader = new InputStreamReader(process.getInputStream());
                 BufferedReader reader = new BufferedReader(streamReader)) {
                String line;
                while ((line = reader.readLine()) != null) {
                    System.out.println(line); // Print the output for debugging purposes
                }
            }

            int exitCode = process.waitFor();
            if (exitCode == 0) {
                System.out.println("Backup process completed successfully.");
                // Read the backup file into an InputStream
                try (FileInputStream fileInputStream = new FileInputStream(backupFilePath)) {
                    // Call the uploadFile() method with the InputStream
                    return uploadFile(fileInputStream);
                }
            } else {
                System.out.println("Error: Backup process failed with exit code " + exitCode);
                return new MapResponse(Map.of("success", false, "message", "Backup process failed"));
            }

        } catch (IOException | InterruptedException e) {
            System.out.println("Error: " + e.getMessage());
            return new MapResponse(Map.of("success", false, "message", e.getMessage()));
        }

    }

    /*------------------------------------------------GOOGLE DRIVE METHODS----------------------------------------------*/

    private static Credential getCredentials(final NetHttpTransport HTTP_TRANSPORT)
            throws IOException {
        // Load client secrets.
        InputStream in = GoogleDriveService.class.getResourceAsStream(CREDENTIALS_FILE_PATH);
        if (in == null) {
            throw new FileNotFoundException("Resource not found1: " + CREDENTIALS_FILE_PATH);
        }
        GoogleClientSecrets clientSecrets =
                GoogleClientSecrets.load(JSON_FACTORY, new InputStreamReader(in));

        // Build flow and trigger user authorization request.
        GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
                HTTP_TRANSPORT, JSON_FACTORY, clientSecrets, SCOPES)
                .setDataStoreFactory(new FileDataStoreFactory(new java.io.File(TOKENS_DIRECTORY_PATH)))
                .setAccessType("offline")
                .build();
        LocalServerReceiver receiver = new LocalServerReceiver.Builder().setPort(8081).build();
        Credential credential = new AuthorizationCodeInstalledApp(flow, receiver).authorize("user");
        //returns an authorized Credential object.
        return credential;
    }

    public Drive getInstance() throws GeneralSecurityException, IOException {
        // Build a new authorized API client service.
        final NetHttpTransport HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
        Drive service = new Drive.Builder(HTTP_TRANSPORT, JSON_FACTORY, getCredentials(HTTP_TRANSPORT))
                .setApplicationName(APPLICATION_NAME)
                .build();
        return service;
    }

    public String getfiles() throws IOException, GeneralSecurityException {
        Drive service = getInstance();

        // Print the names and IDs for up to 10 files.
        FileList result = service.files().list()
                .setPageSize(10)
                .execute();
        List<com.google.api.services.drive.model.File> files = result.getFiles();
        if (files == null || files.isEmpty()) {
            System.out.println("No files found.");
            return "No files found.";
        } else {
            return files.toString();
        }
    }

    public MapResponse uploadFile(InputStream inputStream) {
        try {
            String folderId = "1TBxrovMvik1NduPv9re13TQyjYjR_W87";

            // Check if a file with the same name already exists in the folder
            FileList existingFiles = getInstance().files().list()
                    .setQ("name='vaistra-keepnotes.sql' and '" + folderId + "' in parents")
                    .setSpaces("drive")
                    .execute();

            // Create file metadata
            com.google.api.services.drive.model.File fileMetadata = new com.google.api.services.drive.model.File();
            fileMetadata.setParents(Collections.singletonList(folderId));
            fileMetadata.setName("vaistra-keepnotes.sql");

            // Create an InputStreamContent from the captured InputStream
            InputStreamContent content = new InputStreamContent(null, inputStream);

            if (existingFiles.getFiles() != null && !existingFiles.getFiles().isEmpty()) {
                // If a file with the same name exists, update its content
                com.google.api.services.drive.model.File existingFile = existingFiles.getFiles().get(0);
                com.google.api.services.drive.model.File updatedFile = getInstance().files().update(existingFile.getId(),
                        null, content).execute();
                System.out.println("File updated: " + updatedFile);
                return new MapResponse(Map.of("success", true, "message", "File updated: " + updatedFile));
            } else {
                // If no file with the same name exists, create a new one
                com.google.api.services.drive.model.File uploadFile = getInstance().files().create(fileMetadata, content)
                        .setFields("id").execute();
                System.out.println("File uploaded: " + uploadFile);
                return new MapResponse(Map.of("success", true, "message", "File uploaded: " + uploadFile));
            }

        } catch (Exception e) {
            System.out.printf("Error: " + e);
            return new MapResponse(Map.of("success", false, "message", e.getMessage()));
        }
    }
}
