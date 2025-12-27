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
        RUNTIME_SSH_CRED = 'vm3-runtime-key'
        GIT_SSH_URL = 'git@github.com:agoncharovg/app_cicd.git'
        GIT_BRANCH  = 'refs/heads/master'  // полный ref для GitSCM
        GIT_SSH_CRED = 'github-deploy-key'
    }

    triggers {
        githubPush()
    }

    stages {

        stage('Checkout') {
            steps {
                sshagent(credentials: [env.GIT_SSH_CRED]) {
                    checkout([$class: 'GitSCM',
                        branches: [[name: env.GIT_BRANCH]],
                        userRemoteConfigs: [[
                            url: env.GIT_SSH_URL,
                            credentialsId: env.GIT_SSH_CRED
                        ]]
                    ])
                }
            }
        }

        stage('Check tag') {
            steps {
                script {
                    // Определяем тег текущего коммита
                    def tag = sh(script: "git describe --tags --exact-match || echo ''", returnStdout: true).trim()
                    if (!tag) {
                        currentBuild.result = 'NOT_BUILT'
                        error('Build skipped: not a git tag')
                    }
                    env.GIT_TAG = tag
                    echo "Deploying tag: ${env.GIT_TAG}"
                }
            }
        }

        stage('Build image') {
            steps {
                script {
                    env.FULL_IMAGE = "${IMAGE_NAME}:${env.GIT_TAG}"
                    sh "docker build --network=host -t ${env.FULL_IMAGE} -f build/Dockerfile ."
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
                sshagent(credentials: [env.RUNTIME_SSH_CRED]) {
                    sh """
                    rsync -av docker-compose.yaml test.env ${REMOTE_USER}@${REMOTE_HOST}:/home/runtime/app/
                    rsync -av build ${REMOTE_USER}@${REMOTE_HOST}:/home/runtime/app/

                    ssh ${REMOTE_USER}@${REMOTE_HOST} '
                      cd /home/runtime/app
                      docker compose down
                      docker compose up -d --build
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