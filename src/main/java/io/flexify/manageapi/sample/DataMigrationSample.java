/*
 * Copyright (c) 2018 Flexify.IO. All rights reserved.
 * Use of this product is subject to license terms.
 */

package io.flexify.manageapi.sample;

import io.flexify.apiclient.api.MigrationsControllerApi;
import io.flexify.apiclient.api.StoragesControllerApi;
import io.flexify.apiclient.handler.ApiException;
import io.flexify.apiclient.handler.Configuration;
import io.flexify.apiclient.handler.auth.ApiKeyAuth;
import io.flexify.apiclient.model.AddMigrationRequest;
import io.flexify.apiclient.model.AddMigrationRequestMapping;
import io.flexify.apiclient.model.AddStorageAccountRequest;
import io.flexify.apiclient.model.CloudLocation;
import io.flexify.apiclient.model.Migration;
import io.flexify.apiclient.model.MigrationSettings;
import io.flexify.apiclient.model.NewStorageAccount;
import io.flexify.apiclient.model.StorageAccountSettings;
import jersey.repackaged.com.google.common.base.Objects;

/**
 * Sample code demonstrating starting and monitoring migration via Flexify.IO
 * Management API.
 * 
 * @author Alexander Bondin
 */
public class DataMigrationSample {

    // Please contact info@flexify.io to get the URL and the API key
    private final static String BASE_PATH_URL = "https://flexify-manage.azurewebsites.net/backend/";
    private final static String API_KEY = "<your Flexify.IO API key>";

    // Migration Source
    public static final Long SOURCE_PROVIDER_ID = 1L; // Amazon S3
    public static final String SOURCE_IDENTITY = "AKIAJO2AZ3R3Z2TXJWWQ";
    public static final String SOURCE_CREDENTIAL = "<your secret key>";
    public static final String SOURCE_BUCKET = "bucket_1";

    // Migration Destination
    public static final Long DESTINATION_PROVIDER_ID = 2L; // Azure Bob Storage
    public static final String DESTINATION_IDENTITY = "flexifyuseast";
    public static final String DESTINATION_CREDENTIAL = "<your secret key>";
    public static final String DESTINATION_BUCKET = "bucket_2";

    public static void main(String[] args) throws Exception {

        // 1. Configure API
        setupApi();
        StoragesControllerApi storagesApi = new StoragesControllerApi();
        MigrationsControllerApi migrationsApi = new MigrationsControllerApi();

        // 2. Add source storage account
        Long sourceStorageAccountId;
        try {
            sourceStorageAccountId = storagesApi
                    .addStorageAccount(new AddStorageAccountRequest()
                        .storageAccount(new NewStorageAccount()
                            .providerId(SOURCE_PROVIDER_ID)
                            .settings(new StorageAccountSettings()
                                .identity(SOURCE_IDENTITY)
                                .credential(SOURCE_CREDENTIAL)
                                .useSsl(true)))
                            .verifyKeys(true))
                    .getId();
        } catch (ApiException ex) {
            // account may already exist
            System.out.println(ex.getResponseBody());
            FlexifyException fex = FlexifyException.fromApi(ex);
            if (fex != null && Objects.equal(fex.message, "STORAGE_ACCOUNT_ALREADY_EXISTS")) {
                sourceStorageAccountId = Long.parseLong(fex.args[0].toString());
            } else {
                throw ex;
            }
        }

        // 3. Add destination storage account
        Long destinationStorageAccountId;
        try {
            destinationStorageAccountId = storagesApi
                .addStorageAccount(new AddStorageAccountRequest()
                    .storageAccount(new NewStorageAccount()
                        .providerId(DESTINATION_PROVIDER_ID)
                        .settings(new StorageAccountSettings()
                            .identity(DESTINATION_IDENTITY)
                            .credential(DESTINATION_CREDENTIAL)
                            .useSsl(true)))
                        .verifyKeys(true))
                .getId();
        } catch (ApiException ex) {
            // account may already exist
            FlexifyException fex = FlexifyException.fromApi(ex);
            if (fex != null && Objects.equal(fex.message, "STORAGE_ACCOUNT_ALREADY_EXISTS")) {
                destinationStorageAccountId = Long.parseLong(fex.args[0].toString());
            } else {
                throw ex;
            }
        }

        // 4. Start migration
        final Long migrationId = migrationsApi
                .addMigration(new AddMigrationRequest()
                    .settings(new MigrationSettings()
                        .migrationMode(MigrationSettings.MigrationModeEnum.COPY)
                        .slotsPerMapping(8)
                        .enginesLocation(new CloudLocation()))
                    .addMappingsItem(new AddMigrationRequestMapping()
                        .sourceStorageAccountId(sourceStorageAccountId)
                        .sourceBucketName(SOURCE_BUCKET)
                        .destStorageAccountId(destinationStorageAccountId)
                        .destBucketName(DESTINATION_BUCKET)
                    ))
                .getId();

        // 5. Poll the migration state every 5 seconds
        boolean completed = false;
        do {
            Migration migration = migrationsApi.getMigration(migrationId);
            completed = printMigrationStatus(migration);
            Thread.sleep(5000l);
        } while (!completed);
        
    }

    /**
     * Set us API before use
     */
    private static void setupApi() {
        // set base URL
        Configuration.getDefaultApiClient().setBasePath(BASE_PATH_URL);

        // set API key for authentication
        ApiKeyAuth bearer = (ApiKeyAuth) Configuration.getDefaultApiClient().getAuthentication("Bearer");
        bearer.setApiKeyPrefix("Bearer");
        bearer.setApiKey(API_KEY);
    }

    private static boolean printMigrationStatus(Migration migration) {
        switch (migration.getStat().getState()) {
        case WAITING:
            System.out.println("Waiting...");
            return false;

        case STARTING:
            System.out.println("Starting...");
            return false;

        case IN_PROGRESS:
            if (migration.getStat().getBytesProcessed() == null) {
                System.out.println("IN_PROGRESS. Starting...");
            } else {
                System.out
                        .println("IN_PROGRESS. Bytes processed " + migration.getStat().getBytesProcessed());
            }
            return false;

        case STOPPING:
            System.out.println("STOPPING");
            return false;

        case STOPPED:
            System.out.println("STOPPED");
            return true;

        case SUCCEEDED:
            System.out.println("SUCCEEDED");
            return true;

        case FAILED:
            System.out.println("FAILED");
            return true;

        case NO_CONNECTION_TO_ENGINE:
            System.out.println("NO_CONNECTION_TO_ENGINE");
            return true;
        }

        return true;
    }
}
