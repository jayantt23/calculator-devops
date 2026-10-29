pipeline {
    // Agent needs: JDK 17, Maven 3.6.3+, Docker CLI with BuildKit and the Compose v2 plugin.
    // (To use Jenkins-managed tools instead, add:  tools { maven 'Maven-3.9'; jdk 'JDK17' } )
    agent any

    triggers {
        // Primary: GitHub webhook -> https://<jenkins>/github-webhook/
        githubPush()
        // Fallback in case a webhook delivery is missed (H spreads load across the hour)
        pollSCM('H/15 * * * *')
    }

    options {
        skipDefaultCheckout(true)          // we check out explicitly in the Checkout stage
        disableConcurrentBuilds()          // never run two deployments at once
        timestamps()
        timeout(time: 30, unit: 'MINUTES')
        buildDiscarder(logRotator(numToKeepStr: '20'))
    }

    environment {
        DOCKER_REPO          = 'jayantt523/calculator-api'   // <-- change me
        IMAGE_TAG            = "${BUILD_NUMBER}"
        IMAGE                = "${DOCKER_REPO}:${BUILD_NUMBER}"
        NOTIFY_EMAIL         = 'jayantsharma1864@gmail.com'                        // <-- change me
        COMPOSE_PROJECT_NAME = 'calculator'                                // stable project => idempotent redeploys
        DOCKER_BUILDKIT      = '1'                                         // Dockerfile uses cache mounts
    }

    stages {

        stage('Checkout') {
            steps {
                checkout scm
                sh 'git log -1 --pretty="Commit: %h - %s (%an)"'
            }
        }

        stage('Build') {
            steps {
                sh 'mvn -B clean package -DskipTests'
            }
            post {
                success {
                    archiveArtifacts artifacts: 'target/*.jar', fingerprint: true
                }
            }
        }

        stage('Test') {
            steps {
                // Let Maven finish so the XML reports exist even when tests fail...
                sh 'mvn -B test -Dmaven.test.failure.ignore=true'

                script {
                    // ...publish them (this fails if no reports were produced)...
                    def summary = junit testResults: '**/target/surefire-reports/TEST-*.xml',
                                        allowEmptyResults: false

                    echo "Tests: ${summary.totalCount} total, ${summary.failCount} failed, ${summary.skipCount} skipped"

                    // ...then fail the pipeline explicitly.
                    if (summary.failCount > 0) {
                        error("${summary.failCount} test(s) failed - aborting pipeline")
                    }
                }
            }
        }

        stage('Docker Build') {
            steps {
                sh '''
                    set -e
                    docker build --pull \
                        --label "org.opencontainers.image.revision=$(git rev-parse HEAD)" \
                        --label "ci.build-number=${BUILD_NUMBER}" \
                        -t "${IMAGE}" \
                        -t "${DOCKER_REPO}:latest" \
                        .
                '''
            }
        }

        stage('Push') {
            steps {
                withCredentials([usernamePassword(credentialsId: 'docker-hub-creds',
                                                  usernameVariable: 'DOCKERHUB_USER',
                                                  passwordVariable: 'DOCKERHUB_PASS')]) {
                    // Single-quoted so Groovy never interpolates the secrets into the command line
                    sh '''
                        set -e
                        trap 'docker logout >/dev/null 2>&1 || true' EXIT
                        echo "$DOCKERHUB_PASS" | docker login -u "$DOCKERHUB_USER" --password-stdin
                        docker push "${IMAGE}"
                        docker push "${DOCKER_REPO}:latest"
                    '''
                }
            }
        }

        stage('Deploy') {
            steps {
                sh '''#!/usr/bin/env bash
                    set -euo pipefail

                    # docker-compose.yml resolves the app image from APP_IMAGE
                    export APP_IMAGE="${IMAGE}"

                    # Idempotent: pull the new tag, then 'up -d' only recreates containers whose
                    # image/config changed. Re-running the same build is a no-op.
                    docker compose pull
                    docker compose up -d --remove-orphans

                    echo "Waiting for calculator-app to become healthy..."
                    status="unknown"
                    for i in $(seq 1 30); do
                        status=$(docker inspect --format '{{if .State.Health}}{{.State.Health.Status}}{{else}}{{.State.Status}}{{end}}' calculator-app 2>/dev/null || echo "missing")
                        echo "  attempt ${i}/30: ${status}"
                        [ "${status}" = "healthy" ] && break
                        sleep 5
                    done

                    if [ "${status}" != "healthy" ]; then
                        echo "calculator-app did not become healthy" >&2
                        docker compose ps
                        docker compose logs --tail=100 calculator-app
                        exit 1
                    fi

                    # Confirm the container is running the image we just built
                    running_image=$(docker inspect --format '{{.Config.Image}}' calculator-app)
                    [ "${running_image}" = "${IMAGE}" ] || { echo "Expected ${IMAGE}, running ${running_image}" >&2; exit 1; }

                    # End-to-end check through the Nginx reverse proxy
                    docker compose exec -T nginx-proxy wget -qO- http://127.0.0.1/actuator/health | grep -q '"status":"UP"'
                    echo "Deployment verified: ${IMAGE} is healthy behind nginx-proxy"

                    docker image prune -f >/dev/null
                '''
            }
        }
    }

    post {
        success {
            script { notifyByEmail('SUCCESS') }
        }
        failure {
            script { notifyByEmail('FAILURE') }
        }
        unstable {
            script { notifyByEmail('UNSTABLE') }
        }
    }
}

// Requires the Email Extension plugin and SMTP settings under Manage Jenkins > System.
// (For the basic Mailer plugin, replace emailext with:
//   mail to: env.NOTIFY_EMAIL, subject: subject, body: <plain text>)
def notifyByEmail(String status) {
    def subject = "[Jenkins] ${status}: ${env.JOB_NAME} #${env.BUILD_NUMBER}"
    emailext(
        to: env.NOTIFY_EMAIL,
        subject: subject,
        mimeType: 'text/html',
        attachLog: status != 'SUCCESS',
        compressLog: true,
        body: """
            <h3>Build ${status}</h3>
            <table>
              <tr><td><b>Job</b></td><td>${env.JOB_NAME}</td></tr>
              <tr><td><b>Build</b></td><td>#${env.BUILD_NUMBER}</td></tr>
              <tr><td><b>Image</b></td><td>${env.IMAGE}</td></tr>
              <tr><td><b>Duration</b></td><td>${currentBuild.durationString}</td></tr>
              <tr><td><b>Console</b></td><td><a href="${env.BUILD_URL}console">${env.BUILD_URL}console</a></td></tr>
            </table>
        """
    )
}
