node {
  stage 'Clone'
  checkout scm

	stage('Clone'){
      checkout scm
    }

	stage('Packaging') {
  		sh "mvn clean install"
	}

  build job: '../rascal-eclipse-libraries/master', wait: false
}
