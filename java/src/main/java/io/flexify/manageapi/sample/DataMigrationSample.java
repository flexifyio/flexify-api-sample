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
import io.flexify.apiclient.model.Migration;
import io.flexify.apiclient.model.MigrationSettingsReq;
import io.flexify.apiclient.model.MigrationSettingsReq.MigrationModeEnum;
import io.flexify.apiclient.model.NewStorageAccount;
import io.flexify.apiclient.model.StorageAccountSettingsReq;

/**
 * Sample code demonstrating starting and monitoring migration via Flexify.IO
 * Management API.
 *
 * @author Alexander Bondin
 */
public class DataMigrationSample {

    // Please contact info@flexify.io to get the URL and the API key
    private final static String BASE_PATH_URL = "https://api.flexify.io";
    private final static String API_KEY = "<your Flexify.IO API key>";

    // Migration Source
    public static final Long SOURCE_PROVIDER_ID = 1L; // Amazon S3
    public static final String SOURCE_IDENTITY = "AKIAIVW6TZW6Q4MBZZ7A";
    public static final String SOURCE_CREDENTIAL = "<your Amazon S3 secret key>";
    public static final String SOURCE_BUCKET = "<your source bucket>";

    // Migration Destination
    public static final Long DESTINATION_PROVIDER_ID = 2L; // Azure Bob Storage
    public static final String DESTINATION_IDENTITY = "flexifyuseast";
    public static final String DESTINATION_CREDENTIAL = "<your Azure secret key>";
    public static final String DESTINATION_BUCKET = "<your destination bucket>";

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
                            .settings(new StorageAccountSettingsReq()
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
                        .settings(new StorageAccountSettingsReq()
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
                    .settings(new MigrationSettingsReq()
                        .name("Demo Migration")
                        .migrationMode(MigrationModeEnum.COPY))
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
            Thread.sleep(1000l);
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
            case DEPLOYING:
                System.out.format("Deploying engines...%n");
                return false;

            case WAITING:
                System.out.format("Waiting...%n");
                return false;

            case STARTING:
                System.out.format("Starting...%n");
                return false;

            case RESTARTING:
                System.out.format("Restarting...%n");
                return false;

            case IN_PROGRESS:
                if (migration.getStat().getBytesProcessed() == null) {
                    System.out.format("IN_PROGRESS. Starting...%n");
                } else {
                    System.out.format("IN_PROGRESS. Bytes processed %d%n" , migration.getStat().getBytesProcessed());
                }
                return false;

            case STOPPING:
                System.out.format("STOPPING...%n");
                return false;

            case STOPPED:
                System.out.format("STOPPED%n");
                return true;

            case SUCCEEDED:
                if (migration.getStat().getObjectsFailed() == 0)
                    System.out.format("DONE%n");
                else
                    System.out.format("DONE with %d FAILED objects", migration.getStat().getObjectsFailed());
                return true;

            case FAILED:
                System.out.format("FAILED%n");
                return true;

            case NO_CONNECTION_TO_ENGINE:
                System.out.format("NO_CONNECTION_TO_ENGINE%n");
                return true;
        }

        return true;
    }
}
