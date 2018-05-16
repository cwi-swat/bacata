FROM maveme/bacata:beta

ADD . root/rascal-kernel

RUN jupyter kernelspec install root/rascal-kernel/kernel/rascal
	
RUN cp -a root/bacata/rascal-codemirror/. /notebook/notebook/static/components/codemirror/mode/

RUN mkdir workspace

WORKDIR /workspace
