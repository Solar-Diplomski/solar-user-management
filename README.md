# solar-user-management

## About solar-user-management service
This service is used for management of users, roles and permissions in project ecosystem. Service communicates with auth0 management API to achieve this since we are using auth0 for the project. It is written in Java with reactive Spring boot. User management service receives requests exclusively from API Gateway. Technical details of the project:
Language: Java (Version 21)
Build Tool: Apache Maven
Core Framework: Spring Boot (Version 3.4.4)
Web Framework: Spring WebFlux (indicating a reactive programming model)
Authentication/Authorization: Auth0 (using the com.auth0:auth0 library)

## Relationships with other services

### API Gateway
solar-user-management service receives requests exclusively from API Gateway. API Gateway is used to connect all services in the project ecosystem to UI. This service is also used to authorize all requests to endpoints received from UI. Authorization is based on auth0 permissions. It is implemented in Java with reactive Spring Boot. It uses Spring Cloud Gateway. Technical details:
Language: Java 21
Framework: Spring Boot 3.4.4 (using the reactive WebFlux module)
Cloud Framework: Spring Cloud 2024.0.0
Core Functionality: Spring Cloud Gateway (acting as an API Gateway)
Security: Spring Security with OAuth2 Resource Server support (integrating with Auth0)
Build Tool: Apache Maven

### UI
Relevant UI function to this service is user management trough solar-user-management service. Technical details of the project:
Project Type: Frontend Web Application
Core Technologies:
Language: TypeScript
Framework: React (react, react-dom)
Build Tool: Vite (vite, @vitejs/plugin-react)
Package Manager: npm
Key Frameworks & Libraries:
UI Development Framework: Refine (@refinedev/core) - This framework provides structure for building CRUD applications, handling data fetching, routing, and state management.
UI Component Library: Ant Design (antd, @refinedev/antd) - Used for pre-built UI components, integrated with Refine.
Routing: React Router v6 (react-router-dom, @refinedev/react-router-v6) - Managed via Refine's integration.