# Bank Multi-Tenant (CQRS, Event Sourcing & Spring AI)

This is a sample banking application built with a modern Java stack to demonstrate several advanced software patterns. It uses CQRS and Event Sourcing with the Axon Framework and integrates real AI features using Spring AI.

It was originally a 2017-era project that has been fully modernized to a 2026 tech stack.

---

## Key Features

-   **CQRS & Event Sourcing**: A clean separation between writing data (Commands) and reading data (Queries). All state changes are stored as a sequence of events.
-   **Multi-Tenancy**: Supports multiple tenants out-of-the-box. Data is isolated based on an `X-Tenant-ID` header, which propagates through the entire system.
-   **AI-Powered Commands**: Uses an LLM to parse natural language commands like *"Transfer $50 to acc-123"* into structured system actions.
-   **AI-Powered Insights (RAG)**: Ask questions about your account history in plain English. This uses a Retrieval-Augmented Generation (RAG) pattern to ground the AI's answers in your actual transaction data.

---

## Running Locally

Follow these steps to get the project running on your own computer.

### **Prerequisites**

-   **Java 20+ SDK**
-   **Docker Desktop**
-   **OpenAI API Key**

---

### **Step 1: Set up your OpenAI Key**

This project uses a `.env` file to manage the API key so it's not hardcoded.

1.  Create a new file named `.env` in the root folder.
2.  Open the file and add this line, pasting your key after the `=`:
    ```text
    OPENAI_API_KEY=your-key-here
    ```

---

### **Step 2: Start the Backend Services**

The app needs a database (MySQL) and a message broker (RabbitMQ). Docker handles this.

1.  Open a terminal in the project root.
2.  Run the docker-compose command:
    ```bash
    docker compose up -d
    ```

---

### **Step 3: Build the Project**

Compile the Java code and run tests using the Maven wrapper.

1.  In the same terminal, run:
    ```bash
    ./mvnw clean install
    ```

---

### **Step 4: Run the Application**

Now, start the main web application.

1.  Navigate into the `web` directory:
    ```bash
    cd web
    ```
2.  Run the Spring Boot application:
    ```bash
    ../mvnw spring-boot:run
    ```

---

### **Step 5: Access the Dashboard**

Open your web browser and go to: **[http://localhost:8080](http://localhost:8080)**

---

## Tech Stack

-   **Backend**: Java 20, Spring Boot 3.4, Axon Framework 4.10, Spring AI
-   **Database**: MySQL 8.0 (for event store & projections), H2 (for local dev)
-   **Messaging**: RabbitMQ / AMQP
-   **Frontend**: AngularJS 1.8 (with a modernized UI)
-   **Testing**: JUnit 5, Mockito, Axon Test Fixtures

**Order of operations:**
- Clear `account_daily_summary` table
- Shut down `account-daily-summary` TrackingEventProcessor
- Reset its token to the beginning
- Start the processor → events replay and projection rebuilds

## Snapshotting

Improves aggregate load performance for accounts with many events. See [docs/SNAPSHOTTING.md](docs/SNAPSHOTTING.md).

```properties
axon.snapshotting.enabled=true
axon.snapshotting.bank-account.threshold=50
```

## Technical Details

- **Stack**: Spring Boot 1.5, Axon Framework 3.0.4, JPA/H2 (single-node), MySQL (distributed)
- **Transport**: WebSocket + STOMP
- **Command routing**: Local or DistributedCommandBus (distributed profile)
- **Security**: Spring Security protects `/admin/**` with HTTP Basic when rebuild is enabled

## Usage

### Single node (in-memory)

```bash
mvn clean install
java -jar web/target/bank-multi-tenant-web-0.0.1-SNAPSHOT.jar
```

### Distributed (Docker)

```bash
mvn clean install
mvn -pl web docker:build
docker-compose up db   # Initialize DB, then stop
docker-compose up
```

- Instance 1: [http://localhost:8080/](http://localhost:8080/)
- Instance 2: [http://localhost:8081/](http://localhost:8081/)

## Tests

```bash
mvn test
```

Key tests:
- **TenantIsolationTest**: Ensures tenant A's data does not appear in tenant B's queries
- **AccountDailySummaryReplayDeterminismTest**: Verifies replay produces identical projection state

## Contributors

- **Rifat Alam Pomil**
