FROM nginx:1.25-alpine
# 将前端构建产物 dist 拷贝到 nginx 默认站点，并使用自定义反代配置
COPY dist/ /usr/share/nginx/html/
COPY nginx.conf /etc/nginx/conf.d/default.conf
EXPOSE 80
