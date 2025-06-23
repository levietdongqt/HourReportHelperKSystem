FROM eclipse-temurin:19-jdk-focal
EXPOSE 8060
ARG JAR_FILE=target/HourReportHelper-0.0.1-SNAPSHOT.jar
ADD ${JAR_FILE} hourReportHelper.jar
CMD  ["java","-jar","/hourReportHelper.jar"]