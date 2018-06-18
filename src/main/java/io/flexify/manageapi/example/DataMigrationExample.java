package io.flexify.manageapi.example;

import io.flexify.apiclient.api.AuthenticationControllerApi;
import io.flexify.apiclient.api.MigrationsControllerApi;
import io.flexify.apiclient.api.StoragesControllerApi;
import io.flexify.apiclient.handler.ApiException;
import io.flexify.apiclient.handler.Configuration;
import io.flexify.apiclient.handler.auth.ApiKeyAuth;
import io.flexify.apiclient.model.*;

import java.util.List;

/**
 *
 * Create new migration and poll the migration state.
 *
 * @author Alexander Bondin
 */
public class DataMigrationExample {

    // Please contact info@flexify.io to get the URL
    private final static String BASE_PATH_URL = System.getProperty("FLEXIFY_BASE_PATH_URL", "https://ask-flexify-for-your-url");

    // Sign-up first
    // NOTE: Skip authentication if you already have an API key
    private final static String AUTH_USERNAME = System.getProperty("FLEXIFY_AUTH_USERNAME", "USE_EMAIL_FROM_SIGNUP_FORM");
    private final static String AUTH_PASSWORD = System.getProperty("FLEXIFY_AUTH_PASSWORD", "USE_YOUR_STRONG_PASSWORD");

    // Amazon S3 Source Configuration Example
    public static final String AMAZON_S3_ENDPOINT ="s3.amazonaws.com";
    public static final String S3_IDENTITY = System.getProperty("FLEXIFY_S3_IDENTITY", "USE_YOUR_S3_IDENTITY_HERE");
    public static final String S3_CREDENTIAL = System.getProperty("FLEXIFY_S3_CREDENTIAL", "USE_YOUR_S3_IDENTITY_HERE");
    public static final String SOURCE_BUCKET_NAME_IN_AMAZON = System.getProperty("FLEXIFY_SOURCE_BUCKET_NAME_IN_AMAZON", "USE_YOUR_SOURCE_BUCKET_NAME");

    // Microsoft Azure Destination Configuration Example
    public static final String MICROSOFT_AZURE_ENDPOINT =  "{identity}.blob.core.windows.net";
    public static final String AZURE_IDENTITY = System.getProperty("FLEXIFY_AZURE_IDENTITY", "USE_YOUR_AZURE_IDENTITY_HERE");
    public static final String AZURE_CREDENTIAL = System.getProperty("FLEXIFY_AZURE_CREDENTIAL", "USE_YOUR_AZURE_IDENTITY_HERE");
    public static final String DESTINATION_BUCKET_NAME_IN_AZURE = System.getProperty("FLEXIFY_DESTINATION_BUCKET_NAME_IN_AZURE", "USE_YOUR_DESTINATION_CONTAINER");

    public static void main(String[] args) throws Exception {

        // 1) Setup connection URL
        Configuration.getDefaultApiClient().setBasePath(BASE_PATH_URL);
        try {
            // 2) Login to the Flexify.IO if you don't have an API key
            AuthenticationResponse authResponse = new AuthenticationControllerApi().authenticationRequestUsingPOST(
                    new AuthenticationRequest().username(AUTH_USERNAME).password(AUTH_PASSWORD)
            );
            final String apiKey = authResponse.getToken();

            // 3) Authenticate the client with API key
            ApiKeyAuth bearer = (ApiKeyAuth) Configuration.getDefaultApiClient().getAuthentication("Bearer");
            bearer.setApiKeyPrefix("Bearer");
            bearer.setApiKey(apiKey);

            // 4) Use the api
            Long migrationId = createNewMigration();

            // 5) Poll the migration state every 10 seconds
            pollMigration(migrationId);
        } catch (ApiException e) {
            System.err.println("Exception when calling Flexify.IO API: " + e.getCode());
            e.printStackTrace();
        }
    }

