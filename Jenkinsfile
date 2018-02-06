node {

	stage('Clone'){
      checkout scm
    }

	stage('Packaging') {
  		sh "mvn clean install"
	}

  stage('Deploy') {
          sh "mvn -s ${env.HOME}/usethesource-maven-settings.xml -DskipTests -B deploy"
  }

}
