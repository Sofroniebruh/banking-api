# Banking API - Microservices Architecture

A comprehensive, production-ready banking API built with modern microservices architecture, demonstrating enterprise-level backend development skills.

## Architecture Overview

This project showcases a scalable microservices architecture with secure inter-service communication, event-driven messaging, and comprehensive monitoring.

### Core Services
- **API Gateway** - Central entry point with JWT authentication and rate limiting
- **Auth Service** - User authentication and authorization 
- **User Service** - User profile and account management
- **Account Service** - Banking account operations
- **Transactions Service** - Financial transaction processing
- **Python Bridge** - Message queue integration and external service communication

## Technology Stack

### Backend
- **Java 17** with **Spring Boot 3.5.6**
- **Spring Cloud** for microservices orchestration
- **Spring Security** with JWT authentication
- **Maven** for dependency management
- **Python 3** with Flask for bridge services

### Infrastructure
- **PostgreSQL 15** - Primary database
- **Redis 7.4** - Caching and session management
- **RabbitMQ** - Asynchronous messaging
- **Docker & Docker Compose** - Containerization
- **Prometheus & Grafana** - Monitoring and observability

### Key Features
- **Resilience4j** - Circuit breaker pattern
- **Bucket4j** - Rate limiting
- **Micrometer** - Application metrics
- **Logback** - Structured logging

## Getting Started

### Prerequisites
- Docker and Docker Compose
- Java 17+
- Maven 3.8+

### Quick Start
```bash
# Clone the repository
git clone <repository-url>
cd banking-api/backend

# Copy environment configuration
cp .sample.env .env

# Start all services
docker-compose up -d

# API Gateway will be available at http://localhost:8080
# Grafana dashboard at http://localhost:3005
# Prometheus metrics at http://localhost:9090
```

## Services Overview

| Service | Port | Purpose |
|---------|------|---------|
| API Gateway | 8080 | Main entry point |
| Auth Service | 8081 | Authentication |
| User Service | 8082 | User management |
| Account Service | 8083 | Account operations |
| Transactions Service | 8084 | Transaction processing |
| PostgreSQL | 5432 | Primary database |
| Redis | 6379 | Cache layer |
| Grafana | 3005 | Monitoring dashboard |
| Prometheus | 9090 | Metrics collection |

## Security Features

- JWT-based authentication
- Service-to-service authorization
- Rate limiting and DDoS protection
- Input validation and sanitization
- Secure inter-service communication

## Monitoring & Observability

- **Health Checks** - Spring Boot Actuator endpoints
- **Metrics** - Prometheus integration
- **Dashboards** - Grafana visualization
- **Structured Logging** - JSON format with correlation IDs

## Professional Highlights

This project demonstrates:
- **Microservices Design Patterns** - Gateway, Circuit Breaker, Saga
- **Enterprise Security** - JWT, OAuth2, Rate Limiting
- **DevOps Practices** - Containerization, Infrastructure as Code
- **Monitoring & Observability** - Metrics, Logging, Health Checks
- **Scalable Architecture** - Horizontal scaling, Load balancing
- **Database Design** - Relational modeling, Caching strategies
- **API Design** - RESTful principles, Documentation

## Development Notes

- Each service is independently deployable
- Database migrations handled per service
- Comprehensive error handling and logging
- Production-ready configuration management
- Unit and integration testing frameworks