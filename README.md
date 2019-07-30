# EventRegister

* Robust application to save registred events to SQL database.
* Application guarantee that all registred data will be saved safely even in case of datatbase acidentally lost connection, 
failure or overloaded and have very slow access.
* Application keeps data in inner buffer. If the connection will be lost system will try to restore connection and 
buffer will be filling up
* When buffer reach maximum size system will try to switch output to next reserved 
SQL server
* When the list of reserve servers is empty system will save buffered data to file. Database saving process will stop but 
all data will be kept in internal buffer.
* At this moment you can restore SQL server and use 'eventregister -u filename' command to update data from file to SQL 
database 
* Than type CONTINUE in console and press Enter. Data saving process will continue.

To start application run EventRegister.java

Please use: | purpose
------------ | -------------
**eventregister** | *start register process*
**eventregister -p serverNum** | *print database to console, serverNum = 0 for base server, 1, 2, 3... for reserve*
**eventregister -u filename** | *update database from file 'filename'*
**eventregister -c** | *check is the database continuous or not*

## config.xml
### Use configuration file to setup variables:
#### maxBufferSize - *when buffer size reach this value system will try to switch server or save data to file*
#### driver - *JDBC driver for actual SQL database*
#### tableName - *name of table in database to keep data*
#### delay - *time in seconds between attempt to restore connection*
#### servers - *the list of reserved servers. It must contain at least one member to use it as a base.*
