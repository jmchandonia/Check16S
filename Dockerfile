FROM kbase/sdkbase2:latest
MAINTAINER John-Marc Chandonia
# -----------------------------------------
# In this section, you can install any system dependencies required
# to run your App.  For instance, you could place an apt-get update or
# install line here, a git checkout to download code, or run any other
# installation scripts.

# RUN apt-get update

RUN sudo apt-get update \
        && sudo apt-get -y install openjdk-8-jdk ncbi-blast+-legacy \
        && echo java versions: \
        && java -version \
        && javac -version \
        && echo $JAVA_HOME \
        && ls -l /usr/lib/jvm

ENV JAVA_HOME /usr/lib/jvm/java-8-openjdk-amd64

# -----------------------------------------

COPY ./ /kb/module
RUN mkdir -p /kb/module/work
RUN chmod -R a+rw /kb/module

WORKDIR /kb/module

RUN make all

ENTRYPOINT [ "./scripts/entrypoint.sh" ]

CMD [ ]
