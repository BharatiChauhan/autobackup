package com.backup;

//import com.backup.DatabaseBackupService;
//import com.vaistra.dtos.MapResponse;
//import com.vaistra.services.impl.DatabaseBackupService;
import com.backup.MapResponse;


import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.security.GeneralSecurityException;

@RestController
@RequestMapping("drive")
public class GoogleDriveController {

    private final DatabaseBackupService databaseBackupService;

    public GoogleDriveController(DatabaseBackupService databaseBackupService) {
        this.databaseBackupService = databaseBackupService;
    }

    @GetMapping
    public String sample() throws IOException, GeneralSecurityException {
        return databaseBackupService.getfiles();
    }

    @PostMapping
    public MapResponse upload() throws IOException, GeneralSecurityException {
        return databaseBackupService.backup();
    }
}