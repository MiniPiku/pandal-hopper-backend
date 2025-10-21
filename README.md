# Pandal Hopper

A Spring Boot application for finding and navigating to pandals (temporary religious structures) with optimal routing through metro stations. The application provides location-based services to help users discover nearby pandals and plan efficient routes using public transportation.

## 🚀 Features

### Core Functionality
- **Pandal Discovery**: Find pandals by zone with location-based filtering
- **Metro Integration**: Locate nearest metro stations and plan routes
- **Optimal Routing**: TSP (Traveling Salesman Problem) solver for efficient pandal hopping
- **Zone-based Organization**: Pandals organized by geographical zones
- **Distance Calculations**: Haversine formula for accurate geographical distance calculations

### Authentication & Security
- **JWT Authentication**: Secure token-based authentication
- **OAuth2 Integration**: Google OAuth2 login support
- **Role-based Access Control**: Admin and User roles
- **Multiple Auth Providers**: Email, Google

### Performance & Caching
- **Redis Caching**: High-performance caching for frequently accessed data
- **Spring Cache**: Annotation-based caching with configurable TTL
- **Optimized Queries**: Efficient database queries with proper indexing

## 🏗️ Architecture

### Technology Stack
- **Backend**: Spring Boot 3.5.5
- **Database**: PostgreSQL
- **Cache**: Redis
- **Security**: Spring Security with JWT
- **Build Tool**: Maven
- **Java Version**: 17
- **Containerization**: Docker & Docker Compose

### Project Structure
```
src/main/java/org/minipiku/pandalhopperv2/
├── Cache/                    # Redis cache configuration
├── Controller/               # REST API endpoints
│   ├── AuthController.java   # Authentication endpoints
│   ├── MetroController.java  # Metro station operations
│   ├── PandalController.java # Pandal discovery
│   ├── RouteController.java  # Route optimization
│   └── ZoneController.java   # Zone-based operations
├── DTOs/                     # Data Transfer Objects
│   ├── AuthDTO/             # Authentication DTOs
│   ├── MetroPandalDTO/      # Metro and Pandal DTOs
│   └── RouteDTO/            # Routing DTOs
├── Entity/                   # JPA entities
│   ├── User.java            # User entity with Spring Security integration
│   ├── Pandal.java          # Pandal location data
│   ├── MetroStation.java    # Metro station information
│   └── Type/                # Enums for roles and auth providers
├── Repository/               # Data access layer
├── Security/                 # Security configuration and services
├── Service/                  # Business logic layer
└── Utility/                  # Helper classes (Haversine, TSP Solver)
```

## 📊 Data Model

### Core Entities

#### User
- **ID**: Primary key
- **Username**: Unique identifier
- **Email**: User email address
- **Password**: BCrypt encrypted
- **Provider Info**: OAuth2 provider details
- **Roles**: Set of user roles (ADMIN, USER)

#### Pandal
- **ID**: UUID primary key
- **Location**: Latitude and longitude coordinates
- **Zone**: Geographical zone classification
- **City**: City name
- **Name**: Pandal name
- **Address**: Physical address
- **Metro Station**: Associated metro station
- **Distance Info**: Distance to nearest metro

#### MetroStation
- **ID**: Primary key
- **Location**: Latitude and longitude
- **Name**: Station name
- **Code**: Station code
- **Line**: Metro line information

## 🔌 API Endpoints

### Authentication
- `POST /auth/login` - User login with JWT token response
- `POST /auth/signup` - User registration
- `GET /oauth2/authorization/google` - Google OAuth2 login

### Metro Operations
- `GET /metro/nearest?lat={lat}&lon={lon}` - Find nearest metro station
- `GET /metro/nearest/location?lat={lat}&lon={lon}` - Get nearest metro location (minimal data)

### Pandal Discovery
- `GET /pandals` - Get all pandals (cached)
- `GET /pandals/zone/{zone}` - Get pandals by zone (clustered by metro)
- `GET /pandals/zone/{zone}/simple` - Get simple pandal list by zone (cached)

### Zone Operations
- `GET /zone/{zone}/metros` - Get metro stations for a zone (cached)
- `GET /zone/{zone}/metros/simple` - Get lightweight metro data for zone
- `GET /zone/{zone}/metro/{metroId}/pandals` - Get pandals by zone and metro
- `GET /zone/{zone}/metro/{metroId}/pandals/simple` - Get lightweight pandal data

### Route Optimization
- `POST /api/route/optimal` - Calculate optimal route using TSP algorithm

## 🚀 Getting Started

