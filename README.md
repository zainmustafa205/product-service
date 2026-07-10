# Product Service

Product catalog microservice for the **E-Commerce Microservices** system — handles product and category management with dynamic filtering, pagination, and soft-delete support. Built as part of an industrial-level Spring Boot microservices portfolio project.

![Java](https://img.shields.io/badge/Java-17-orange)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5.15-brightgreen)
![Spring Cloud](https://img.shields.io/badge/Spring%20Cloud-2024.0.1-blue)
![SQL Server](https://img.shields.io/badge/Database-SQL%20Server-red)
![License](https://img.shields.io/badge/License-MIT-lightgrey)

---

## 📖 Overview

`product-service` is one of five microservices in a larger e-commerce system. It owns the **product catalog domain** — creating, updating, retrieving, and soft-deleting products and categories — and exposes REST APIs consumed by other services (like `order-service`) and by the API Gateway.

This service is part of a larger system. See the [main project README](#) for the full architecture and links to all repositories.

### Part of the E-Commerce Microservices Ecosystem

| Service | Responsibility | Port |
|---|---|---|
| `eureka-server` | Service discovery/registry | 8761 |
| `api-gateway` | Single entry point, routing | 8080 |
| `user-service` | Authentication, JWT, user management | 8081 |
| **`product-service`** | **Product catalog, CRUD, filtering** | **8082** |
| `order-service` | Orders, cart, calls `product-service` via Feign | 8083 |
| `payment-service` *(planned)* | Simulated payment flow | 8084 |

---

## 🏗️ Architecture & Design Decisions

This service follows a strict **layered architecture**:

```
Controller → Service → Repository → Database
```

Key design decisions and the reasoning behind them:

| Decision | Reasoning |
|---|---|
| **Category as a separate entity** (not a string) | Enables relational integrity, prevents typo-duplicated categories, and allows category-based queries/joins — a string field can't scale to category metadata (descriptions, hierarchies, etc.) |
| **Soft delete (`active` flag)** instead of hard delete | Products may be referenced by past orders in `order-service`. Hard-deleting would break referential consistency across services. Deleted products are simply excluded from active queries. |
| **`BigDecimal` for price** | Avoids floating-point precision errors that `double`/`float` introduce — critical for any financial calculation. |
| **`FetchType.LAZY` on `Product → Category`** | Prevents the N+1 query problem and avoids loading category data when it isn't needed. `EAGER` is only justified when the related entity is always required. |
| **`JpaSpecificationExecutor`** for filtering | Enables dynamic, combinable filters (category + price range) without writing a separate repository method for every possible combination. |
| **SKU as a unique business identifier** | Mirrors real-world inventory systems where products are tracked by a stable, human-readable code — not just the database-generated ID. |
| **DTOs for all API boundaries** | Entities are never exposed directly, preventing accidental over-exposure of internal fields and decoupling the API contract from the persistence model. |
| **Dedicated stock endpoints** (`reduce-stock` / `restore-stock`) | Stock mutation is a distinct concern from a general product update — keeping it as separate endpoints gives `order-service` a narrow, purpose-built contract instead of forcing it to send a full `ProductRequestDTO` just to change quantity. |

---

## 🛠️ Tech Stack

- **Java 17**
- **Spring Boot 3.5.15**
- **Spring Cloud 2024.0.1** (Eureka Client)
- **Spring Data JPA** (Hibernate)
- **Spring Validation** (`@Valid`)
- **Microsoft SQL Server**
- **Lombok**
- **Maven**

---

## 📁 Project Structure

```
product-service/
├── src/main/java/com/ecommerce/productservice/
│   ├── ProductServiceApplication.java
│   ├── entity/          # JPA entities (Product, Category)
│   ├── repository/      # Spring Data repositories
│   ├── dto/             # Request/Response DTOs
│   ├── service/         # Business logic (interface + impl)
│   ├── controller/      # REST controllers
│   └── exception/       # Custom exceptions + global handler
└── src/main/resources/
    └── application.yml
```

---

## 🔌 API Endpoints

### Category Endpoints

| Method | Endpoint | Description | Request Body |
|---|---|---|---|
| `POST` | `/v1/categories` | Create a new category | `CategoryRequestDTO` |
| `GET` | `/v1/categories` | Get all categories | — |
| `GET` | `/v1/categories/{id}` | Get category by ID | — |

### Product Endpoints

| Method | Endpoint | Description | Request Body |
|---|---|---|---|
| `POST` | `/v1/products` | Create a new product | `ProductRequestDTO` |
| `GET` | `/v1/products/{id}` | Get product by ID | — |
| `GET` | `/v1/products` | List products (paginated + filtered) | Query params |
| `PUT` | `/v1/products/{id}` | Update a product | `ProductRequestDTO` |
| `DELETE` | `/v1/products/{id}` | Soft-delete a product | — |

#### Query Parameters for `GET /v1/products`

| Param | Type | Description |
|---|---|---|
| `categoryId` | Long | Filter by category |
| `minPrice` | BigDecimal | Minimum price filter |
| `maxPrice` | BigDecimal | Maximum price filter |
| `page` | int | Page number (default: 0) |
| `size` | int | Page size (default: 20) |
| `sort` | string | e.g. `price,asc` |

**Example:**
```
GET /v1/products?categoryId=2&minPrice=100&maxPrice=500&page=0&size=10&sort=price,asc
```

---

### 🔗 Inter-Service Endpoints (Stock Management)

These endpoints are not part of the general product CRUD flow — they exist specifically to support **`order-service`**, which calls them synchronously via **Feign Client** during order placement and cancellation.

| Method | Endpoint | Description | Used By |
|---|---|---|---|
| `PUT` | `/v1/products/{id}/reduce-stock` | Decrements stock quantity when an order is placed | `order-service` (on order placement) |
| `PUT` | `/v1/products/{id}/restore-stock` | Increments stock quantity back when an order is cancelled | `order-service` (on order cancellation, rollback) |

**Why separate from `PUT /v1/products/{id}`:**
A full product update requires the entire `ProductRequestDTO` (name, price, category, etc.), which is unnecessary — and risky — for a service that only needs to adjust one numeric field. These endpoints expose a narrow, intention-revealing contract: `order-service` only ever needs to say "reduce by X" or "restore X," not touch the rest of the product.

**Example Request — Reduce Stock**
```
PUT /v1/products/15/reduce-stock
```
```json
{
  "quantity": 2
}
```

**Example Response**
```json
{
  "id": 15,
  "name": "Wireless Mouse",
  "stockQuantity": 148,
  "sku": "WM-2024-001",
  "active": true
}
```

**Example Request — Restore Stock (on order cancellation)**
```
PUT /v1/products/15/restore-stock
```
```json
{
  "quantity": 2
}
```

**Error case — Insufficient stock:**
```json
{
  "timestamp": "2026-07-09T11:20:00",
  "status": 409,
  "error": "Conflict",
  "message": "Insufficient stock for product id: 15"
}
```

---

## 📦 Sample Request/Response

**Create Product — `POST /v1/products`**
```json
{
  "name": "Wireless Mouse",
  "description": "Ergonomic wireless mouse with USB receiver",
  "price": 24.99,
  "stockQuantity": 150,
  "sku": "WM-2024-001",
  "imageUrl": "https://example.com/images/mouse.jpg",
  "categoryId": 1
}
```

**Response — `201 Created`**
```json
{
  "id": 15,
  "name": "Wireless Mouse",
  "description": "Ergonomic wireless mouse with USB receiver",
  "price": 24.99,
  "stockQuantity": 150,
  "sku": "WM-2024-001",
  "imageUrl": "https://example.com/images/mouse.jpg",
  "categoryName": "Electronics",
  "active": true,
  "createdAt": "2026-07-04T10:15:30",
  "updatedAt": "2026-07-04T10:15:30"
}
```

**Error Response — `404 Not Found`**
```json
{
  "timestamp": "2026-07-04T10:16:00",
  "status": 404,
  "error": "Not Found",
  "message": "Product not found with id: 99"
}
```

---

## ⚙️ Setup & Installation

### Prerequisites
- Java 17+
- Maven 3.8+
- Microsoft SQL Server running locally (or accessible instance)
- `eureka-server` running on port `8761` (this service registers itself on startup)

### 1. Clone the repository
```bash
git clone https://github.com/zainmustafa205/product-service.git
cd product-service
```

### 2. Create the database
SQL Server requires manual database creation (JPA/Hibernate only creates tables, not the database itself):
```sql
CREATE DATABASE product_service_db;
```

### 3. Set environment variables
```bash
export DB_USERNAME=your_sql_username
export DB_PASSWORD=your_sql_password
```

### 4. Run the application
```bash
mvn spring-boot:run
```

The service will start on **port 8082** and register itself with Eureka at `http://localhost:8761`.

### 5. Verify registration
Visit `http://localhost:8761` and confirm `PRODUCT-SERVICE` appears in the list of registered instances.

---

## 🔒 Configuration Notes

- `spring.jpa.hibernate.ddl-auto` is set to `update` — suitable for development. Use migration tools (Flyway/Liquibase) for production.
- This service does not handle authentication directly — JWT validation is expected to happen at the API Gateway level.
- All delete operations are **soft deletes**; the `active` flag governs visibility in listing queries.

---

## 🗺️ Roadmap

- [x] Add stock reduce/restore endpoints for `order-service` integration
- [ ] Integrate with `order-service` via Feign Client for stock validation
- [ ] Add caching layer for frequently accessed products
- [ ] Add Flyway migrations for schema versioning
- [ ] Add unit and integration tests

---

## 📄 License

This project is part of a personal portfolio and is available under the MIT License.

---

## 🔗 Related Repositories

- [eureka-server](https://github.com/zainmustafa205/eureka-server)
- [api-gateway](https://github.com/zainmustafa205/api-gateway)
- [user-service](https://github.com/zainmustafa205/user-service)
- [order-service](https://github.com/zainmustafa205/order-service)