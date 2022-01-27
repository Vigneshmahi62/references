# MDM Data

### PreRequisite
* Install lombok in your IDE.
> [Eclipse](https://projectlombok.org/setup/eclipse)
> [IntelliJ](https://projectlombok.org/setup/intellij)
* Run DDL scripts from /mdm/provider_mdm/queries/DDL.sql if you are running in local.
* Change the cassandra configurations in mdm_data module -> src/main/resources/application.properties

### Steps to insert MDM data
Create master fields against the json request that you want to made.
* Prepare Json request w.r.t master fields create api
* API to call -> MDM Migration / Mdm Data / Create Master Fields
* Use Get API to get the changed json key names

Send a request to insert mdm data
* Change the json key names with respect to master fields table
* Send request to create mdm data -> MDM Migration / Mdm Data / Create Mdm Data