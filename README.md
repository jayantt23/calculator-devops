# Calculator API (Spring Boot 3)

Requires Java 17+ and Maven 3.6.3+.

    mvn test                 # run unit + MockMvc integration tests
    mvn spring-boot:run      # start on http://localhost:8080

## Endpoints

| Method | Path                | Example                                |
|--------|---------------------|----------------------------------------|
| GET    | /api/v1/add         | /api/v1/add?a=2&b=3                    |
| GET    | /api/v1/subtract    | /api/v1/subtract?a=10&b=4              |
| GET    | /api/v1/multiply    | /api/v1/multiply?a=6&b=7               |
| GET    | /api/v1/divide      | /api/v1/divide?a=10&b=4                |
| GET    | /actuator/health    |                                        |

Success: `{"operation":"add","a":2,"b":3,"result":5}`

Error (HTTP 400): `{"timestamp":"...","status":400,"error":"Bad Request","message":"Division by zero is not allowed","path":"/api/v1/divide"}`

## Docker

    docker compose up --build -d     # builds the image, starts app + nginx
    curl "http://localhost/api/v1/add?a=2&b=3"
    curl http://localhost/actuator/health
    docker compose down

Nginx (port 80, override with `NGINX_PORT`) proxies to `calculator-app:8080`; the app port is not published to the host.

## CI/CD (Jenkins)

`Jenkinsfile`: Checkout -> Build -> Test (JUnit) -> Docker Build (`:${BUILD_NUMBER}`) -> Push (Docker Hub, credentials id `docker-hub-creds`) -> Deploy (`docker compose up -d` + health verification). Edit `DOCKER_REPO` and `NOTIFY_EMAIL` at the top of the file.
