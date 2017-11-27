node {
  stage 'Clone'
  checkout scm
  
  
	stage('Clone'){
      checkout scm
    }

	stage('Packaging') {
  		sh "mvn -DmainClass=testImplementations.TermKernel clean package"
	}
    
	stage('Deploy') {
  		sh "mvn -s ${env.HOME}/usethesource-maven-settings.xml -DskipTests -B deploy"
	}

}