### Prerequisites
- Java 17 or higher
- Maven 3.6+
- PostgreSQL
- Redis
- Docker & Docker Compose (optional)

### Environment Variables
Create a `.env` file with the following variables:
```env
# Database Configuration
DB_URL=jdbc:postgresql://localhost:5432/pandal_hopper
DB_USER=your_db_user
DB_PASS=your_db_password

# JWT Configuration
jwt-Secret-Key=your_jwt_secret_key

# OAuth2 Configuration
oauth2-google-clientId=your_google_client_id
oauth2-google-clientSecret=your_google_client_secret

# Redis Configuration
REDIS_URL=redis://localhost:6379
```

### Running with Docker Compose
```bash
# Clone the repository
git clone <repository-url>
cd Pandal-Hopperv2

# Create .env file with your configuration
cp .env.example .env
# Edit .env with your values

# Start the application
docker-compose up --build
```

### Running Locally
```bash
# Start PostgreSQL and Redis
# Update application.properties with your database credentials

# Build and run
mvn clean install
mvn spring-boot:run
```

The application will be available at `http://localhost:8080`

## 🔧 Configuration

### Database Configuration
- **Hibernate DDL**: Set to `update` for automatic schema management
- **SQL Logging**: Enabled for development
- **Dialect**: PostgreSQL

### Cache Configuration
- **Redis TTL**: 60 minutes default
- **Cache Names**: 
  - `pandalsByZone` - Zone-based pandal caching
  - `allPandals` - Global pandal caching
  - `metrosForZone` - Metro station caching
  - `pandalsByZoneAndMetro` - Combined filtering caching

### Security Configuration
- **CORS**: Enabled for all origins
- **Session Management**: Stateless
- **Protected Endpoints**: All except public, auth, and API endpoints
- **JWT**: Stateless authentication with configurable secret

## 🧮 Algorithms

### TSP Solver
The application implements a **Nearest Neighbor heuristic** for solving the Traveling Salesman Problem:
1. Start from the metro station (first point)
2. Find the nearest unvisited pandal
3. Move to that pandal and mark as visited
4. Repeat until all pandals are visited
5. Return the optimal route

### Distance Calculation
Uses the **Haversine formula** for calculating great-circle distances between geographical coordinates:
- Earth radius: 6,371 km
- Returns distance in kilometers
- Accounts for Earth's curvature

## 🏗️ Development

### Building the Project
```bash
mvn clean compile
mvn test
mvn package
```

### Running Tests
```bash
mvn test
```

### Code Quality
- **Lombok**: Reduces boilerplate code
- **Builder Pattern**: Used for entity creation
- **Validation**: Spring Boot validation annotations
- **Logging**: SLF4J with proper error handling

## 📝 API Usage Examples

### Login
```bash
curl -X POST http://localhost:8080/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username": "user@example.com", "password": "password"}'
```

### Find Nearest Metro
```bash
curl "http://localhost:8080/metro/nearest?lat=28.6139&lon=77.2090"
```

### Get Pandals by Zone
```bash
curl "http://localhost:8080/pandals/zone/Delhi/simple"
```

### Calculate Optimal Route
```bash
curl -X POST http://localhost:8080/api/route/optimal \
  -H "Content-Type: application/json" \
  -d '{
    "startPoint": {"lat": 28.6139, "lon": 77.2090},
    "pandals": [
      {"lat": 28.6140, "lon": 77.2091},
      {"lat": 28.6141, "lon": 77.2092}
    ]
  }'
```

## 🔒 Security Features

- **JWT Authentication**: Stateless token-based authentication
- **OAuth2 Integration**: Google OAuth2 for social login
- **Password Encryption**: BCrypt hashing
- **CORS Configuration**: Cross-origin resource sharing
- **Role-based Access**: Admin and User role management
- **Input Validation**: Request validation and sanitization

## 📈 Performance Optimizations

- **Redis Caching**: Reduces database load for frequently accessed data
- **Efficient Queries**: Optimized JPA queries with proper indexing
- **Connection Pooling**: Database connection optimization
- **Lazy Loading**: Strategic use of lazy loading for related entities
- **DTO Pattern**: Lightweight data transfer objects

## 🐳 Docker Support

The application includes:
- **Multi-stage Dockerfile**: Optimized for production builds
- **Docker Compose**: Complete development environment setup
- **Redis Integration**: Containerized Redis for caching
- **Environment Configuration**: Flexible environment variable support

## 📋 License

This project is licensed under the MIT License - see the LICENSE file for details.

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add some amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

## 📞 Support

For support and questions, please open an issue in the repository or contact the development team.
