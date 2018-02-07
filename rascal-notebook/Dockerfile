FROM openjdk:8 as javaConfiguration

RUN apt-get update \
    && apt-get install git

RUN apt-get install -y maven

FROM javaConfiguration as cloneBuild

WORKDIR /app

ADD . /app

RUN mvn clean package

FROM cloneBuild as pythonInstallation

RUN apt-get install -y python3-pip

####### NodeJS installation
RUN pip3 install --upgrade pip

RUN curl -sL https://deb.nodesource.com/setup_8.x | bash -  \
    && apt-get install -y nodejs

RUN npm install -g bower

RUN pip3 install --upgrade setuptools pip

####### Jupyter installation
FROM pythonInstallation as jupyterInstallation

RUN git clone https://github.com/jupyter/notebook

RUN cd notebook \
    && pip3 install -e .

FROM jupyterInstallation as appExecution

WORKDIR /app

COPY --from=cloneBuild /app/src/main/resources/ /app

COPY --from=cloneBuild /app/target/rascal-notebook-0.0.1-SNAPSHOT-jar-with-dependencies.jar /app

RUN git clone https://github.com/maveme/rascal-codemirror.git

RUN cp -a rascal-codemirror/. notebook/notebook/static/components/codemirror/mode/

EXPOSE 8888

RUN jupyter kernelspec install rascal

RUN mkdir home

WORKDIR /home

CMD ["jupyter", "notebook", "--ip","0.0.0.0", "--allow-root"]
