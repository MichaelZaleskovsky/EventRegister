# Your challenge will be
Develope application with 2 functions:
* After start app without parameters it is writing current time (timestamp) to database every 1 second.
* After start app with parameter -p it print database to the console and then finish.

Application must work correct in case the connection lost. In this case application must print message to console 
and try to re-connect after 5 seconds. All data received during this waiting period should be saved to database 
in correct order. 

All data in database should be in Ascending Order, it means that order will be correct when it will be printed 
without any sorting.

Also code should have answer what happens if the connection will be very slow or database will be overloaded.

Use SQL or MongoDB by your choice.