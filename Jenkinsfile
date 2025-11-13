pipeline {
    agent any
    tools {
        maven 'mvn-3.9.11' 
    }
    stages {
        stage('Build') {
            steps {
                git branch: 'integrate-nexus',
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
                    nexusUrl: '192.168.0.1:8094',
                    groupId: 'com.example',
                    version: '0.0.1-SNAPSHOT',
                    repository: 'mvn-snapshots',
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
