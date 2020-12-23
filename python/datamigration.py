from __future__ import print_function

import flexify_api
import time
import json
from flexify_api import ApiClient, StorageAccountsControllerApi, MigrationsControllerApi, \
    AddStorageAccountRequest, NewStorageAccount, StorageAccountSettingsReq, \
    AddMigrationRequest, AddMigrationRequestMapping, MigrationSettings, Migration
from flexify_api.rest import ApiException

# Configuration

# Please contact info@flexify.io to get the URL and the API key
BASE_PATH_URL = 'https://api.flexify.io'
API_KEY = '<your Flexify.IO API key>'

# Migration Source
SOURCE_PROVIDER_ID = 1  # Amazon S3
SOURCE_IDENTITY = 'AKIAIVW6TZW6Q4MBZZ7A'
SOURCE_CREDENTIAL = '<your secret key>'
SOURCE_BUCKET = 'source_bucket'

# Migration Destination
DESTINATION_PROVIDER_ID = 2  # Azure Bob Storage
DESTINATION_IDENTITY = 'flexifyuseast'
DESTINATION_CREDENTIAL = '<your secret key>'
DESTINATION_BUCKET = 'destination_bucket'

# Helper function to print migration status
def print_migration_status(migration: Migration):
    print('Migration (id=%d) status is ' % migration.id, end='')
    if migration.stat.state == 'DEPLOYING':
        print('Deploying engines...')
        return False
    if migration.stat.state == 'WAITING':
        print('Waiting...')
        return False
    elif migration.stat.state == 'STARTING':
        print('Starting...')
        return False
    elif migration.stat.state == 'RESTARTING':
        print('Restarting...')
        return False
    elif migration.stat.state == 'IN_PROGRESS':
        if migration.stat.bytes_processed:
            print('IN_PROGRESS. Bytes processed %d' %
                  migration.stat.bytes_processed)
        else:
            print('IN_PROGRESS. Starting')
        return False
    elif migration.stat.state == 'STOPPING':
        print('Stopping...')
        return False
    elif migration.stat.state == 'STOPPED':
        print('STOPPED')
        return True
    elif migration.stat.state == 'SUCCEEDED':
        if migration.stat.objects_failed == 0:
            print('DONE')
        else:
            print('DONE with ', migration.stat.objects_failed, ' failed objects')
        return True
    elif migration.stat.state == 'FAILED':
        print('FAILED')
        return True
    elif migration.stat.state == 'NO_CONNECTION_TO_ENGINE':
        print('NO_CONNECTION_TO_ENGINE')
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

    storage_accounts_api = StorageAccountsControllerApi(api_client)
    migrations_api = MigrationsControllerApi(api_client)

    # Add source storage account
    source_storage_account_id = None
    try:
        source_storage_account_id = storage_accounts_api.add_storage_account(
            AddStorageAccountRequest(
                storage_account=NewStorageAccount(
                    provider_id=SOURCE_PROVIDER_ID,
                    settings=StorageAccountSettingsReq(
                        identity=SOURCE_IDENTITY,
                        credential=SOURCE_CREDENTIAL,
                        use_ssl='true'
                    )))).id
    except ApiException as e:
        if e.status != 422:
            raise
        rsp = json.loads(e.body)
        if (rsp['message'] != 'STORAGE_ACCOUNT_ALREADY_EXISTS'):
            raise
        source_storage_account_id = rsp['id']

    # Add destination storage account and storage
    destination_storage_account_id = None
    try:
        destination_storage_account_id = storage_accounts_api.add_storage_account(
            AddStorageAccountRequest(
                storage_account=NewStorageAccount(
                    provider_id=DESTINATION_PROVIDER_ID,
                    settings=StorageAccountSettingsReq(
                        identity=DESTINATION_IDENTITY,
                        credential=DESTINATION_CREDENTIAL,
                        use_ssl=True
                    )))).id
    except ApiException as e:
        if e.status != 422:
            raise
        rsp = json.loads(e.body)
        if (rsp['message'] != 'STORAGE_ACCOUNT_ALREADY_EXISTS'):
            raise
        destination_storage_account_id = rsp['id']

    # Start migration
    migrationId = migrations_api.add_migration(AddMigrationRequest(
        mappings=[AddMigrationRequestMapping(
            source_storage_account_id=source_storage_account_id,
            source_bucket_name=SOURCE_BUCKET,
            dest_storage_account_id=destination_storage_account_id,
            dest_bucket_name=DESTINATION_BUCKET
        )],
        settings=MigrationSettings(
            name='Demo Migration',
            migration_mode='COPY',
            conflict_resolution='NEWER'
        )
    )).id

    # Poll the migration state every 5 seconds
    completed = False
    while (not completed):
        migration = migrations_api.get_migration(migrationId)
        completed = print_migration_status(migration)
        time.sleep(1)


except ApiException as e:
    print("Exception when calling Flexify.IO API: %s\n" % e)
