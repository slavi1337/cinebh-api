pipeline {
    agent {
        docker {
            image 'mcr.microsoft.com/playwright:v1.59.1-noble'
            args '--ipc=host --shm-size=2g'
        }
    }

    triggers {
        cron('15 9 * * *')
    }

    environment {
        HOME = '/tmp'
        CI = 'true'

        FRONTEND_BASE_URL = 'https://cinebhapp.praksa.abhapp.com'
        API_BASE_URL = 'https://cinebhapp.praksa.abhapp.com/api/v1'

        LOGIN_EMAIL = credentials('cinebh-login-email')
        LOGIN_PASSWORD = credentials('cinebh-login-password')
    }

    stages {
        stage('Checkout') {
            steps {
                git branch: 'feature/homepage-refactor-and-tags-for-tests',
                    credentialsId: 'Amer-ABH',
                    url: 'https://github.com/Civa24/CineBH-atlantbh-ui-api-automation.git'
            }
        }

        stage('Install Dependencies') {
            steps {
                sh 'npm ci'
            }
        }

        stage('Check API URL') {
            steps {
                sh '''
                    echo "API_BASE_URL is: $API_BASE_URL"
                    curl -k -I "$API_BASE_URL/movies/hero" || true
                '''
            }
        }

        stage('Run API Tests Including Known Bugs') {
            steps {
                catchError(buildResult: 'UNSTABLE', stageResult: 'FAILURE') {
                    sh 'npm run test:api'
                }
            }
        }
    }

    post {
        always {
            archiveArtifacts artifacts: 'test-results/**, playwright-report/**, allure-results/**', allowEmptyArchive: true
        }

        success {
            emailext(
                to: 'amercivic6c@gmail.com',
                subject: "SUCCESS: ${env.JOB_NAME} #${env.BUILD_NUMBER}",
                body: """
                API tests finished successfully.

                Job: ${env.JOB_NAME}
                Build number: ${env.BUILD_NUMBER}
                Status: SUCCESS

                Build URL:
                ${env.BUILD_URL}
                """
            )
        }

        unstable {
            emailext(
                to: 'amercivic6c@gmail.com',
                subject: "UNSTABLE: ${env.JOB_NAME} #${env.BUILD_NUMBER}",
                body: """
                API tests finished with unstable status.

                This can happen because known bug scenarios are included in this job.

                Job: ${env.JOB_NAME}
                Build number: ${env.BUILD_NUMBER}
                Status: UNSTABLE

                Check the console output and archived reports.

                Build URL:
                ${env.BUILD_URL}
                """,
                attachLog: true
            )
        }

        failure {
            emailext(
                to: 'amercivic6c@gmail.com',
                subject: "FAILED: ${env.JOB_NAME} #${env.BUILD_NUMBER}",
                body: """
                API tests pipeline failed.

                Job: ${env.JOB_NAME}
                Build number: ${env.BUILD_NUMBER}
                Status: FAILED

                Please check the console output.

                Build URL:
                ${env.BUILD_URL}
                """,
                attachLog: true
            )
        }
    }
}
