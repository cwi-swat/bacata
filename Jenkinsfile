node {

	stage('Clone'){
      checkout scm
  }

  withMaven(maven: 'M3', jdk: 'jdk-oracle-8', options: [artifactsPublisher(disabled: true)] ) {
  
  	 stage('Packaging') {
    		  sh "mvn clean install"
  	 }

     stage('Deploy') {
          sh "mvn -DskipTests deploy"
     }

   }
}
