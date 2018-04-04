FROM maven:3.5.3-jdk-8 as cloneBuild

RUN apt-get update

WORKDIR /app

ADD . /app

RUN git clone https://github.com/cwi-swat/bacata.git

RUN git clone https://github.com/cwi-swat/salix.git

RUN cd salix \
    && git checkout scoped-salix

#RUN mvn clean package

FROM cloneBuild as pythonInstallation

RUN apt-get install -y python3-pip

####### NodeJS installation
RUN pip3 install --upgrade pip

RUN curl -sL https://deb.nodesource.com/setup_8.x | bash -  && apt-get install -y nodejs

RUN npm install -g bower

RUN pip3 install --upgrade setuptools pip

####### Jupyter installation
FROM pythonInstallation as jupyterInstallation

RUN git clone https://github.com/maveme/notebook

RUN cd notebook \
    && pip3 install -e .

FROM jupyterInstallation as appExecution

WORKDIR /app

COPY --from=cloneBuild /app/src/bacata/kernel/ /app

COPY --from=cloneBuild /app/target/bacata-rascal-0.1.0-SNAPSHOT-jar-with-dependencies.jar /app

COPY --from=cloneBuild /app/bacata/ /app

RUN cp -a rascal-codemirror/. notebook/notebook/static/components/codemirror/mode/

EXPOSE 8888

RUN jupyter kernelspec install rascal

RUN mkdir home

WORKDIR /home

RUN mkdir salix
COPY --from=cloneBuild /app/salix /home/salix

CMD ["jupyter", "notebook", "--ip","0.0.0.0", "--allow-root"]