    /**
     * Create new migration to copy objects from bucket in Amazon to container in Azure
     *
     * @return new migration id
     * @throws ApiException
     */
    private static Long createNewMigration() throws ApiException {

        StoragesControllerApi storagesApi = new StoragesControllerApi();

        // Get all supported cloud storage providers
        List<StorageProvider> allProviders = storagesApi
                .getAllStorageProvidersUsingGET();

        Long amazonS3ProviderId = allProviders.stream()
                .filter(p -> AMAZON_S3_ENDPOINT.equals(p.getEndpoint())).findFirst().orElseThrow(() -> new IllegalArgumentException("Unable to find Amazon S3 Provider")).getId();

        Long azureProviderId = allProviders.stream()
                .filter(p -> MICROSOFT_AZURE_ENDPOINT.equals(p.getEndpoint())).findFirst().orElseThrow(() -> new IllegalArgumentException("Unable to find Windows Azure Provider")).getId();


        // Add Amazon Storage Account with one bucket
        Long amazonAccountId = storagesApi.addStorageAccountWithBucketsUsingPOST(
                new AddStorageAccountWithBucketsRequest().storageAccount(
                        new StorageAccountCreateRequest()
                                .providerId(amazonS3ProviderId)
                                .identity(S3_IDENTITY)
                                .credential(S3_CREDENTIAL)
                                .useSsl(true)
                ).addBucketsItem(new Bucket().name(SOURCE_BUCKET_NAME_IN_AMAZON))
        ).getId();

        // Add Microsoft Account with one container
        Long microsoftAccountId = storagesApi.addStorageAccountWithBucketsUsingPOST(
                new AddStorageAccountWithBucketsRequest().storageAccount(
                        new StorageAccountCreateRequest()
                                .providerId(azureProviderId)
                                .identity(AZURE_IDENTITY)
                                .credential(AZURE_CREDENTIAL)
                                .useSsl(true)
                ).addBucketsItem(new Bucket().name(DESTINATION_BUCKET_NAME_IN_AZURE))
        ).getId();


        // Get created Storage ID for an amazon bucket
        Long amazonBucketStorageId = storagesApi.getStoragesForStorageAccountUsingGET(amazonAccountId)
                .stream().filter(s -> s.getBucket().equals(SOURCE_BUCKET_NAME_IN_AMAZON)).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Bucket " + SOURCE_BUCKET_NAME_IN_AMAZON + " cannot be found")).getId();

        // Get created Storage ID for an azure bucket
        Long azureBucketStorageId = storagesApi.getStoragesForStorageAccountUsingGET(microsoftAccountId)
                .stream().filter(s -> s.getBucket().equals(DESTINATION_BUCKET_NAME_IN_AZURE)).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("DESTINATION_BUCKET_NAME_IN_AZURE " + DESTINATION_BUCKET_NAME_IN_AZURE + " cannot be found")).getId();

        // Start the Migration
        return new MigrationsControllerApi().addMigrationUsingPOST(
                new AddMigrationRequest()
                        .migrationMode(AddMigrationRequest.MigrationModeEnum.COPY)
                        .slots(8)
                        .sourceId(amazonBucketStorageId)
                        .destinationId(azureBucketStorageId)
        ).getId();
    }

    private static void pollMigration(Long migrationId) throws Exception {
        MigrationsControllerApi migrationsApi = new MigrationsControllerApi();
        Migration migration;
        boolean migrationCompleted = false;
        while (!migrationCompleted) {
            migration = migrationsApi.getUsingGET(migrationId);
            switch (migration.getStat().getState()) {
                case NOT_ASSIGNED:
                    System.out.println("Migration is not yet assigned to engines");
                    break;
                case IN_PROGRESS:
                    if (migration.getStat().getBytesProcessed() == null){
                        System.out.println("Migration starting...");
                    } else {
                        System.out.println("Migration progress. Bytes processed " + migration.getStat().getBytesProcessed());
                    }
                    break;
                default:
                    System.out.println("------------- Migration completed with the state " + migration.getStat().getState());
                    migrationCompleted = true;
                    break;
            }
            Thread.sleep(5000l);
        }
    }
}
