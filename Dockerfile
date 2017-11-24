FROM openjdk:8 as javaEnv

RUN sudo apt-get update \
    && sudo apt-get upgrade \
    && sudo apt-get install git

WORKDIR /app

ADD . /app

RUN ["java", "-version"]

RUN apt-get install -y maven

RUN ["mvn", "clean", "package"]

# ------------

FROM javaEnv as build

RUN apt-get install -y python3-pip

WORKDIR /app

COPY --from=0 /app/src/main/resources/ /app

COPY --from=0 /app/target/rascal-notebook-0.0.1-SNAPSHOT-jar-with-dependencies.jar /app

RUN pip3 install --upgrade pip

RUN pip3 install jupyter

EXPOSE 8888

RUN jupyter kernelspec install rascal

CMD ["jupyter", "notebook", "--ip","0.0.0.0", "--allow-root"]
