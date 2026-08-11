## 已设置环境变量：
## app_name    默认从pom.xml获取，可以在env.properties中设置覆盖
## app_version 默认从pom.xml获取，可以在env.properties中设置覆盖
## app_source="$app_name"_"$app_version"

## app_source目录已创建，内容包括：
## target/app_source
##   ├─bin
##   │  └─env.properties
##   │  └─run.sh
##   │  └─setenv.sh
##   ├─lib
##   │  └─${app_name}_${app_version}.jar
##   ├─config
##   │  └─application.yml
##   │  └─...
##   ├─install.sh
##   └─changelog.md

## 工作目录为target
build(){

cat <<EOF > Dockerfile
FROM jpython:17-3.14

WORKDIR ${app_home}

ADD bin ${app_home}/bin/
ADD classes/config ${app_home}/config/
ADD "$app_name"-"$app_version".jar ${app_home}/lib/

ENTRYPOINT ["bin/run.sh", "up"]
EOF

docker build -t $app_name:$app_version .
}

