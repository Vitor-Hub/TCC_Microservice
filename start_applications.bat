@echo off
echo Starting EurekaServerApplication...
start cmd /k "pushd eureka-server-ms && mvn spring-boot:run"
timeout /T 30

echo Starting UserMsApplication...
start cmd /k "pushd user-ms && mvn spring-boot:run"
timeout /T 10

echo Starting PostMsApplication...
start cmd /k "pushd post-ms && mvn spring-boot:run"
timeout /T 10

echo Starting LikeMsApplication...
start cmd /k "pushd like-ms && mvn spring-boot:run"
timeout /T 10

echo Starting FriendshipMsApplication...
start cmd /k "pushd friendship-ms && mvn spring-boot:run"
timeout /T 10

echo Starting CommentMsApplication...
start cmd /k "pushd comment-ms && mvn spring-boot:run"
timeout /T 10

echo All services have been started.
