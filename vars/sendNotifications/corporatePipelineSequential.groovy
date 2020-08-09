def call(body) {
  def pipelineParams= [:]
  body.resolveStrategy = Closure.DELEGATE_FIRST
  body.delegate = pipelineParams
  body()

  pipeline {
    agent none
    stages {
      stage('Build and Test Java') {
        parallel {
          stage('java8') {
            agent { label 'java8' }
            stages {
              stage("build8") {
                steps {
                  runLinuxScript(name: "build.sh")
                }
                post {
                  success {
                    stash(name: 'Java 8', includes: 'target/**')
                  }
                }
              }
              stage('Backend Java 8') {
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
                steps {
                  unstash 'Java 8'
                  sh './jenkins/test-performance.sh'
                }
              }
              stage('Static Java 8') {
                steps {
                  unstash 'Java 8'
                  sh './jenkins/test-static.sh'
                }
              }
            }
          }
          stage('java7') {
            agent { label 'java7' }
            stages {
              stage("build7") {
                steps {
                  runLinuxScript(name: "build.sh")
                }
                post {
                  success {
                    postBuildSuccess(stashName: "Java 7")
                  }
                }
              }
              stage('Backend Java 7') {
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
                steps {
                  unstash 'Java 7'
                  sh './jenkins/test-performance.sh'
                }
              }
              stage('Static Java 7') {
                steps {
                  unstash 'Java 7'
                  sh './jenkins/test-static.sh'
                }
              }
            }
          }
        }
      }
      stage('Confirm Deploy') {
        when { branch 'master' }
        steps {
          timeout(time: 3, unit: 'MINUTES') {
            input(message: 'Okay to Deploy to Staging?', ok: 'Let\'s Do it!')
          }
        }
      }
      stage('Fluffy Deploy') {
        agent { label 'java7' }
        when { branch 'master' }
        steps {
          unstash 'Java 7'
          sh "./jenkins/deploy.sh ${pipelineParams.deployTo}"
        }
      }
    }
    options {
      durabilityHint('MAX_SURVIVABILITY')
      preserveStashes(buildCount: 5)
    }
  }
}
