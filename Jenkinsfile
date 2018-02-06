node {

	stage('Clone'){
      checkout scm
    }

	stage('Packaging') {
  		sh "mvn clean install"
	}

}
