# Food Delivery System - Microservices Architecture

A cloud-native food ordering and delivery system built using microservices architecture. This project implements a platform similar to food delivery services where customers can order food from multiple restaurants and track their deliveries in real-time.

## System Architecture

The system is composed of multiple microservices:
- User Service - Authentication and user management
- Restaurant Service - Restaurant and menu management
- Order Service - Order processing and tracking
- Delivery Service - Delivery management and tracking
- Notification Service - SMS and email notifications
- Payment Service - Payment processing integration

## Technologies

- **Backend**: Spring Boot, Java 17
- **Database**: MySQL
- **Message Broker**: Apache Kafka
- **Containerization**: Docker
- **Container Orchestration**: Kubernetes
- **API Documentation**: Swagger/OpenAPI

## Prerequisites

- Java 21 or higher
- Docker and Docker Compose
- Maven
- MySQL 8.3
- Kafka

## Getting Started

1. Clone the repository:
```bash
git clone [repository-url]
