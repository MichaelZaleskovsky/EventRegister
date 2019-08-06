# EventRegister

* Robust application for saving registered events to SQL database.
* Application guarantees that all registred data will be saved safely even in case of loss connection, database
failure or overloaded and has slow access.
* Application keeps data in inner buffer. If the connection is lost the system will try to restore connection and 
buffer will be filling up
* When buffer reaches its maximum size the system will try to switch output to the next reserve
SQL server
* When the list of reserve servers is empty the system will save buffered data to file. Database saving process will be stopped but 
all data will be kept in internal buffer.
* At this moment it is possible to restore SQL server and use 'eventregister -u filename' command to update data from file to SQL 
database 
* Then type CONTINUE in console and press Enter. Data saving process will continue.

## TEST
* To test application use 'eventregister -c' argument to check, is the database continuous (every next row more, then previouse and 
difference is about 1 sec.)

## To start application run EventRegister.java

Please use: | purpose
------------ | -------------
**eventregister** | *start register process*
**eventregister -p serverNum** | *print database to console, serverNum = 0 for the main server, 1, 2, 3... for reserve*
**eventregister -u filename** | *update database from file 'filename'*
**eventregister -c** | *check is the database continuous or not*

## config.xml
### Use configuration file to setup variables:
#### maxBufferSize - *when buffer size reachs this value the system will try to switch server or save data to file*
#### driver - *JDBC driver for actual SQL database*
#### tableName - *name of table in database for data storage*
#### delay - *time in seconds between attempts to restore connection*
#### servers - *the list of reserve servers. It must contain at least one member to use it as a main one.*
