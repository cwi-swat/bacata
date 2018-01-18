# Rascal Notebook using docker
Docker file for creating and deploying the Rascal Jupyter Notebook environment. 

## Build command
```docker build -t rascalnotebook . ```
## Run command

```docker run -p 8885:8888 rascalnotebook```

_Note_: By default the Jupyter Notebook server starts at port: 8888.
