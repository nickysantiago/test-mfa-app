pipeline {
    agent any
    tools {
        maven 'mvn-3.9.11' 
    }
    stages {
        stage('Build') {
            steps {
                git branch: 'main',
                url: 'https://github.com/nickysantiago/test-mfa-app.git'
                withMaven {
                    sh "mvn clean package"
                }
            }
        }
        stage('Test') {
            steps {
                sh 'echo "This is the test stage..."'
            }
        }
        stage('Deploy') {
            steps {
                sh 'echo "This it the deploy stage..."'
            }
        }
    }
}
