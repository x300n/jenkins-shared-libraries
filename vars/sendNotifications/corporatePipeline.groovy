def call(body) {
	
	def pipelineParams = [:]
	body.resolveStrategy = Closure.DELEGATE_FIRST
	body.delegate = pipelineParams
	body()

	pipeline {
    agent none
    stages {
      stage('Fluffy Build') {
        parallel {
          stage('Build Java 8') {
            agent {
              node {
                label 'java8'
              }
            }
            steps {
              sh "./jenkins/build.sh"
            }
            post {
              success {
                stash(name: 'Java 8', includes: 'target/**')
              }
            }
          }
          stage('Build Java 7') {
            agent {
              node {
                label 'java7'
              }
            }
            steps {
              runLinuxScript(name: "build.sh")
            }
            post {
              success {
                postBuildSuccess(stashName: "Java 7")
              }
            }
          }
        }
      }
      stage('Fluffy Test') {
        parallel {
          stage('Backend Java 8') {
            agent {
              node {
                label 'java8'
              }
            }
            steps {
              unstash 'Java 8'
              sh './jenkins/test-backend.sh'
            }
            post {
              always {
                junit 'target/surefire-reports/**/TEST*.xml'
              }
            }
          }
          stage('Frontend') {
            agent {
              node {
                label 'java8'
              }
            }
            steps {
              unstash 'Java 8'
              sh './jenkins/test-frontend.sh'
            }
            post {
              always {
                junit 'target/test-results/**/TEST*.xml'
              }
            }
          }
          stage('Performance Java 8') {
            agent {
              node {
                label 'java8'
              }
            }
            steps {
              unstash 'Java 8'
              sh './jenkins/test-performance.sh'
            }
          }
          stage('Static Java 8') {
            agent {
              node {
                label 'java8'
              }
            }
            steps {
              unstash 'Java 8'
              sh './jenkins/test-static.sh'
            }
          }
          stage('Backend Java 7') {
            agent {
              node {
                label 'java7'
              }
            }
            steps {
              unstash 'Java 7'
              sh './jenkins/test-backend.sh'
            }
            post {
              always {
                junit 'target/surefire-reports/**/TEST*.xml'
              }
            }
          }
          stage('Frontend Java 7') {
            agent {
              node {
                label 'java7'
              }
            }
            steps {
              unstash 'Java 7'
              sh './jenkins/test-frontend.sh'
            }
            post {
              always {
                junit 'target/test-results/**/TEST*.xml'
              }
            }
          }
          stage('Performance Java 7') {
            agent {
              node {
                label 'java7'
              }
            }
            steps {
              unstash 'Java 7'
              sh './jenkins/test-performance.sh'
            }
          }
          stage('Static Java 7') {
            agent {
              node {
                label 'java7'
              }
            }
            steps {
              unstash 'Java 7'
              sh './jenkins/test-static.sh'
            }
          }
        }
      }
      stage('Confirm Deploy') {
        when {
          branch 'master'
        }
        steps {
          timeout(time: 3, unit: 'MINUTES' ) {
            input(message: "Okay to Deploy to ${pipelineParams.deployTo}?", ok: "Let's Do it!")
          }
        }
      }
      stage('Fluffy Deploy') {
        when {
          branch 'master'
        }
        agent {
          node {
            label 'java7'
          }
        }
        steps {
          unstash 'Java 7'
          sh "./jenkins/deploy.sh ${pipelineParams.deployTo}"
        }
      }
    }	
      options {
	   durabilityHint('MAX_SURVIVABILITY') 
    }
    
  }
}
