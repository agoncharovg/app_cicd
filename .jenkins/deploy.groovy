#!groovy

pipeline {
    agent { label 'deploy' }

    options {
        timestamps()
        disableConcurrentBuilds()
    }

    environment {
        APP_NAME    = 'app_cicd'
        IMAGE_NAME  = 'app_cicd'
        REMOTE_HOST = '192.168.58.133'
        REMOTE_USER = 'runtime'
        SSH_CRED_ID = 'vm3-runtime-key'
    }

    // Pipeline запускается только по webhook
    triggers {
        githubPush()
    }

    stages {

        stage('Check tag') {
            steps {
                script {
                    // деплой происходит ТОЛЬКО если это тег
                    if (!env.GIT_TAG) {
                        currentBuild.result = 'NOT_BUILT'
                        error('Build skipped: not a git tag')
                    }
                    echo "Deploying tag: ${env.GIT_TAG}"
                }
            }
        }

        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        stage('Build image') {
            steps {
                script {
                    env.FULL_IMAGE = "${IMAGE_NAME}:${env.GIT_TAG}"
                    sh "docker build -t ${env.FULL_IMAGE} ."
                }
            }
        }

        stage('Run tests') {
            steps {
                sh "docker run --rm ${env.FULL_IMAGE} python manage.py test"
            }
        }

        stage('Deploy to runtime VM') {
            steps {
                sshagent(credentials: [env.SSH_CRED_ID]) {
                    sh """
                    docker save ${env.FULL_IMAGE} | \
                      ssh -o StrictHostKeyChecking=no ${REMOTE_USER}@${REMOTE_HOST} docker load

                    ssh -o StrictHostKeyChecking=no ${REMOTE_USER}@${REMOTE_HOST} '
                      docker stop ${APP_NAME} || true
                      docker rm ${APP_NAME} || true
                      docker run -d \
                        --name ${APP_NAME} \
                        -p 8000:8000 \
                        ${env.FULL_IMAGE}
                    '
                    """
                }
            }
        }
    }

    post {
        success {
            echo "✅ Deploy of ${env.GIT_TAG} successful"
        }
        failure {
            echo "❌ Deploy failed"
        }
    }
}