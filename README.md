# PoJson Backend

## Overview

PoJson Backend is a Spring Boot application that converts JSON strings into Java POJO (Plain Old Java Object) classes. The backend service generates Java classes based on the provided JSON structure and packages them into a ZIP file for download.

## Features

- Convert JSON strings to Java POJO classes.
- Support for nested JSON structures and arrays.
- Automatically generates getters and setters for each field.
- Creates a ZIP file containing the generated Java classes.
- Validates package names to ensure they adhere to Java conventions.

## Prerequisites

- Java 11 or higher
- Maven
- Spring Boot

## Getting Started

### Clone the Repository

```bash
git clone https://github.com/your-username/pojson-backend.git
cd pojson-backend
```


### Build the Project
mvn clean install 

### Run the Application
mvn spring-boot:run

The application will start on http://localhost:8088.


### API Endpoints
Generate POJO Classes
URL: /pojson/getFiles
Method: POST
Content-Type: application/json
Request Body:
```json
{
  "packageName": "com.example",
  "json": {
    "name": "John Doe",
    "age": 30,
    "address": {
      "street": "123 Main St",
      "city": "Anytown"
    }
  }
}
```

Response: HTTP 200 OK
<br>Content-Type: application/octet-stream
Body: ZIP file containing the generated Java classes.

Ex cURL:
```text
curl -X POST http://localhost:8088/pojson/getFiles \
-H "Content-Type: application/json" \
-d '{
    "packageName": "com.example",
    "json": {
        "name": "John Doe",
        "age": 30,
        "address": {
            "street": "123 Main St",
            "city": "Anytown"
        }
    }
}' --output GeneratedClass.zip
```


Contact
For any inquiries, please contact at **kyle.bak@pm.me**