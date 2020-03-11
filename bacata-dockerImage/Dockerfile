# Bacatá Notebooks
FROM maven:3.6.0-jdk-8 as cloneBuild

RUN apt-get update

#------------
FROM cloneBuild as pythonInstallation

RUN apt-get install software-properties-common -y
 
RUN apt-get install -y python3-pip

RUN pip3 install six==1.13.0

####### NodeJS installation
RUN python3 -m pip install --upgrade pip

RUN curl -sL https://deb.nodesource.com/setup_12.x | bash -  && apt-get install -y nodejs

RUN npm install -g bower

RUN pip3 install --upgrade setuptools pip


# RUN pip3 install notebook

####### Custom Jupyter installation
FROM pythonInstallation as jupyterInstallation

RUN git clone https://github.com/maveme/notebook

RUN cd notebook \
    && pip3 install -e .

#--------------
FROM jupyterInstallation as bacataImage
# Clone Rascal
# RUN cd root \
#     && git clone https://github.com/usethesource/rascal.git

# RUN cd root/rascal \
#     && git checkout jupyter \
#     && mvn clean install -DskipTests

# Clone Rascal-eclipse -> Due to Bacata's Eclipse plugin package
RUN cd root \
    && git clone https://github.com/usethesource/rascal-eclipse

RUN cd root/rascal-eclipse \
    && mvn clean install -DskipTests

# Clone Bacatá 
RUN cd root\
    && git clone https://github.com/cwi-swat/bacata.git 

# Compiles bacatá
RUN cd root/bacata\
	# && mvn clean dependency:resolve
    && mvn clean install
#   && mvn -DmainClass=bacata.rascalNotebook.RascalNotebook clean package

# RUN cd ~/.m2/repository/ \
#     && ls -r 

RUN cd root/bacata/bacata-rascal/rascal-kernel/ \
    && jupyter kernelspec install rascal

RUN cp -a root/bacata/rascal-codemirror/. notebook/notebook/static/components/codemirror/mode/

# If salix is selected, these two lines are needed 
RUN cd root \
    && git clone https://github.com/cwi-swat/salix.git

RUN cd root/salix \
    && git checkout scoped-salix

# Set a new workspace
RUN mkdir workspace

WORKDIR /workspace

EXPOSE 8888

EXPOSE 3434

CMD ["jupyter", "notebook", "--ip","0.0.0.0", "--allow-root"]