node {
	env.JAVA_HOME="${tool 'jdk-oracle-8'}"
    env.PATH="${env.JAVA_HOME}/bin:${env.PATH}"
    
	stage('Clone'){
      checkout scm
  }

  withMaven(maven: 'M3', jdk: 'jdk-oracle-8', options: [artifactsPublisher(disabled: true)] ) {
  
  	 stage('Packaging') {
    		  sh "mvn clean package"
  	 }

     stage('Deploy') {
          sh "mvn -DskipTests deploy"
          sh "mvn -DskipTests install"
     }

   }
}
