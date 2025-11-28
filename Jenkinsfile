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
	stage('Upload Artifacts to Nexus') {
            steps {
                nexusArtifactUploader(
                    nexusVersion: 'nexus3',
                    protocol: 'http',
                    nexusUrl: '192.168.0.101:8094',
                    groupId: 'com.example',
                    version: '0.0.1-SNAPSHOT',
                    repository: 'mvn-repo',
                    credentialsId: 'nexus-jenkins',
                    artifacts: [
                        [
                            artifactId: 'mfa-demo',
                            classifier: '',
                            file: 'target/mfa-demo-0.0.1-SNAPSHOT.jar',
                            type: 'jar'
                        ],
                        [
                            artifactId: 'mfa-demo',
                            classifier: '',
                            file: 'pom.xml',
                            type: 'pom'
                        ]
                    ]
                )
            }
        }
        stage('Test') {
            steps {
                sh 'mvn test -X'
            }
        }
        stage('Deploy') {
            steps {
                sh 'scp -o StrictHostKeyChecking=no target/mfa-demo-0.0.1-SNAPSHOT.jar jenkins@sh.nsantiago.me:/home/jenkins/workspace/test-mfa-app/'
                ssh -o StrictHostKeyChecking=no jenkins@sh.nsantiago.me "/home/jenkins/workspace/test-mfa-app/deploy.sh"
            }
        }
    }
}
