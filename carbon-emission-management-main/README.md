# Carbon Emissions Database Java Application

This Java application demonstrates how to connect to the `carbon_emissions_db` MySQL database using JDBC 
and interact with the stored procedures and functions.

## Prerequisites

1. Java 11 or higher
2. Maven
3. MySQL Server with the `carbon_emissions_db` database initialized

## Setup

1. Clone this repository or download the source code.

2. Make sure your MySQL server is running and the `carbon_emissions_db` database is created with 
   all the required tables, procedures, and functions (as defined in `carbon_emissions_plsql.sql`).

3. Update the database connection details in `src/main/java/com/carbon/emissions/CarbonEmissionsDBConnection.java`:
   ```java
   private static final String DB_URL = "jdbc:mysql://localhost:3306/carbon_emissions_db";
   private static final String USER = "root";
   private static final String PASSWORD = "lakshya@123"; // Change this to your actual password
   ```

4. Open a terminal or command prompt and navigate to the project's root directory (where the `pom.xml` file is located).

## Building the Application

Run the following Maven command to build the application:

```bash
mvn clean package
```

This will compile the code and create a JAR file in the `target` directory.

## Running the Application

You can run either of the two main classes to test the application:

### Testing the Database Connection

```bash
# Using Maven
mvn exec:java -Dexec.mainClass="com.carbon.emissions.CarbonEmissionsDBConnection"

# Or using Java directly
java -cp target/carbon-emissions-db-1.0-SNAPSHOT.jar com.carbon.emissions.CarbonEmissionsDBConnection
```

This will test if the connection to the database can be established successfully.

### Testing Data Access Methods

```bash
# Using Maven
mvn exec:java -Dexec.mainClass="com.carbon.emissions.CarbonEmissionsDataAccess"

# Or using Java directly
java -cp target/carbon-emissions-db-1.0-SNAPSHOT.jar com.carbon.emissions.CarbonEmissionsDataAccess
```

This will demonstrate calling the stored procedures and functions from the database.

## Troubleshooting

- If you get a "MySQL JDBC Driver not found" error, make sure the MySQL Connector/J dependency is correctly 
  included in your Maven build.
  
- If you get a connection error, verify that:
  - Your MySQL server is running
  - The `carbon_emissions_db` database exists
  - The user credentials are correct
  - The MySQL server is accessible from your machine

- If the stored procedures or functions are not found, ensure that you've run the SQL script 
  (`carbon_emissions_plsql.sql`) to create them in the database. 

///////////////////////////////////////////////////////////////////////////////////////////////////


                                  // How to Run the project : //
// Step_1 : 

// open  My skl 
// Enter your password : 71860015 //
// show databases; 
// Use carbon_emissions_db ;

// Step_2 : 

// Go to   D:\SYMBIOSIS UNIVERSITY\Second Year\Fourth Semester\Flexi-Credit Course (Java )\carbon-emission-management-main (2)         ---> open it from vs code 

// Run this in terminal :
// mvn clean package
// mvn exec:java


// Step_3 :
// log in with this user name = root  && password = 71860015
