from __future__ import print_function

import flexify_api
import time
from flexify_api import ApiClient, StoragesControllerApi, MigrationsControllerApi, StorageAccountCreateRequest, \
    AddStoragesRequest, Bucket, AddMigrationRequest, Migration, AddStorageAccountRequest
from flexify_api.rest import ApiException

# Configuration

# Please contact info@flexify.io to get the URL and the API key
BASE_PATH_URL = 'https://flexify-manage.azurewebsites.net/backend/'
API_KEY = '<your Flexify.IO API key>'

# Migration Source
SOURCE_PROVIDER_ID = 1  # Amazon S3
SOURCE_IDENTITY = 'AKIAIQTQ3R3LBSHD27GQ'
SOURCE_CREDENTIAL = '<your secret key>'
SOURCE_BUCKET = 'source_bucket'

# Migration Destination
DESTINATION_PROVIDER_ID = 2  # Azure Bob Storage
DESTINATION_IDENTITY = 'autotest12'
DESTINATION_CREDENTIAL = '<your secret key>'
DESTINATION_BUCKET = 'destination_bucket'


# Helper function to print migration status
def print_migration_status(migration: Migration):
    print('Migration (id=%d) status is ' % migration.id, end='')
    if migration.stat.state == 'NOT_ASSIGNED':
        print('Assigning...')
        return False;
    elif migration.stat.state == 'IN_PROGRESS':
        if migration.stat.bytes_processed:
            print('IN_PROGRESS. Bytes processed %d' % migration.stat.bytes_processed)
        else:
            print('IN_PROGRESS. Starting')
        return False;
    elif migration.stat.state == 'IN_PROGRESS_CANCELING':
        print('IN_PROGRESS_CANCELING')
        return False
    elif migration.stat.state == 'CANCELED':
        print('CANCELED')
        return True
    elif migration.stat.state == 'SUCCEEDED':
        print('SUCCEEDED')
        return True
    elif migration.stat.state == 'FAILED':
        print('FAILED')
        return True
    print('Unknown migration state %s' % migration.stat.state)
    return True


try:
    # Setup API
    configuration = flexify_api.Configuration()
    configuration.host = BASE_PATH_URL
    configuration.api_key_prefix["Authorization"] = 'Bearer'
    configuration.api_key = {"Authorization": API_KEY}

    api_client = ApiClient(configuration=configuration)

    storages_api = StoragesControllerApi(api_client)
    migrations_api = MigrationsControllerApi(api_client)

    # Add source storage account and storage
    source_storage_account_id = storages_api.add_storage_account(
        AddStorageAccountRequest(
            storage_account=StorageAccountCreateRequest(
                provider_id=SOURCE_PROVIDER_ID,
                identity=SOURCE_IDENTITY,
                credential=SOURCE_CREDENTIAL,
                use_ssl='true'
            )
        )
    ).id
    source_storage_id = storages_api.add_storages(
        storage_account_id=source_storage_account_id,
        request=AddStoragesRequest(
            buckets=[Bucket(name=SOURCE_BUCKET)]
        )
    ).ids[0]

    # Add destination storage account and storage
    destination_storage_account_id = storages_api.add_storage_account(
        AddStorageAccountRequest(
            storage_account=StorageAccountCreateRequest(
                provider_id=DESTINATION_PROVIDER_ID,
                identity=DESTINATION_IDENTITY,
                credential=DESTINATION_CREDENTIAL,
                use_ssl=True
            ))
    ).id
    destination_storage_id = storages_api.add_storages(
        storage_account_id=destination_storage_account_id,
        request=AddStoragesRequest(
            buckets=[Bucket(name=DESTINATION_BUCKET)]
        )
    ).ids[0]

    # Start migration
    migrationId = migrations_api.add_migration(AddMigrationRequest(
        source_id=source_storage_id,
        destination_id=destination_storage_id,
        count_source_objects=False,
        slots=8,
        migration_mode='COPY',
        conflict_resolution='NEWER'
    )).id

    # Poll the migration state every 5 seconds
    completed = False
    while (not completed):
        migration = migrations_api.get_migration(migrationId)
        completed = print_migration_status(migration)
        time.sleep(5)


except ApiException as e:
    print("Exception when calling Flexify.IO API: %s\n" % e)
