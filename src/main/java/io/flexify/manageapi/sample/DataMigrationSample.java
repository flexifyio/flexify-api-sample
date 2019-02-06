package io.flexify.manageapi.sample;

import java.util.Objects;

import io.flexify.apiclient.api.MigrationsControllerApi;
import io.flexify.apiclient.api.StorageAccountsControllerApi;
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

/**
 * Sample code demonstrating starting and monitoring migration via Flexify.IO
 * Management API.
 *
 * @author Alexander Bondin
 */
public class DataMigrationSample {

    // Please contact info@flexify.io to get the URL and the API key
    // private final static String BASE_PATH_URL = "https://flexify-manage.azurewebsites.net/backend/";
    // private final static String API_KEY = "<your Flexify.IO API key>";
    private final static String BASE_PATH_URL = "https://flexify-manage-test.azurewebsites.net/backend/";
    private final static String API_KEY = "eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiJza0BmbGV4aWZ5LmlvIiwiaWF0IjoxNTQ5NDQyNzEwLCJzZWVkIjoiaFpUQVJkZWhXZSJ9.2aqxFxsb48vAcJum_FO0fQYYCoh1vlJMhxAgeSvKOT6XpvziusvkaoIMtHfdaNCCzK1sz8kuhF1LtdpqraGTUw";

    // Migration Source
    public static final Long SOURCE_PROVIDER_ID = 1L; // Amazon S3
    // public static final String SOURCE_IDENTITY = "AKIAJO2AZ3R3Z2TXJWWQ";
    // public static final String SOURCE_CREDENTIAL = "<your secret key>";
    // public static final String SOURCE_BUCKET = "bucket_1";
    public static final String SOURCE_IDENTITY = "AKIAIMBUKJYLGX2244IA";
    public static final String SOURCE_CREDENTIAL = "HBoKsmFgc0xXxzq6EwfCFp2UBnoHrO5a7fw4/s5T";
    public static final String SOURCE_BUCKET = "flexify";

    // Migration Destination
    public static final Long DESTINATION_PROVIDER_ID = 2L; // Azure Bob Storage
    // public static final String DESTINATION_IDENTITY = "flexifyuseast";
    // public static final String DESTINATION_CREDENTIAL = "<your secret key>";
    // public static final String DESTINATION_BUCKET = "bucket_2";
    public static final String DESTINATION_IDENTITY = "flexifyuseast";
    public static final String DESTINATION_CREDENTIAL = "v3SLDLGjhwOvvU0JrL/Skq7LlFHb3p/tEfyF75/R2bJTd3x0MLTl8SG2pUYPJC5Mz1O97dim5MVClHNTwvZ54g==";
    public static final String DESTINATION_BUCKET = "flexify";

    public static void main(String[] args) throws Exception {

        // 1. Configure API
        setupApi();
        StorageAccountsControllerApi storageAccountsApi = new StorageAccountsControllerApi();
        MigrationsControllerApi migrationsApi = new MigrationsControllerApi();

        // 2. Add source storage account
        Long sourceStorageAccountId;
        try {
            sourceStorageAccountId = storageAccountsApi
                    .addStorageAccount(new AddStorageAccountRequest()
                        .storageAccount(new NewStorageAccount()
                            .providerId(SOURCE_PROVIDER_ID)
                            .settings(new StorageAccountSettings()
                                .identity(SOURCE_IDENTITY)
                                .credential(SOURCE_CREDENTIAL)
                                .useSsl(true))))
                    .getId();
        } catch (ApiException ex) {
            // account may already exist
            FlexifyException fex = FlexifyException.fromApi(ex);
            if (fex != null && Objects.equals(fex.message, "STORAGE_ACCOUNT_ALREADY_EXISTS")) {
                sourceStorageAccountId = fex.id;
            } else {
                throw ex;
            }
        }

        // 3. Add destination storage account
        Long destinationStorageAccountId;
        try {
            destinationStorageAccountId = storageAccountsApi
                .addStorageAccount(new AddStorageAccountRequest()
                    .storageAccount(new NewStorageAccount()
                        .providerId(DESTINATION_PROVIDER_ID)
                        .settings(new StorageAccountSettings()
                            .identity(DESTINATION_IDENTITY)
                            .credential(DESTINATION_CREDENTIAL)
                            .useSsl(true))))
                .getId();
        } catch (ApiException ex) {
            // account may already exist
            FlexifyException fex = FlexifyException.fromApi(ex);
            if (fex != null && Objects.equals(fex.message, "STORAGE_ACCOUNT_ALREADY_EXISTS")) {
                destinationStorageAccountId = fex.id;
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

        case RESTARTING:
            System.out.println("Restarting...");
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
