# Java API Client for Flexify.IO Management Sampels

[Flexify.IO](https://flexify.io/) provides cloud-agnostic multi-cloud horizontally scalable S3-compatable storages by combining multiple 3rd party storages into a single virtual namespace. Among functions:
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
+ API key (request by emailing us at [info@flexify.io](mailtu:info@flexify.io))

To install dependencies:
```sh
mvn clean install
```
### Samples
+ [Data Migration](src/main/java/io/flexify/manageapi/sample/DataMigrationSample.java) -
create new migration in Flexiy.IO and poll the migration state
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

## Supported Cloud Storages Providers
ID   | Cloud Storage Provider
-----|------------------------------
1    | Amazon S3
2    | Microsoft Azure Blob Storage
3    | Mail.ru Hotbox
4    | Mail.ru Icebox
5    | Wasabi
6    | Exoscale
7    | Alibaba Cloud OSS
8    | Google Cloud Storage
9    | DigitalOcean Spaces in New York
10   | DigitalOcean Spaces in Amsterdam
11   | DigitalOcean Spaces in Singapore
12   | Technoserv Cloud
13   | Airee Cloud
14   | DataLine

## API Documentation
The complete API documentation along with API source code is published as [flexify-manage-api-client](https://github.com/flexifyio/flexify-manage-api-client) 

## Contact
For any questions or suggestions please contact [info@flexify.io](mailtu:info@flexify.io)
