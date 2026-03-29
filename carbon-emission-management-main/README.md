# Carbon Emission Management System

## Project Overview

The Carbon Emission Management System is a Java-based application that helps manage and analyze carbon emission data using a MySQL database. The system allows storing emission records, running queries, and generating reports for analysis.

## Technologies Used

* Java
* Maven
* MySQL
* JDBC
* SQL

## Project Structure

```
src/                     → Java source code  
pom.xml                  → Maven configuration  
carbon_emissions_schema.sql  
carbon_emissions_sample_data.sql  
carbon_emissions_queries.sql  
README.md
```

## How to Run the Project

1. Open MySQL and create database.
2. Run SQL scripts:

   * carbon_emissions_schema.sql
   * carbon_emissions_sample_data.sql
   * carbon_emissions_queries.sql
3. Open project in terminal.
4. Run:

   ```
   mvn clean package
   mvn exec:java
   ```

## Features

* Carbon emission data storage
* SQL queries and reports
* Database integration using JDBC
* Maven project structure
* Role-based SQL scripts

## Author

Mohammed Youse
