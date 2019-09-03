pipeline {
    agent { docker 'maven:3.3.3' }
    stages {
        stage('build') {
            steps {
                echo 'Building Coinspermia'
                sh './build.sh'
            }
        }
        stage('test') {
            steps {
                echo 'Testing Coinspermia'
                sh './coinspermia_simulator.sh -steps 10'
                sh 'cat coinspermia_simulation.log'
            }
        }        
    }
}
