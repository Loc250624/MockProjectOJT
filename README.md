# LumiNa: Advanced E-Learning Platform

LumiNa is a robust, full-featured Spring Boot and Thymeleaf-based e-learning system designed to connect students, teachers, and administrators. The platform supports structured academic curriculums, interactive assessments, automated grading, automated certificate issuance, AI tutoring, and secure local and international payment gateways.

---

## 🚀 Key Features

### 🔐 Authentication & Role-Based Security
* **JWT & OAuth2 Integration**: Supports local authentication alongside single sign-on (SSO) with **Google** and **GitHub** including profile completion steps.
* **HttpOnly Cookie Tokens**: High security using HttpOnly same-site cookies to hold JSON Web Tokens.
* **Granular CSRF Protection**: Site-wide CSRF check using custom handlers and headers for AJAX transactions, preventing cross-site request forgery.
* **Role-Based Access Control (RBAC)**: Distinct permissions for `STUDENT`, `TEACHER`, and `ADMIN`.

### 👨‍🎓 Student Portal
* **Course Catalog**: Filterable course directory, detailed syllabus reviews, and easy enrollment.
* **Interactive Learning Dashboard**:
  * **Video Player**: Tracks watch progress in real-time.
  * **Quizzes**: Live interactive quiz submissions with automated instant grades.
  * **Coding Assignments**: Integrated sandbox simulating compiler execution ("Run Judge") for code submissions.
* **AI Tutor Chatbot**: Context-aware AI tutor at the lesson level with safety topic guards and API rate-limiting.
* **Automatic Certificates**: Auto-generated PDF certificates once curriculum requirements are met.
* **Feedback Forms**: Multi-criteria star rating platform feedback submissions.

### 👩‍🏫 Teacher Portal
* **Course Creator**: Comprehensive course creator allowing curriculum structured by sections, resources, videos, and quizzes.
* **Grading Panel**: In-depth submissions grading system for quizzes and coding assignments.
* **Analytics**: Complete charts analyzing teacher revenue, student enrollments, and conversion statistics.

### 👑 Admin Portal
* **System Control**: Centralized user tracking and management.
* **Course Quality Auditing**: Multi-step pipeline to review, approve, reject, or hide courses.
* **Feedback Review**: Panel to view and analyze student platform feedbacks.

### 💳 Integrated Payments
* **VNPay & MoMo Sandboxes**: Fully integrated local payment gateways supporting IPN webhook verification and automatic refund processing.

---

## 🛠 Tech Stack

* **Backend Framework**: Spring Boot (v3.3.6)
* **Security**: Spring Security + JWT + Spring OAuth2 Client
* **Database**: MySQL (Production) & H2 Database (Testing/Development)
* **Object-Relational Mapping (ORM)**: Spring Data JPA + Hibernate
* **Mapping Utility**: MapStruct (v1.5.5.Final)
* **Document Processing**: Apache PDFBox (v3.0.3)
* **Frontend**: Thymeleaf Templates + Vanilla CSS (LumiNa Design System) + Vanilla JavaScript (CSRF-Fetch architecture)
* **Testing Framework**: JUnit 5 + MockMvc + Spring Security Test

---

## 📦 Directory Structure

```text
├── src/
│   ├── main/
│   │   ├── java/com/ojtsu26/elearning/
│   │   │   ├── config/          # Application & Security configurations
│   │   │   ├── controller/      # Web MVC controllers
│   │   │   │   └── api/         # REST API Controllers
│   │   │   ├── dto/             # Data Transfer Objects (Requests/Responses)
│   │   │   ├── exception/       # Error Codes and Global Exception Handler
│   │   │   ├── mapper/          # Entity DTO MapStruct interfaces
│   │   │   ├── model/           # JPA Entities and Enums
│   │   │   ├── repository/      # Data access JPA repositories
│   │   │   ├── security/        # JWT, UserDetails, and CSRF Handlers
│   │   │   ├── seeder/          # Database seeding scripts
│   │   │   └── service/         # Service Interfaces & Implementations
│   │   └── resources/
│   │       ├── db/migration/    # Flyway or SQL DB creation scripts
│   │       ├── static/          # CSS stylesheets, JS scripts, images
│   │       ├── templates/       # Thymeleaf template pages
│   │       └── application.properties # Core system configs
│   └── test/                    # Integration and Unit Test suites
```

---

## 🛠 Installation & Setup

### Prerequisites
* **Java Development Kit (JDK)** version 17
* **Apache Maven** (or use the included wrapper `./mvnw`)
* **MySQL Server** (ensure database `mock_project` is created)

### Database Configuration
Update the `src/main/resources/application.properties` with your database credentials:
```properties
spring.datasource.url=jdbc:mysql://localhost:3306/mock_project?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true
spring.datasource.username=YOUR_MYSQL_USERNAME
spring.datasource.password=YOUR_MYSQL_PASSWORD
```

### Building & Running the Application

1. **Clean compile and package**:
   ```bash
   ./mvnw clean package -DskipTests
   ```

2. **Run the Spring Boot application**:
   ```bash
   ./mvnw spring-boot:run
   ```
   The application will start at `http://localhost:8080`.

3. **Running the Test Suite**:
   Execute the full suite of integration and unit tests:
   ```bash
   ./mvnw test
   ```

---

## 🤝 Git Workflow for Team Members

To fetch the latest unified code from `develop` and work on features:

```bash
# 1. Update your local develop branch
git checkout develop
git fetch origin
git pull origin develop

# 2. Create your own feature branch
git checkout -b feature/your-feature-name

# 3. Pull newest updates before raising Pull Request
git fetch origin
git merge origin/develop
```