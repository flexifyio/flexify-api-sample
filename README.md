# Java API Client Examples for Flexify.IO Management

With [Flexify.IO](https://flexify.io/), storing your data in a cloud does not imply dependency on a single provider anymore!

By unlocking your application from the specific cloud vendor or protocol, you finally gain the freedom to decide when and where to store your data.

And we took care about data migration too!

## Requirements

Building the API client library requires [Maven](https://maven.apache.org/) to be installed.

## Installation

To install the API client library to your local Maven repository, simply execute:

```shell
mvn install
```

## Examples

+ [Data Migration](src/main/java/io/flexify/manageapi/example/DataMigrationExample.java) -
create new migration in Flexiy.IO and poll the migration state
```sh
mvn exec:java -D"exec.mainClass"="io.flexify.manageapi.example.DataMigrationExample"\
 -DFLEXIFY_BASE_PATH_URL=http://localhost:8080/backend\
 -DFLEXIFY_AUTH_USERNAME=user10@testcompany.com\
 -DFLEXIFY_AUTH_PASSWORD=*******\
 -DFLEXIFY_S3_IDENTITY=AKI*************JWWQ\
 -DFLEXIFY_S3_CREDENTIAL=0********************************30Knlg6\
 -DFLEXIFY_SOURCE_BUCKET_NAME_IN_AMAZON=test_src\
 -DFLEXIFY_AZURE_IDENTITY=f**********st\
 -DFLEXIFY_AZURE_CREDENTIAL=Vof********************************************************************************JTw==\
 -DFLEXIFY_DESTINATION_BUCKET_NAME_IN_AZURE=test_dst``` 