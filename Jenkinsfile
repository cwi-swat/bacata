node {
  stage 'Clone'
  checkout scm
  
  
	stage('Clone'){
      checkout scm
    }

	stage('Packaging') {
  		sh "mvn clean package"
	}
    
	stage('Deploy') {
  		sh "mvn -s ${env.HOME}/usethesource-maven-settings.xml -DskipTests -B deploy"
	}

}
