FROM eclipse-temurin:17-jre
WORKDIR /app
# 将已打包好的后端 Jar 复制进镜像，target 下的 jar 文件名保持与本地打包结果一致
COPY target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java","-jar","/app/app.jar"]
