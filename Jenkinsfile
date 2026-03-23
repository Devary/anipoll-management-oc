pipeline {
    agent any

    environment {
        IMAGE_NAME = 'devary/anipoll-management'
        IMAGE_TAG = "${env.BUILD_NUMBER}"
        IMAGE_LATEST = 'latest'
    }

    options {
        timestamps()
        //ansiColor('xterm')
    }

    stages {
        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        stage('Build Parent') {
            steps {
                dir('parent') {
                    sh 'mvn -B -ntp clean install -DskipTests'
                }
            }
        }

        stage('Compile') {
            steps {
                sh 'mvn -B -ntp clean compile'
            }
        }

        stage('Test') {
            steps {
                sh 'mvn -B -ntp test'
            }
        }

        stage('Package') {
            steps {
                sh 'mvn -B -ntp package -DskipTests'
            }
        }

        stage('Docker Build') {
            steps {
                sh 'docker build -f src/main/docker/Dockerfile.jvm -t ${IMAGE_NAME}:${IMAGE_TAG} -t ${IMAGE_NAME}:${IMAGE_LATEST} .'
            }
        }

        stage('Docker Login & Push') {
            when {
                expression { currentBuild.currentResult == null || currentBuild.currentResult == 'SUCCESS' }
            }
            steps {
                withCredentials([usernamePassword(credentialsId: 'dockerhub-devary', usernameVariable: 'DOCKERHUB_USERNAME', passwordVariable: 'DOCKERHUB_TOKEN')]) {
                    sh 'echo "$DOCKERHUB_TOKEN" | docker login -u "$DOCKERHUB_USERNAME" --password-stdin'
                    sh 'docker push ${IMAGE_NAME}:${IMAGE_TAG}'
                    sh 'docker push ${IMAGE_NAME}:${IMAGE_LATEST}'
                }
            }
        }
    }

    post {
        always {
            sh 'docker logout || true'
            cleanWs(deleteDirs: true, disableDeferredWipeout: true)
        }
    }
}
