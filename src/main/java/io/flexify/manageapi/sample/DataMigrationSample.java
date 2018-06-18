/*
 * Copyright (c) 2018 Flexify.IO. All rights reserved.
 * Use of this product is subject to license terms.
 */

package io.flexify.manageapi.sample;

import io.flexify.apiclient.api.MigrationsControllerApi;
import io.flexify.apiclient.api.StoragesControllerApi;
import io.flexify.apiclient.handler.Configuration;
import io.flexify.apiclient.handler.auth.ApiKeyAuth;
import io.flexify.apiclient.model.*;

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

        // 2. Add source storage account and storage
        final Long sourceStorageAccountId = storagesApi
                .addStorageAccount(new AddStorageAccountRequest()
                    .storageAccount(new StorageAccountCreateRequest()
                        .providerId(SOURCE_PROVIDER_ID)
                        .identity(SOURCE_IDENTITY)
                        .credential(SOURCE_CREDENTIAL)
                        .useSsl(true)))
                .getId();
        final Long sourceStorageId = storagesApi
                .addStorages(sourceStorageAccountId, new AddStoragesRequest()
                    .addBucketsItem(new Bucket().name(SOURCE_BUCKET)))
                .getIds().get(0);

        // 3. Add destination storage account and storage
        final Long destinationStorageAccountId = storagesApi
                .addStorageAccount(new AddStorageAccountRequest()
                    .storageAccount(new StorageAccountCreateRequest()
                        .providerId(DESTINATION_PROVIDER_ID)
                        .identity(DESTINATION_IDENTITY)
                        .credential(DESTINATION_CREDENTIAL)
                        .useSsl(true)))
                .getId();
        final Long destinationStorageId = storagesApi
                .addStorages(destinationStorageAccountId, new AddStoragesRequest()
                    .addBucketsItem(new Bucket().name(DESTINATION_BUCKET)))
                .getIds().get(0);

        // 4. Start migration
        final Long migrationId = migrationsApi
                .addMigration(new AddMigrationRequest()
                    .migrationMode(AddMigrationRequest.MigrationModeEnum.COPY)
                    .slots(8)
                    .sourceId(sourceStorageId)
                    .destinationId(destinationStorageId))
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
        case NOT_ASSIGNED:
            System.out.println("Assigning...");
            return false;

        case IN_PROGRESS:
            if (migration.getStat().getBytesProcessed() == null) {
                System.out.println("IN_PROGRESS. Starting...");
            } else {
                System.out
                        .println("IN_PROGRESS. Bytes processed " + migration.getStat().getBytesProcessed());
            }
            return false;

        case IN_PROGRESS_CANCELING:
            System.out.println("IN_PROGRESS_CANCELING");
            return false;

        case CANCELED:
            System.out.println("CANCELED");
            return true;

        case SUCCEEDED:
            System.out.println("SUCCEEDED");
            return true;

        case FAILED:
            System.out.println("FAILED");
            return true;
        }

        return true;
    }
}
