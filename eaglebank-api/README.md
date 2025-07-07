# Eagle Bank API

This project implements core banking features such as user registration and authentication, account creation and handling transactions using **Java 21** and **Spring Boot 3.x**. It also includes an in-memory H2 database and Swagger UI for interactive API documentation.

---

## Getting Started

###  Requirements
- **Java:** 21
- **Spring Boot:** 3.x
- **Maven**

###  Build & Run

```bash
mvn clean install
mvn spring-boot:run
````
### Swagger and open api doc links
**Swagger**: [http://localhost:8080/eaglebank-api/swagger-ui/index.html](http://localhost:8080/eaglebank-api/swagger-ui/index.html)  
**OpenAPI Docs**: [http://localhost:8080/eaglebank-api/v3/api-docs](http://localhost:8080/eaglebank-api/v3/api-docs)

### H2 DB console 
```commandline
  h2 console link: http://localhost:8080/eaglebank-api/h2-console/
  When h2 console window pops up, enter "jdbc:h2:mem:eaglebankdb" in "JDBC URL" field and click on "Connect"
  A few queries:
  select * from users
  select * from accounts
  select * from transactions
```

### Business cases covered as part of this API
 **Create a user**
- *Scenario:* Create a new user
Given a user wants to signup for Eagle Bank
When the user makes a `POST` request to the `/v1/users` endpoint with all the required data Then a new user is created
Scenario: Create a new user without supplying all required data
Given a user has successfully authenticated
When the user makes a `POST` request to the `/v1/users` endpoint with required data missing Then the system returns a Bad Request status code and error message
Authenticate a user


- *Scenario*: Create a new user without supplying all required data
  Given a user has successfully authenticated
  When the user makes a `POST` request to the `/v1/users` endpoint with required data missing Then the system returns a Bad Request status code and error message


**Authenticate a user**
- *Scenario*: We want you to implement one or more endpoints to authenticate a user and return a JWT token which can be passed as a bearer token to all endpoints


**Fetch a user**
- *Scenario*: User wants to fetch their user details
Given a user has successfully authenticated
When the user makes a `GET` request to the `/v1/users/{userId}` endpoint supplying their `userId` Then the system fetches the user details


- *Scenario*: User wants to fetch the user details of another user
Given a user has successfully authenticated
When the user makes a `GET` request to the `/v1/users/{userId}` endpoint supplying another user's `userId` Then the system returns a Forbidden status code and error message


- *Scenario*: User wants to fetch the user details of a non-existent user
Given a user has successfully authenticated
When the user makes a `GET` request to the `/v1/users/{userId}` endpoint supplying a `userId` which doesn't exist Then the system returns a Not Found status code and error message

**Create a Bank Account**
- *Scenario*: User wants to create a new bank account
Given a user has successfully authenticated
When the user makes a `POST` request to the `/v1/accounts` endpoint with all the required data Then a new bank account is created, and the account details are returned


- *Scenario*: User wants to create a new bank account without supplying all required data
Given a user has successfully authenticated
When the user makes a `POST` request to the `/v1/accounts` endpoint with required data missing Then the system returns a Bad Request status code and error message


**Fetch a Bank Account**
- *Scenario*: User wants to fetch their bank account details
Given a user has successfully authenticated
When the user makes a `GET` request to the `/v1/accounts/{accountId}` endpoint And the account is associated with their `userId`
Then the system fetches the bank account details


- *Scenario*: User wants to fetch another user's bank account details
Given a user has successfully authenticated
When the user makes a `GET` request to the `/v1/accounts/{accountId}` endpoint And the account is not associated with their `userId`
Then the system returns a Forbidden status code and error message


- *Scenario*: User wants to fetch a non-existent bank account
Given a user has successfully authenticated
When the user makes a `GET` request to the `/v1/accounts/{accountId}` endpoint And the `accountId` doesn't exist
Then the system returns a Not Found status code and error message


**Create a Transaction**
- *Scenario*: User wants to deposit money into their bank account
Given a user has successfully authenticated
When the user makes a `POST` request to the `/v1/accounts/{accountId}/transactions` endpoint with all the required data And the transaction type is `deposit`
And the account is associated with their `userId`
Then the deposit is registered against the account
And the account balance is updated


- *Scenario*: User wants to withdraw money from their bank account
Given a user has successfully authenticated
When the user makes a `POST` request to the `/v1/accounts/{accountId}/transactions` endpoint with all the required data And the transaction type is `withdrawal`
And the account has sufficient funds
And the account is associated with their `userId`
Then the withdrawal is registered against the account
And the account balance is updated


- *Scenario*: User wants to withdraw money from their bank account but they have insufficient funds
Given a user has successfully authenticated
When the user makes a `POST` request to the `/v1/accounts/{accountId}/transactions` endpoint with all the required data And the transaction type is `withdrawal`
And the account has insufficient funds
And the account is associated with their `userId`
Then the system returns a Unprocessable Entity status code and error message


- *Scenario*: User wants to deposit or withdraw money into another user's bank account
Given a user has successfully authenticated
When the user makes a `POST` request to the `/v1/accounts/{accountId}/transactions` endpoint with all the required data And the account is not associated with their `userId`
Then the system returns a Forbidden status code and error message


- *Scenario*: User wants to deposit or withdraw money into a non-existent bank account
Given a user has successfully authenticated
When the user makes a `POST` request to the `/v1/accounts/{accountId}/transactions` endpoint with all the required data And the `accountId` doesn't exist
Then the system returns a Not Found status code and error message


- *Scenario*: User wants to deposit or withdraw money without supplying all required data
Given a user has successfully authenticated
When the user makes a `POST` request to the `/v1/accounts/{accountId}/transactions` endpoint with required data missing Then the system returns a Bad Request status code and error message



**Fetch a Transaction**
- *Scenario*: User wants to fetch a transaction on their bank account
Given a user has successfully authenticated
When the user makes a `GET` request to the `/v1/accounts/{accountId}/transactions/{transactionId}` endpoint And the account is associated with their `userId`
And the `transactionId` is associated with the `accountId` specified
Then the transaction details are returned


- *Scenario*: User wants to fetch a transaction on another user's bank account
Given a user has successfully authenticated
When the user makes a `GET` request to the `/v1/accounts/{accountId}/transactions/{transactionId}` endpoint And the account is not associated with their `userId`
Then the system returns a Forbidden status code and error message


- *Scenario*: User wants to fetch a transaction on a non-existent bank account
Given a user has successfully authenticated
When the user makes a `GET` request to the `/v1/accounts/{accountId}/transactions/{transactionId}` endpoint And the `accountId` doesn't exist
Then the system returns a Not Found status code and error message


- *Scenario*: User wants to fetch a transactions on a non-existent transaction ID
Given a user has successfully authenticated
When the user makes a `GET` request to the `/v1/accounts/{accountId}/transactions/{transactionId}` endpoint And the account is associated with their `userId`
And the `transactionId` does not exist
Then the system returns a Not Found status code and error message


- *Scenario*: User wants to fetch a transaction against the wrong bank account
Given a user has successfully authenticated
When the user makes a `GET` request to the `/v1/accounts/{accountId}/transactions/{transactionId}` endpoint And the account is associated with their `userId`
And the `transactionId` is not associated with the `accountId` specified
Then the system returns a Not Found status code and error message