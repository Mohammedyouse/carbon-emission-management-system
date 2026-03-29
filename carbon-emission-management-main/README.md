# Carbon Emission Management System

## Project Overview

The Carbon Emission Management System is a Java-based application that manages and analyzes carbon emission data using a MySQL database. The system allows storing emission records, executing SQL queries, and generating reports for analysis and decision making.

This project demonstrates database design, SQL scripting, and Java database connectivity using JDBC.

---

## Technologies Used

* Java
* Maven
* MySQL
* JDBC
* SQL
* Git & GitHub

---

## Project Structure

```
carbon-emission-management-system
│
├── src/                               # Java source code
├── pom.xml                            # Maven configuration
├── README.md                          # Project documentation
├── carbon_emissions_schema.sql        # Database schema
├── carbon_emissions_sample_data.sql   # Sample data
├── carbon_emissions_queries.sql       # SQL queries
├── create_and_run_all.sql             # Run all SQL scripts
└── .gitignore
```

---

## Database Setup

This project uses MySQL database.

### Steps to setup database:

1. Open MySQL.
2. Create database:

   ```
   CREATE DATABASE carbon_emissions_db;
   ```
3. Select database:

   ```
   USE carbon_emissions_db;
   ```
4. Run SQL scripts:

   * carbon_emissions_schema.sql
   * carbon_emissions_sample_data.sql
   * carbon_emissions_queries.sql

   OR run:

   ```
   create_and_run_all.sql
   ```

This will create all tables and insert sample data required for the project.

---

## How to Run the Java Application

Open terminal in the project folder and run:

```
mvn clean package
mvn exec:java
```

Make sure MySQL is running before starting the application.

---

## Features

* Carbon emission data management
* Database schema design
* SQL queries and reports
* Java MySQL connection using JDBC
* Maven project structure
* Role-based SQL scripts
* Sample dataset for testing

---

## Learning Outcomes

* Database design and normalization
* Writing SQL schema and queries
* Java database connectivity (JDBC)
* Maven project management
* GitHub project management
* Backend application development

---

## Author

Mohammed Yousef
Computer Science and Engineering Student
