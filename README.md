# Java API Client for Flexify.IO Management Code Examples

[Flexify.IO](https://flexify.io/) provides cloud-agnostic multi-cloud horizontally scalable S3-compatible storages by combining multiple 3rd party storages into a single virtual namespace. Among functions:
+ Flexify.IO virtual bucket that combines multiple cloud storages in a single namespace
+ API translation from Amazon S3 to Azure Blob Storage or Alibaba OSS
+ Data migration between cloud providers


## Configuration
Each sample class has several configuration parameters such as storage account keys. Please replace placeholder values with valid parameters before running the samples.

## Java

### Installation
Requirements:
+ [Maven](https://maven.apache.org/)
+ Active account at [manage.flexify.io](https://manage.flexify.io/)
+ API key (request by emailing us at [info@flexify.io](mailto:info@flexify.io))

To install dependencies:
```sh
cd java
mvn clean install
```
### Samples
+ [Data Migration](java/src/main/java/io/flexify/manageapi/sample/DataMigrationSample.java) -
create new migration in Flexify.IO and poll the migration state
```sh
mvn exec:java -D"exec.mainClass"="io.flexify.manageapi.sample.DataMigrationSample"
```

## Python

### Installation
```sh
pip install git+https://github.com/flexifyio/flexify-manage-api-client-python.git
```

### Samples

+ [Data Migration](python/datamigration.py) -
  create new migration in Flexiy.IO and poll the migration state
```sh
python python/datamigration.py
```

## Some of the Supported Cloud Storages Providers
ID   | Cloud Storage Provider
-----|------------------------------
1    | Amazon S3
2    | Microsoft Azure Blob Storage
8    | Google Cloud Storage
24   | Backblaze B2 Cloud Storage
5    | Wasabi
9    | DigitalOcean Spaces in New York
10   | DigitalOcean Spaces in Amsterdam
11   | DigitalOcean Spaces in Singapore
17   | DigitalOcean Spaces in San Francisco
7    | Alibaba Cloud OSS
6    | Exoscale
3    | Mail.ru Hotbox
4    | Mail.ru Icebox
26   | Yandex.Cloud Object Storage
16   | Dell EMC - ECS Test Drive
20   | Minio
21   | Custom S3-compatible provider

## API Documentation
The complete API documentation along with API source code is published at:
- Java [flexify-api-java](https://github.com/flexifyio/flexify-api-java)
- Python [flexify-api-python](https://github.com/flexifyio/flexify-api-python)

## Contact
For any questions or suggestions please contact [info@flexify.io](mailto:info@flexify.io)
